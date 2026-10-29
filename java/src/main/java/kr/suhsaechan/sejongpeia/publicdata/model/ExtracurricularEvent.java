package kr.suhsaechan.sejongpeia.publicdata.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * 비교과 일정 1건
 * 값이 없으면 빈 문자열
 */
@Getter
@Builder
@ToString
public class ExtracurricularEvent {

  private final String id;
  private final String title;
  private final String start;
  private final String end;
  private final String categoryTitle;
  private final String url;
  private final String description;
  private final String location;
}
