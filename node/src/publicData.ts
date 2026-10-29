// 공개 데이터(로그인 없는 소스) 조회: 요청 > JSON 파싱 > 매핑 > 캐시, 업스트림 실패는 마지막 캐시 또는 빈 값 (spec/PUBLIC_DATA.md)
import { USER_AGENT } from './client.js';
import { ErrorCode, SejongAuthError } from './errors.js';
import type {
  CafeteriaMenuItem, CalendarEvent, CampusPlace, CourseType, Department, DormitoryMenuDay, DormitoryNotice, DormitoryNotices,
  ExtraFields, ExtracurricularEvent, Lecture, OrganizationCategory, SelectiveDomain, Term,
} from './publicDataModels.js';
import { NodeHttpsSession, type Session } from './transport.js';

export type * from './publicDataModels.js';

const SIS_BASE_URL = 'https://sjpt.sejong.ac.kr/sch/sch/ext/SueOpenTimeQ/';
const HOMEPAGE_BASE_URL = 'https://www.sejong.ac.kr/kor/';
const DORMITORY_BASE_URL = 'https://happydorm.sejong.ac.kr/';
const EXTRACURRICULAR_URL = 'https://do.sejong.ac.kr/ko/process/schedule/getEvents';

const SIS_JSON_HEADERS = { 'Content-Type': 'application/json; charset="UTF-8"', Accept: 'application/json' };
const FORM_HEADERS = { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8', 'X-Requested-With': 'XMLHttpRequest' };

// 결과 extra 에서 제외: 교직원 이름 등 개인 식별, 서버 내부 정보
const EXTRA_DENYLIST = new Set(['CD_ALIAS1', 'CD_ALIAS2', 'CD_ALIAS3', 'CD_ALIAS1_ENG', 'CD_ALIAS2_ENG', 'CD_ALIAS3_ENG', 'regname', 'regid', 'STACK_TRACE']);
const SIS_SUBMIT_ERROR_KEY = '_SUBMIT_ERROR_';
const DORMITORY_DAYS_PER_WEEK = 7;
const DEFAULT_CALENDAR_CATEGORY_CODE = '0001';
const DEFAULT_DORMITORY_LOCATION = 'SJ';

type JsonObject = Record<string, unknown>;
type SchoolRow = JsonObject;

export interface SejongPublicDataOptions {
  /** 요청 제한 시간 (초, 기본 10) */
  timeoutSeconds?: number;
  /** 캐시 유효 시간 (초, 기본 86400) */
  cacheTtlSeconds?: number;
  /** 캐시 없는 상태의 실패 기억 시간 (초, 기본 60) */
  failureCacheSeconds?: number;
  /** 캐시 최대 항목 수 (기본 500, 초과 시 가장 오래된 항목 제거) */
  cacheMaxEntries?: number;
  /** 인증서 검증 (기본 true) */
  sslVerification?: boolean;
  /** @internal 테스트용 세션 주입 */
  _sessionFactory?: () => Session;
  /** @internal 테스트용 시계 (밀리초) */
  _nowMilliseconds?: () => number;
}

export interface DepartmentsQuery { categoryCode: string; year: string; semesterCode: string }
export interface LecturesQuery extends DepartmentsQuery { departmentCode?: string; courseName?: string; instructorName?: string }
export interface CalendarQuery { start: string; end: string; categoryCode?: string }
export interface DormitoryNoticesQuery { page?: number; rows?: number; keyword?: string; location?: string }
export interface ExtracurricularEventsQuery { start: string; end: string; category?: string }

// isFailure: 캐시 없이 실패해 빈 값을 잠시 기억하는 항목
interface CacheEntry { value: unknown; storedAtMilliseconds: number; isFailure: boolean }

// 학교 값 > 문자열 (null 은 빈 문자열, 객체·배열은 공백 없는 JSON)
const toText = (value: unknown): string => {
  if (value === null || value === undefined) return '';
  return typeof value === 'object' ? JSON.stringify(value) : String(value);
};

// 학교 값 > 숫자 (비었거나 숫자가 아니면 0)
const toNumber = (value: unknown): number => {
  const parsed = typeof value === 'number' ? value : Number.parseFloat(toText(value));
  return Number.isFinite(parsed) ? parsed : 0;
};

// 매핑에 쓰지 않은 필드를 원문 문자열로 (빈 값과 제외 목록 필드는 뺌)
function extraOf(row: SchoolRow, usedKeys: string[]): ExtraFields {
  const used = new Set(usedKeys);
  const extra: ExtraFields = {};
  for (const key of Object.keys(row).sort()) {
    const value = row[key];
    if (used.has(key) || EXTRA_DENYLIST.has(key) || value === null || value === undefined || value === '') continue;
    extra[key] = toText(value);
  }
  return extra;
}

const isObject = (value: unknown): value is JsonObject => typeof value === 'object' && value !== null && !Array.isArray(value);

// 행이 객체가 아니면 빈 행으로 (행은 버리지 않음)
const asObject = (value: unknown): JsonObject => (isObject(value) ? value : {});

// 컨테이너가 배열이 아니면 실패
function asRows(value: unknown): SchoolRow[] {
  if (!Array.isArray(value)) throw new Error('expected array');
  return value.map(asObject);
}

// 없거나 배열이 아니면 빈 목록 (결과 없음)
const asRowsOrEmpty = (value: unknown): SchoolRow[] => (Array.isArray(value) ? value.map(asObject) : []);

const rowsOf = (body: JsonObject, key: string): SchoolRow[] => asRows(body[key]);

const toCount = (value: unknown): number => Number.parseInt(toText(value), 10) || 0;

const DEFAULT_FAILURE_CACHE_SECONDS = 60;
const DEFAULT_CACHE_MAX_ENTRIES = 500;

const requestBodyOfDataset = (searchFields: JsonObject) => JSON.stringify({ dm_search: searchFields });

/** 세종대 공개 데이터 12종 조회 (캐시, 같은 요청 합치기, 실패 시 마지막 캐시 또는 빈 값) */
export class SejongPublicData {
  readonly timeoutSeconds: number;
  readonly cacheTtlSeconds: number;
  readonly failureCacheSeconds: number;
  readonly cacheMaxEntries: number;
  readonly sslVerification: boolean;
  private readonly sessionFactory: () => Session;
  private readonly nowMilliseconds: () => number;
  private session?: Session;
  private readonly cacheByKey = new Map<string, CacheEntry>();
  private readonly pendingByKey = new Map<string, Promise<unknown>>();

  constructor({
    timeoutSeconds = 10, cacheTtlSeconds = 86400, failureCacheSeconds = DEFAULT_FAILURE_CACHE_SECONDS, cacheMaxEntries = DEFAULT_CACHE_MAX_ENTRIES,
    sslVerification = true, _sessionFactory, _nowMilliseconds,
  }: SejongPublicDataOptions = {}) {
    this.timeoutSeconds = timeoutSeconds;
    this.cacheTtlSeconds = cacheTtlSeconds;
    this.failureCacheSeconds = failureCacheSeconds;
    this.cacheMaxEntries = cacheMaxEntries;
    this.sslVerification = sslVerification;
    this.sessionFactory = _sessionFactory ?? (() => new NodeHttpsSession(this.timeoutSeconds, this.sslVerification, USER_AGENT));
    this.nowMilliseconds = _nowMilliseconds ?? Date.now;
  }

  /** 기관 구분 (학부, 대학원 등) */
  organizationCategories(): Promise<OrganizationCategory[]> {
    return this.fetchSisDataset('organizationCategories', 'doOnload.do', {}, 'dl_code_orgn', (row) => ({
      code: toText(row['COM_CD']), name: toText(row['CD_NM']), nameEn: toText(row.CD_NM_ENG),
      extra: extraOf(row, ['COM_CD', 'CD_NM', 'CD_NM_ENG']),
    }));
  }

  /** 학기 목록 */
  terms(categoryCode: string): Promise<Term[]> {
    return this.fetchSisDataset('terms', 'doYearsmtlist.do', { ORGN_CLSF_CD: categoryCode }, 'dl_code_yearSmt', (row) => ({
      year: toText(row['YEAR']), semesterCode: toText(row['SMT_CD']), code: toText(row['YEAR_SMT_CD']),
      name: toText(row['YEAR_SMT_NM']), extra: extraOf(row, ['YEAR', 'SMT_CD', 'YEAR_SMT_CD', 'YEAR_SMT_NM']),
    }), [categoryCode]);
  }

  /** 학과 목록 */
  departments({ categoryCode, year, semesterCode }: DepartmentsQuery): Promise<Department[]> {
    const usedKeys = ['DEPT_CD', 'DEPT_LEVEL_CD', 'DEPT_L_CD', 'DEPT_L_PRINT', 'DEPT_M_CD', 'DEPT_M_PRINT', 'DEPT_S_CD', 'DEPT_S_PRINT', 'DEPT_NM_ENG'];
    return this.fetchSisDataset('departments', 'doLesndeptgroup.do', termSearchFields(categoryCode, year, semesterCode), 'dl_code_dept', (row) => ({
      code: toText(row['DEPT_CD']), level: toText(row['DEPT_LEVEL_CD']),
      collegeCode: toText(row.DEPT_L_CD), collegeName: toText(row.DEPT_L_PRINT),
      unitCode: toText(row.DEPT_M_CD), unitName: toText(row.DEPT_M_PRINT),
      majorCode: toText(row.DEPT_S_CD), majorName: toText(row.DEPT_S_PRINT), nameEn: toText(row.DEPT_NM_ENG),
      extra: extraOf(row, usedKeys),
    }), [categoryCode, year, semesterCode]);
  }

  /** 이수 구분 (공필, 전필 등) */
  courseTypes(): Promise<CourseType[]> {
    return this.fetchSisDataset('courseTypes', 'doOnload.do', {}, 'dl_code_curiType', (row) => ({
      code: toText(row['CURI_TYPE_CD']), name: toText(row['CURI_TYPE_CD_NM']), categoryCode: toText(row.ORGN_CLSF_CD),
      extra: extraOf(row, ['CURI_TYPE_CD', 'CURI_TYPE_CD_NM', 'ORGN_CLSF_CD']),
    }));
  }

  /** 선택 교양 영역 */
  selectiveDomains(): Promise<SelectiveDomain[]> {
    return this.fetchSisDataset('selectiveDomains', 'doOnload.do', {}, 'dl_code_sltDomain', (row) => ({
      code: toText(row['COM_CD']), name: toText(row['CD_NM']), nameEn: toText(row.CD_NM_ENG),
      extra: extraOf(row, ['COM_CD', 'CD_NM', 'CD_NM_ENG']),
    }));
  }

  /** 개설 강좌 (학과 코드, 과목명, 교수명 중 하나 이상 필요) */
  lectures(query: LecturesQuery): Promise<Lecture[]> {
    const { categoryCode, year, semesterCode, departmentCode = '', courseName = '', instructorName = '' } = query;
    // 전체 조회로 학교 서버에 부하를 주지 않도록 필터 필수
    if (!departmentCode.trim() && !courseName.trim() && !instructorName.trim()) {
      throw new SejongAuthError(ErrorCode.INVALID_INPUT, 'Lecture search needs a department code, course name or instructor name.');
    }
    const usedKeys = ['CURI_NO', 'CLASS', 'CURI_NM', 'CURI_TYPE_CD', 'CURI_TYPE_CD_NM', 'SLT_DOMAIN_CD', 'SLT_DOMAIN_CD_NM', 'CDT', 'TM_NUM', 'TOT_TM_NUM',
      'LESN_TIME', 'LESN_ROOM', 'LESN_EMP', 'STUDENT_YEAR', 'CURI_LANG_CD', 'CURI_LANG_NM', 'DEPT_CD', 'SCH_DEPT_ALIAS', 'MANAGE_DEPT_NM', 'REMARK', 'YEAR', 'SMT_CD'];
    const searchFields = { ...termSearchFields(categoryCode, year, semesterCode), DEPT_CD: departmentCode, CURI_NM: courseName, EMP_NM: instructorName, LANG_CHK: 'KOR' };
    return this.fetchSisDataset('lectures', 'doList.do', searchFields, 'dl_main', (row) => ({
      courseNumber: toText(row['CURI_NO']), section: toText(row['CLASS']), name: toText(row['CURI_NM']),
      courseTypeCode: toText(row.CURI_TYPE_CD), courseTypeName: toText(row.CURI_TYPE_CD_NM),
      domainCode: toText(row.SLT_DOMAIN_CD), domainName: toText(row.SLT_DOMAIN_CD_NM),
      credits: toText(row.CDT), hours: toText(row.TM_NUM), totalHours: toText(row.TOT_TM_NUM), schedule: toText(row.LESN_TIME),
      room: toText(row.LESN_ROOM), instructor: toText(row.LESN_EMP), gradeLevel: toText(row.STUDENT_YEAR),
      languageCode: toText(row.CURI_LANG_CD), languageName: toText(row.CURI_LANG_NM),
      departmentCode: toText(row.DEPT_CD), departmentName: toText(row.SCH_DEPT_ALIAS), managingDepartment: toText(row.MANAGE_DEPT_NM),
      remark: toText(row.REMARK), year: toText(row.YEAR), semesterCode: toText(row.SMT_CD), extra: extraOf(row, usedKeys),
    }), [categoryCode, year, semesterCode, departmentCode, courseName, instructorName]);
  }

  /** 학사 일정 */
  calendar({ start, end, categoryCode = DEFAULT_CALENDAR_CATEGORY_CODE }: CalendarQuery): Promise<CalendarEvent[]> {
    const query = new URLSearchParams({ mode: 'getCalendarData', start, end, collDiv: categoryCode });
    const url = `${HOMEPAGE_BASE_URL}academics/academic-calendar.do?${query}`;
    const usedKeys = ['scmYear', 'scmMonth', 'collDiv', 'collDivNm', 'scmSubject', 'scmSubjectEng', 'frDt', 'toDt'];
    return this.fetchCached('calendar', [start, end, categoryCode], () => [], () => this.requestJson('GET', url), (body) =>
      rowsOf(body, 'data').map((row) => ({
        year: toText(row.scmYear), month: toText(row.scmMonth), categoryCode: toText(row['collDiv']), categoryName: toText(row.collDivNm),
        title: toText(row.scmSubject), titleEn: toText(row.scmSubjectEng), startDate: toText(row['frDt']), endDate: toText(row['toDt']),
        extra: extraOf(row, usedKeys),
      })));
  }

  /** 기숙사 주간 식단 (7일) */
  dormitoryMenu(location = DEFAULT_DORMITORY_LOCATION): Promise<DormitoryMenuDay[]> {
    const form = new URLSearchParams({ locgbn: location });
    return this.fetchCached('dormitoryMenu', [location], () => [], () => this.requestJson('POST', `${DORMITORY_BASE_URL}food/indexFoodList.do`, form), (body) => {
      const row = rowsOf(body, 'root')[0];
      if (!row) throw new Error('empty root');
      return Array.from({ length: DORMITORY_DAYS_PER_WEEK }, (_, index) => {
        const dayNumber = index + 1;
        return {
          date: toText(row[`fo_date${dayNumber}`]), breakfast: toText(row[`fo_menu_mor${dayNumber}`]), lunch: toText(row[`fo_menu_lun${dayNumber}`]),
          dinner: toText(row[`fo_menu_eve${dayNumber}`]), alternative: toText(row[`fo_sub_menu${dayNumber}`]),
        };
      });
    });
  }

  /** 기숙사 공지 (작성자 정보는 반환하지 않음) */
  dormitoryNotices({ page = 1, rows = 10, keyword = '', location = DEFAULT_DORMITORY_LOCATION }: DormitoryNoticesQuery = {}): Promise<DormitoryNotices> {
    const form = new URLSearchParams({ cPage: String(page), rows: String(rows), bbs_locgbn: location, bbs_id: 'notice', sType: 'subject', sWord: keyword });
    const toNotice = (row: SchoolRow): DormitoryNotice => ({
      id: toText(row['seq']), title: toText(row.subject), date: toText(row.regdate), views: toCount(row.visitcnt), fileName: toText(row.file_name1),
    });
    return this.fetchCached('dormitoryNotices', [page, rows, keyword, location], () => ({ total: 0, pinned: [], items: [] }),
      () => this.requestJson('POST', `${DORMITORY_BASE_URL}bbs/getBbsList.do`, form), (body) => {
        // root 만 필수, 나머지가 없으면 결과 없음
        const block = rowsOf(body, 'root')[0] ?? {};
        return {
          total: toCount(asRowsOrEmpty(block.totalCount)[0]?.cnt),
          pinned: asRowsOrEmpty(block.topList).map(toNotice),
          items: asRowsOrEmpty(block.list).map(toNotice),
        };
      });
  }

  /** 학생식당 메뉴 */
  cafeteriaMenu(placeId: string | number): Promise<CafeteriaMenuItem[]> {
    const url = `${HOMEPAGE_BASE_URL}unilife/cafeteria-info.do?${new URLSearchParams({ mode: 'getMenuList', placeId: String(placeId) })}`;
    return this.fetchCached('cafeteriaMenu', [String(placeId)], () => [], () => this.requestJson('GET', url), (body) =>
      rowsOf(body, 'items').map((row) => ({
        buildingName: toText(row.buildingName), location: toText(row.location), placeId: toText(row.placeId), placeName: toText(row.placeName),
        date: toText(row.menuDate), day: toText(row.day), mealTypeId: toText(row.mealTypeId), mealName: toText(row.mealName),
        menuName: toText(row.menuName), price: toText(row.price),
      })));
  }

  /** 캠퍼스 시설 */
  campusPlaces(placeType = ''): Promise<CampusPlace[]> {
    const url = `${HOMEPAGE_BASE_URL}intro/campus-map.do?${new URLSearchParams({ mode: 'getPlaceUserList', placeType })}`;
    const usedKeys = ['id', 'placeName', 'placeNameEn', 'placeType', 'welfareCategory', 'latitude', 'longitude', 'placeInfo', 'telNum', 'description'];
    return this.fetchCached('campusPlaces', [placeType], () => [], () => this.requestJson('GET', url), (body) =>
      rowsOf(body, 'items').map((row) => {
        return {
          id: toText(row['id']), name: toText(row.placeName), nameEn: toText(row.placeNameEn), placeType: toText(row.placeType),
          category: toText(row.welfareCategory), latitude: toNumber(row.latitude), longitude: toNumber(row.longitude), info: toText(row.placeInfo), phone: toText(row.telNum),
          description: toText(row.description), extra: extraOf(row, usedKeys),
        };
      }));
  }

  /** 비교과 프로그램 일정 */
  extracurricularEvents({ start, end, category = '' }: ExtracurricularEventsQuery): Promise<ExtracurricularEvent[]> {
    const yearOf = (date: string) => date.slice(0, 4);
    const form = new URLSearchParams({ cid: 'eco', category, start, end, start_year: yearOf(start), end_year: yearOf(end) });
    return this.fetchCached('extracurricularEvents', [start, end, category], () => [], () => this.requestJson('POST', EXTRACURRICULAR_URL, form), (body) =>
      rowsOf(body, 'events').map((row) => {
        const detail = asObject(row.data);
        return {
          id: toText(row['id']), title: toText(row.title), start: toText(row.start), end: toText(row.end), categoryTitle: toText(row.category_title),
          url: toText(detail.url), description: toText(detail.description), location: toText(detail.location),
        };
      }));
  }

  // SIS 데이터셋 1건 조회 > 행마다 매핑
  private fetchSisDataset<T>(
    methodName: string, endpoint: string, searchFields: JsonObject, datasetName: string, mapRow: (row: SchoolRow) => T, cacheArguments: unknown[] = [],
  ): Promise<T[]> {
    return this.fetchCached(methodName, cacheArguments, () => [] as T[],
      () => this.requestJson('POST', SIS_BASE_URL + endpoint, undefined, requestBodyOfDataset(searchFields)),
      (body) => rowsOf(body, datasetName).map(mapRow));
  }

  // 캐시 확인 > 같은 키 진행 중 요청 공유 > 요청·파싱 > 캐시 저장, 실패 시 마지막 캐시 또는 빈 값(캐시 없으면 잠시 기억)
  private fetchCached<T>(methodName: string, cacheArguments: unknown[], createEmptyValue: () => T, request: () => Promise<JsonObject>, parse: (body: JsonObject) => T): Promise<T> {
    const cacheKey = `${methodName}|${JSON.stringify(cacheArguments)}`;
    const cachedEntry = this.cacheByKey.get(cacheKey);
    if (cachedEntry) {
      const validSeconds = cachedEntry.isFailure ? this.failureCacheSeconds : this.cacheTtlSeconds;
      if (this.nowMilliseconds() - cachedEntry.storedAtMilliseconds < validSeconds * 1000) {
        return Promise.resolve(structuredClone(cachedEntry.value) as T);
      }
    }
    const pendingRequest = this.pendingByKey.get(cacheKey);
    if (pendingRequest) return pendingRequest.then((value) => structuredClone(value) as T);

    // 만료된 실패 기억은 마지막 값이 아님
    const lastValueEntry = cachedEntry && !cachedEntry.isFailure ? cachedEntry : undefined;
    let newRequest: Promise<T> | undefined;
    newRequest = (async (): Promise<T> => {
      try {
        const value = parse(await request());
        this.storeEntry(cacheKey, { value, storedAtMilliseconds: this.nowMilliseconds(), isFailure: false });
        return value;
      } catch {
        // 업스트림 실패는 내부 정보 없이 마지막 캐시(만료 포함) 또는 빈 값
        if (lastValueEntry) return lastValueEntry.value as T;
        const emptyValue = createEmptyValue();
        this.storeEntry(cacheKey, { value: emptyValue, storedAtMilliseconds: this.nowMilliseconds(), isFailure: true });
        return emptyValue;
      } finally {
        if (this.pendingByKey.get(cacheKey) === newRequest) this.pendingByKey.delete(cacheKey);
      }
    })();
    this.pendingByKey.set(cacheKey, newRequest);
    return newRequest.then((value) => structuredClone(value) as T);
  }

  // 캐시 저장 (가득 차면 가장 오래된 항목과 그 진행 중 요청 제거)
  private storeEntry(cacheKey: string, entry: CacheEntry): void {
    this.cacheByKey.delete(cacheKey);
    this.cacheByKey.set(cacheKey, entry);
    while (this.cacheByKey.size > this.cacheMaxEntries) {
      const oldestKey = this.cacheByKey.keys().next().value as string;
      this.cacheByKey.delete(oldestKey);
      this.pendingByKey.delete(oldestKey);
    }
  }

  // 요청 1건: 2xx 아님, JSON 아님, _SUBMIT_ERROR_ 는 예외 (호출부가 실패 격리)
  private async requestJson(method: string, url: string, form?: URLSearchParams, jsonBody?: string): Promise<JsonObject> {
    this.session ??= this.sessionFactory();
    const isJsonRequest = jsonBody !== undefined;
    const response = await this.session.request(method, url, {
      headers: isJsonRequest ? SIS_JSON_HEADERS : form ? FORM_HEADERS : {},
      body: isJsonRequest ? jsonBody : form?.toString(),
    });
    if (response.status < 200 || response.status >= 300) throw new Error(`status ${response.status}`);
    // happydorm 은 JSON 을 text/html 로 보내므로 항상 본문을 JSON 으로 파싱
    const parsed: unknown = JSON.parse(response.body.toString('utf8'));
    if (!isObject(parsed)) throw new Error('expected object');
    const body = parsed;
    if (SIS_SUBMIT_ERROR_KEY in body) throw new Error('school reported an error');
    return body;
  }
}

function termSearchFields(categoryCode: string, year: string, semesterCode: string): JsonObject {
  return { ORGN_CLSF_CD: categoryCode, YEAR: year, SMT_CD: semesterCode, YEAR_SMT: `${year}/${semesterCode}` };
}
