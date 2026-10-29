package kr.suhsaechan.sejongpeia.model;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 세종대학교 고전독서 인증 정보
 */
@Getter
@Builder
public class SejongClassicReading {

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
    return String.format("SejongClassicReading{certifications=%d, examRecords=%d, subjectSubstitutions=%d, contestRecords=%d, curriculumRecords=%d}",
        certifications != null ? certifications.size() : 0,
        examRecords != null ? examRecords.size() : 0,
        subjectSubstitutions != null ? subjectSubstitutions.size() : 0,
        contestRecords != null ? contestRecords.size() : 0,
        curriculumRecords != null ? curriculumRecords.size() : 0);
  }
}
