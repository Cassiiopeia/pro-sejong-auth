package kr.suhsaechan.sejongpeia.model;

import lombok.Builder;
import lombok.Getter;

/**
 * 고전독서 인증 시험 현황
 */
@Getter
@Builder
public class ClassicExamRecord {

  /** 년도/학기 */
  private final String semester;

  /** 영역명 */
  private final String area;

  /** 도서명 */
  private final String bookTitle;

  /** 응시일자 */
  private final String examDate;

  /** 점수 */
  private final String score;

  /** 합격여부 */
  private final String passStatus;

  @Override
  public String toString() {
    return String.format("ClassicExamRecord{semester='%s', area='%s', bookTitle='%s', examDate='%s', score='%s', passStatus='%s'}",
        semester, area, bookTitle, examDate, score, passStatus);
  }
}
