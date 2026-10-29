package kr.suhsaechan.sejong.auth.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import kr.suhsaechan.sejong.auth.config.SejongAuthProperties;
import kr.suhsaechan.sejong.auth.exception.SejongAuthErrorCode;
import org.junit.jupiter.api.Test;

/**
 * 세 언어 공통 값: spec/contract.json <-> Java 상수·설정 기본값·에러 코드 (어긋나면 실패)
 */
class ContractConstantsTest {

  private final JsonNode contract = readContract();

  @Test
  void 포털_값이_contract와_같다() {
    JsonNode portal = contract.get("portal");
    SejongAuthProperties defaults = new SejongAuthProperties();
    Map<String, String> errorCodeNameByResult = new TreeMap<>();
    SejongHttpSupport.PORTAL_LOGIN_ERROR_CODE_BY_RESULT.forEach((result, code) -> errorCodeNameByResult.put(result, code.name()));

    assertEquals(portal.get("loginUrl").asText(), defaults.getPortalLoginUrl());
    assertEquals(portal.get("referer").asText(), SejongHttpSupport.PORTAL_REFERER);
    assertEquals(portal.get("defaultCookie").asText(), SejongHttpSupport.PORTAL_DEFAULT_COOKIE);
    assertEquals(portal.get("loginAcceptedResult").asText(), SejongHttpSupport.PORTAL_LOGIN_ACCEPTED);
    assertEquals(toSortedMap(portal.get("loginErrorCodeByResult")), errorCodeNameByResult);
  }

  @Test
  void 포털_메인_값이_contract와_같다() {
    JsonNode portalMain = contract.get("portalMain");

    assertEquals(portalMain.get("url").asText(), SejongPortalClient.PORTAL_MAIN_URL);
    assertEquals(portalMain.get("loginPageMarker").asText(), SejongPortalClient.PORTAL_LOGIN_PAGE_MARKER);
  }

  @Test
  void 고전독서_학사정보시스템_값이_contract와_같다() {
    JsonNode classic = contract.get("classic");
    JsonNode sis = contract.get("sis");
    SejongAuthProperties defaults = new SejongAuthProperties();

    assertEquals(classic.get("returnUrl").asText(), SejongPortalClient.CLASSIC_RETURN_URL);
    assertEquals(classic.get("ssoUrl").asText(), defaults.getSsoRedirectUrl());
    assertEquals(classic.get("statusUrl").asText(), defaults.getClassicStatusUrl());
    assertEquals(sis.get("returnUrl").asText(), SejongSisClient.SJPT_RETURN_URL);
    assertEquals(sis.get("ssoUrl").asText(), SejongSisClient.SJPT_SSO_URL);
    assertEquals(sis.get("userInfoUrl").asText(), SejongSisClient.SJPT_INIT_USER_INFO_URL);
    assertEquals(sis.get("origin").asText(), SejongSisClient.SJPT_ORIGIN);
  }

  @Test
  void 에러_메시지가_contract와_같다() {
    // Java 전용 deprecated 코드(INVALID_CREDENTIALS)는 contract 밖
    contract.get("errorMessageByCode").fields().forEachRemaining(entry ->
        assertEquals(entry.getValue().asText(), SejongAuthErrorCode.valueOf(entry.getKey()).getMessage(), entry.getKey()));
  }

  private Map<String, String> toSortedMap(JsonNode objectNode) {
    Map<String, String> sortedMap = new TreeMap<>();
    objectNode.fields().forEachRemaining(entry -> sortedMap.put(entry.getKey(), entry.getValue().asText()));
    return sortedMap;
  }

  private static JsonNode readContract() {
    try {
      return new ObjectMapper().readTree(Path.of("..", "spec", "contract.json").toFile());
    } catch (Exception e) {
      throw new IllegalStateException("spec/contract.json read failed", e);
    }
  }
}
