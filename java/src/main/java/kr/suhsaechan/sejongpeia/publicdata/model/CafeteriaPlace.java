package kr.suhsaechan.sejongpeia.publicdata.model;

import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * 학식 식당 1곳 (번호, 이름, 건물, 위치)
 * 값이 없으면 빈 문자열
 */
@Getter
@Builder
@ToString
public class CafeteriaPlace {

  private final String placeId;
  private final String placeName;
  private final String buildingName;
  private final String location;

  /** 메뉴 응답에서 식당 정보로 쓰지 않은 학교 원문 필드 (키는 학교 원문, 빈 값·제외 목록 필드 없음) */
  private final Map<String, String> extra;
}
