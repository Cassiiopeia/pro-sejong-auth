package kr.suhsaechan.sejongpeia.util;

import java.security.Security;

/**
 * 세종 포털 TLS 호환: jdk.tls.disabledAlgorithms 에서 TLS_RSA_* 제거 (포털 cipher TLS_RSA_WITH_AES_256_CBC_SHA 허용)
 * JVM 전역 설정, JSSE 첫 초기화 전에만 효과 > 앱 시작 직후 호출 (Spring Boot 는 SejongTlsEnvironmentPostProcessor 가 호출, spec/PORTAL_FLOW.md)
 */
public final class SejongTlsSupport {

  private static final String DISABLED_ALGORITHMS_KEY = "jdk.tls.disabledAlgorithms";
  // 클래스 로드 시 1회 조정 (로깅 시스템 초기화 전일 수 있어 여기서는 로그 없음)
  private static final boolean HAS_ADJUSTED_DISABLED_ALGORITHMS = removeLegacyRsaFromDisabledAlgorithms();

  private SejongTlsSupport() {
  }

  /**
   * 레거시 RSA cipher 재허용 보장 (클래스 로드 트리거, 여러 번 불러도 1회만 적용)
   */
  public static void ensureLegacyCipherEnabled() {
    // static 초기화 실행만 보장
  }

  /**
   * 이 JVM 의 기본 설정이 TLS_RSA_* 를 막고 있어서 조정했는지 (handshake 실패 원인 안내용)
   */
  public static boolean hasAdjustedDisabledAlgorithms() {
    return HAS_ADJUSTED_DISABLED_ALGORITHMS;
  }

  private static boolean removeLegacyRsaFromDisabledAlgorithms() {
    String disabledAlgorithms = Security.getProperty(DISABLED_ALGORITHMS_KEY);
    if (disabledAlgorithms == null || !disabledAlgorithms.toUpperCase().contains("TLS_RSA_")) {
      return false;
    }

    // "TLS_RSA_*" 항목만 제거 (앞뒤 콤마·공백 정리)
    String cleanedAlgorithms = disabledAlgorithms.replaceAll("(?i)\\s*,?\\s*TLS_RSA_\\*", "")
        .replaceAll("^\\s*,\\s*", "").trim();
    Security.setProperty(DISABLED_ALGORITHMS_KEY, cleanedAlgorithms);
    return true;
  }
}
