package kr.suhsaechan.sejongpeia.client;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import kr.suhsaechan.sejongpeia.config.SejongAuthProperties;
import kr.suhsaechan.sejongpeia.exception.SejongAuthErrorCode;
import kr.suhsaechan.sejongpeia.exception.SejongAuthException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;

/**
 * 포털 로그인 판정값 > 에러 코드 (포털 화면과 같은 분기, spec/fixtures/expected_portal_login.json)
 */
class PortalLoginResultTest {

  private static final Path FIXTURE_DIRECTORY = Path.of("..", "spec", "fixtures");

  @Test
  void 포털_판정값이_포털_화면과_같은_에러코드가_된다() throws Exception {
    JsonNode expected = new ObjectMapper().readTree(readFixture("expected_portal_login.json"));
    for (Iterator<Map.Entry<String, JsonNode>> fields = expected.fields(); fields.hasNext(); ) {
      Map.Entry<String, JsonNode> field = fields.next();
      if (field.getKey().startsWith("_")) {
        continue;
      }
      String body = portalLoginBody(field.getKey());
      if (field.getValue().isNull()) {
        assertDoesNotThrow(() -> SejongHttpSupport.ensurePortalLoginAccepted(body), field.getKey());
        continue;
      }
      SejongAuthException exception = assertThrows(SejongAuthException.class,
          () -> SejongHttpSupport.ensurePortalLoginAccepted(body), field.getKey());
      assertEquals(field.getValue().asText(), exception.getErrorCode().name(), field.getKey());
    }
  }

  @Test
  void 실제_포털_응답을_판정한다() throws Exception {
    String acceptedBody = readFixture("portal_login_ok.html");
    String wrongPasswordBody = readFixture("portal_login_erridpwd.html");

    assertDoesNotThrow(() -> SejongHttpSupport.ensurePortalLoginAccepted(acceptedBody));
    SejongAuthException exception = assertThrows(SejongAuthException.class,
        () -> SejongHttpSupport.ensurePortalLoginAccepted(wrongPasswordBody));
    assertEquals(SejongAuthErrorCode.AUTHENTICATION_FAILED, exception.getErrorCode());
  }

  @Test
  void 잠금_메시지에_비밀번호_재설정_주소가_있다() throws Exception {
    String lockedBody = portalLoginBody("pwdNeedChg");

    SejongAuthException exception = assertThrows(SejongAuthException.class,
        () -> SejongHttpSupport.ensurePortalLoginAccepted(lockedBody));
    assertTrue(exception.getMessage().contains("https://portal.sejong.ac.kr/jsp/inquiry/pwinquiry.jsp"));
  }

  @Test
  void 판정값이_없으면_기존_401_판정에_맡긴다() {
    assertDoesNotThrow(() -> SejongHttpSupport.ensurePortalLoginAccepted(""));
    assertDoesNotThrow(() -> SejongHttpSupport.ensurePortalLoginAccepted("<html>changed portal page</html>"));
  }

  @Test
  void 잠긴_계정은_재시도_없이_한번만_로그인한다() throws Exception {
    String lockedBody = portalLoginBody("pwdNeedChg");
    AtomicInteger loginCallCount = new AtomicInteger();
    OkHttpClient client = new OkHttpClient.Builder()
        .addInterceptor(chain -> {
          loginCallCount.incrementAndGet();
          return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
              .body(ResponseBody.create(lockedBody, MediaType.get("text/html; charset=UTF-8"))).build();
        })
        .build();

    SejongAuthException exception = assertThrows(SejongAuthException.class,
        () -> SejongHttpSupport.loginToPortal(client, new SejongAuthProperties(), "21011234", "pw", "classic.sejong.ac.kr"));

    assertEquals(SejongAuthErrorCode.ACCOUNT_LOCKED, exception.getErrorCode());
    assertEquals(1, loginCallCount.get());
  }

  private static String portalLoginBody(String resultValue) throws Exception {
    return readFixture("portal_login_erridpwd.html").replace("var result = 'erridpwd'", "var result = '" + resultValue + "'");
  }

  private static String readFixture(String fileName) throws Exception {
    return Files.readString(FIXTURE_DIRECTORY.resolve(fileName), StandardCharsets.UTF_8);
  }
}
