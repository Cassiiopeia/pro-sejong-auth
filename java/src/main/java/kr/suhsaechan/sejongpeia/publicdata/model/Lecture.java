package kr.suhsaechan.sejongpeia.publicdata.model;

import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * 개설 강의 1건 (분반 단위)
 * 값이 없으면 빈 문자열
 */
@Getter
@Builder
@ToString
public class Lecture {

  private final String courseNumber;
  private final String section;
  private final String name;
  private final String courseTypeCode;
  private final String courseTypeName;
  private final String domainCode;
  private final String domainName;
  private final String credits;
  private final String hours;
  private final String totalHours;
  private final String schedule;
  private final String room;
  private final String instructor;
  private final String gradeLevel;
  private final String languageCode;
  private final String languageName;
  private final String departmentCode;
  private final String departmentName;
  private final String managingDepartment;
  private final String remark;
  private final String year;
  private final String semesterCode;

  /** 매핑에 쓰지 않은 학교 원문 필드 (키는 학교 원문, 빈 값·제외 목록 필드 없음) */
  private final Map<String, String> extra;
}
