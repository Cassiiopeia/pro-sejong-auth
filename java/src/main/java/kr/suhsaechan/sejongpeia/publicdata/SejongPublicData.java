package kr.suhsaechan.sejongpeia.publicdata;

import static kr.suhsaechan.sejongpeia.publicdata.PublicDataMapper.listOf;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import kr.suhsaechan.sejongpeia.common.config.SejongAuthProperties;
import kr.suhsaechan.sejongpeia.common.exception.SejongAuthErrorCode;
import kr.suhsaechan.sejongpeia.common.exception.SejongAuthException;
import kr.suhsaechan.sejongpeia.publicdata.model.*;

/**
 * 로그인 없는 학교 공개 데이터 조회 15종: 학사 코드, 강의, 학과 교육과정, 학사 일정, 기숙사, 학식, 캠퍼스 장소, 비교과
 * 메서드당 요청 1건 (cafeteriaPlaces 만 최대 10건) > 메모리 캐시, 상류 실패는 마지막 캐시 또는 빈 값 (예외는 잘못된 입력뿐: curriculumCourses, graduateCalendar)
 */
public class SejongPublicData {

  // 강좌 조회는 나머지 필터 키가 모두 빈 값으로 있어야 학교가 받아들임 (하나라도 빠지면 _SUBMIT_ERROR_)
  private static final List<String> LECTURE_EMPTY_FILTER_KEYS = List.of(
      "SMT_NM", "CURI_TYPE_CD", "SLT_DOMAIN_CD", "CURI_NO", "CLASS", "SLT_DEPT_CD", "PLAN_REG_YN", "CORS_SCHE_GRP_CD");

  private static final String SIS_OPEN_TIME_URL = "https://sjpt.sejong.ac.kr/sch/sch/ext/SueOpenTimeQ/";
  // 학과 교육과정 과목 1건: deptNo 는 SIS 학과 코드(DEPT_CD)와 같은 값
  private static final String CURRICULUM_COURSE_URL = "https://www.sejong.ac.kr/curriculum/getCurriculumOneKor.do";
  private static final String ACADEMIC_CALENDAR_URL = "https://www.sejong.ac.kr/kor/academics/academic-calendar.do";
  // 대학원 사이트별 학사일정: siteId 는 graduate, gsi, mba 같은 사이트 식별자 (코드표 없이 호출자가 전달)
  private static final String GRADUATE_CALENDAR_PATH = ".sejong.ac.kr/_custom/sejong/_app/calendar-graduate/proc/getCalendarData.jsp";
  // 사이트 식별자 허용 형태 (호스트 이름에 들어가므로 소문자·숫자·하이픈만)
  private static final java.util.regex.Pattern GRADUATE_SITE_ID_PATTERN = java.util.regex.Pattern.compile("[a-z0-9][a-z0-9-]{0,30}");
  private static final String CAFETERIA_URL = "https://www.sejong.ac.kr/kor/unilife/cafeteria-info.do";
  // 학교에 식당 목록 API가 없어 번호 1~10을 차례로 조회해 식당을 모음 (학교 화면도 번호를 스크립트에 고정해 씀, spec/PUBLIC_DATA.md)
  private static final List<String> CAFETERIA_PROBE_PLACE_IDS = List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
  private static final String CAMPUS_MAP_URL = "https://www.sejong.ac.kr/kor/intro/campus-map.do";
  private static final String DORMITORY_MENU_URL = "https://happydorm.sejong.ac.kr/food/indexFoodList.do";
  private static final String DORMITORY_NOTICE_URL = "https://happydorm.sejong.ac.kr/bbs/getBbsList.do";
  private static final String EXTRACURRICULAR_EVENTS_URL = "https://do.sejong.ac.kr/ko/process/schedule/getEvents";

