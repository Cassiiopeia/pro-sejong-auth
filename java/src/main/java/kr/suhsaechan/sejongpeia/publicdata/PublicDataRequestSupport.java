package kr.suhsaechan.sejongpeia.publicdata;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

/**
 * 공개 데이터 요청 조립 공통: 요청 키 + 값 > 쿼리·폼 문자열, 캐시 키, null 처리 (모든 소스가 공유)
 */
final class PublicDataRequestSupport {

  private PublicDataRequestSupport() {
  }

  /** 요청 키 목록 + 값 > key=value&... (보낼 순서 그대로, UTF-8 퍼센트 인코딩) */
  static String pairsOf(List<String> keys, String... values) {
    StringJoiner joiner = new StringJoiner("&");
    for (int index = 0; index < keys.size(); index++) {
      joiner.add(URLEncoder.encode(keys.get(index), StandardCharsets.UTF_8) + "="
          + URLEncoder.encode(nullToEmpty(values[index]), StandardCharsets.UTF_8));
    }
    return joiner.toString();
  }

  /** 캐시 키: 메서드명|인자... (첫 구분자 앞이 로그용 메서드명) */
  static String cacheKeyOf(String methodName, String... arguments) {
    return methodName + "|" + String.join("\u0001", Arrays.stream(arguments).map(PublicDataRequestSupport::nullToEmpty).toList());
  }

  static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
