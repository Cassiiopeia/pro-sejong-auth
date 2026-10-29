package kr.suhsaechan.sejong.auth.model;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 학사정보시스템(SIS) 인증 결과
 * - sjpt.sejong.ac.kr에서 획득
 * - 연락처 정보 보장
 */
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
