package kr.suhsaechan.sejongpeia.auth.service;

import kr.suhsaechan.sejongpeia.TestApplication;
import kr.suhsaechan.sejongpeia.auth.model.SejongAuthResult;
import kr.suhsaechan.sejongpeia.auth.model.SejongDhcAuthResult;
import kr.suhsaechan.sejongpeia.auth.model.SejongSisAuthResult;
import kr.suhsaechan.sejongpeia.common.exception.SejongAuthErrorCode;
import kr.suhsaechan.sejongpeia.common.exception.SejongAuthException;
import kr.suhsaechan.sejongpeia.support.SejongTestAccountProperties;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 세종대학교 인증 서비스 스프링 테스트
 *
 * 실제 포털 연동 테스트는 @Disabled 처리되어 있습니다.
 * 로컬에서 테스트하려면 @Disabled를 제거하고 학번/비밀번호를 입력하세요.
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("dev")
@Slf4j
class SuhSejongAuthEngineTest {

  @Autowired
  SuhSejongAuthEngine suhSejongAuthEngine;

  @Autowired
  SejongTestAccountProperties testAccountProperties;

  @Test
  public void mainTest() {
    log.info("============ 테스트시작 ============");

    sejongAuthService_테스트();

    log.info("============ 테스트종료 ============");
  }

  public void sejongAuthService_테스트() {
    // TODO: 테스트 로직 작성
    log.info("테스트 실행중");
    assertNotNull(suhSejongAuthEngine, "SejongAuthService가 정상적으로 주입되어야 합니다.");
    log.info("SejongAuthService 빈 주입 확인 완료");
  }

  @Test
  void 입력값검증_학번_빈값() {
    log.info("============ 입력값 검증 테스트 - 학번 빈값 ============");

    SejongAuthException exception = assertThrows(SejongAuthException.class, () -> {
      suhSejongAuthEngine.authenticate("", "password");
    });

    assertEquals(SejongAuthErrorCode.INVALID_INPUT, exception.getErrorCode());
    log.info("예외 발생 확인: {}", exception.getMessage());
  }

  @Test
  void 입력값검증_비밀번호_빈값() {
    log.info("============ 입력값 검증 테스트 - 비밀번호 빈값 ============");

    SejongAuthException exception = assertThrows(SejongAuthException.class, () -> {
      suhSejongAuthEngine.authenticate("20012345", "");
    });

    assertEquals(SejongAuthErrorCode.INVALID_INPUT, exception.getErrorCode());
    log.info("예외 발생 확인: {}", exception.getMessage());
  }

  @Test
  void 입력값검증_학번_null() {
    log.info("============ 입력값 검증 테스트 - 학번 null ============");

    SejongAuthException exception = assertThrows(SejongAuthException.class, () -> {
      suhSejongAuthEngine.authenticate(null, "password");
    });

    assertEquals(SejongAuthErrorCode.INVALID_INPUT, exception.getErrorCode());
    log.info("예외 발생 확인: {}", exception.getMessage());
  }

  @Test
  @Disabled("실제 포털 연동 테스트 - 로컬에서만 실행")
  void 실제인증_전체정보() {
    log.info("============ 실제 포털 인증 테스트 - 전체 정보 ============");

    String testStudentId = testAccountProperties.getStudentId();
    String testPassword = testAccountProperties.getPassword();

    assertNotNull(testStudentId, "테스트용 학번이 설정되지 않았습니다.");
    assertNotNull(testPassword, "테스트용 비밀번호가 설정되지 않았습니다.");
    assertFalse(testStudentId.isEmpty(), "테스트용 학번이 설정되지 않았습니다.");
    assertFalse(testPassword.isEmpty(), "테스트용 비밀번호가 설정되지 않았습니다.");

    SejongAuthResult result = suhSejongAuthEngine.authenticate(testStudentId, testPassword);

    assertTrue(result.isSuccess());
    assertNotNull(result.getMajor());
    assertNotNull(result.getStudentId());
    assertNotNull(result.getName());
    assertNotNull(result.getClassicReading());
    assertNotNull(result.getAuthenticatedAt());

    log.info("=== 인증 결과 ===");
    log.info("학과: {}", result.getMajor());
    log.info("학번: {}", result.getStudentId());
    log.info("이름: {}", result.getName());
    log.info("학년: {}", result.getGrade());
    log.info("상태: {}", result.getStatus());
  }

  // 참고: authenticateBasic 메서드는 제거됨 - authenticateWithDHC로 대체
  // 참고: authenticateWithRawHtml 메서드는 제거됨 - authenticateWithDHCRaw로 대체

