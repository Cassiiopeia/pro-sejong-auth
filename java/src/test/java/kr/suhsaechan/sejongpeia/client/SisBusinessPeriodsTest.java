package kr.suhsaechan.sejongpeia.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import kr.suhsaechan.sejongpeia.config.SejongAuthProperties;
import kr.suhsaechan.sejongpeia.exception.SejongAuthErrorCode;
import kr.suhsaechan.sejongpeia.exception.SejongAuthException;
import kr.suhsaechan.sejongpeia.model.SisBusinessPeriod;
import kr.suhsaechan.sejongpeia.parser.SejongClassicReadingParser;
import kr.suhsaechan.sejongpeia.parser.SejongSisParser;
import kr.suhsaechan.sejongpeia.parser.SejongStudentInfoParser;
import kr.suhsaechan.sejongpeia.service.SuhSejongAuthEngine;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;

/**
 * SIS 학사 업무 기간: 브라우저와 같은 요청 흐름·실행 파라미터, 세션 종료 시 재시도 없이 중단, 잘못된 입력은 요청 없음 (spec/fixtures 응답, 네트워크 없음)
 */
class SisBusinessPeriodsTest {

  private static final Path FIXTURES = Path.of("..", "spec", "fixtures");
  private static final MediaType JSON = MediaType.get("application/json; charset=UTF-8");
  private static final String PROGRAM_KEY = "SELF_STUDSELF_SUB_20SELF_MENU_10SudBaseScheSelfServQ";

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final List<String[]> requests = new ArrayList<>(); // {url, body}
  private String listBody;

  @Test
  void 브라우저와_같은_순서와_실행_파라미터로_요청한다() throws Exception {
    listBody = readFixture("sis_business_periods.json");

    List<SisBusinessPeriod> periods = createEngine().fetchSisBusinessPeriods("21011234", "password", "2099", "3");

    // 로그인 > SSO > 사용자 정보 > 메뉴 목록 > 화면 열기 > 목록 (포털 로그인 1회)
    List<String> paths = requests.stream().map(request -> request[0].split("\\?")[0].replaceFirst("^https://[^/]+", "")).toList();
    assertEquals(List.of("/jsp/login/login_action.jsp", "/main/view/Login/doSsoLogin.do", "/main/sys/UserInfo/initUserInfo.do",
        "/main/view/Menu/doListUserMenuListLeft.do", "/main/sys/UserRole/initUserRole.do",
        "/sch/sch/sud/SudBaseScheSelfServQ/doList.do"), paths);
    String[] menuRequest = requests.get(3);
    String[] roleRequest = requests.get(4);
    String[] listRequest = requests.get(5);
    assertEquals(objectMapper.readTree("{\"dm_ReqLeftMenu\":{\"MENU_SYS_ID\":\"SYS1\",\"SYSTEM_DIV\":\"SCH\",\"MENU_SYS_NM\":\"학사\"}}"),
        objectMapper.readTree(menuRequest[1]));
    // 화면 열기: 본문과 addParam 이 같은 실행 파라미터 (사용자 정보 응답의 값)
    JsonNode expectedRole = objectMapper.readTree("{\"pbForceLog\":\"false\",\"_runPgmKey\":\"" + PROGRAM_KEY
        + "\",\"_runSysKey\":\"SCH\",\"_runIntgUsrNo\":\"21011234\",\"_runPgLoginDt\":\"20991030120000\","
        + "\"_runningSejong\":\"synthetic-session-value\"}");
    assertEquals(expectedRole, objectMapper.readTree(roleRequest[1]));
    assertEquals(expectedRole, decodeAddParam(roleRequest[0]));
    // 목록: 화면 로그인 일시(PG_LOGIN_DT), 조직·사용자 구분 코드는 SIS 사용자 정보에서
    assertEquals("20991030120005", decodeAddParam(listRequest[0]).get("_runPgLoginDt").asText());
    assertEquals(objectMapper.readTree("{\"dm_search\":{\"ORGN_CLSF_CD\":\"20\",\"YEAR\":\"2099\",\"SMT_CD\":\"\",\"SMT_NM\":\"\","
        + "\"USER_CLSFM_CD\":\"20\",\"MONTH\":\"3\"}}"), objectMapper.readTree(listRequest[1]));

    JsonNode expected = objectMapper.readTree(readFixture("expected_sis_business_periods.json"));
    assertEquals(expected.size(), periods.size());
    for (int index = 0; index < periods.size(); index++) {
      JsonNode expectedRow = expected.get(index);
      SisBusinessPeriod period = periods.get(index);
      assertEquals(expectedRow.get("scheduleName").asText(), period.scheduleName());
      assertEquals(expectedRow.get("startDate").asText(), period.startDate());
      assertEquals(expectedRow.get("timeText").asText(), period.timeText());
      assertEquals(expectedRow.get("sortOrder").asText(), period.sortOrder());
      assertEquals(expectedRow.get("extra").size(), period.extra().size());
    }
  }

