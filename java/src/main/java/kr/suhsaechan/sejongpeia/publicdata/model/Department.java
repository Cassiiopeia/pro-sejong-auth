package kr.suhsaechan.sejongpeia.publicdata.model;

import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * 학과·전공 1건 (대학 > 학부·학과 > 전공 계층)
 * 값이 없으면 빈 문자열
 */
@Getter
@Builder
@ToString
public class Department {

  private final String code;
  private final String level;
  private final String collegeCode;
  private final String collegeName;
  private final String unitCode;
  private final String unitName;
  private final String majorCode;
  private final String majorName;
  private final String nameEn;

  /** 매핑에 쓰지 않은 학교 원문 필드 (키는 학교 원문, 빈 값·제외 목록 필드 없음) */
  private final Map<String, String> extra;
}
