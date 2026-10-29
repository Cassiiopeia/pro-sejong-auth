package kr.suhsaechan.sejongpeia.publicdata.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * 기숙사 식단 1일
 * 값이 없으면 빈 문자열
 */
@Getter
@Builder
@ToString
public class DormitoryMenuDay {

  private final String date;
  private final String breakfast;
  private final String lunch;
  private final String dinner;
  private final String alternative;
}