  @Test
  void 학교가_세션을_끝내면_재시도_없이_SESSION_ERROR로_중단한다() throws Exception {
    listBody = readFixture("sis_submit_error_logout.json");

    SejongAuthException exception = assertThrows(SejongAuthException.class,
        () -> createEngine().fetchSisBusinessPeriods("21011234", "password", "2099", ""));

    assertEquals(SejongAuthErrorCode.SESSION_ERROR, exception.getErrorCode());
    assertEquals(1, requests.stream().filter(request -> request[0].contains("SudBaseScheSelfServQ/doList.do")).count());
  }

  @Test
  void 잘못된_연도와_월은_요청_없이_INVALID_INPUT() throws Exception {
    for (String[] yearAndMonth : new String[][] {{"", ""}, {"26", ""}, {"2099", "13"}, {"2099", "ab"}, {null, ""}}) {
      SejongAuthException exception = assertThrows(SejongAuthException.class,
          () -> createEngine().fetchSisBusinessPeriods("21011234", "password", yearAndMonth[0], yearAndMonth[1]));
      assertEquals(SejongAuthErrorCode.INVALID_INPUT, exception.getErrorCode());
    }
    assertTrue(requests.isEmpty());
  }

  private JsonNode decodeAddParam(String url) throws Exception {
    String addParam = url.substring(url.indexOf("addParam=") + "addParam=".length());
    String urlEncoded = new String(Base64.getDecoder().decode(addParam), StandardCharsets.UTF_8);
    return objectMapper.readTree(URLDecoder.decode(urlEncoded, StandardCharsets.UTF_8));
  }

  // 가짜 SIS: 로그인 OK > SSO > 사용자 정보 > 메뉴 목록 > 화면 열기 > 목록 (listBody)
  private SuhSejongAuthEngine createEngine() throws Exception {
    String loginOkHtml = readFixture("portal_login_ok.html");
    String userInfoJson = readFixture("sis_user_info.json");
    String roleJson = readFixture("sis_user_role.json");
    OkHttpClient fakeClient = new OkHttpClient.Builder()
        .addInterceptor(chain -> {
          String url = chain.request().url().toString();
          String requestBody = "";
          if (chain.request().body() != null) {
            okio.Buffer buffer = new okio.Buffer();
            chain.request().body().writeTo(buffer);
            requestBody = buffer.readUtf8();
          }
          requests.add(new String[] {url, requestBody});
          String body = "{}";
          if (url.contains("login_action.jsp")) {
            body = loginOkHtml;
          } else if (url.contains("initUserInfo.do")) {
            body = userInfoJson;
          } else if (url.contains("doListUserMenuListLeft.do")) {
            body = "{\"dl_UserMenuListLeft\":[]}";
          } else if (url.contains("initUserRole.do")) {
            body = roleJson;
          } else if (url.contains("SudBaseScheSelfServQ/doList.do")) {
            body = listBody;
          }
          return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
              .code(200).message("fake").body(ResponseBody.create(body, JSON)).build();
        })
        .build();
    SejongAuthProperties properties = new SejongAuthProperties();
    return new SuhSejongAuthEngine(new SejongPortalClient(properties, fakeClient), new SejongStudentInfoParser(),
        new SejongClassicReadingParser(), new SejongSisClient(properties, fakeClient), new SejongSisParser());
  }

  private String readFixture(String fileName) throws Exception {
    return Files.readString(FIXTURES.resolve(fileName), StandardCharsets.UTF_8);
  }
}
