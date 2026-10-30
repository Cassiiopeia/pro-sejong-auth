package kr.suhsaechan.sejongpeia.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 라이브러리 공용 Jackson ObjectMapper: 기본 설정 하나를 모든 소스가 공유
 * 설정을 바꾸지 않는 읽기·쓰기 전용 (스레드 안전, 설정 변경 금지)
 */
public final class JsonMapperSupport {

  public static final ObjectMapper PLAIN_JSON_MAPPER = new ObjectMapper();

  private JsonMapperSupport() {
  }
}
