package kr.suhsaechan.sejongpeia.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 세종대학교 인증 설정 프로퍼티 (접두사 sejongpeia)
 *
 * application.yml 예시:
 * sejongpeia:
 *   ssl-verification: true
 *   timeout-seconds: 10
 *   max-retry: 3
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "sejongpeia")
public class SejongAuthProperties {

  /** SSL 인증서 검증 활성화 여부 (기본: true) */
  private boolean sslVerification = true;

  /** HTTP 요청 타임아웃 (초, 기본: 10) */
  private int timeoutSeconds = 10;

  /** 요청 실패 시 최대 재시도 횟수 (기본: 3) */
  private int maxRetry = 3;

  /** 세종포털 로그인 URL */
  private String portalLoginUrl = "https://portal.sejong.ac.kr/jsp/login/login_action.jsp";

  /** SSO 리다이렉트 URL (classic.sejong.ac.kr HTTP 평문 서비스 중단으로 https 필수) */
  private String ssoRedirectUrl = "https://classic.sejong.ac.kr/_custom/sejong/sso/sso-return.jsp?returnUrl=https://classic.sejong.ac.kr/classic/index.do";

  /** 고전독서인증현황 페이지 URL */
  private String classicStatusUrl = "https://classic.sejong.ac.kr/classic/reading/status.do";

  /** 공개 데이터(로그인 없는 소스) 캐시 유효 시간 (초, 기본: 86400) */
  private long publicDataCacheTtlSeconds = 86400;

  /** 공개 데이터 실패 기억 시간 (초, 기본: 60) */
  private long publicDataFailureCacheSeconds = 60;

  /** 공개 데이터 캐시 최대 항목 수 (기본: 500, 가득 차면 가장 오래된 항목 제거) */
  private int publicDataCacheMaxEntries = 500;
}
