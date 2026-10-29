package kr.suhsaechan.sejongpeia.publicdata.model;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * 기숙사 공지 목록: 전체 건수, 고정 공지, 일반 공지
 * 조회 실패 시 total 0과 빈 목록
 */
@Getter
@Builder
@ToString
public class DormitoryNotices {

  private final int total;
  private final List<DormitoryNotice> pinned;
  private final List<DormitoryNotice> items;
}
