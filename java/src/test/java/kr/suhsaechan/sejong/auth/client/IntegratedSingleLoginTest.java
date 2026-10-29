package kr.suhsaechan.sejong.auth.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import kr.suhsaechan.sejong.auth.config.SejongAuthProperties;
import kr.suhsaechan.sejong.auth.model.SejongAuthResult;
import kr.suhsaechan.sejong.auth.parser.SejongClassicReadingParser;
import kr.suhsaechan.sejong.auth.parser.SejongSisParser;
import kr.suhsaechan.sejong.auth.parser.SejongStudentInfoParser;
import kr.suhsaechan.sejong.auth.service.SuhSejongAuthEngine;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * 통합 인증: 포털 로그인 1회로 DHC·SIS 조회, SIS 실패 시 연락처만 null (spec/fixtures 응답, 네트워크 없음)
 */
class IntegratedSingleLoginTest {

  private static final Path FIXTURES = Path.of("..", "spec", "fixtures");
  private static final MediaType HTML = MediaType.get("text/html; charset=UTF-8");

  private final AtomicInteger portalLoginCount = new AtomicInteger();
  private final AtomicInteger portalMainCount = new AtomicInteger();

  // 포털 메인 응답 모양 (정상 / 호출 실패 4종 / 내 정보 영역 없음)
  private enum PortalMainCase { NORMAL, TIMEOUT, HTTP_500, EMPTY_BODY, LOGIN_PAGE, NO_MY_INFO }

  @Test
  void 통합인증은_포털에_한번만_로그인하고_연락처까지_채운다() throws Exception {
    SejongAuthResult result = createEngine(200).authenticate("21011234", "password");

    JsonNode expectedSis = new ObjectMapper().readTree(readFixture("expected_sis.json"));
    assertEquals(1, portalLoginCount.get());
    assertEquals("21011234", result.getStudentId());
    assertEquals(expectedSis.get("email").asText(), result.getEmail());
    assertEquals(expectedSis.get("phoneNumber").asText(), result.getPhoneNumber());
    assertEquals(expectedSis.get("englishName").asText(), result.getEnglishName());
    assertEquals(expectedSis.get("userCategory").asText(), result.getUserCategory());
    assertEquals("공과대학", result.getParentUnitName());
    assertEquals(1, portalMainCount.get());
  }

  @ParameterizedTest
  @EnumSource(value = PortalMainCase.class, names = {"TIMEOUT", "HTTP_500", "EMPTY_BODY", "LOGIN_PAGE"})
  void 포털_메인_호출이_실패하면_상위_소속만_null이고_다른_필드는_그대로(PortalMainCase failureCase) throws Exception {
    SejongAuthResult result = createEngine(200, failureCase).authenticate("21011234", "password");

    assertAll(
        () -> assertNull(result.getParentUnitName()),
        () -> assertEquals("student@example.com", result.getEmail()),
        () -> assertEquals("학부생", result.getUserCategory()),
        () -> assertEquals("홍길동", result.getName()),
        () -> assertEquals("6 학기", result.getClassicReading().getCompletedSemesters()),
        // 보조 조회는 재시도 없이 1회
        () -> assertEquals(1, portalMainCount.get()));
  }

  @Test
  void 포털_메인에_내_정보_영역이_없으면_null이_아니라_빈_문자열() throws Exception {
    assertEquals("", createEngine(200, PortalMainCase.NO_MY_INFO).authenticate("21011234", "password").getParentUnitName());
  }

  @Test
  void SIS가_실패해도_DHC_결과를_돌려주고_연락처는_null() throws Exception {
    SejongAuthResult result = createEngine(500).authenticate("21011234", "password");

    assertEquals(1, portalLoginCount.get());
    assertEquals("21011234", result.getStudentId());
    assertNull(result.getEmail());
    assertNull(result.getPhoneNumber());
    assertNull(result.getUserCategory());
  }

  // 가짜 포털: 로그인 OK > classic SSO·고전독서 페이지 > sjpt SSO·initUserInfo (sisStatusCode)
  private SuhSejongAuthEngine createEngine(int sisStatusCode) throws Exception {
    return createEngine(sisStatusCode, PortalMainCase.NORMAL);
  }

  private SuhSejongAuthEngine createEngine(int sisStatusCode, PortalMainCase portalMainCase) throws Exception {
    String loginOkHtml = readFixture("portal_login_ok.html");
    String dhcHtml = readFixture("dhc_status.html");
    String sisJson = readFixture("sis_user_info.json");
    String portalMainHtml = readFixture("portal_main.html");
    OkHttpClient fakePortalClient = new OkHttpClient.Builder()
        .addInterceptor(chain -> {
          String url = chain.request().url().toString();
          String body = "";
          int code = 200;
          if (url.contains("login_action.jsp")) {
            portalLoginCount.incrementAndGet();
            body = loginOkHtml;
          } else if (url.contains("reading/status.do")) {
            body = dhcHtml;
          } else if (url.contains("initUserInfo.do")) {
            code = sisStatusCode;
            body = sisJson;
          } else if (url.contains("user/index.do")) {
            portalMainCount.incrementAndGet();
            switch (portalMainCase) {
              case TIMEOUT -> throw new SocketTimeoutException("timeout");
              case HTTP_500 -> code = 500;
              case EMPTY_BODY -> body = "";
              case LOGIN_PAGE -> body = "<html><form action='/jsp/login/login_action.jsp'>통합 로그인</form></html>";
              case NO_MY_INFO -> body = "<html><body>교직원 화면</body></html>";
              default -> body = portalMainHtml;
            }
          }
          return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
              .code(code).message("fake").body(ResponseBody.create(body, HTML)).build();
        })
        .build();

    SejongAuthProperties properties = new SejongAuthProperties();
    return new SuhSejongAuthEngine(new SejongPortalClient(properties, fakePortalClient), new SejongStudentInfoParser(),
        new SejongClassicReadingParser(), new SejongSisClient(properties, fakePortalClient), new SejongSisParser());
  }

  private String readFixture(String fileName) throws Exception {
    return Files.readString(FIXTURES.resolve(fileName), StandardCharsets.UTF_8);
  }
}
