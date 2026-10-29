package kr.suhsaechan.sejongpeia.publicdata.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * 학식 메뉴 1건
 * 값이 없으면 빈 문자열
 */
@Getter
@Builder
@ToString
public class CafeteriaMenuItem {

  private final String buildingName;
  private final String location;
  private final String placeId;
  private final String placeName;
  private final String date;
  private final String day;
  private final String mealTypeId;
  private final String mealName;
  private final String menuName;
  private final String price;
}
