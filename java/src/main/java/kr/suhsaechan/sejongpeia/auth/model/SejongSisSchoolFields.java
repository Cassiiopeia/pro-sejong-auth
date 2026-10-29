package kr.suhsaechan.sejongpeia.auth.model;

import java.util.Map;

/**
 * SIS 학교 제공 코드·구분 원문: 없으면 빈 문자열, 이름 매핑·값 목록 없음
 * extra = 이름 붙이지 않은 나머지 필드 {"데이터셋.필드": 원문}, 민감 키 제외
 */
public record SejongSisSchoolFields(
    String departmentCode,
    String organizationClassificationCode,
    String enrollmentStatusCode,
    String userClassificationCode,
    String userClassificationLargeCode,
    String userClassificationMiddleCode,
    String roleCode,
    String roleName,
    String menuSystemId,
    String menuSystemName,
    String locale,
    Map<String, String> extra) {

  /** SIS 조회 실패 시 값: 전부 null */
  public static SejongSisSchoolFields unavailable() {
    return new SejongSisSchoolFields(null, null, null, null, null, null, null, null, null, null, null, null);
  }
}
