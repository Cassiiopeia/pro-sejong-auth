package kr.suhsaechan.sejongpeia.publicdata;

import lombok.Getter;
import lombok.ToString;

/**
 * 공개 데이터 HTTP 요청 1건: 메서드, URL, 본문, 본문 형식
 * contentType 이 null 이면 본문 없는 GET
 */
@Getter
@ToString
public class PublicDataRequest {

  private final String httpMethod;
  private final String url;
  private final String body;
  private final String contentType;

  public PublicDataRequest(String httpMethod, String url, String body, String contentType) {
    this.httpMethod = httpMethod;
    this.url = url;
    this.body = body;
    this.contentType = contentType;
  }
}
