package kr.suhsaechan.sejongpeia.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import kr.suhsaechan.sejongpeia.auth.client.SejongPortalClient;
import kr.suhsaechan.sejongpeia.auth.client.SejongSisClient;
import kr.suhsaechan.sejongpeia.auth.parser.SejongClassicReadingParser;
import kr.suhsaechan.sejongpeia.auth.parser.SejongSisParser;
import kr.suhsaechan.sejongpeia.auth.parser.SejongStudentInfoParser;
import kr.suhsaechan.sejongpeia.common.config.SejongAuthProperties;
import kr.suhsaechan.sejongpeia.common.exception.SejongAuthErrorCode;
import kr.suhsaechan.sejongpeia.common.exception.SejongAuthException;
import org.junit.jupiter.api.Test;

/**
 * SIS 틀린 비밀번호: initUserInfo 200 + _SUBMIT_ERROR_(LOGOUT) > AUTHENTICATION_FAILED (spec/PORTAL_FLOW.md)
 */
class SisWrongPasswordTest {

  @Test
  void sis_submit_error_logout_raises_authentication_failed() throws Exception {
    String logoutJson = Files.readString(Path.of("..", "spec", "fixtures", "sis_submit_error_logout.json"), StandardCharsets.UTF_8);
    SejongAuthProperties properties = new SejongAuthProperties();
    SejongSisClient sisClientReturningLogout = new SejongSisClient(properties) {
      @Override
      public String authenticateAndFetchJson(String studentId, String password) {
        return logoutJson;
      }
    };
    SuhSejongAuthEngine engine = new SuhSejongAuthEngine(new SejongPortalClient(properties), new SejongStudentInfoParser(),
        new SejongClassicReadingParser(), sisClientReturningLogout, new SejongSisParser());

    SejongAuthException exception = assertThrows(SejongAuthException.class,
        () -> engine.authenticateWithSIS("21011234", "wrong"));

    assertEquals(SejongAuthErrorCode.AUTHENTICATION_FAILED, exception.getErrorCode());
  }
}