  @Test
  @Disabled("실제 포털 연동 테스트 - 로컬에서만 실행")
  void 실제인증_잘못된비밀번호() {
    log.info("============ 실제 포털 인증 테스트 - 잘못된 비밀번호 ============");

    String testStudentId = testAccountProperties.getStudentId();
    assertNotNull(testStudentId, "테스트용 학번이 설정되지 않았습니다.");
    assertFalse(testStudentId.isEmpty(), "테스트용 학번이 설정되지 않았습니다.");

    // 잘못된 비밀번호로 테스트
    String wrongPassword = "wrongpassword123";

    SejongAuthException exception = assertThrows(SejongAuthException.class, () -> {
      suhSejongAuthEngine.authenticate(testStudentId, wrongPassword);
    });

    assertEquals(SejongAuthErrorCode.AUTHENTICATION_FAILED, exception.getErrorCode());
    log.info("=== 예외 발생 확인 ===");
    log.info("에러 코드: {}", exception.getErrorCode());
    log.info("메시지: {}", exception.getMessage());
  }

  // ==================== 새로운 API 테스트 ====================

  @Test
  void DHC인증_입력값검증_학번_빈값() {
    log.info("============ DHC 인증 입력값 검증 - 학번 빈값 ============");

    SejongAuthException exception = assertThrows(SejongAuthException.class, () -> {
      suhSejongAuthEngine.authenticateWithDHC("", "password");
    });

    assertEquals(SejongAuthErrorCode.INVALID_INPUT, exception.getErrorCode());
    log.info("예외 발생 확인: {}", exception.getMessage());
  }

  @Test
  void SIS인증_입력값검증_학번_빈값() {
    log.info("============ SIS 인증 입력값 검증 - 학번 빈값 ============");

    SejongAuthException exception = assertThrows(SejongAuthException.class, () -> {
      suhSejongAuthEngine.authenticateWithSIS("", "password");
    });

    assertEquals(SejongAuthErrorCode.INVALID_INPUT, exception.getErrorCode());
    log.info("예외 발생 확인: {}", exception.getMessage());
  }

  @Test
  void DHC인증_입력값검증_비밀번호_null() {
    log.info("============ DHC 인증 입력값 검증 - 비밀번호 null ============");

    SejongAuthException exception = assertThrows(SejongAuthException.class, () -> {
      suhSejongAuthEngine.authenticateWithDHC("20171234", null);
    });

    assertEquals(SejongAuthErrorCode.INVALID_INPUT, exception.getErrorCode());
    log.info("예외 발생 확인: {}", exception.getMessage());
  }

  @Test
  void SIS인증_입력값검증_비밀번호_null() {
    log.info("============ SIS 인증 입력값 검증 - 비밀번호 null ============");

    SejongAuthException exception = assertThrows(SejongAuthException.class, () -> {
      suhSejongAuthEngine.authenticateWithSIS("20171234", null);
    });

    assertEquals(SejongAuthErrorCode.INVALID_INPUT, exception.getErrorCode());
    log.info("예외 발생 확인: {}", exception.getMessage());
  }

  @Test
  @Disabled("실제 포털 연동 테스트 - 로컬에서만 실행")
  void 실제인증_DHC_고전독서정보() {
    log.info("============ 실제 포털 인증 테스트 - DHC (고전독서 정보) ============");

    String testStudentId = testAccountProperties.getStudentId();
    String testPassword = testAccountProperties.getPassword();

    assertNotNull(testStudentId, "테스트용 학번이 설정되지 않았습니다.");
    assertNotNull(testPassword, "테스트용 비밀번호가 설정되지 않았습니다.");

    SejongDhcAuthResult result = suhSejongAuthEngine.authenticateWithDHC(testStudentId, testPassword);

    assertTrue(result.isSuccess());
    assertNotNull(result.getMajor());
    assertNotNull(result.getStudentId());
    assertNotNull(result.getName());
    assertNotNull(result.getClassicReading());
    assertNotNull(result.getAuthenticatedAt());

    log.info("=== DHC 인증 결과 ===");
    log.info("학과: {}", result.getMajor());
    log.info("학번: {}", result.getStudentId());
    log.info("이름: {}", result.getName());
    log.info("학년: {}", result.getGrade());
    log.info("상태: {}", result.getStatus());
    log.info("고전독서: {}", result.getClassicReading());
  }

