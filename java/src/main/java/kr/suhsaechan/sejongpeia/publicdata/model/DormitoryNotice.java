package kr.suhsaechan.sejongpeia.publicdata.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * 기숙사 공지 1건 (작성자 정보 제외)
 * 값이 없으면 빈 문자열
 */
@Getter
@Builder
@ToString
public class DormitoryNotice {

  private final String id;
  private final String title;
  private final String date;
  private final int views;
  private final String fileName;
}
