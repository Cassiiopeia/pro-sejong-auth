package kr.suhsaechan.sejongpeia.publicdata;

import lombok.Getter;
import lombok.ToString;

/**
 * 공개 데이터 HTTP 응답: 상태 코드, 본문 (본문 없으면 빈 문자열)
 */
@Getter
@ToString
public class PublicDataHttpResponse {

  private final int statusCode;
  private final String body;

  public PublicDataHttpResponse(int statusCode, String body) {
    this.statusCode = statusCode;
    this.body = body == null ? "" : body;
  }

  public boolean isSuccessful() {
    return statusCode >= 200 && statusCode < 300;
  }
}
