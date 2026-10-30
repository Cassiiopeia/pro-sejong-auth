// 공개 데이터(로그인 없는 소스) 조회: 요청 > JSON 파싱 > 매핑 > 캐시, 업스트림 실패는 마지막 캐시 또는 빈 값 (spec/PUBLIC_DATA.md)
import { ErrorCode, SejongAuthError } from '../common/errors.js';
import type {
  CafeteriaMenuItem, CafeteriaPlace, CalendarEvent, CampusPlace, CourseType, CurriculumCourse, Department, DormitoryMenuDay, DormitoryNotice, DormitoryNotices,
  ExtraFields, ExtracurricularEvent, Lecture, OrganizationCategory, SelectiveDomain, Term,
} from './models.js';
import { NodeHttpsSession, USER_AGENT, type Session } from '../common/transport.js';

export type * from './models.js';

// 학교 주소·요청 키·고정 값의 원본은 spec/contract.json 의 publicData (test/contract.test.mjs 가 일치 검사)
export const SIS_BASE_URL = 'https://sjpt.sejong.ac.kr/sch/sch/ext/SueOpenTimeQ/';
export const SIS_CONTENT_TYPE = 'application/json; charset="UTF-8"';
export const SIS_SEARCH_KEY = 'dm_search';
export const SIS_ONLOAD_ENDPOINT = 'doOnload.do';
export const SIS_TERM_LIST_ENDPOINT = 'doYearsmtlist.do';
export const SIS_DEPARTMENT_LIST_ENDPOINT = 'doLesndeptgroup.do';
export const SIS_LECTURE_LIST_ENDPOINT = 'doList.do';
export const JSON_ACCEPT = 'application/json';
export const FORM_CONTENT_TYPE = 'application/x-www-form-urlencoded; charset=UTF-8';
export const FORM_REQUESTED_WITH = 'XMLHttpRequest';
// 학과 교육과정 과목 1건: deptNo 는 SIS 학과 코드(DEPT_CD)와 같은 값
export const CURRICULUM_COURSE_URL = 'https://www.sejong.ac.kr/curriculum/getCurriculumOneKor.do';
export const CURRICULUM_QUERY_KEYS = ['curiNo', 'deptNo'];
export const ACADEMIC_CALENDAR_URL = 'https://www.sejong.ac.kr/kor/academics/academic-calendar.do';
export const CALENDAR_QUERY_KEYS = ['mode', 'start', 'end', 'collDiv'];
export const CALENDAR_MODE = 'getCalendarData';
export const DEFAULT_CALENDAR_CATEGORY_CODE = '0001'; // 학부 일정
// 대학원 사이트별 학사일정: {siteId} 는 graduate, gsi, mba 같은 사이트 식별자 (코드표 없이 호출자가 전달)
export const GRADUATE_CALENDAR_URL = 'https://{siteId}.sejong.ac.kr/_custom/sejong/_app/calendar-graduate/proc/getCalendarData.jsp';
export const GRADUATE_CALENDAR_QUERY_KEYS = ['year', 'month', 'siteId'];
// 사이트 식별자 허용 형태 (호스트 이름에 들어가므로 소문자·숫자·하이픈만)
export const GRADUATE_SITE_ID_PATTERN_SOURCE = '[a-z0-9][a-z0-9-]{0,30}';
const GRADUATE_SITE_ID_PATTERN = new RegExp(`^${GRADUATE_SITE_ID_PATTERN_SOURCE}$`);
export const DORMITORY_MENU_URL = 'https://happydorm.sejong.ac.kr/food/indexFoodList.do';
export const DORMITORY_MENU_FORM_KEYS = ['locgbn'];
export const DORMITORY_NOTICE_URL = 'https://happydorm.sejong.ac.kr/bbs/getBbsList.do';
export const DORMITORY_NOTICE_FORM_KEYS = ['cPage', 'rows', 'bbs_locgbn', 'bbs_id', 'sType', 'sWord'];
export const DORMITORY_NOTICE_BOARD_ID = 'notice';
export const DORMITORY_NOTICE_SEARCH_TYPE = 'subject'; // 제목 검색
export const DEFAULT_DORMITORY_LOCATION = 'SJ';
export const CAFETERIA_URL = 'https://www.sejong.ac.kr/kor/unilife/cafeteria-info.do';
export const CAFETERIA_QUERY_KEYS = ['mode', 'placeId'];
export const CAFETERIA_MENU_MODE = 'getMenuList';
export const CAMPUS_MAP_URL = 'https://www.sejong.ac.kr/kor/intro/campus-map.do';
export const CAMPUS_MAP_QUERY_KEYS = ['mode', 'placeType'];
export const CAMPUS_PLACE_LIST_MODE = 'getPlaceUserList';
export const EXTRACURRICULAR_URL = 'https://do.sejong.ac.kr/ko/process/schedule/getEvents';
export const EXTRACURRICULAR_FORM_KEYS = ['cid', 'category', 'start', 'end', 'start_year', 'end_year'];
export const EXTRACURRICULAR_CID = 'eco';
// remark 는 교과목 개요 본문이라 overview 로 받음 (CurriculumCourse 필드 순서와 별개, 매핑에 쓴 키 목록)
const CURRICULUM_COURSE_USED_KEYS = ['deptCd', 'deptNm', 'orgnClsfCd', 'year', 'curiNo', 'curiNm', 'curiTypeCd', 'curiTypeNm', 'cdt',
  'lecTmNum', 'exeTmNum', 'curiTm', 'stuYearCd', 'smtDanCd', 'smtDanNm', 'remark'];
