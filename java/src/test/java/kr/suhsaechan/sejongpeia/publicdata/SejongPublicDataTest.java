package kr.suhsaechan.sejongpeia.publicdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import kr.suhsaechan.sejongpeia.exception.SejongAuthErrorCode;
import kr.suhsaechan.sejongpeia.exception.SejongAuthException;
import kr.suhsaechan.sejongpeia.publicdata.model.CafeteriaPlace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

/**
 * 공개 데이터 12종: fixture 주입 가짜 HTTP 결과 == expected_public_data.json, 캐시, 실패 격리, 입력 검증, 요청 형태
 */
class SejongPublicDataTest {

  private static final Path FIXTURE_DIRECTORY = Path.of("..", "spec", "fixtures", "public_data");
  private static final Duration CACHE_TIME_TO_LIVE = Duration.ofSeconds(100);

  private final ObjectMapper snakeCaseMapper = new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
  private final ObjectMapper plainMapper = new ObjectMapper();

  private JsonNode expected;
  private FakeCaller caller;
  private MutableClock clock;
  private SejongPublicData publicData;

  /** 가짜 HTTP: URL 에 따라 fixture 응답, 요청 기록 */
  private static class FakeCaller implements PublicDataHttpCaller {
    final List<PublicDataRequest> requests = new ArrayList<>();
    final AtomicInteger callCount = new AtomicInteger();
    volatile int statusCode = 200;
    volatile String bodyOverride;
    volatile boolean isConnectionBroken;
    // 이 문자열이 URL 에 있는 요청만 연결 실패
    volatile String brokenUrlPart;
    // 있으면 fixture 대신 이 함수의 본문을 응답
    volatile java.util.function.Function<PublicDataRequest, String> bodyProvider;
    volatile CountDownLatch holdLatch;

