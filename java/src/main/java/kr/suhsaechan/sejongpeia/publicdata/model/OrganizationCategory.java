package kr.suhsaechan.sejongpeia.publicdata.model;

import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * 조직 구분(학부, 대학원 등) 코드 1건
 * 값이 없으면 빈 문자열
 */
@Getter
@Builder
@ToString
public class OrganizationCategory {

  private final String code;
  private final String name;
  private final String nameEn;

  /** 매핑에 쓰지 않은 학교 원문 필드 (키는 학교 원문, 빈 값·제외 목록 필드 없음) */
  private final Map<String, String> extra;
}