const CALENDAR_EVENT_USED_KEYS = ['scmYear', 'scmMonth', 'collDiv', 'collDivNm', 'scmSubject', 'scmSubjectEng', 'frDt', 'toDt'];

const SIS_JSON_HEADERS = { 'Content-Type': SIS_CONTENT_TYPE, Accept: JSON_ACCEPT };
// GET 도 Accept 가 없으면 학교(교육과정)가 404 를 돌려줌
const JSON_ACCEPT_HEADERS = { Accept: JSON_ACCEPT };
const FORM_HEADERS = { 'Content-Type': FORM_CONTENT_TYPE, 'X-Requested-With': FORM_REQUESTED_WITH };

// 결과 extra 에서 제외: 교직원 이름 등 개인 식별, 서버 내부 정보
const EXTRA_DENYLIST = new Set(['CD_ALIAS1', 'CD_ALIAS2', 'CD_ALIAS3', 'CD_ALIAS1_ENG', 'CD_ALIAS2_ENG', 'CD_ALIAS3_ENG', 'regname', 'regid', 'STACK_TRACE']);
export const SIS_SUBMIT_ERROR_KEY = '_SUBMIT_ERROR_';
// 강좌 조회는 나머지 필터 키가 모두 빈 값으로 있어야 학교가 받아들임 (하나라도 빠지면 _SUBMIT_ERROR_)
export const LECTURE_EMPTY_FILTER_KEYS = ['SMT_NM', 'CURI_TYPE_CD', 'SLT_DOMAIN_CD', 'CURI_NO', 'CLASS', 'SLT_DEPT_CD', 'PLAN_REG_YN', 'CORS_SCHE_GRP_CD'];
const LECTURE_EMPTY_FILTERS = Object.fromEntries(LECTURE_EMPTY_FILTER_KEYS.map((key) => [key, '']));
// 학교에 식당 목록 API가 없어 번호 1부터 차례로 조회해 식당을 모음 (학교 화면도 번호를 스크립트에 고정해 씀, spec/PUBLIC_DATA.md)
// 식당 번호는 1부터 호출자가 정한 상한까지 차례로 조회 (기본 10, 최대 50: 한 번 갱신의 요청 수를 제한)
export const CAFETERIA_DEFAULT_MAX_PLACE_ID = 10;
export const CAFETERIA_LIMIT_MAX_PLACE_ID = 50;
// 메뉴 응답 행의 알려진 필드 (식당 extra 에는 이 밖의 필드만)
const CAFETERIA_MENU_USED_KEYS = ['placeId', 'placeName', 'buildingName', 'location', 'menuDate', 'day', 'mealTypeId', 'mealName', 'menuName', 'price'];
const DORMITORY_DAYS_PER_WEEK = 7;

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

