package kr.suhsaechan.sejongpeia.publicdata;

import java.io.IOException;

/**
 * 공개 데이터 HTTP 호출 1건 (테스트에서 fixture 로 교체 가능)
 */
@FunctionalInterface
public interface PublicDataHttpCaller {

  PublicDataHttpResponse call(PublicDataRequest request) throws IOException;
}
