package kr.suhsaechan.sejongpeia.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import kr.suhsaechan.sejongpeia.config.SejongAuthProperties;
import kr.suhsaechan.sejongpeia.model.SejongAuthResult;
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
 * 통합 인증: 포털 로그인 1회로 DHC·SIS 조회, SIS 실패 시 연락처만 null (spec/fixtures 응답, 네트워크 없음)
 */
class IntegratedSingleLoginTest {

  private static final Path FIXTURES = Path.of("..", "spec", "fixtures");
  private static final MediaType HTML = MediaType.get("text/html; charset=UTF-8");

  private final AtomicInteger portalLoginCount = new AtomicInteger();

  @Test
  void 통합인증은_포털에_한번만_로그인하고_연락처까지_채운다() throws Exception {
    SejongAuthResult result = createEngine(200).authenticate("21011234", "password");

    JsonNode expectedSis = new ObjectMapper().readTree(readFixture("expected_sis.json"));
    assertEquals(1, portalLoginCount.get());
    assertEquals("21011234", result.getStudentId());
    assertEquals(expectedSis.get("email").asText(), result.getEmail());
    assertEquals(expectedSis.get("phoneNumber").asText(), result.getPhoneNumber());
    assertEquals(expectedSis.get("englishName").asText(), result.getEnglishName());
  }

  @Test
  void SIS가_실패해도_DHC_결과를_돌려주고_연락처는_null() throws Exception {
    SejongAuthResult result = createEngine(500).authenticate("21011234", "password");

    assertEquals(1, portalLoginCount.get());
    assertEquals("21011234", result.getStudentId());
    assertNull(result.getEmail());
    assertNull(result.getPhoneNumber());
  }

  // 가짜 포털: 로그인 OK > classic SSO·고전독서 페이지 > sjpt SSO·initUserInfo (sisStatusCode)
  private SuhSejongAuthEngine createEngine(int sisStatusCode) throws Exception {
    String loginOkHtml = readFixture("portal_login_ok.html");
    String dhcHtml = readFixture("dhc_status.html");
    String sisJson = readFixture("sis_user_info.json");
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
