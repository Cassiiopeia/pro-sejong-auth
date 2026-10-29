package kr.suhsaechan.sejongpeia.common.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import kr.suhsaechan.sejongpeia.common.exception.SejongAuthErrorCode;
import kr.suhsaechan.sejongpeia.common.exception.SejongAuthException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import org.junit.jupiter.api.Test;

/**
 * 재시도 실행: 실패 응답 닫기, 성공 시 즉시 반환, 횟수 초과 시 CONNECTION_FAILED
 */
class SejongHttpSupportTest {

  private static final Request SAMPLE_REQUEST = new Request.Builder().url("https://portal.sejong.ac.kr/").build();

  @Test
  void 실패응답은_닫고_재시도하며_성공응답을_반환한다() throws Exception {
    List<TrackingResponseBody> issuedBodies = new ArrayList<>();
    OkHttpClient client = clientReturningCodes(issuedBodies, 500, 503, 200);

    try (Response response = SejongHttpSupport.executeWithRetry(client, SAMPLE_REQUEST, 3)) {
      assertEquals(200, response.code());
    }

    assertEquals(3, issuedBodies.size());
    assertEquals(1, issuedBodies.get(0).closeCount.get(), "첫 실패 응답은 닫혀야 함");
    assertEquals(1, issuedBodies.get(1).closeCount.get(), "두 번째 실패 응답은 닫혀야 함");
  }

  @Test
  void 재시도_횟수를_넘기면_CONNECTION_FAILED_이고_모든_응답이_닫힌다() {
    List<TrackingResponseBody> issuedBodies = new ArrayList<>();
    OkHttpClient client = clientReturningCodes(issuedBodies, 500, 500, 500);

    SejongAuthException exception = assertThrows(SejongAuthException.class,
        () -> SejongHttpSupport.executeWithRetry(client, SAMPLE_REQUEST, 3));

    assertEquals(SejongAuthErrorCode.CONNECTION_FAILED, exception.getErrorCode());
    issuedBodies.forEach(body -> assertEquals(1, body.closeCount.get()));
  }

  // 네트워크 없이 상태 코드를 순서대로 돌려주는 클라이언트
  private OkHttpClient clientReturningCodes(List<TrackingResponseBody> issuedBodies, int... statusCodes) {
    AtomicInteger callIndex = new AtomicInteger();
    return new OkHttpClient.Builder()
        .addInterceptor(chain -> {
          TrackingResponseBody body = new TrackingResponseBody();
          issuedBodies.add(body);
          return new Response.Builder()
              .request(chain.request())
              .protocol(Protocol.HTTP_1_1)
              .code(statusCodes[callIndex.getAndIncrement()])
              .message("test")
              .body(body)
              .build();
        })
        .build();
  }

  // close 호출 횟수를 세는 응답 본문
  private static final class TrackingResponseBody extends ResponseBody {
    private final AtomicInteger closeCount = new AtomicInteger();
    private final Buffer source = new Buffer().writeUtf8("body");

    @Override
    public MediaType contentType() {
      return null;
    }

    @Override
    public long contentLength() {
      return 4;
    }

    @Override
    public BufferedSource source() {
      return source;
    }

    @Override
    public void close() {
      closeCount.incrementAndGet();
      super.close();
    }
  }
}