    @Override
    public PublicDataHttpResponse call(PublicDataRequest request) throws IOException {
      synchronized (requests) {
        requests.add(request);
      }
      callCount.incrementAndGet();
      if (holdLatch != null) {
        try {
          holdLatch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
      if (isConnectionBroken || (brokenUrlPart != null && request.getUrl().contains(brokenUrlPart))) {
        throw new IOException("connection refused");
      }
      String body = bodyProvider != null ? bodyProvider.apply(request)
          : bodyOverride != null ? bodyOverride : fixtureBodyOf(request);
      return new PublicDataHttpResponse(statusCode, body);
    }

    PublicDataRequest lastRequest() {
      return requests.get(requests.size() - 1);
    }
  }

  private static class MutableClock extends Clock {
    private Instant now = Instant.parse("2099-01-01T00:00:00Z");

    void advanceSeconds(long seconds) {
      now = now.plusSeconds(seconds);
    }

    @Override
    public java.time.ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(java.time.ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }

  @BeforeEach
  void setUp() throws Exception {
    expected = plainMapper.readTree(readFixture("expected_public_data.json"));
    caller = new FakeCaller();
    clock = new MutableClock();
    publicData = new SejongPublicData(caller, clock, CACHE_TIME_TO_LIVE);
  }

  // ---- 결과 == expected

  @Test
  void organization_categories_match_expected() {
    assertMatches("organization_categories", publicData.organizationCategories());
  }

  @Test
  void terms_match_expected() {
    assertMatches("terms", publicData.terms("20"));
  }

  @Test
  void departments_match_expected() {
    assertMatches("departments", publicData.departments("20", "2099", "20"));
  }

  @Test
  void course_types_match_expected() {
    assertMatches("course_types", publicData.courseTypes());
  }

  @Test
  void selective_domains_match_expected() {
    assertMatches("selective_domains", publicData.selectiveDomains());
  }

  @Test
  void lectures_match_expected() {
    assertMatches("lectures", publicData.lectures("20", "2099", "20", "9101", "", ""));
  }

  @Test
  void curriculum_courses_match_expected_and_follow_request_contract() {
    assertMatches("curriculum_courses", publicData.curriculumCourses("9101", "900001"));
    assertEquals("GET", caller.lastRequest().getHttpMethod());
    assertEquals("https://www.sejong.ac.kr/curriculum/getCurriculumOneKor.do?curiNo=900001&deptNo=9101",
        caller.lastRequest().getUrl());
  }

  @Test
  void curriculum_courses_reject_blank_input_without_request_and_accept_empty_items() {
    int callCountBefore = caller.callCount.get();
    for (String[] blankPair : new String[][] {{"", "900001"}, {"9101", "  "}, {null, "900001"}}) {
      SejongAuthException exception = assertThrows(SejongAuthException.class,
          () -> publicData.curriculumCourses(blankPair[0], blankPair[1]));
      assertEquals(SejongAuthErrorCode.INVALID_INPUT, exception.getErrorCode());
    }
    assertEquals(callCountBefore, caller.callCount.get());
    // 그 학과 교육과정에 없는 과목은 빈 items 로 오며 정상 응답
    caller.bodyOverride = "{\"success\":true,\"items\":[]}";
    assertTrue(new SejongPublicData(caller, clock, CACHE_TIME_TO_LIVE).curriculumCourses("9101", "000000").isEmpty());
  }

  @Test
  void calendar_matches_expected() {
    assertMatches("calendar", publicData.calendar("2099-10-01", "2099-10-31"));
  }

  @Test
  void graduate_calendar_matches_expected_and_follows_request_contract() {
    assertMatches("graduate_calendar", publicData.graduateCalendar("graduate", 2099, 10));
    assertEquals("GET", caller.lastRequest().getHttpMethod());
    assertEquals("https://graduate.sejong.ac.kr/_custom/sejong/_app/calendar-graduate/proc/getCalendarData.jsp"
        + "?year=2099&month=10&siteId=graduate", caller.lastRequest().getUrl());
  }

  @Test
  void graduate_calendar_rejects_bad_site_id_and_isolates_success_false() {
    for (String badSiteId : new String[] {"", "  ", "evil.example.com/x", "Graduate", null}) {
      SejongAuthException exception = assertThrows(SejongAuthException.class,
          () -> publicData.graduateCalendar(badSiteId, 2099, 10));
      assertEquals(SejongAuthErrorCode.INVALID_INPUT, exception.getErrorCode());
    }
    assertEquals(0, caller.callCount.get());
    caller.bodyOverride = "{\"success\":false,\"items\":\"[]\"}";
    assertTrue(new SejongPublicData(caller, clock, CACHE_TIME_TO_LIVE).graduateCalendar("graduate", 2099, 1).isEmpty());
    caller.bodyOverride = "{\"success\":true,\"items\":\"[]\"}";
    assertTrue(new SejongPublicData(caller, clock, CACHE_TIME_TO_LIVE).graduateCalendar("arts", 2099, 1).isEmpty());
  }

  @Test
  void dormitory_menu_matches_expected() {
    assertMatches("dormitory_menu", publicData.dormitoryMenu());
  }

  @Test
  void dormitory_notices_match_expected() {
    assertMatches("dormitory_notices", publicData.dormitoryNotices());
  }

  @Test
  void cafeteria_menu_matches_expected() {
    assertMatches("cafeteria_menu", publicData.cafeteriaMenu("1"));
  }

  @Test
  void cafeteria_places_match_expected() {
    assertMatches("cafeteria_places", publicData.cafeteriaPlaces());
  }

  @Test
  void cafeteria_places_collects_each_place_once_in_id_order_with_bounded_requests() throws Exception {
    caller.bodyProvider = request -> {
      String placeId = request.getUrl().substring(request.getUrl().lastIndexOf('=') + 1);
      return switch (placeId) {
        case "1" -> cafeteriaBody(cafeteriaRow(1, "첫째", ""), cafeteriaRow(1, "첫째", ""));
        case "3" -> cafeteriaBody(cafeteriaRow(3, "셋째", ",\"operatingHours\":\"예시\""));
        case "6" -> cafeteriaBody(cafeteriaRow(6, "여섯째", ""));
        default -> "{\"items\":[]}";
      };
    };
    List<CafeteriaPlace> places = publicData.cafeteriaPlaces();
    assertEquals(List.of("1", "3", "6"), places.stream().map(CafeteriaPlace::getPlaceId).toList());
    assertEquals(List.of("첫째", "셋째", "여섯째"), places.stream().map(CafeteriaPlace::getPlaceName).toList());
    assertEquals(java.util.Map.of(), places.get(0).getExtra());
    assertEquals(java.util.Map.of("operatingHours", "예시"), places.get(1).getExtra());
    // 번호 1~10을 순서대로 한 번씩
    List<String> requestedUrls = caller.requests.stream().map(PublicDataRequest::getUrl).toList();
    assertEquals(10, requestedUrls.size());
    for (int number = 1; number <= 10; number++) {
      assertTrue(requestedUrls.get(number - 1).endsWith("&placeId=" + number), requestedUrls.get(number - 1));
    }
    publicData.cafeteriaPlaces();
    assertEquals(10, caller.callCount.get()); // 캐시 유효 중에는 요청 없음
  }

  @Test
  void cafeteria_places_one_failed_request_fails_the_whole_list_and_keeps_stale_value() {
    List<CafeteriaPlace> first = publicData.cafeteriaPlaces();
    assertEquals(1, first.size());
    caller.brokenUrlPart = "placeId=4";
    clock.advanceSeconds(CACHE_TIME_TO_LIVE.getSeconds() + 1);
    // 일부만 성공한 목록을 새 값으로 저장하지 않음
    assertEquals(first.get(0).getPlaceId(), publicData.cafeteriaPlaces().get(0).getPlaceId());
    assertTrue(new SejongPublicData(caller, clock, CACHE_TIME_TO_LIVE).cafeteriaPlaces().isEmpty());
  }

  private static String cafeteriaRow(int placeId, String placeName, String extraFields) {
    return "{\"placeId\":" + placeId + ",\"placeName\":\"" + placeName + "\",\"buildingName\":\"예시관\",\"location\":\"B1층\","
        + "\"menuName\":\"예시메뉴\",\"price\":1000" + extraFields + "}";
  }

  private static String cafeteriaBody(String... rows) {
    return "{\"items\":[" + String.join(",", rows) + "]}";
  }

  @Test
  void campus_places_match_expected() {
    assertMatches("campus_places", publicData.campusPlaces());
  }

  @Test
  void extracurricular_events_match_expected() {
    assertMatches("extracurricular_events", publicData.extracurricularEvents("2099-10-01", "2099-10-31"));
  }

  @Test
  void extra_never_contains_deny_listed_fields() throws Exception {
    String json = snakeCaseMapper.writeValueAsString(publicData.organizationCategories())
        + snakeCaseMapper.writeValueAsString(publicData.dormitoryNotices());
    for (String denied : List.of("CD_ALIAS", "regname", "regid", "STACK_TRACE", "가상담당자")) {
      assertFalse(json.contains(denied), denied);
    }
  }

  // ---- 요청 형태

  @Test
  void sis_requests_use_json_body_and_endpoint() throws Exception {
    publicData.departments("20", "2099", "20");
    PublicDataRequest request = caller.lastRequest();
    assertEquals("POST", request.getHttpMethod());
    assertEquals("https://sjpt.sejong.ac.kr/sch/sch/ext/SueOpenTimeQ/doLesndeptgroup.do", request.getUrl());
    assertEquals("application/json; charset=\"UTF-8\"", request.getContentType());
    assertEquals(plainMapper.readTree("{\"dm_search\":{\"ORGN_CLSF_CD\":\"20\",\"YEAR\":\"2099\",\"SMT_CD\":\"20\","
        + "\"YEAR_SMT\":\"2099/20\"}}"), plainMapper.readTree(request.getBody()));
  }

  @Test
  void lectures_request_has_all_filters_and_korean_language() throws Exception {
    publicData.lectures("20", "2099", "10", "", "예시", "");
    JsonNode search = plainMapper.readTree(caller.lastRequest().getBody()).get("dm_search");
    assertEquals("2099/10", search.get("YEAR_SMT").asText());
    assertEquals("", search.get("DEPT_CD").asText());
    assertEquals("예시", search.get("CURI_NM").asText());
    assertEquals("", search.get("EMP_NM").asText());
    assertEquals("KOR", search.get("LANG_CHK").asText());
    // 학교가 필터 키 전체를 요구함 (하나라도 빠지면 _SUBMIT_ERROR_)
    for (String key : List.of("SMT_NM", "CURI_TYPE_CD", "SLT_DOMAIN_CD", "CURI_NO", "CLASS", "SLT_DEPT_CD", "PLAN_REG_YN", "CORS_SCHE_GRP_CD")) {
      assertTrue(search.has(key), key);
    }
    assertTrue(caller.lastRequest().getUrl().endsWith("/doList.do"));
  }

  @Test
  void onload_requests_send_empty_search() throws Exception {
    publicData.organizationCategories();
    assertEquals(plainMapper.readTree("{\"dm_search\":{}}"), plainMapper.readTree(caller.lastRequest().getBody()));
    assertTrue(caller.lastRequest().getUrl().endsWith("/doOnload.do"));
  }

  @Test
  void get_requests_have_contract_urls() {
    publicData.calendar("2099-10-01", "2099-10-31");
    assertEquals("GET", caller.lastRequest().getHttpMethod());
    assertEquals("https://www.sejong.ac.kr/kor/academics/academic-calendar.do?mode=getCalendarData"
        + "&start=2099-10-01&end=2099-10-31&collDiv=0001", caller.lastRequest().getUrl());
    publicData.cafeteriaMenu("7");
    assertEquals("https://www.sejong.ac.kr/kor/unilife/cafeteria-info.do?mode=getMenuList&placeId=7", caller.lastRequest().getUrl());
    publicData.campusPlaces("BUILDING");
    assertEquals("https://www.sejong.ac.kr/kor/intro/campus-map.do?mode=getPlaceUserList&placeType=BUILDING",
        caller.lastRequest().getUrl());
  }

  @Test
  void form_requests_have_contract_bodies() {
    publicData.dormitoryMenu();
    assertEquals("https://happydorm.sejong.ac.kr/food/indexFoodList.do", caller.lastRequest().getUrl());
    assertEquals("locgbn=SJ", caller.lastRequest().getBody());
    assertEquals("application/x-www-form-urlencoded; charset=UTF-8", caller.lastRequest().getContentType());

    publicData.dormitoryNotices(2, 5, "공지", "SJ");
    assertEquals("https://happydorm.sejong.ac.kr/bbs/getBbsList.do", caller.lastRequest().getUrl());
    assertEquals("cPage=2&rows=5&bbs_locgbn=SJ&bbs_id=notice&sType=subject&sWord="
        + java.net.URLEncoder.encode("공지", StandardCharsets.UTF_8), caller.lastRequest().getBody());

    publicData.extracurricularEvents("2099-12-20", "2100-01-10", "특강");
    assertEquals("https://do.sejong.ac.kr/ko/process/schedule/getEvents", caller.lastRequest().getUrl());
    assertEquals("cid=eco&category=" + java.net.URLEncoder.encode("특강", StandardCharsets.UTF_8)
        + "&start=2099-12-20&end=2100-01-10&start_year=2099&end_year=2100", caller.lastRequest().getBody());
    assertEquals("category=특강", URLDecoder.decode(caller.lastRequest().getBody().split("&")[1], StandardCharsets.UTF_8));
  }

  // ---- 캐시

  @Test
  void fresh_cache_skips_request_and_expired_cache_refreshes() {
    publicData.courseTypes();
    publicData.courseTypes();
    assertEquals(1, caller.callCount.get());
    clock.advanceSeconds(99);
    publicData.courseTypes();
    assertEquals(1, caller.callCount.get());
    clock.advanceSeconds(2);
    publicData.courseTypes();
    assertEquals(2, caller.callCount.get());
  }

  @Test
  void different_arguments_use_different_cache_entries() {
    publicData.cafeteriaMenu("1");
    publicData.cafeteriaMenu("2");
    assertEquals(2, caller.callCount.get());
  }

  @Test
  void expired_cache_is_returned_when_refresh_fails() throws Exception {
    List<?> first = publicData.courseTypes();
    clock.advanceSeconds(1000);
    caller.isConnectionBroken = true;
    List<?> stale = publicData.courseTypes();
    assertEquals(2, caller.callCount.get());
    assertEquals(snakeCaseMapper.valueToTree(first), snakeCaseMapper.valueToTree(stale));
    assertEquals(1, stale.size());
  }

  @Test
  void concurrent_calls_with_same_arguments_share_one_request() throws Exception {
    caller.holdLatch = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(4);
    try {
      List<Future<?>> futures = new ArrayList<>();
      for (int index = 0; index < 4; index++) {
        futures.add(executor.submit(() -> publicData.courseTypes()));
      }
      Thread.sleep(300);
      caller.holdLatch.countDown();
      for (Future<?> future : futures) {
        assertEquals(1, ((List<?>) future.get(5, TimeUnit.SECONDS)).size());
      }
      assertEquals(1, caller.callCount.get());
    } finally {
      executor.shutdownNow();
    }
  }

  // ---- 실패 격리

  @Test
  void upstream_failures_return_empty_values_without_exception() {
    String[] failingBodies = {"not json", "{\"unexpected\":[]}", "{\"_SUBMIT_ERROR_\":{\"STACK_TRACE\":\"secret\"}}", "[]", ""};
    for (String body : failingBodies) {
      caller.bodyOverride = body;
      assertEquals(0, new SejongPublicData(caller, clock, CACHE_TIME_TO_LIVE).courseTypes().size(), body);
      assertEquals(0, new SejongPublicData(caller, clock, CACHE_TIME_TO_LIVE).dormitoryMenu().size(), body);
    }
  }

  @Test
  void non_success_status_and_connection_failure_are_isolated() {
    caller.statusCode = 500;
    assertTrue(publicData.calendar("2099-10-01", "2099-10-31").isEmpty());
    caller.statusCode = 200;
    caller.isConnectionBroken = true;
    assertTrue(publicData.campusPlaces().isEmpty());
    assertTrue(publicData.extracurricularEvents("2099-10-01", "2099-10-31").isEmpty());
    var notices = publicData.dormitoryNotices();
    assertEquals(0, notices.getTotal());
    assertTrue(notices.getPinned().isEmpty() && notices.getItems().isEmpty());
  }

  @Test
  void failure_is_remembered_briefly_and_then_retried() {
    caller.isConnectionBroken = true;
    assertTrue(publicData.courseTypes().isEmpty());
    caller.isConnectionBroken = false;
    // 실패 기억 60초 안: 요청 없이 빈 값
    clock.advanceSeconds(59);
    assertTrue(publicData.courseTypes().isEmpty());
    assertEquals(1, caller.callCount.get());
    // 기억 만료 후 재시도
    clock.advanceSeconds(2);
    assertEquals(1, publicData.courseTypes().size());
    assertEquals(2, caller.callCount.get());
  }

  @Test
  void dormitory_notices_without_result_arrays_are_empty_not_failure() {
    caller.bodyOverride = "{\"root\":[{}]}";
    var notices = publicData.dormitoryNotices();
    assertEquals(0, notices.getTotal());
    assertTrue(notices.getItems().isEmpty());
    // 정상 응답으로 캐시됨 (실패 기억 아님): 만료 후 재요청 없이 같은 결과
    publicData.dormitoryNotices();
    assertEquals(1, caller.callCount.get());
  }

  @Test
  void cache_evicts_oldest_entry_when_full() {
    SejongPublicData smallCache = new SejongPublicData(caller, clock, CACHE_TIME_TO_LIVE, Duration.ofSeconds(60), 2);
    smallCache.cafeteriaMenu("1");
    smallCache.cafeteriaMenu("2");
    smallCache.cafeteriaMenu("3");
    assertEquals(3, caller.callCount.get());
    smallCache.cafeteriaMenu("3");
    smallCache.cafeteriaMenu("2");
    assertEquals(3, caller.callCount.get());
    // 가장 오래된 "1" 은 제거돼 다시 요청
    smallCache.cafeteriaMenu("1");
    assertEquals(4, caller.callCount.get());
  }

  // ---- 입력 검증

  @Test
  void lectures_without_any_filter_lists_whole_semester_with_one_request() throws Exception {
    // 학교 서버는 검색 값이 모두 비어도 그 학기 전체를 돌려줌 (2026-10-30 확인)
    assertFalse(publicData.lectures("20", "2099", "20", "", "  ", null).isEmpty());
    assertEquals(1, caller.callCount.get());
    JsonNode search = plainMapper.readTree(caller.lastRequest().getBody()).get("dm_search");
    assertEquals("", search.get("DEPT_CD").asText());
    assertEquals("", search.get("CURI_NM").asText());
    assertEquals("", search.get("EMP_NM").asText());
  }

  // ---- 라이브 (환경변수 없으면 건너뜀)

  @Test
  void live_course_types_have_codes() {
    Assumptions.assumeTrue("true".equals(System.getenv("SEJONG_LIVE_PUBLIC_DATA")), "SEJONG_LIVE_PUBLIC_DATA not set");
    assertFalse(new SejongPublicData().courseTypes().isEmpty());
  }

  // ---- 헬퍼

  private void assertMatches(String expectedKey, Object actual) {
    assertEquals(expected.get(expectedKey), snakeCaseMapper.valueToTree(actual), expectedKey);
  }

  private static String readFixture(String fileName) throws IOException {
    return Files.readString(FIXTURE_DIRECTORY.resolve(fileName), StandardCharsets.UTF_8);
  }

  private static String fixtureBodyOf(PublicDataRequest request) throws IOException {
    String url = request.getUrl();
    if (url.endsWith("doOnload.do")) {
      return readFixture("reference_onload.json");
    } else if (url.endsWith("doYearsmtlist.do")) {
      return readFixture("reference_terms.json");
    } else if (url.endsWith("doLesndeptgroup.do")) {
      return readFixture("reference_departments.json");
    } else if (url.endsWith("doList.do")) {
      return readFixture("lectures.json");
    } else if (url.contains("getCurriculumOneKor.do")) {
      return readFixture("curriculum_course.json");
    } else if (url.contains("academic-calendar.do")) {
      return readFixture("calendar.json");
    } else if (url.contains("calendar-graduate")) {
      return readFixture("graduate_calendar.json");
    } else if (url.contains("indexFoodList.do")) {
      return readFixture("dormitory_menu.json");
    } else if (url.contains("getBbsList.do")) {
      return readFixture("dormitory_notices.json");
    } else if (url.contains("cafeteria-info.do")) {
      return readFixture("cafeteria.json");
    } else if (url.contains("campus-map.do")) {
      return readFixture("campus_places.json");
    } else if (url.contains("getEvents")) {
      return readFixture("extracurricular_events.json");
    }
    throw new IOException("unexpected url " + url);
  }
}