export interface DepartmentsQuery {
  /** 기관 구분 코드 */
  categoryCode: string;
  /** 연도 4자리 (예: 2026) */
  year: string;
  /** 학기 코드 (10 1학기, 11 하계, 20 2학기, 21 동계) */
  semesterCode: string;
}
export interface LecturesQuery extends DepartmentsQuery {
  /** 학과 코드 (departments 결과의 code), 비우면 전체 */
  departmentCode?: string;
  /** 과목명 검색어 (앞뒤 공백 제거) */
  courseName?: string;
  /** 교수명 검색어 (앞뒤 공백 제거) */
  instructorName?: string;
}
export interface CurriculumCoursesQuery {
  /** 학과 코드 (departments 결과의 code) */
  departmentCode: string;
  /** 학수번호 (lectures 결과의 courseNumber) */
  courseNumber: string;
}
export interface CalendarQuery {
  /** 시작일 yyyy-MM-dd */
  start: string;
  /** 종료일 yyyy-MM-dd */
  end: string;
  /** 일정 분류 (기본 0001 학부, 0002~0007 대학원 계열) */
  categoryCode?: string;
}
export interface GraduateCalendarQuery {
  /** 대학원 사이트 이름 (graduate, gsi, mba, pub, tourgrad, edu, arts 등) */
  siteId: string;
  /** 연도 */
  year: number | string;
  /** 월 1~12 */
  month: number | string;
}
export interface DormitoryNoticesQuery {
  /** 쪽 번호 (1부터, 기본 1) */
  page?: number;
  /** 쪽당 건수 (기본 10) */
  rows?: number;
  /** 제목 검색어 */
  keyword?: string;
  /** 기숙사 위치 코드 (기본 SJ) */
  location?: string;
}
export interface CafeteriaPlacesQuery {
  /** 조회할 식당 번호 상한 (1~50, 기본 10) */
  maxPlaceId?: number;
}
export interface ExtracurricularEventsQuery {
  /** 시작일 yyyy-MM-dd */
  start: string;
  /** 종료일 yyyy-MM-dd */
  end: string;
  /** 비교과 분류 (학교 값 그대로), 비우면 전체 */
  category?: string;
}

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

const requestBodyOfDataset = (searchFields: JsonObject) => JSON.stringify({ [SIS_SEARCH_KEY]: searchFields });

// 요청 키 목록 + 값 > 보낼 순서 그대로의 키-값 쌍
const pairsOf = (keys: string[], ...values: string[]): URLSearchParams => new URLSearchParams(keys.map((key, index): [string, string] => [key, values[index]]));

// 학사일정 행 > 일정 (홈페이지와 대학원 사이트 공통)
const toCalendarEvent = (row: SchoolRow): CalendarEvent => ({
  year: toText(row.scmYear), month: toText(row.scmMonth), categoryCode: toText(row['collDiv']), categoryName: toText(row.collDivNm),
  title: toText(row.scmSubject), titleEn: toText(row.scmSubjectEng), startDate: toText(row['frDt']), endDate: toText(row['toDt']),
  extra: extraOf(row, CALENDAR_EVENT_USED_KEYS),
});

