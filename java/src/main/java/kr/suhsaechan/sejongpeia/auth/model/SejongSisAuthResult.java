package kr.suhsaechan.sejongpeia.auth.model;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/** 학사정보시스템(SIS) 인증 결과: sjpt.sejong.ac.kr 에서 획득, 연락처 정보 보장 */
@Getter
@Builder
public class SejongSisAuthResult {

  /** 인증 성공 여부 */
  private final boolean success;

  /** 학과명 (예: 컴퓨터공학과) */
  private final String major;

  /** 학번 (예: 20171234) */
  private final String studentId;

  /** 이름 (예: 홍길동) */
  private final String name;

  /** 학년 (SIS에서는 제공하지 않음) */
  private final String grade;

  /** 재학 상태 (SIS에서는 제공하지 않음) */
  private final String status;

  /** 이메일 주소 */
  private final String email;

  /** 전화번호 */
  private final String phoneNumber;

  /** 영어 이름 */
  private final String englishName;

  /** 사용자 구분 (예: 학부생) */
  private final String userCategory;

  /** 학과 코드 (dm_UserInfoSch.DEPT_CD 우선, 없으면 dm_UserInfoGam.DEPT_NO) */
  private final String departmentCode;

  /** 조직분류 코드 (ORGN_CLSF_CD) */
  private final String organizationClassificationCode;

  /** 학적 상태 코드 (STATUS_DIV_CD, 의미 목록 없음) */
  private final String enrollmentStatusCode;

  /** 사용자 구분 코드 (USER_CLSF_CD) */
  private final String userClassificationCode;

  /** 사용자 구분 대분류 코드 (USER_CLSF_L_CD) */
  private final String userClassificationLargeCode;

  /** 사용자 구분 중분류 코드 (USER_CLSF_M_CD) */
  private final String userClassificationMiddleCode;

  /** 역할 코드 (RPSN_ROLE_CD) */
  private final String roleCode;

  /** 역할 이름 (RPSN_ROLE_CD_NM) */
  private final String roleName;

  /** 화면 구분 ID (MENU_SYS_ID) */
  private final String menuSystemId;

  /** 화면 구분 이름 (MENU_SYS_NM) */
  private final String menuSystemName;

  /** 언어 설정 (INTG_LOCALE) */
  private final String locale;

  /** 이름 붙이지 않은 나머지 학교 제공 필드 {"데이터셋.필드": 원문}, 민감 키 제외 (extra 옵션일 때만, 아니면 null) */
  private final java.util.Map<String, String> extra;

  /** 인증 시간 */
  private final LocalDateTime authenticatedAt;

  /** 원본 JSON (디버깅용, 기본 null) */
  private final String rawJson;

  @Override
  public String toString() {
    return String.format("SejongSisAuthResult{success=%s, major='%s', studentId='%s', name='%s', email='%s', phoneNumber='%s', englishName='%s', userCategory='%s', authenticatedAt=%s}",
        success, major, studentId, name, email, phoneNumber, englishName, userCategory, authenticatedAt);
  }
}
