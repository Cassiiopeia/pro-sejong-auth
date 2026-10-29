package kr.suhsaechan.sejongpeia.common.util;

/**
 * 문자열 공통 처리: 공백 판정, 기본값, trim (null 안전)
 */
public final class CommonUtil {

  private CommonUtil() {
  }

  /** null 이거나 공백만 있으면 true */
  public static boolean isBlank(String str) {
    return str == null || str.trim().isEmpty();
  }

  /** null 아니고 공백 아닌 문자 포함하면 true */
  public static boolean hasText(String str) {
    return str != null && !str.trim().isEmpty();
  }

  /** 공백이면 defaultValue, 아니면 str */
  public static String defaultIfBlank(String str, String defaultValue) {
    return isBlank(str) ? defaultValue : str;
  }

  /** trim (null > 빈 문자열) */
  public static String trimToEmpty(String str) {
    return str != null ? str.trim() : "";
  }
}
