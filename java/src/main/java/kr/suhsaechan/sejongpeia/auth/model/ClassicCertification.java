package kr.suhsaechan.sejongpeia.auth.model;

import lombok.Builder;
import lombok.Getter;

/**
 * 고전독서 영역별 인증현황
 */
@Getter
@Builder
public class ClassicCertification {

  /** 영역명 */
  private final String area;

  /** 이수권수 */
  private final String requiredCount;

  /** 인증권수 */
  private final String certifiedCount;

  @Override
  public String toString() {
    return String.format("ClassicCertification{area='%s', requiredCount='%s', certifiedCount='%s'}",
        area, requiredCount, certifiedCount);
  }
}
