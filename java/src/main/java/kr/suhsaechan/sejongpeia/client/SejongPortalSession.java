package kr.suhsaechan.sejongpeia.client;

import okhttp3.OkHttpClient;

/**
 * 포털 로그인을 마친 세션 1개 (쿠키 저장소 1개): 고전독서·학사정보시스템 SSO 를 같은 로그인으로 진행
 * 인증 1건 안에서만 사용 (스레드 간 공유·재사용 금지)
 */
public final class SejongPortalSession {

  private final OkHttpClient sessionClient;

  SejongPortalSession(OkHttpClient sessionClient) {
    this.sessionClient = sessionClient;
  }

  OkHttpClient sessionClient() {
    return sessionClient;
  }
}
