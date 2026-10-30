// 공개 데이터 진입점(로그인 없음): 공개 메서드 15개는 출처별 모듈(*Source.ts)로 위임, 캐시·실패 격리·전송은 fetcher.ts (spec/PUBLIC_DATA.md)
import { AcademicSource } from './academicSource.js';
import { CafeteriaSource } from './cafeteriaSource.js';
import { CalendarSource } from './calendarSource.js';
import { CampusSource } from './campusSource.js';
import { DEFAULT_DORMITORY_LOCATION, DormitorySource } from './dormitorySource.js';
import { ExtracurricularSource } from './extracurricularSource.js';
import { PublicDataFetcher } from './fetcher.js';
import type {
  CafeteriaMenuItem, CafeteriaPlace, CalendarEvent, CampusPlace, CourseType, CurriculumCourse, Department, DormitoryMenuDay, DormitoryNotices,
  ExtracurricularEvent, Lecture, OrganizationCategory, SelectiveDomain, Term,
} from './models.js';
import type {
  CafeteriaPlacesQuery, CalendarQuery, CurriculumCoursesQuery, DepartmentsQuery, DormitoryNoticesQuery, ExtracurricularEventsQuery,
  GraduateCalendarQuery, LecturesQuery,
} from './queries.js';
import type { Session } from '../common/transport.js';

export type * from './models.js';
export type * from './queries.js';

const DEFAULT_FAILURE_CACHE_SECONDS = 60;
const DEFAULT_CACHE_MAX_ENTRIES = 500;

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

/** 공개 데이터 클라이언트(로그인 없음): 학사 코드, 강의, 학과 교육과정, 학사일정, 기숙사, 학식, 캠퍼스, 비교과 15개 메서드 */
export class SejongPublicData {
  readonly timeoutSeconds: number;
  readonly cacheTtlSeconds: number;
  readonly failureCacheSeconds: number;
  readonly cacheMaxEntries: number;
  readonly sslVerification: boolean;
  private readonly academic: AcademicSource;
  private readonly calendarSource: CalendarSource;
  private readonly dormitory: DormitorySource;
  private readonly cafeteria: CafeteriaSource;
  private readonly campus: CampusSource;
  private readonly extracurricular: ExtracurricularSource;

  constructor({
    timeoutSeconds = 10, cacheTtlSeconds = 86400, failureCacheSeconds = DEFAULT_FAILURE_CACHE_SECONDS, cacheMaxEntries = DEFAULT_CACHE_MAX_ENTRIES,
    sslVerification = true, _sessionFactory, _nowMilliseconds,
  }: SejongPublicDataOptions = {}) {
    this.timeoutSeconds = timeoutSeconds;
    this.cacheTtlSeconds = cacheTtlSeconds;
    this.failureCacheSeconds = failureCacheSeconds;
    this.cacheMaxEntries = cacheMaxEntries;
    this.sslVerification = sslVerification;
    const fetcher = new PublicDataFetcher({
      timeoutSeconds, cacheTtlSeconds, failureCacheSeconds, cacheMaxEntries, sslVerification,
      sessionFactory: _sessionFactory, nowMilliseconds: _nowMilliseconds,
    });
    this.academic = new AcademicSource(fetcher);
    this.calendarSource = new CalendarSource(fetcher);
    this.dormitory = new DormitorySource(fetcher);
    this.cafeteria = new CafeteriaSource(fetcher);
    this.campus = new CampusSource(fetcher);
    this.extracurricular = new ExtracurricularSource(fetcher);
  }

  /** 기관 구분 목록 (학부 20, 대학원 30 등) */
  organizationCategories(): Promise<OrganizationCategory[]> {
    return this.academic.organizationCategories();
  }

  /** 기관 구분의 학기 목록: 기관 구분 코드 > Term 목록 */
  terms(categoryCode: string): Promise<Term[]> {
    return this.academic.terms(categoryCode);
  }

  /** 학기의 학과 목록: 기관 구분·연도·학기 코드 > Department 목록 */
  departments(query: DepartmentsQuery): Promise<Department[]> {
    return this.academic.departments(query);
  }

  /** 이수 구분 목록 (공필, 전필 등) */
  courseTypes(): Promise<CourseType[]> {
    return this.academic.courseTypes();
  }

  /** 선택 교양 영역 목록 */
  selectiveDomains(): Promise<SelectiveDomain[]> {
    return this.academic.selectiveDomains();
  }

  /** 개설 강좌 검색: 학과 코드·과목명·교수명은 선택, 모두 비우면 그 학기 전체 (학부 약 2,800행·2.4MB) */
  lectures(query: LecturesQuery): Promise<Lecture[]> {
    return this.academic.lectures(query);
  }

  /** 학과 교육과정의 과목 1건 (학년·학기·이수구분·학점·교과목 개요) */
  curriculumCourses(query: CurriculumCoursesQuery): Promise<CurriculumCourse[]> {
    return this.academic.curriculumCourses(query);
  }

  /** 학부·대학원 홈페이지 학사일정: 기간·분류 > CalendarEvent 목록 */
  calendar(query: CalendarQuery): Promise<CalendarEvent[]> {
    return this.calendarSource.calendar(query);
  }

  /** 대학원 사이트별 한 달 학사일정 (날짜는 MM.dd 형태) */
  graduateCalendar(query: GraduateCalendarQuery): Promise<CalendarEvent[]> {
    return this.calendarSource.graduateCalendar(query);
  }

  /** 기숙사 일주일 식단 (7일: 조식·중식·석식·대체) */
  dormitoryMenu(location = DEFAULT_DORMITORY_LOCATION): Promise<DormitoryMenuDay[]> {
    return this.dormitory.dormitoryMenu(location);
  }

  /** 기숙사 공지: 고정 공지와 일반 공지, 전체 건수 (작성자 정보는 반환하지 않음) */
  dormitoryNotices(query: DormitoryNoticesQuery = {}): Promise<DormitoryNotices> {
    return this.dormitory.dormitoryNotices(query);
  }

  /** 학생식당 한 곳의 메뉴 */
  cafeteriaMenu(placeId: string | number): Promise<CafeteriaMenuItem[]> {
    return this.cafeteria.cafeteriaMenu(placeId);
  }

  /** 학식 식당 목록 (번호, 이름, 건물, 위치): 번호 1~maxPlaceId의 메뉴 응답에서 모음, 메뉴가 없는 식당은 빠짐 */
  cafeteriaPlaces(query: CafeteriaPlacesQuery = {}): Promise<CafeteriaPlace[]> {
    return this.cafeteria.cafeteriaPlaces(query);
  }

  /** 캠퍼스 지도의 장소 (이름, 위치 위도·경도, 안내) */
  campusPlaces(placeType = ''): Promise<CampusPlace[]> {
    return this.campus.campusPlaces(placeType);
  }

  /** 비교과 프로그램 일정: 기간·분류 > ExtracurricularEvent 목록 */
  extracurricularEvents(query: ExtracurricularEventsQuery): Promise<ExtracurricularEvent[]> {
    return this.extracurricular.extracurricularEvents(query);
  }
}
