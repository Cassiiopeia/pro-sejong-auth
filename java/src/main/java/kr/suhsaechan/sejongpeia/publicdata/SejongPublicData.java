package kr.suhsaechan.sejongpeia.publicdata;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import kr.suhsaechan.sejongpeia.common.config.SejongAuthProperties;
import kr.suhsaechan.sejongpeia.publicdata.model.*;

/**
 * 로그인 없는 학교 공개 데이터 조회 15종: 학사 코드, 강의, 학과 교육과정, 학사일정, 기숙사, 학식, 캠퍼스 장소, 비교과
 * 출처별 클래스로 위임, 요청 > 변환 > 캐시, 상류 실패는 예외 없이 마지막 캐시 또는 빈 값 (spec/PUBLIC_DATA.md)
 */
public class SejongPublicData {

  // 실패 기억 기본 60초, 캐시 기본 최대 500항목 (SejongAuthProperties 기본값과 동일)
  private static final long DEFAULT_FAILURE_CACHE_SECONDS = 60;
  private static final int DEFAULT_CACHE_MAX_ENTRIES = 500;
  private static final int DEFAULT_NOTICE_PAGE = 1;
  private static final int DEFAULT_NOTICE_ROWS = 10;

  private final AcademicSource academic;
  private final CalendarSource calendarSource;
  private final DormitorySource dormitory;
  private final CafeteriaSource cafeteria;
  private final CampusSource campus;
  private final ExtracurricularSource extracurricular;

  /** 기본 설정 (타임아웃 10초, 캐시 86400초, 인증서 검증) */
  public SejongPublicData() {
    this(new SejongAuthProperties());
  }

  /** 설정 객체로 생성 (Spring 자동 설정이 쓰는 생성자) */
  public SejongPublicData(SejongAuthProperties properties) {
    this(new OkHttpPublicDataHttpCaller(properties), Clock.systemUTC(),
        Duration.ofSeconds(properties.getPublicDataCacheTtlSeconds()),
        Duration.ofSeconds(properties.getPublicDataFailureCacheSeconds()), properties.getPublicDataCacheMaxEntries());
  }

  /** 호출기와 시계 주입 (테스트, 프록시 등) */
  public SejongPublicData(PublicDataHttpCaller httpCaller, Clock clock, Duration cacheTimeToLive) {
    this(httpCaller, clock, cacheTimeToLive, Duration.ofSeconds(DEFAULT_FAILURE_CACHE_SECONDS), DEFAULT_CACHE_MAX_ENTRIES);
  }

  /** 호출기, 시계, 캐시 유효 시간, 실패 기억 시간, 캐시 최대 항목 수 주입 */
  public SejongPublicData(PublicDataHttpCaller httpCaller, Clock clock, Duration cacheTimeToLive,
      Duration failureMemoryDuration, int cacheMaxEntries) {
    PublicDataFetcher fetcher = new PublicDataFetcher(httpCaller, clock, cacheTimeToLive, failureMemoryDuration, cacheMaxEntries);
    this.academic = new AcademicSource(fetcher);
    this.calendarSource = new CalendarSource(fetcher);
    this.dormitory = new DormitorySource(fetcher);
    this.cafeteria = new CafeteriaSource(fetcher);
    this.campus = new CampusSource(fetcher);
    this.extracurricular = new ExtracurricularSource(fetcher);
  }

  /** 기관 구분 목록 (학부 20, 대학원 30 등) */
  public List<OrganizationCategory> organizationCategories() {
    return academic.organizationCategories();
  }

  /** 기관 구분의 학기 목록: 기관 구분 코드 > Term 목록 */
  public List<Term> terms(String categoryCode) {
    return academic.terms(categoryCode);
  }

  /** 학기의 학과 목록: 기관 구분·연도·학기 코드 > Department 목록 */
  public List<Department> departments(String categoryCode, String year, String semesterCode) {
    return academic.departments(categoryCode, year, semesterCode);
  }

  /** 이수 구분 목록 (공필, 전필 등) */
  public List<CourseType> courseTypes() {
    return academic.courseTypes();
  }

  /** 선택 교양 영역 목록 */
  public List<SelectiveDomain> selectiveDomains() {
    return academic.selectiveDomains();
  }

