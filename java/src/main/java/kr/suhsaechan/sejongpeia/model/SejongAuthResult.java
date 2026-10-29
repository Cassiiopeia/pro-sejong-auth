package kr.suhsaechan.sejongpeia.model;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 세종대학교 통합 인증 결과
 * - DHC(대양휴머니티칼리지) + SIS(학사정보시스템) 정보 통합
 */
@Getter
@Builder
public class SejongAuthResult {

  /** 인증 성공 여부 */
  private final boolean success;

  /** 학과명 (예: 컴퓨터공학과) */
  private final String major;

  /** 학번 (예: 20171234) */
  private final String studentId;

  /** 이름 (예: 홍길동) */
  private final String name;

  /** 학년 (예: 4학년) */
  private final String grade;

  /** 재학 상태 (예: 재학, 휴학, 졸업) */
  private final String status;

  /** 고전독서 인증 정보 (DHC에서 획득) */
  private final SejongClassicReading classicReading;

  /** 이메일 주소 (SIS에서 획득) */
  private final String email;

  /** 전화번호 (SIS에서 획득) */
  private final String phoneNumber;

  /** 영어 이름 (SIS에서 획득) */
  private final String englishName;

  /** 사용자 구분 (SIS에서 획득, 예: 학부생) */
  private final String userCategory;

  /**
   * 학과의 상위 소속 이름 (단과대학 또는 학부): 포털 메인 화면 표기에서 추출한 부가 정보, 조회 실패 시 null, 학적 구분에 따라 빈 문자열일 수 있음, 인증 판정에 쓰지 말 것
   * 학년도마다 바뀌는 포털 표기 원문, 값 목록으로 분기하지 말 것
   */
  private final String parentUnitName;

  /** 학과 코드 (dm_UserInfoSch.DEPT_CD 우선, 없으면 dm_UserInfoGam.DEPT_NO) (SIS에서 획득, SIS 조회 실패 시 null) */
  private final String departmentCode;

  /** 조직분류 코드 (ORGN_CLSF_CD) (SIS에서 획득, SIS 조회 실패 시 null) */
  private final String organizationClassificationCode;

  /** 학적 상태 코드 (STATUS_DIV_CD, 의미 목록 없음) (SIS에서 획득, SIS 조회 실패 시 null) */
  private final String enrollmentStatusCode;

  /** 사용자 구분 코드 (USER_CLSF_CD) (SIS에서 획득, SIS 조회 실패 시 null) */
  private final String userClassificationCode;

  /** 사용자 구분 대분류 코드 (USER_CLSF_L_CD) (SIS에서 획득, SIS 조회 실패 시 null) */
  private final String userClassificationLargeCode;

  /** 사용자 구분 중분류 코드 (USER_CLSF_M_CD) (SIS에서 획득, SIS 조회 실패 시 null) */
  private final String userClassificationMiddleCode;

  /** 역할 코드 (RPSN_ROLE_CD) (SIS에서 획득, SIS 조회 실패 시 null) */
  private final String roleCode;

  /** 역할 이름 (RPSN_ROLE_CD_NM) (SIS에서 획득, SIS 조회 실패 시 null) */
  private final String roleName;

  /** 화면 구분 ID (MENU_SYS_ID) (SIS에서 획득, SIS 조회 실패 시 null) */
  private final String menuSystemId;

  /** 화면 구분 이름 (MENU_SYS_NM) (SIS에서 획득, SIS 조회 실패 시 null) */
  private final String menuSystemName;

  /** 언어 설정 (INTG_LOCALE) (SIS에서 획득, SIS 조회 실패 시 null) */
  private final String locale;

  /** 이름 붙이지 않은 나머지 학교 제공 필드 {"데이터셋.필드": 원문}, 민감 키 제외 (extra 옵션일 때만, 아니면 null) */
  private final java.util.Map<String, String> extra;

  /** 인증 시간 */
  private final LocalDateTime authenticatedAt;

  /** DHC 원본 HTML (원본 옵션일 때만, 기본 null) */
  private final String rawHtml;

  /** SIS 원본 JSON (원본 옵션일 때만, SIS 조회 실패 시 null): 세션값·접속 IP 포함이라 외부 전달 금지 */
  private final String rawJson;

  @Override
  public String toString() {
    return String.format("SejongAuthResult{success=%s, major='%s', studentId='%s', name='%s', grade='%s', status='%s', email='%s', phoneNumber='%s', englishName='%s', userCategory='%s', parentUnitName='%s', authenticatedAt=%s}",
        success, major, studentId, name, grade, status, email, phoneNumber, englishName, userCategory, parentUnitName, authenticatedAt);
  }
}