  private static final String SIS_CONTENT_TYPE = "application/json; charset=\"UTF-8\"";
  private static final String FORM_CONTENT_TYPE = OkHttpPublicDataHttpCaller.FORM_CONTENT_TYPE;
  private static final String DEFAULT_CALENDAR_CATEGORY_CODE = "0001"; // 학부 일정
  private static final String DEFAULT_DORMITORY_LOCATION = "SJ";
  // 실패 기억 기본 60초, 캐시 기본 최대 500항목 (SejongAuthProperties 기본값과 동일)
  private static final long DEFAULT_FAILURE_CACHE_SECONDS = 60;
  private static final int DEFAULT_CACHE_MAX_ENTRIES = 500;
  private static final int DEFAULT_NOTICE_PAGE = 1;
  private static final int DEFAULT_NOTICE_ROWS = 10;
  private static final int YEAR_TEXT_LENGTH = 4;

  private final PublicDataFetcher fetcher;

  /** 기본 설정 (타임아웃 10초, 캐시 86400초, SSL 검증) */
  public SejongPublicData() {
    this(new SejongAuthProperties());
  }

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
    this.fetcher = new PublicDataFetcher(httpCaller, clock, cacheTimeToLive, failureMemoryDuration, cacheMaxEntries);
  }

  public List<OrganizationCategory> organizationCategories() {
    return sisDataset("doOnload.do", new LinkedHashMap<>(), "dl_code_orgn", PublicDataMapper::toOrganizationCategory);
  }

  public List<Term> terms(String categoryCode) {
    Map<String, String> search = new LinkedHashMap<>();
    search.put("ORGN_CLSF_CD", nullToEmpty(categoryCode));
    return sisDataset("doYearsmtlist.do", search, "dl_code_yearSmt", PublicDataMapper::toTerm);
  }

  public List<Department> departments(String categoryCode, String year, String semesterCode) {
    Map<String, String> search = termSearch(categoryCode, year, semesterCode);
    return sisDataset("doLesndeptgroup.do", search, "dl_code_dept", PublicDataMapper::toDepartment);
  }

  public List<CourseType> courseTypes() {
    return sisDataset("doOnload.do", new LinkedHashMap<>(), "dl_code_curiType", PublicDataMapper::toCourseType);
  }

  public List<SelectiveDomain> selectiveDomains() {
    return sisDataset("doOnload.do", new LinkedHashMap<>(), "dl_code_sltDomain", PublicDataMapper::toSelectiveDomain);
  }

  /** 강의 검색: 학과 코드, 과목명, 교수명은 선택, 모두 비우면 그 학기 전체 (학부 약 2,800행·2.4MB) */
  public List<Lecture> lectures(String categoryCode, String year, String semesterCode,
      String departmentCode, String courseName, String instructorName) {
    Map<String, String> search = termSearch(categoryCode, year, semesterCode);
    LECTURE_EMPTY_FILTER_KEYS.forEach(key -> search.put(key, ""));
    // 공백만 있는 값도 학교는 검색어로 받아 결과가 줄어들므로 빈 값으로 정리
    search.put("DEPT_CD", nullToEmpty(departmentCode).trim());
    search.put("CURI_NM", nullToEmpty(courseName).trim());
    search.put("EMP_NM", nullToEmpty(instructorName).trim());
    search.put("LANG_CHK", "KOR");
    return sisDataset("doList.do", search, "dl_main", PublicDataMapper::toLecture);
  }

  /** 학과 교육과정의 과목 1건: 학과 코드·과목 번호 필수 (없으면 INVALID_INPUT), 그 학과 교육과정에 없으면 빈 목록 */
  public List<CurriculumCourse> curriculumCourses(String departmentCode, String courseNumber) {
    if (isBlank(departmentCode) || isBlank(courseNumber)) {
      throw new SejongAuthException(SejongAuthErrorCode.INVALID_INPUT, "Department code and course number are required");
    }
    String query = queryOf("curiNo", courseNumber, "deptNo", departmentCode);
    return fetcher.fetch(cacheKeyOf("curriculumCourses", departmentCode, courseNumber),
        new PublicDataRequest("GET", CURRICULUM_COURSE_URL + "?" + query, null, null),
        listOf("items", PublicDataMapper::toCurriculumCourse), List.of());
  }

  public List<CalendarEvent> calendar(String startDate, String endDate) {
    return calendar(startDate, endDate, DEFAULT_CALENDAR_CATEGORY_CODE);
  }

  /** 학사 일정 (날짜는 yyyy-MM-dd) */
  public List<CalendarEvent> calendar(String startDate, String endDate, String categoryCode) {
    String query = queryOf("mode", "getCalendarData", "start", startDate, "end", endDate, "collDiv", categoryCode);
    return fetcher.fetch(cacheKeyOf("calendar", categoryCode, startDate, endDate),
        new PublicDataRequest("GET", ACADEMIC_CALENDAR_URL + "?" + query, null, null),
        listOf("data", PublicDataMapper::toCalendarEvent), List.of());
  }

  /** 대학원 사이트 학사일정 (siteId: graduate, gsi, mba, pub, tourgrad, edu, arts 등, 형태가 틀리면 INVALID_INPUT) */
  public List<CalendarEvent> graduateCalendar(String siteId, int year, int month) {
    if (siteId == null || !GRADUATE_SITE_ID_PATTERN.matcher(siteId).matches()) {
      throw new SejongAuthException(SejongAuthErrorCode.INVALID_INPUT,
          "Graduate site id must be lowercase letters, digits and hyphens");
    }
    String query = queryOf("year", String.valueOf(year), "month", String.valueOf(month), "siteId", siteId);
    return fetcher.fetch(cacheKeyOf("graduateCalendar", siteId, String.valueOf(year), String.valueOf(month)),
        new PublicDataRequest("GET", "https://" + siteId + GRADUATE_CALENDAR_PATH + "?" + query, null, null),
        PublicDataMapper::toGraduateCalendarEvents, List.of());
  }

  public List<DormitoryMenuDay> dormitoryMenu() {
    return dormitoryMenu(DEFAULT_DORMITORY_LOCATION);
  }

  public List<DormitoryMenuDay> dormitoryMenu(String location) {
    return fetcher.fetch(cacheKeyOf("dormitoryMenu", location),
        new PublicDataRequest("POST", DORMITORY_MENU_URL, formOf("locgbn", location), FORM_CONTENT_TYPE),
        PublicDataMapper::toDormitoryMenu, List.of());
  }

  public DormitoryNotices dormitoryNotices() {
    return dormitoryNotices(DEFAULT_NOTICE_PAGE, DEFAULT_NOTICE_ROWS, "", DEFAULT_DORMITORY_LOCATION);
  }

  public DormitoryNotices dormitoryNotices(int page, int rows, String keyword, String location) {
    String form = formOf("cPage", String.valueOf(page), "rows", String.valueOf(rows), "bbs_locgbn", location,
        "bbs_id", "notice", "sType", "subject", "sWord", keyword);
    DormitoryNotices emptyNotices = DormitoryNotices.builder().total(0).pinned(List.of()).items(List.of()).build();
    return fetcher.fetch(cacheKeyOf("dormitoryNotices", String.valueOf(page), String.valueOf(rows), keyword, location),
        new PublicDataRequest("POST", DORMITORY_NOTICE_URL, form, FORM_CONTENT_TYPE),
        PublicDataMapper::toDormitoryNotices, emptyNotices);
  }

  public List<CafeteriaMenuItem> cafeteriaMenu(String placeId) {
    String query = queryOf("mode", "getMenuList", "placeId", placeId);
    return fetcher.fetch(cacheKeyOf("cafeteriaMenu", placeId),
        new PublicDataRequest("GET", CAFETERIA_URL + "?" + query, null, null),
        listOf("items", PublicDataMapper::toCafeteriaMenuItem), List.of());
  }

  /**
   * 학식 식당 목록 (번호, 이름, 건물, 위치): 번호 1~10의 메뉴 응답에서 모음, 메뉴가 없는 식당은 빠짐
   * 하나라도 실패하면 목록 전체를 실패로 보고 마지막 캐시 또는 빈 목록 (일부만 하루 캐시하지 않음)
   */
  public List<CafeteriaPlace> cafeteriaPlaces() {
    List<PublicDataRequest> requests = CAFETERIA_PROBE_PLACE_IDS.stream()
        .map(placeId -> new PublicDataRequest("GET", CAFETERIA_URL + "?" + queryOf("mode", "getMenuList", "placeId", placeId), null, null))
        .toList();
    return fetcher.fetchInSequence(cacheKeyOf("cafeteriaPlaces"), requests,
        responses -> PublicDataMapper.toCafeteriaPlaces(CAFETERIA_PROBE_PLACE_IDS, responses), List.of());
  }

  public List<CampusPlace> campusPlaces() {
    return campusPlaces("");
  }

  public List<CampusPlace> campusPlaces(String placeType) {
    String query = queryOf("mode", "getPlaceUserList", "placeType", placeType);
    return fetcher.fetch(cacheKeyOf("campusPlaces", placeType),
        new PublicDataRequest("GET", CAMPUS_MAP_URL + "?" + query, null, null),
        listOf("items", PublicDataMapper::toCampusPlace), List.of());
  }

  public List<ExtracurricularEvent> extracurricularEvents(String startDate, String endDate) {
    return extracurricularEvents(startDate, endDate, "");
  }

  /** 비교과 일정 (날짜는 yyyy-MM-dd, start_year·end_year 는 각 날짜의 연도) */
  public List<ExtracurricularEvent> extracurricularEvents(String startDate, String endDate, String category) {
    String form = formOf("cid", "eco", "category", category, "start", startDate, "end", endDate,
        "start_year", yearOf(startDate), "end_year", yearOf(endDate));
    return fetcher.fetch(cacheKeyOf("extracurricularEvents", startDate, endDate, category),
        new PublicDataRequest("POST", EXTRACURRICULAR_EVENTS_URL, form, FORM_CONTENT_TYPE),
        listOf("events", PublicDataMapper::toExtracurricularEvent), List.of());
  }

  // ---- 요청 조립

  /** SIS 조회 1건: {"dm_search": ...} JSON 본문 > 데이터셋 배열 > 모델 목록 */
  private <T> List<T> sisDataset(String endpoint, Map<String, String> search, String datasetName,
      java.util.function.Function<com.fasterxml.jackson.databind.JsonNode, T> rowMapper) {
    return fetcher.fetch(cacheKeyOf("sis", endpoint, datasetName, search.toString()),
        new PublicDataRequest("POST", SIS_OPEN_TIME_URL + endpoint, sisBodyOf(search), SIS_CONTENT_TYPE),
        listOf(datasetName, rowMapper), List.of());
  }

  private static Map<String, String> termSearch(String categoryCode, String year, String semesterCode) {
    Map<String, String> search = new LinkedHashMap<>();
    search.put("ORGN_CLSF_CD", nullToEmpty(categoryCode));
    search.put("YEAR", nullToEmpty(year));
    search.put("SMT_CD", nullToEmpty(semesterCode));
    search.put("YEAR_SMT", nullToEmpty(year) + "/" + nullToEmpty(semesterCode));
    return search;
  }

  private static String sisBodyOf(Map<String, String> search) {
    try {
      return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(Map.of("dm_search", search));
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalStateException("cannot build SIS body", e);
    }
  }

  private static String queryOf(String... namesAndValues) {
    return encodedPairs(namesAndValues);
  }

  private static String formOf(String... namesAndValues) {
    return encodedPairs(namesAndValues);
  }

  /** name=value&... (UTF-8 퍼센트 인코딩) */
  private static String encodedPairs(String... namesAndValues) {
    StringJoiner joiner = new StringJoiner("&");
    for (int index = 0; index < namesAndValues.length; index += 2) {
      joiner.add(URLEncoder.encode(namesAndValues[index], StandardCharsets.UTF_8) + "="
          + URLEncoder.encode(nullToEmpty(namesAndValues[index + 1]), StandardCharsets.UTF_8));
    }
    return joiner.toString();
  }

  /** 캐시 키: 메서드명|인자... (첫 구분자 앞이 로그용 메서드명) */
  private static String cacheKeyOf(String methodName, String... arguments) {
    return methodName + "|" + String.join("\u0001", java.util.Arrays.stream(arguments).map(SejongPublicData::nullToEmpty).toList());
  }

  private static String yearOf(String date) {
    String text = nullToEmpty(date);
    return text.length() >= YEAR_TEXT_LENGTH ? text.substring(0, YEAR_TEXT_LENGTH) : text;
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
