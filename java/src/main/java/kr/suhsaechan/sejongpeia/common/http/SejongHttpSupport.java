package kr.suhsaechan.sejongpeia.common.http;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import kr.suhsaechan.sejongpeia.common.config.SejongAuthProperties;
import kr.suhsaechan.sejongpeia.common.exception.SejongAuthErrorCode;
import kr.suhsaechan.sejongpeia.common.exception.SejongAuthException;
import kr.suhsaechan.sejongpeia.common.util.SejongTlsSupport;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 세종 HTTP 공통: 공용 클라이언트 생성(TLS·타임아웃), 재시도 실행
 * 인증과 공개 데이터 소스가 공유 (포털 로그인 전용 처리는 auth.client.SejongPortalHttpSupport)
 */
@Slf4j
public final class SejongHttpSupport {

  private SejongHttpSupport() {
  }

  /**
   * 공용 OkHttpClient 생성 (연결 풀 공유): 레거시 cipher 보장 > 타임아웃 > (설정 시) SSL 검증 해제
   */
  public static OkHttpClient buildBaseClient(SejongAuthProperties properties) {
    try {
      SejongTlsSupport.ensureLegacyCipherEnabled();
      if (SejongTlsSupport.hasAdjustedDisabledAlgorithms()) {
        log.debug("[sejong tls] TLS_RSA_* removed from jdk.tls.disabledAlgorithms for the Sejong portal");
      }

      int timeoutSeconds = properties.getTimeoutSeconds();
      OkHttpClient.Builder builder = new OkHttpClient.Builder()
          .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
          .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
          .writeTimeout(timeoutSeconds, TimeUnit.SECONDS);

      if (!properties.isSslVerification()) {
        X509TrustManager trustAllManager = createTrustAllManager();
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{trustAllManager}, new SecureRandom());
        builder.sslSocketFactory(sslContext.getSocketFactory(), trustAllManager);
        builder.hostnameVerifier((hostname, session) -> true);
        log.debug("[sejong auth] SSL verification disabled");
      }

      return builder.build();
    } catch (Exception e) {
      throw new SejongAuthException(SejongAuthErrorCode.SSL_CONFIGURATION_ERROR, e);
    }
  }

  /**
   * 요청 실행: 실패 응답·타임아웃 시 maxRetry 회까지 재시도 (실패 응답은 닫고 다음 시도)
   */
  public static Response executeWithRetry(OkHttpClient client, Request request, int maxRetry) throws IOException {
    for (int attempt = 1; attempt <= maxRetry; attempt++) {
      try {
        Response response = client.newCall(request).execute();
        if (response.isSuccessful()) {
          return response;
        }
        response.close();
        log.warn("[sejong auth] request failed, retrying (attempt {}, code {})", attempt, response.code());
      } catch (SocketTimeoutException e) {
        log.warn("[sejong auth] timeout, retrying (attempt {})", attempt);
        if (attempt == maxRetry) {
          throw new SejongAuthException(SejongAuthErrorCode.CONNECTION_TIMEOUT, e);
        }
      }
    }
    throw new SejongAuthException(SejongAuthErrorCode.CONNECTION_FAILED, "Max retry count exceeded : " + maxRetry);
  }

  /**
   * 모든 인증서 신뢰 (ssl-verification=false 일 때만 사용)
   */
  private static X509TrustManager createTrustAllManager() {
    return new X509TrustManager() {
      @Override
      public void checkClientTrusted(X509Certificate[] chain, String authType) {
      }

      @Override
      public void checkServerTrusted(X509Certificate[] chain, String authType) {
      }

      @Override
      public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
      }
    };
  }

}
