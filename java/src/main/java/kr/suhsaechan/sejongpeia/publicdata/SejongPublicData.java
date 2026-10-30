package kr.suhsaechan.sejongpeia.publicdata;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import kr.suhsaechan.sejongpeia.common.config.SejongAuthProperties;
import kr.suhsaechan.sejongpeia.common.exception.SejongAuthException;
import kr.suhsaechan.sejongpeia.publicdata.model.*;

/**
 * 로그인 없는 학교 공개 데이터 조회 15종: 학사 코드, 강의, 학과 교육과정, 학사일정, 기숙사, 학식, 캠퍼스 장소, 비교과
 * 요청 > JSON 검증 > 모델 변환 > 메모리 캐시, 상류 실패는 마지막 캐시 또는 빈 값 (예외는 잘못된 입력뿐: curriculumCourses, graduateCalendar, cafeteriaPlaces)
 * 메서드당 요청 1건 (cafeteriaPlaces 만 번호만큼, 기본 10건, 최대 50건), 같은 인자의 동시 호출은 요청 1건 공유
 * 값 규칙: 학교 원문 문자열, 없으면 빈 문자열 (null 없음), 쓰지 않은 학교 필드는 extra (spec/PUBLIC_DATA.md)
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

  /**
   * 기본 설정 (타임아웃 10초, 캐시 86400초, 인증서 검증)
   */
  public SejongPublicData() {
    this(new SejongAuthProperties());
  }

  /**
   * 설정 객체로 생성 (Spring 자동 설정이 쓰는 생성자)
   *
   * @param properties 타임아웃, 캐시 유효 시간, 실패 기억 시간, 캐시 최대 항목 수, 인증서 검증
   */
  public SejongPublicData(SejongAuthProperties properties) {
    this(new OkHttpPublicDataHttpCaller(properties), Clock.systemUTC(),
        Duration.ofSeconds(properties.getPublicDataCacheTtlSeconds()),
        Duration.ofSeconds(properties.getPublicDataFailureCacheSeconds()), properties.getPublicDataCacheMaxEntries());
  }

  /**
   * 호출기와 시계 주입 (테스트, 프록시 등)
   *
   * @param httpCaller HTTP 호출기
   * @param clock 시계 (캐시 만료 판단)
   * @param cacheTimeToLive 캐시 유효 시간
   */
  public SejongPublicData(PublicDataHttpCaller httpCaller, Clock clock, Duration cacheTimeToLive) {
    this(httpCaller, clock, cacheTimeToLive, Duration.ofSeconds(DEFAULT_FAILURE_CACHE_SECONDS), DEFAULT_CACHE_MAX_ENTRIES);
  }

  /**
   * 호출기, 시계, 캐시 유효 시간, 실패 기억 시간, 캐시 최대 항목 수 주입
   *
   * @param httpCaller HTTP 호출기
   * @param clock 시계 (캐시 만료 판단)
   * @param cacheTimeToLive 캐시 유효 시간
   * @param failureMemoryDuration 캐시 없는 실패를 기억하는 시간
   * @param cacheMaxEntries 캐시 최대 항목 수
   */
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

  /**
   * 기관 구분 목록 (학부 20, 대학원 30 등)
   * 실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
   * 캐시: 인자별 캐시 유효 시간 (기본 86400초), 요청 1건
   *
   * @return OrganizationCategory 목록
   */
  public List<OrganizationCategory> organizationCategories() {
    return academic.organizationCategories();
  }

  /**
   * 기관 구분의 학기 목록: 기관 구분 코드 > Term 목록
   * 실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
   * 캐시: 인자별 캐시 유효 시간 (기본 86400초), 요청 1건
   *
   * @param categoryCode 기관 구분 코드 (organizationCategories 결과의 code, 예: 20 학부)
   * @return Term 목록
   */
  public List<Term> terms(String categoryCode) {
    return academic.terms(categoryCode);
  }

  /**
   * 학기의 학과 목록: 기관 구분·연도·학기 코드 > Department 목록
   * 실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
   * 캐시: 인자별 캐시 유효 시간 (기본 86400초), 요청 1건
   *
   * @param categoryCode 기관 구분 코드
   * @param year 연도 4자리 (예: 2026)
   * @param semesterCode 학기 코드 (10 1학기, 11 하계, 20 2학기, 21 동계)
   * @return Department 목록
   */
  public List<Department> departments(String categoryCode, String year, String semesterCode) {
    return academic.departments(categoryCode, year, semesterCode);
  }

  /**
   * 이수 구분 목록 (공필, 전필 등)
   * 실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
   * 캐시: 인자별 캐시 유효 시간 (기본 86400초), 요청 1건
   *
   * @return CourseType 목록
   */
  public List<CourseType> courseTypes() {
    return academic.courseTypes();
  }

  /**
   * 선택 교양 영역 목록
   * 실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
   * 캐시: 인자별 캐시 유효 시간 (기본 86400초), 요청 1건
   *
   * @return SelectiveDomain 목록
   */
  public List<SelectiveDomain> selectiveDomains() {
    return academic.selectiveDomains();
  }

  /**
   * 개설 강좌 검색: 학과 코드, 과목명, 교수명은 선택, 모두 비우면 그 학기 전체 (학부 약 2,800행·2.4MB)
   * 실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
   * 캐시: 인자별 캐시 유효 시간 (기본 86400초), 요청 1건
   *
   * @param categoryCode 기관 구분 코드
   * @param year 연도 4자리
   * @param semesterCode 학기 코드 (10, 11, 20, 21)
   * @param departmentCode 학과 코드 (departments 결과의 code), 빈 문자열이면 전체
   * @param courseName 과목명 검색어 (앞뒤 공백 제거), 빈 문자열이면 전체
   * @param instructorName 교수명 검색어 (앞뒤 공백 제거), 빈 문자열이면 전체
   * @return Lecture 목록
   */
  public List<Lecture> lectures(String categoryCode, String year, String semesterCode,
      String departmentCode, String courseName, String instructorName) {
    return academic.lectures(categoryCode, year, semesterCode, departmentCode, courseName, instructorName);
  }

  /**
   * 학과 교육과정의 과목 1건 (학년·학기·이수구분·학점·교과목 개요)
   * 실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
   * 캐시: 인자별 캐시 유효 시간 (기본 86400초), 요청 1건
   *
   * @param departmentCode 학과 코드 (departments 결과의 code), 필수
   * @param courseNumber 학수번호 (lectures 결과의 courseNumber), 필수
   * @return CurriculumCourse 목록 (그 학과 교육과정에 없는 과목은 빈 목록, 정상)
   * @throws SejongAuthException INVALID_INPUT: 학과 코드나 학수번호가 비면 (요청 없음)
   */
  public List<CurriculumCourse> curriculumCourses(String departmentCode, String courseNumber) {
    return academic.curriculumCourses(departmentCode, courseNumber);
  }

  /**
   * 학사일정 (학부 분류 0001 적용)
   *
   * @return CalendarEvent 목록
   */
  public List<CalendarEvent> calendar(String startDate, String endDate) {
    return calendar(startDate, endDate, CalendarSource.DEFAULT_CALENDAR_CATEGORY_CODE);
  }

  /**
   * 학부·대학원 홈페이지 학사일정: 기간·분류 > CalendarEvent 목록
   * 실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
   * 캐시: 인자별 캐시 유효 시간 (기본 86400초), 요청 1건
   *
   * @param startDate 시작일 yyyy-MM-dd
   * @param endDate 종료일 yyyy-MM-dd
   * @param categoryCode 일정 분류 (0001 학부, 0002~0007 대학원 계열)
   * @return CalendarEvent 목록
   */
  public List<CalendarEvent> calendar(String startDate, String endDate, String categoryCode) {
    return calendarSource.calendar(startDate, endDate, categoryCode);
  }

  /**
   * 대학원 사이트별 한 달 학사일정 (날짜는 MM.dd 형태)
   * 실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
   * 캐시: 인자별 캐시 유효 시간 (기본 86400초), 요청 1건
   *
   * @param siteId 대학원 사이트 이름 (graduate, gsi, mba, pub, tourgrad, edu, arts 등, 소문자·숫자·하이픈)
   * @param year 연도 (예: 2026)
   * @param month 월 1~12
   * @return CalendarEvent 목록 (빈 달은 빈 목록, 정상)
   * @throws SejongAuthException INVALID_INPUT: siteId 형태가 틀리면 (요청 없음)
   */
  public List<CalendarEvent> graduateCalendar(String siteId, int year, int month) {
    return calendarSource.graduateCalendar(siteId, year, month);
  }

  /**
   * 기숙사 일주일 식단 (위치 SJ 적용)
   *
   * @return DormitoryMenuDay 목록 (7개)
   */
  public List<DormitoryMenuDay> dormitoryMenu() {
    return dormitoryMenu(DormitorySource.DEFAULT_DORMITORY_LOCATION);
  }

  /**
   * 기숙사 일주일 식단 (7일: 조식·중식·석식·대체)
   * 실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
   * 캐시: 인자별 캐시 유효 시간 (기본 86400초), 요청 1건
   *
   * @param location 기숙사 위치 코드 (기본 SJ, 다른 값은 확인하지 않음)
   * @return DormitoryMenuDay 목록 (7개)
   */
  public List<DormitoryMenuDay> dormitoryMenu(String location) {
    return dormitory.dormitoryMenu(location);
  }

  /**
   * 기숙사 공지 (1쪽, 10건, 검색어 없음, 위치 SJ 적용)
   *
   * @return DormitoryNotices
   */
  public DormitoryNotices dormitoryNotices() {
    return dormitoryNotices(DEFAULT_NOTICE_PAGE, DEFAULT_NOTICE_ROWS, "", DormitorySource.DEFAULT_DORMITORY_LOCATION);
  }

  /**
   * 기숙사 공지: 고정 공지와 일반 공지, 전체 건수 (작성자 정보는 반환하지 않음)
   * 실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
   * 캐시: 인자별 캐시 유효 시간 (기본 86400초), 요청 1건
   *
   * @param page 쪽 번호 (1부터)
   * @param rows 쪽당 건수
   * @param keyword 제목 검색어, 빈 문자열이면 전체
   * @param location 기숙사 위치 코드
   * @return DormitoryNotices (실패하면 total 0과 빈 목록, 결과 없음도 total 0)
   */
  public DormitoryNotices dormitoryNotices(int page, int rows, String keyword, String location) {
    return dormitory.dormitoryNotices(page, rows, keyword, location);
  }

  /**
   * 학생식당 한 곳의 메뉴
   * 실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
   * 캐시: 인자별 캐시 유효 시간 (기본 86400초), 요청 1건
   *
   * @param placeId 학교의 식당 번호 (cafeteriaPlaces 결과의 placeId), 빈 목록이 번호 오류의 증거는 아님
   * @return CafeteriaMenuItem 목록
   */
  public List<CafeteriaMenuItem> cafeteriaMenu(String placeId) {
    return cafeteria.cafeteriaMenu(placeId);
  }

  /**
   * 학식 식당 목록 (번호 1~10 적용)
   *
   * @return CafeteriaPlace 목록
   */
  public List<CafeteriaPlace> cafeteriaPlaces() {
    return cafeteriaPlaces(CafeteriaSource.CAFETERIA_DEFAULT_MAX_PLACE_ID);
  }

  /**
   * 학식 식당 목록 (번호, 이름, 건물, 위치): 번호 1~maxPlaceId의 메뉴 응답에서 모음, 메뉴가 없는 식당은 빠짐
   * 실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
   * 캐시: maxPlaceId 별 캐시 유효 시간, 요청은 번호만큼 차례로 (기본 10건, 최대 50건)
   *
   * @param maxPlaceId 조회할 식당 번호 상한 (1~50)
   * @return CafeteriaPlace 목록 (번호 순서, 하나라도 실패하면 목록 전체를 실패로 보고 일부만 캐시하지 않음)
   * @throws SejongAuthException INVALID_INPUT: maxPlaceId 가 1~50 밖이면 (요청 없음)
   */
  public List<CafeteriaPlace> cafeteriaPlaces(int maxPlaceId) {
    return cafeteria.cafeteriaPlaces(maxPlaceId);
  }

  /**
   * 캠퍼스 장소 전체
   *
   * @return CampusPlace 목록
   */
  public List<CampusPlace> campusPlaces() {
    return campusPlaces("");
  }

  /**
   * 캠퍼스 지도의 장소 (이름, 위치 위도·경도, 안내)
   * 실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
   * 캐시: 인자별 캐시 유효 시간 (기본 86400초), 요청 1건
   *
   * @param placeType 장소 종류 (학교 값 그대로), 빈 문자열이면 전체
   * @return CampusPlace 목록
   */
  public List<CampusPlace> campusPlaces(String placeType) {
    return campus.campusPlaces(placeType);
  }

  /**
   * 비교과 일정 (분류 전체 적용)
   *
   * @return ExtracurricularEvent 목록
   */
  public List<ExtracurricularEvent> extracurricularEvents(String startDate, String endDate) {
    return extracurricularEvents(startDate, endDate, "");
  }

  /**
   * 비교과 프로그램 일정: 기간·분류 > ExtracurricularEvent 목록
   * 실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
   * 캐시: 인자별 캐시 유효 시간 (기본 86400초), 요청 1건
   *
   * @param startDate 시작일 yyyy-MM-dd
   * @param endDate 종료일 yyyy-MM-dd
   * @param category 비교과 분류 (학교 값 그대로), 빈 문자열이면 전체
   * @return ExtracurricularEvent 목록
   */
  public List<ExtracurricularEvent> extracurricularEvents(String startDate, String endDate, String category) {
    return extracurricular.extracurricularEvents(startDate, endDate, category);
  }
}
