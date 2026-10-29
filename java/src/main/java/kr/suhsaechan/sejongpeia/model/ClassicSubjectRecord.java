package kr.suhsaechan.sejongpeia.model;

import lombok.Builder;
import lombok.Getter;

/**
 * 고전독서 과목 대체/교과연계 인증 현황
 */
@Getter
@Builder
public class ClassicSubjectRecord {

  /** 년도/학기 */
  private final String semester;

  /** 과목명 */
  private final String subjectName;

  /** 영역명 */
  private final String area;

  /** 도서명 */
  private final String bookTitle;

  /** 이수여부/이수구분 */
  private final String completion;

  @Override
  public String toString() {
    return String.format("ClassicSubjectRecord{semester='%s', subjectName='%s', area='%s', bookTitle='%s', completion='%s'}",
        semester, subjectName, area, bookTitle, completion);
  }
}
