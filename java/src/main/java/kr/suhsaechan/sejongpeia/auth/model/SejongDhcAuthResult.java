package kr.suhsaechan.sejongpeia.auth.model;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 대양휴머니티칼리지(DHC) 인증 결과
 * - classic.sejong.ac.kr에서 획득
 * - 고전독서 인증 정보 보장
 */
@Getter
@Builder
public class SejongDhcAuthResult {

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

  /** 고전독서 인증 정보 */
  private final SejongClassicReading classicReading;

  /** 인증 시간 */
  private final LocalDateTime authenticatedAt;

  /** 원본 HTML (디버깅용, 기본 null) */
  private final String rawHtml;

  @Override
  public String toString() {
    return String.format("SejongDhcAuthResult{success=%s, major='%s', studentId='%s', name='%s', grade='%s', status='%s', classicReading=%s, authenticatedAt=%s}",
        success, major, studentId, name, grade, status, classicReading, authenticatedAt);
  }
}
