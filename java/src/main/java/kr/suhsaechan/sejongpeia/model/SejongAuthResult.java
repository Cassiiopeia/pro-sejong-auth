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

  /** 인증 시간 */
  private final LocalDateTime authenticatedAt;

  /** 원본 HTML (디버깅용, 기본 null) */
  private final String rawHtml;

  @Override
  public String toString() {
    return String.format("SejongAuthResult{success=%s, major='%s', studentId='%s', name='%s', grade='%s', status='%s', email='%s', phoneNumber='%s', englishName='%s', userCategory='%s', parentUnitName='%s', authenticatedAt=%s}",
        success, major, studentId, name, grade, status, email, phoneNumber, englishName, userCategory, parentUnitName, authenticatedAt);
  }
}
