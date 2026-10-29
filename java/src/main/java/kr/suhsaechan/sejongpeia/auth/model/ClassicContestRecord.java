package kr.suhsaechan.sejongpeia.auth.model;

import lombok.Builder;
import lombok.Getter;

/**
 * 고전독서 대회 인증 현황
 */
@Getter
@Builder
public class ClassicContestRecord {

  /** 년도/학기 */
  private final String semester;

  /** 대회명 */
  private final String contestName;

  /** 영역명 */
  private final String area;

  /** 도서명 */
  private final String bookTitle;

  @Override
  public String toString() {
    return String.format("ClassicContestRecord{semester='%s', contestName='%s', area='%s', bookTitle='%s'}",
        semester, contestName, area, bookTitle);
  }
}
