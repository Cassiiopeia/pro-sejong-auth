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
import kr.suhsaechan.sejongpeia.config.SejongAuthProperties;
import kr.suhsaechan.sejongpeia.exception.SejongAuthErrorCode;
import kr.suhsaechan.sejongpeia.exception.SejongAuthException;
import kr.suhsaechan.sejongpeia.publicdata.model.*;

/**
 * 로그인 없는 학교 공개 데이터 조회 12종: 학사 코드, 강의, 학사 일정, 기숙사, 학식, 캠퍼스 장소, 비교과
 * 메서드당 요청 1건 > 메모리 캐시, 상류 실패는 마지막 캐시 또는 빈 값 (예외는 lectures 입력 검증뿐)
 */
public class SejongPublicData {

  private static final String SIS_OPEN_TIME_URL = "https://sjpt.sejong.ac.kr/sch/sch/ext/SueOpenTimeQ/";
  private static final String ACADEMIC_CALENDAR_URL = "https://www.sejong.ac.kr/kor/academics/academic-calendar.do";
  private static final String CAFETERIA_URL = "https://www.sejong.ac.kr/kor/unilife/cafeteria-info.do";
  private static final String CAMPUS_MAP_URL = "https://www.sejong.ac.kr/kor/intro/campus-map.do";
  private static final String DORMITORY_MENU_URL = "https://happydorm.sejong.ac.kr/food/indexFoodList.do";
  private static final String DORMITORY_NOTICE_URL = "https://happydorm.sejong.ac.kr/bbs/getBbsList.do";
  private static final String EXTRACURRICULAR_EVENTS_URL = "https://do.sejong.ac.kr/ko/process/schedule/getEvents";

  private static final String SIS_CONTENT_TYPE = "application/json; charset=\"UTF-8\"";
  private static final String FORM_CONTENT_TYPE = OkHttpPublicDataHttpCaller.FORM_CONTENT_TYPE;
  private static final String DEFAULT_CALENDAR_CATEGORY_CODE = "0001"; // 학부 일정
  private static final String DEFAULT_DORMITORY_LOCATION = "SJ";
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
        Duration.ofSeconds(properties.getPublicDataCacheTtlSeconds()));
  }

  /** 호출기와 시계 주입 (테스트, 프록시 등) */
  public SejongPublicData(PublicDataHttpCaller httpCaller, Clock clock, Duration cacheTimeToLive) {
    this.fetcher = new PublicDataFetcher(httpCaller, clock, cacheTimeToLive);
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

  /** 강의 검색: 학과 코드, 과목명, 교수명 중 하나 이상 필수 (없으면 INVALID_INPUT) */
  public List<Lecture> lectures(String categoryCode, String year, String semesterCode,
      String departmentCode, String courseName, String instructorName) {
    if (isBlank(departmentCode) && isBlank(courseName) && isBlank(instructorName)) {
      throw new SejongAuthException(SejongAuthErrorCode.INVALID_INPUT,
          "At least one of department code, course name, instructor name is required");
    }
    Map<String, String> search = termSearch(categoryCode, year, semesterCode);
    search.put("DEPT_CD", nullToEmpty(departmentCode));
    search.put("CURI_NM", nullToEmpty(courseName));
    search.put("EMP_NM", nullToEmpty(instructorName));
    search.put("LANG_CHK", "KOR");
    return sisDataset("doList.do", search, "dl_main", PublicDataMapper::toLecture);
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
