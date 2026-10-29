package kr.suhsaechan.sejongpeia.publicdata.model;

import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * 학기 1건
 * 값이 없으면 빈 문자열
 */
@Getter
@Builder
@ToString
public class Term {

  private final String year;
  private final String semesterCode;
  private final String code;
  private final String name;

  /** 매핑에 쓰지 않은 학교 원문 필드 (키는 학교 원문, 빈 값·제외 목록 필드 없음) */
  private final Map<String, String> extra;
}
