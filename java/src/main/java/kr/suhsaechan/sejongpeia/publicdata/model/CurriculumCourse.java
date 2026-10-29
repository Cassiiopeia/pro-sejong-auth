package kr.suhsaechan.sejongpeia.publicdata.model;

import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * 학과 교육과정의 과목 1건 (권장 학년·학기, 이수구분, 학점, 교과목 개요)
 * 값이 없으면 빈 문자열
 */
@Getter
@Builder
@ToString
public class CurriculumCourse {

  private final String departmentCode;
  private final String departmentName;
  private final String categoryCode;
  private final String year;
  private final String courseNumber;
  private final String name;
  private final String courseTypeCode;
  private final String courseTypeName;
  private final String credits;
  private final String lectureHours;
  private final String practiceHours;
  private final String totalHours;
  private final String gradeLevel;
  private final String semesterCode;
  private final String semesterName;
  /** 교과목 개요 본문 (학교 필드 remark) */
  private final String overview;

  /** 매핑에 쓰지 않은 학교 원문 필드 (키는 학교 원문, 빈 값·제외 목록 필드 없음) */
  private final Map<String, String> extra;
}