  /** 개설 강좌 검색: 학과 코드, 과목명, 교수명은 선택, 모두 비우면 그 학기 전체 (학부 약 2,800행·2.4MB) */
  public List<Lecture> lectures(String categoryCode, String year, String semesterCode,
      String departmentCode, String courseName, String instructorName) {
    return academic.lectures(categoryCode, year, semesterCode, departmentCode, courseName, instructorName);
  }

  /** 학과 교육과정의 과목 1건 (학년·학기·이수구분·학점·교과목 개요) */
  public List<CurriculumCourse> curriculumCourses(String departmentCode, String courseNumber) {
    return academic.curriculumCourses(departmentCode, courseNumber);
  }

  /** 학사일정 (학부 분류 0001 적용) */
  public List<CalendarEvent> calendar(String startDate, String endDate) {
    return calendar(startDate, endDate, CalendarSource.DEFAULT_CALENDAR_CATEGORY_CODE);
  }

  /** 학부·대학원 홈페이지 학사일정: 기간·분류 > CalendarEvent 목록 */
  public List<CalendarEvent> calendar(String startDate, String endDate, String categoryCode) {
    return calendarSource.calendar(startDate, endDate, categoryCode);
  }

  /** 대학원 사이트별 한 달 학사일정 (날짜는 MM.dd 형태) */
  public List<CalendarEvent> graduateCalendar(String siteId, int year, int month) {
    return calendarSource.graduateCalendar(siteId, year, month);
  }

  /** 기숙사 일주일 식단 (위치 SJ 적용) */
  public List<DormitoryMenuDay> dormitoryMenu() {
    return dormitoryMenu(DormitorySource.DEFAULT_DORMITORY_LOCATION);
  }

  /** 기숙사 일주일 식단 (7일: 조식·중식·석식·대체) */
  public List<DormitoryMenuDay> dormitoryMenu(String location) {
    return dormitory.dormitoryMenu(location);
  }

  /** 기숙사 공지 (1쪽, 10건, 검색어 없음, 위치 SJ 적용) */
  public DormitoryNotices dormitoryNotices() {
    return dormitoryNotices(DEFAULT_NOTICE_PAGE, DEFAULT_NOTICE_ROWS, "", DormitorySource.DEFAULT_DORMITORY_LOCATION);
  }

  /** 기숙사 공지: 고정 공지와 일반 공지, 전체 건수 (작성자 정보는 반환하지 않음) */
  public DormitoryNotices dormitoryNotices(int page, int rows, String keyword, String location) {
    return dormitory.dormitoryNotices(page, rows, keyword, location);
  }

  /** 학생식당 한 곳의 메뉴 */
  public List<CafeteriaMenuItem> cafeteriaMenu(String placeId) {
    return cafeteria.cafeteriaMenu(placeId);
  }

  /** 학식 식당 목록 (번호 1~10 적용) */
  public List<CafeteriaPlace> cafeteriaPlaces() {
    return cafeteriaPlaces(CafeteriaSource.CAFETERIA_DEFAULT_MAX_PLACE_ID);
  }

  /** 학식 식당 목록 (번호, 이름, 건물, 위치): 번호 1~maxPlaceId의 메뉴 응답에서 모음, 메뉴가 없는 식당은 빠짐 */
  public List<CafeteriaPlace> cafeteriaPlaces(int maxPlaceId) {
    return cafeteria.cafeteriaPlaces(maxPlaceId);
  }

  /** 캠퍼스 장소 전체 */
  public List<CampusPlace> campusPlaces() {
    return campusPlaces("");
  }

  /** 캠퍼스 지도의 장소 (이름, 위치 위도·경도, 안내) */
  public List<CampusPlace> campusPlaces(String placeType) {
    return campus.campusPlaces(placeType);
  }

  /** 비교과 일정 (분류 전체 적용) */
  public List<ExtracurricularEvent> extracurricularEvents(String startDate, String endDate) {
    return extracurricularEvents(startDate, endDate, "");
  }

  /** 비교과 프로그램 일정: 기간·분류 > ExtracurricularEvent 목록 */
  public List<ExtracurricularEvent> extracurricularEvents(String startDate, String endDate, String category) {
    return extracurricular.extracurricularEvents(startDate, endDate, category);
  }
}
