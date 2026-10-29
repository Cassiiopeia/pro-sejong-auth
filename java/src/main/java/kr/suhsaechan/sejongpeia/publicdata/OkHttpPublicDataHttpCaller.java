package kr.suhsaechan.sejongpeia.publicdata;

import java.io.IOException;
import kr.suhsaechan.sejongpeia.client.SejongHttpSupport;
import kr.suhsaechan.sejongpeia.config.SejongAuthProperties;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * OkHttp 기반 기본 호출기: 라이브러리 공용 클라이언트(레거시 TLS, 타임아웃), 재시도 없음
 */
class OkHttpPublicDataHttpCaller implements PublicDataHttpCaller {

  private static final String USER_AGENT = "pro-sejongpeia-java";
  static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded; charset=UTF-8";

  private final OkHttpClient client;

  OkHttpPublicDataHttpCaller(SejongAuthProperties properties) {
    this.client = SejongHttpSupport.buildBaseClient(properties);
  }

  @Override
  public PublicDataHttpResponse call(PublicDataRequest request) throws IOException {
    Request.Builder builder = new Request.Builder()
        .url(request.getUrl())
        .header("User-Agent", USER_AGENT)
        .header("Accept", "application/json");
    boolean isPost = "POST".equals(request.getHttpMethod());
    if (isPost) {
      // 본문 형식 지정: 문자 집합 포함 값을 그대로 헤더로 (OkHttp 가 charset 을 덧붙이지 않게 바이트 본문 사용)
      RequestBody body = RequestBody.create(request.getBody().getBytes(java.nio.charset.StandardCharsets.UTF_8),
          MediaType.parse(request.getContentType()));
      builder.post(body).header("Content-Type", request.getContentType());
      if (FORM_CONTENT_TYPE.equals(request.getContentType())) {
        builder.header("X-Requested-With", "XMLHttpRequest");
      }
    }
    try (Response response = client.newCall(builder.build()).execute()) {
      // happydorm 은 text/html 로 JSON 응답 > 본문은 항상 문자열로 읽음
      return new PublicDataHttpResponse(response.code(), response.body() == null ? "" : response.body().string());
    }
  }
}