  @Test
  @Disabled("실제 포털 연동 테스트 - 로컬에서만 실행")
  void 실제인증_SIS_연락처정보() {
    log.info("============ 실제 포털 인증 테스트 - SIS (연락처 정보) ============");

    String testStudentId = testAccountProperties.getStudentId();
    String testPassword = testAccountProperties.getPassword();

    assertNotNull(testStudentId, "테스트용 학번이 설정되지 않았습니다.");
    assertNotNull(testPassword, "테스트용 비밀번호가 설정되지 않았습니다.");

    SejongSisAuthResult result = suhSejongAuthEngine.authenticateWithSIS(testStudentId, testPassword);

    assertTrue(result.isSuccess());
    assertNotNull(result.getMajor());
    assertNotNull(result.getStudentId());
    assertNotNull(result.getName());
    assertNotNull(result.getAuthenticatedAt());

    log.info("=== SIS 인증 결과 ===");
    log.info("학과: {}", result.getMajor());
    log.info("학번: {}", result.getStudentId());
    log.info("이름: {}", result.getName());
    log.info("영어이름: {}", result.getEnglishName());
    log.info("이메일: {}", result.getEmail());
    log.info("전화번호: {}", result.getPhoneNumber());
  }

  @Test
  @Disabled("실제 포털 연동 테스트 - 로컬에서만 실행")
  void 실제인증_통합_전체정보() {
    log.info("============ 실제 포털 인증 테스트 - 통합 (전체 정보) ============");

    String testStudentId = testAccountProperties.getStudentId();
    String testPassword = testAccountProperties.getPassword();

    assertNotNull(testStudentId, "테스트용 학번이 설정되지 않았습니다.");
    assertNotNull(testPassword, "테스트용 비밀번호가 설정되지 않았습니다.");

    SejongAuthResult result = suhSejongAuthEngine.authenticate(testStudentId, testPassword);

    assertTrue(result.isSuccess());
    assertNotNull(result.getMajor());
    assertNotNull(result.getStudentId());
    assertNotNull(result.getName());
    assertNotNull(result.getClassicReading());
    // email, phoneNumber, englishName은 SIS 실패 시 null일 수 있음
    assertNotNull(result.getAuthenticatedAt());

    log.info("=== 통합 인증 결과 ===");
    log.info("학과: {}", result.getMajor());
    log.info("학번: {}", result.getStudentId());
    log.info("이름: {}", result.getName());
    log.info("학년: {}", result.getGrade());
    log.info("상태: {}", result.getStatus());
    log.info("고전독서: {}", result.getClassicReading());
    log.info("영어이름: {}", result.getEnglishName());
    log.info("이메일: {}", result.getEmail());
    log.info("전화번호: {}", result.getPhoneNumber());
  }

  @Test
  @Disabled("실제 포털 연동 테스트 - 로컬에서만 실행")
  void 실제인증_DHC_원본HTML포함() {
    log.info("============ 실제 포털 인증 테스트 - DHC 원본 HTML 포함 ============");

    String testStudentId = testAccountProperties.getStudentId();
    String testPassword = testAccountProperties.getPassword();

    assertNotNull(testStudentId, "테스트용 학번이 설정되지 않았습니다.");
    assertNotNull(testPassword, "테스트용 비밀번호가 설정되지 않았습니다.");

    SejongDhcAuthResult result = suhSejongAuthEngine.authenticateWithDHCRaw(testStudentId, testPassword);

    assertTrue(result.isSuccess());
    assertNotNull(result.getRawHtml());
    assertTrue(result.getRawHtml().length() > 0);

    log.info("=== DHC 원본 HTML 포함 인증 결과 ===");
    log.info("HTML 길이: {} 바이트", result.getRawHtml().length());
  }

  @Test
  @Disabled("실제 포털 연동 테스트 - 로컬에서만 실행")
  void 실제인증_SIS_원본JSON포함() {
    log.info("============ 실제 포털 인증 테스트 - SIS 원본 JSON 포함 ============");

    String testStudentId = testAccountProperties.getStudentId();
    String testPassword = testAccountProperties.getPassword();

    assertNotNull(testStudentId, "테스트용 학번이 설정되지 않았습니다.");
    assertNotNull(testPassword, "테스트용 비밀번호가 설정되지 않았습니다.");

    SejongSisAuthResult result = suhSejongAuthEngine.authenticateWithSISRaw(testStudentId, testPassword);

    assertTrue(result.isSuccess());
    assertNotNull(result.getRawJson());
    assertTrue(result.getRawJson().length() > 0);

    log.info("=== SIS 원본 JSON 포함 인증 결과 ===");
    log.info("JSON 길이: {} 바이트", result.getRawJson().length());
    log.info("JSON 내용:\n{}", result.getRawJson());
  }
}