/**
 * 공개 데이터 클라이언트(로그인 없음): 학사 코드, 강의, 학과 교육과정, 학사일정, 기숙사, 학식, 캠퍼스, 비교과 15개 메서드
 * 요청 > JSON 검증 > 모델 변환 > 메모리 캐시, 같은 인자의 동시 호출은 요청 1건 공유
 * 값 규칙: 학교 원문 문자열, 없으면 빈 문자열 (null 없음), 쓰지 않은 학교 필드는 extra
 * 실패 격리: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 값, 잘못된 입력만 INVALID_INPUT 예외 (spec/PUBLIC_DATA.md)
 */
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

  /**
   * 기관 구분 목록 (학부 20, 대학원 30 등)
   * 실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
   * 캐시: 인자별 cacheTtlSeconds (기본 86400), 요청 1건
   * @returns OrganizationCategory 목록
   */
  organizationCategories(): Promise<OrganizationCategory[]> {
    return this.fetchSisDataset('organizationCategories', SIS_ONLOAD_ENDPOINT, {}, 'dl_code_orgn', (row) => ({
      code: toText(row['COM_CD']), name: toText(row['CD_NM']), nameEn: toText(row.CD_NM_ENG),
      extra: extraOf(row, ['COM_CD', 'CD_NM', 'CD_NM_ENG']),
    }));
  }

  /**
   * 기관 구분의 학기 목록: 기관 구분 코드 > Term 목록
   * 실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
   * 캐시: 인자별 cacheTtlSeconds (기본 86400), 요청 1건
   * @param categoryCode 기관 구분 코드 (organizationCategories 결과의 code, 예: 20 학부)
   * @returns Term 목록
   */
  terms(categoryCode: string): Promise<Term[]> {
    return this.fetchSisDataset('terms', SIS_TERM_LIST_ENDPOINT, { ORGN_CLSF_CD: categoryCode }, 'dl_code_yearSmt', (row) => ({
      year: toText(row['YEAR']), semesterCode: toText(row['SMT_CD']), code: toText(row['YEAR_SMT_CD']),
      name: toText(row['YEAR_SMT_NM']), extra: extraOf(row, ['YEAR', 'SMT_CD', 'YEAR_SMT_CD', 'YEAR_SMT_NM']),
    }), [categoryCode]);
  }

  /**
   * 학기의 학과 목록: 기관 구분·연도·학기 코드 > Department 목록
   * 실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
   * 캐시: 인자별 cacheTtlSeconds (기본 86400), 요청 1건
   * @param query DepartmentsQuery (categoryCode, year, semesterCode)
   * @returns Department 목록
   */
  departments({ categoryCode, year, semesterCode }: DepartmentsQuery): Promise<Department[]> {
    const usedKeys = ['DEPT_CD', 'DEPT_LEVEL_CD', 'DEPT_L_CD', 'DEPT_L_PRINT', 'DEPT_M_CD', 'DEPT_M_PRINT', 'DEPT_S_CD', 'DEPT_S_PRINT', 'DEPT_NM_ENG'];
    return this.fetchSisDataset('departments', SIS_DEPARTMENT_LIST_ENDPOINT, termSearchFields(categoryCode, year, semesterCode), 'dl_code_dept', (row) => ({
      code: toText(row['DEPT_CD']), level: toText(row['DEPT_LEVEL_CD']),
      collegeCode: toText(row.DEPT_L_CD), collegeName: toText(row.DEPT_L_PRINT),
      unitCode: toText(row.DEPT_M_CD), unitName: toText(row.DEPT_M_PRINT),
      majorCode: toText(row.DEPT_S_CD), majorName: toText(row.DEPT_S_PRINT), nameEn: toText(row.DEPT_NM_ENG),
      extra: extraOf(row, usedKeys),
    }), [categoryCode, year, semesterCode]);
  }

  /**
   * 이수 구분 목록 (공필, 전필 등)
   * 실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
   * 캐시: 인자별 cacheTtlSeconds (기본 86400), 요청 1건
   * @returns CourseType 목록
   */
  courseTypes(): Promise<CourseType[]> {
    return this.fetchSisDataset('courseTypes', SIS_ONLOAD_ENDPOINT, {}, 'dl_code_curiType', (row) => ({
      code: toText(row['CURI_TYPE_CD']), name: toText(row['CURI_TYPE_CD_NM']), categoryCode: toText(row.ORGN_CLSF_CD),
      extra: extraOf(row, ['CURI_TYPE_CD', 'CURI_TYPE_CD_NM', 'ORGN_CLSF_CD']),
    }));
  }

  /**
   * 선택 교양 영역 목록
   * 실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
   * 캐시: 인자별 cacheTtlSeconds (기본 86400), 요청 1건
   * @returns SelectiveDomain 목록
   */
  selectiveDomains(): Promise<SelectiveDomain[]> {
    return this.fetchSisDataset('selectiveDomains', SIS_ONLOAD_ENDPOINT, {}, 'dl_code_sltDomain', (row) => ({
      code: toText(row['COM_CD']), name: toText(row['CD_NM']), nameEn: toText(row.CD_NM_ENG),
      extra: extraOf(row, ['COM_CD', 'CD_NM', 'CD_NM_ENG']),
    }));
  }

  /**
   * 개설 강좌 검색: 학과 코드·과목명·교수명은 선택, 모두 비우면 그 학기 전체 (학부 약 2,800행·2.4MB)
   * 실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
   * 캐시: 인자별 cacheTtlSeconds (기본 86400), 요청 1건
   * @param query LecturesQuery (categoryCode, year, semesterCode 필수, departmentCode, courseName, instructorName 선택)
   * @returns Lecture 목록
   */
  lectures(query: LecturesQuery): Promise<Lecture[]> {
    const { categoryCode, year, semesterCode, departmentCode = '', courseName = '', instructorName = '' } = query;
    const usedKeys = ['CURI_NO', 'CLASS', 'CURI_NM', 'CURI_TYPE_CD', 'CURI_TYPE_CD_NM', 'SLT_DOMAIN_CD', 'SLT_DOMAIN_CD_NM', 'CDT', 'TM_NUM', 'TOT_TM_NUM',
      'LESN_TIME', 'LESN_ROOM', 'LESN_EMP', 'STUDENT_YEAR', 'CURI_LANG_CD', 'CURI_LANG_NM', 'DEPT_CD', 'SCH_DEPT_ALIAS', 'MANAGE_DEPT_NM', 'REMARK', 'YEAR', 'SMT_CD'];
    const searchFields = { ...LECTURE_EMPTY_FILTERS, ...termSearchFields(categoryCode, year, semesterCode), DEPT_CD: departmentCode.trim(), CURI_NM: courseName.trim(), EMP_NM: instructorName.trim(), LANG_CHK: 'KOR' };
    return this.fetchSisDataset('lectures', SIS_LECTURE_LIST_ENDPOINT, searchFields, 'dl_main', (row) => ({
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

  /**
   * 학과 교육과정의 과목 1건 (학년·학기·이수구분·학점·교과목 개요)
   * 실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
   * 캐시: 인자별 cacheTtlSeconds (기본 86400), 요청 1건
   * @param query CurriculumCoursesQuery (departmentCode, courseNumber 모두 필수)
   * @returns CurriculumCourse 목록 (그 학과 교육과정에 없는 과목은 빈 목록, 정상)
   * @throws SejongAuthError INVALID_INPUT: 학과 코드나 학수번호가 빔 (요청 전에 바로 던짐)
   */
  curriculumCourses({ departmentCode, courseNumber }: CurriculumCoursesQuery): Promise<CurriculumCourse[]> {
    if (!String(departmentCode ?? '').trim() || !String(courseNumber ?? '').trim()) {
      throw new SejongAuthError(ErrorCode.INVALID_INPUT, 'Department code and course number are required.');
    }
    const url = `${CURRICULUM_COURSE_URL}?${pairsOf(CURRICULUM_QUERY_KEYS, courseNumber, departmentCode)}`;
    return this.fetchCached('curriculumCourses', [departmentCode, courseNumber], () => [], () => this.requestJson('GET', url), (body) =>
      rowsOf(body, 'items').map((row) => ({
        departmentCode: toText(row['deptCd']), departmentName: toText(row['deptNm']), categoryCode: toText(row['orgnClsfCd']), year: toText(row['year']),
        courseNumber: toText(row['curiNo']), name: toText(row['curiNm']), courseTypeCode: toText(row['curiTypeCd']), courseTypeName: toText(row['curiTypeNm']),
        credits: toText(row['cdt']), lectureHours: toText(row['lecTmNum']), practiceHours: toText(row['exeTmNum']), totalHours: toText(row['curiTm']),
        gradeLevel: toText(row['stuYearCd']), semesterCode: toText(row['smtDanCd']), semesterName: toText(row['smtDanNm']), overview: toText(row['remark']),
        extra: extraOf(row, CURRICULUM_COURSE_USED_KEYS),
      })));
  }

  /**
   * 학부·대학원 홈페이지 학사일정: 기간·분류 > CalendarEvent 목록
   * 실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
   * 캐시: 인자별 cacheTtlSeconds (기본 86400), 요청 1건
   * @param query CalendarQuery (start, end 는 yyyy-MM-dd, categoryCode 기본 0001 학부, 0002~0007 대학원 계열)
   * @returns CalendarEvent 목록
   */
  calendar({ start, end, categoryCode = DEFAULT_CALENDAR_CATEGORY_CODE }: CalendarQuery): Promise<CalendarEvent[]> {
    const url = `${ACADEMIC_CALENDAR_URL}?${pairsOf(CALENDAR_QUERY_KEYS, CALENDAR_MODE, start, end, categoryCode)}`;
    return this.fetchCached('calendar', [start, end, categoryCode], () => [], () => this.requestJson('GET', url), (body) =>
      rowsOf(body, 'data').map(toCalendarEvent));
  }

  /**
   * 대학원 사이트별 한 달 학사일정 (날짜는 MM.dd 형태)
   * 실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
   * 캐시: 인자별 cacheTtlSeconds (기본 86400), 요청 1건
   * @param query GraduateCalendarQuery (siteId: graduate, gsi, mba, pub, tourgrad, edu, arts 등 소문자·숫자·하이픈, year, month 1~12)
   * @returns CalendarEvent 목록 (빈 달은 빈 목록, 정상)
   * @throws SejongAuthError INVALID_INPUT: siteId 형태가 틀림 (요청 전에 바로 던짐)
   */
  graduateCalendar({ siteId, year, month }: GraduateCalendarQuery): Promise<CalendarEvent[]> {
    if (!GRADUATE_SITE_ID_PATTERN.test(String(siteId ?? ''))) {
      throw new SejongAuthError(ErrorCode.INVALID_INPUT, 'Graduate site id must be lowercase letters, digits and hyphens.');
    }
    const url = `${GRADUATE_CALENDAR_URL.replace('{siteId}', siteId)}?${pairsOf(GRADUATE_CALENDAR_QUERY_KEYS, String(year), String(month), siteId)}`;
    return this.fetchCached('graduateCalendar', [siteId, String(year), String(month)], () => [], () => this.requestJson('GET', url), (body) => {
      // success 거짓·없음은 실패, items 는 JSON 문자열이라 한 번 더 해석
      if (toText(body.success).toLowerCase() !== 'true') throw new Error('graduate calendar did not report success');
      const items = typeof body.items === 'string' ? JSON.parse(body.items) : body.items;
      return asRows(items).map(toCalendarEvent);
    });
  }

  /**
   * 기숙사 일주일 식단 (7일: 조식·중식·석식·대체)
   * 실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
   * 캐시: 인자별 cacheTtlSeconds (기본 86400), 요청 1건
   * @param location 기숙사 위치 코드 (기본 SJ, 다른 값은 확인하지 않음)
   * @returns DormitoryMenuDay 목록 (7개)
   */
  dormitoryMenu(location = DEFAULT_DORMITORY_LOCATION): Promise<DormitoryMenuDay[]> {
    const form = pairsOf(DORMITORY_MENU_FORM_KEYS, location);
    return this.fetchCached('dormitoryMenu', [location], () => [], () => this.requestJson('POST', DORMITORY_MENU_URL, form), (body) => {
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

  /**
   * 기숙사 공지: 고정 공지와 일반 공지, 전체 건수 (작성자 정보는 반환하지 않음)
   * 실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
   * 캐시: 인자별 cacheTtlSeconds (기본 86400), 요청 1건
   * @param query DormitoryNoticesQuery (page 기본 1, rows 기본 10, keyword 제목 검색어, location 기본 SJ), 모두 선택
   * @returns DormitoryNotices (실패하면 total 0과 빈 목록, 결과 없음도 total 0)
   */
  dormitoryNotices({ page = 1, rows = 10, keyword = '', location = DEFAULT_DORMITORY_LOCATION }: DormitoryNoticesQuery = {}): Promise<DormitoryNotices> {
    const form = pairsOf(DORMITORY_NOTICE_FORM_KEYS, String(page), String(rows), location, DORMITORY_NOTICE_BOARD_ID, DORMITORY_NOTICE_SEARCH_TYPE, keyword);
    const toNotice = (row: SchoolRow): DormitoryNotice => ({
      id: toText(row['seq']), title: toText(row.subject), date: toText(row.regdate), views: toCount(row.visitcnt), fileName: toText(row.file_name1),
    });
    return this.fetchCached('dormitoryNotices', [page, rows, keyword, location], () => ({ total: 0, pinned: [], items: [] }),
      () => this.requestJson('POST', DORMITORY_NOTICE_URL, form), (body) => {
        // root 만 필수, 나머지가 없으면 결과 없음
        const block = rowsOf(body, 'root')[0] ?? {};
        return {
          total: toCount(asRowsOrEmpty(block.totalCount)[0]?.cnt),
          pinned: asRowsOrEmpty(block.topList).map(toNotice),
          items: asRowsOrEmpty(block.list).map(toNotice),
        };
      });
  }

  /**
   * 학생식당 한 곳의 메뉴
   * 실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
   * 캐시: 인자별 cacheTtlSeconds (기본 86400), 요청 1건
   * @param placeId 학교의 식당 번호 (cafeteriaPlaces 결과의 placeId), 빈 목록이 번호 오류의 증거는 아님
   * @returns CafeteriaMenuItem 목록
   */
  cafeteriaMenu(placeId: string | number): Promise<CafeteriaMenuItem[]> {
    const url = `${CAFETERIA_URL}?${pairsOf(CAFETERIA_QUERY_KEYS, CAFETERIA_MENU_MODE, String(placeId))}`;
    return this.fetchCached('cafeteriaMenu', [String(placeId)], () => [], () => this.requestJson('GET', url), (body) =>
      rowsOf(body, 'items').map((row) => ({
        buildingName: toText(row.buildingName), location: toText(row.location), placeId: toText(row.placeId), placeName: toText(row.placeName),
        date: toText(row.menuDate), day: toText(row.day), mealTypeId: toText(row.mealTypeId), mealName: toText(row.mealName),
        menuName: toText(row.menuName), price: toText(row.price),
      })));
  }

  /**
   * 학식 식당 목록 (번호, 이름, 건물, 위치): 번호 1~maxPlaceId의 메뉴 응답에서 모음, 메뉴가 없는 식당은 빠짐
   * 실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
   * 캐시: maxPlaceId 별 cacheTtlSeconds, 요청은 번호만큼 차례로 (기본 10건, 최대 50건)
   * @param query CafeteriaPlacesQuery (maxPlaceId 조회할 번호 상한, 1~50, 기본 10)
   * @returns CafeteriaPlace 목록 (번호 순서, 하나라도 실패하면 목록 전체를 실패로 보고 일부만 캐시하지 않음)
   * @throws SejongAuthError INVALID_INPUT: maxPlaceId 가 1~50 밖이거나 정수가 아님 (요청 전에 바로 던짐)
   */
  cafeteriaPlaces({ maxPlaceId = CAFETERIA_DEFAULT_MAX_PLACE_ID }: CafeteriaPlacesQuery = {}): Promise<CafeteriaPlace[]> {
    if (!Number.isInteger(maxPlaceId) || maxPlaceId < 1 || maxPlaceId > CAFETERIA_LIMIT_MAX_PLACE_ID) {
      throw new SejongAuthError(ErrorCode.INVALID_INPUT, `Max place id must be an integer from 1 to ${CAFETERIA_LIMIT_MAX_PLACE_ID}.`);
    }
    const probePlaceIds = Array.from({ length: maxPlaceId }, (_, index) => String(index + 1));
    const requestAllPlaceIds = async (): Promise<JsonObject> => {
      const responses: JsonObject[] = [];
      for (const placeId of probePlaceIds) { // 순서대로 한 건씩, 병렬 요청 없음
        const url = `${CAFETERIA_URL}?${pairsOf(CAFETERIA_QUERY_KEYS, CAFETERIA_MENU_MODE, placeId)}`;
        responses.push(await this.requestJson('GET', url));
      }
      return { responses };
    };
    return this.fetchCached('cafeteriaPlaces', [String(maxPlaceId)], () => [], requestAllPlaceIds, (body) => {
      // 번호 순서대로 첫 행의 식당 정보, 같은 식당 번호는 한 번만
      const placeById = new Map<string, CafeteriaPlace>();
      (body.responses as JsonObject[]).forEach((response, index) => {
        const firstRow = rowsOf(response, 'items').find((row) => Object.keys(row).length > 0);
        if (!firstRow) return;
        const placeId = toText(firstRow.placeId) || probePlaceIds[index];
        if (placeById.has(placeId)) return;
        placeById.set(placeId, {
          placeId, placeName: toText(firstRow.placeName), buildingName: toText(firstRow.buildingName), location: toText(firstRow.location),
          extra: extraOf(firstRow, CAFETERIA_MENU_USED_KEYS),
        });
      });
      return [...placeById.values()];
    });
  }

  /**
   * 캠퍼스 지도의 장소 (이름, 위치 위도·경도, 안내)
   * 실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
   * 캐시: 인자별 cacheTtlSeconds (기본 86400), 요청 1건
   * @param placeType 장소 종류 (학교 값 그대로), 비우면 전체
   * @returns CampusPlace 목록
   */
  campusPlaces(placeType = ''): Promise<CampusPlace[]> {
    const url = `${CAMPUS_MAP_URL}?${pairsOf(CAMPUS_MAP_QUERY_KEYS, CAMPUS_PLACE_LIST_MODE, placeType)}`;
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

  /**
   * 비교과 프로그램 일정: 기간·분류 > ExtracurricularEvent 목록
   * 실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
   * 캐시: 인자별 cacheTtlSeconds (기본 86400), 요청 1건
   * @param query ExtracurricularEventsQuery (start, end 는 yyyy-MM-dd, category 학교 값 그대로, 비우면 전체)
   * @returns ExtracurricularEvent 목록
   */
  extracurricularEvents({ start, end, category = '' }: ExtracurricularEventsQuery): Promise<ExtracurricularEvent[]> {
    const yearOf = (date: string) => date.slice(0, 4);
    const form = pairsOf(EXTRACURRICULAR_FORM_KEYS, EXTRACURRICULAR_CID, category, start, end, yearOf(start), yearOf(end));
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
      headers: isJsonRequest ? SIS_JSON_HEADERS : form ? FORM_HEADERS : JSON_ACCEPT_HEADERS,
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
