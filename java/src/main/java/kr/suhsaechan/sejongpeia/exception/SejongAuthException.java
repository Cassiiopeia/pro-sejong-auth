package kr.suhsaechan.sejongpeia.exception;

import lombok.Getter;

/**
 * 세종대학교 인증 관련 커스텀 예외
 */
@Getter
public class SejongAuthException extends RuntimeException {

  private final SejongAuthErrorCode errorCode;
  private final String detail;

  public SejongAuthException(SejongAuthErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
    this.detail = null;
  }

  public SejongAuthException(SejongAuthErrorCode errorCode, String detail) {
    super(errorCode.getMessage() + " - " + detail);
    this.errorCode = errorCode;
    this.detail = detail;
  }

  public SejongAuthException(SejongAuthErrorCode errorCode, String detail, Throwable cause) {
    super(errorCode.getMessage() + " - " + detail, cause);
    this.errorCode = errorCode;
    this.detail = detail;
  }

  public SejongAuthException(SejongAuthErrorCode errorCode, Throwable cause) {
    super(errorCode.getMessage(), cause);
    this.errorCode = errorCode;
    this.detail = cause.getMessage();
  }
}
