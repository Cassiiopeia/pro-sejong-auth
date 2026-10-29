package kr.suhsaechan.sejongpeia.auth.model;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 세종대학교 고전독서 인증 정보
 */
@Getter
@Builder
public class SejongClassicReading {

  /** 이수 학기 (사용자 정보 표 원문, 예: 8 학기) */
  private final String completedSemesters;

  /** 인증완료 학기 (사용자 정보 표 원문, 예: 6 학기) */
  private final String certifiedSemesters;

  /** 인증여부 (사용자 정보 표 원문 자유 텍스트, 대체이수 사유 포함 가능) */
  private final String certificationStatus;

  /** 영역별 인증현황 */
  private final List<ClassicCertification> certifications;

  /** 인증 시험 현황 */
  private final List<ClassicExamRecord> examRecords;

  /** 과목 대체 인증 현황 */
  private final List<ClassicSubjectRecord> subjectSubstitutions;

  /** 대회 인증 현황 */
  private final List<ClassicContestRecord> contestRecords;

  /** 교과연계 인증 현황 */
  private final List<ClassicSubjectRecord> curriculumRecords;

  @Override
  public String toString() {
    return String.format("SejongClassicReading{completedSemesters='%s', certifiedSemesters='%s', certificationStatus='%s', certifications=%d, examRecords=%d, subjectSubstitutions=%d, contestRecords=%d, curriculumRecords=%d}",
        completedSemesters, certifiedSemesters, certificationStatus,
        certifications != null ? certifications.size() : 0,
        examRecords != null ? examRecords.size() : 0,
        subjectSubstitutions != null ? subjectSubstitutions.size() : 0,
        contestRecords != null ? contestRecords.size() : 0,
        curriculumRecords != null ? curriculumRecords.size() : 0);
  }
}
