package kr.suhsaechan.sejongpeia.publicdata.model;

import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * 캠퍼스 장소 1건 (위도·경도 포함)
 * 값이 없으면 빈 문자열
 */
@Getter
@Builder
@ToString
public class CampusPlace {

  private final String id;
  private final String name;
  private final String nameEn;
  private final String placeType;
  private final String category;
  private final double latitude;
  private final double longitude;
  private final String info;
  private final String phone;
  private final String description;

  /** 매핑에 쓰지 않은 학교 원문 필드 (키는 학교 원문, 빈 값·제외 목록 필드 없음) */
  private final Map<String, String> extra;
}
