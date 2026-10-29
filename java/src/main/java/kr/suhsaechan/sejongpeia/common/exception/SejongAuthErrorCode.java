package kr.suhsaechan.sejongpeia.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 세종대학교 인증 관련 에러 코드
 */
@Getter
@AllArgsConstructor
public enum SejongAuthErrorCode {

  // 연결 관련
  CONNECTION_FAILED("SEJONG_AUTH_001", "Cannot connect to the Sejong University portal."),
  CONNECTION_TIMEOUT("SEJONG_AUTH_002", "Connection to the Sejong University portal timed out."),

  // 인증 관련
  /** 미사용: 비밀번호 불일치는 AUTHENTICATION_FAILED 로 던짐. 기존 사용처 호환용으로만 유지 */
  @Deprecated
  INVALID_CREDENTIALS("SEJONG_AUTH_003", "Invalid student ID or password."),
  AUTHENTICATION_FAILED("SEJONG_AUTH_009", "Sejong University portal authentication failed. Check the student ID and password."),
  SESSION_ERROR("SEJONG_AUTH_004", "An error occurred while handling the portal session."),
  INVALID_INPUT("SEJONG_AUTH_008", "Invalid input."),
  ACCOUNT_LOCKED("SEJONG_AUTH_010", "The portal account is locked after too many wrong passwords. "
      + "Reset the password at https://portal.sejong.ac.kr/jsp/inquiry/pwinquiry.jsp"),
  ACCOUNT_UNAVAILABLE("SEJONG_AUTH_011", "The portal account cannot sign in (use period ended or blocked by the administrator)."),

  // 데이터 관련
  DATA_FETCH_FAILED("SEJONG_AUTH_005", "Failed to fetch student information."),
  PARSE_ERROR("SEJONG_AUTH_006", "Failed to parse student information."),

  // 설정 관련
  SSL_CONFIGURATION_ERROR("SEJONG_AUTH_007", "Failed to configure SSL.");

  private final String code;
  private final String message;
}
