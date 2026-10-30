package kr.suhsaechan.sejongpeia.auth.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kr.suhsaechan.sejongpeia.auth.model.ClassicCertification;
import kr.suhsaechan.sejongpeia.auth.model.ClassicContestRecord;
import kr.suhsaechan.sejongpeia.auth.model.ClassicExamRecord;
import kr.suhsaechan.sejongpeia.auth.model.ClassicSubjectRecord;
import kr.suhsaechan.sejongpeia.auth.model.SejongClassicReading;
import kr.suhsaechan.sejongpeia.common.exception.SejongAuthErrorCode;
import kr.suhsaechan.sejongpeia.common.exception.SejongAuthException;
import kr.suhsaechan.sejongpeia.common.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * 세종대학교 고전독서 인증 정보 파서
 * - 영역별 인증현황, 시험현황, 대체인증, 대회, 교과연계, 이수·인증 학기와 인증여부 파싱
 */
@Slf4j
public class SejongClassicReadingParser {

  private static final String USER_INFO_SELECTOR =
      ".b-con-box:has(h4.b-h4-tit01:contains(사용자 정보)) table.b-board-table tbody tr";
  private static final String CERT_SELECTOR =
      ".b-con-box:has(h4.b-h4-tit01:contains(영역별 인증현황)) table.b-board-table tbody tr";
  private static final String EXAM_SELECTOR =
      ".b-con-box:has(h4.b-h4-tit01:contains(인증 시험 현황)) table.b-board-table tbody tr";
  private static final String SUBJECT_SUB_SELECTOR =
      ".b-con-box:has(h4.b-h4-tit01:contains(과목 대체 인증 현황)) table.b-board-table tbody tr";
  private static final String CONTEST_SELECTOR =
      ".b-con-box:has(h4.b-h4-tit01:contains(대회 인증 현황)) table.b-board-table tbody tr";
  private static final String CURRICULUM_SELECTOR =
      ".b-con-box:has(h4.b-h4-tit01:contains(교과연계 인증 현황)) table.b-board-table tbody tr";

  // 사용자 정보 표 라벨 (포털 화면 문구 그대로, 바꾸면 값이 빈 문자열)
  private static final String LABEL_COMPLETED_SEMESTERS = "이수 학기";
  private static final String LABEL_CERTIFIED_SEMESTERS = "인증완료 학기";
  private static final String LABEL_CERTIFICATION_STATUS = "인증여부";

  /** HTML에서 고전독서 전체 정보 파싱 */
  public SejongClassicReading parse(String html) {
    try {
      Document doc = Jsoup.parse(html);

      Map<String, String> userInfoValueByLabel = parseUserInfoValues(doc);
      SejongClassicReading reading = SejongClassicReading.builder()
          .completedSemesters(userInfoValueByLabel.getOrDefault(LABEL_COMPLETED_SEMESTERS, ""))
          .certifiedSemesters(userInfoValueByLabel.getOrDefault(LABEL_CERTIFIED_SEMESTERS, ""))
          .certificationStatus(userInfoValueByLabel.getOrDefault(LABEL_CERTIFICATION_STATUS, ""))
          .certifications(parseCertifications(doc))
          .examRecords(parseExamRecords(doc))
          .subjectSubstitutions(parseSubjectSubstitutions(doc))
          .contestRecords(parseContestRecords(doc))
          .curriculumRecords(parseCurriculumRecords(doc))
          .build();

      log.debug("[sejong auth] classic reading parsed : {}", reading);
      return reading;

    } catch (Exception e) {
      throw new SejongAuthException(SejongAuthErrorCode.PARSE_ERROR, e);
    }
  }

  /**
   * 사용자 정보 표 > 라벨별 값 (고전독서 관련 3개 라벨만 사용)
   */
  private Map<String, String> parseUserInfoValues(Document doc) {
    Map<String, String> valueByLabel = new HashMap<>();
    for (Element row : doc.select(USER_INFO_SELECTOR)) {
      String label = CommonUtil.trimToEmpty(row.select("th").text());
      if (CommonUtil.hasText(label)) {
        valueByLabel.put(label, CommonUtil.trimToEmpty(row.select("td").text()));
      }
    }
    return valueByLabel;
  }

  /**
   * 영역별 인증현황 파싱
   */
  private List<ClassicCertification> parseCertifications(Document doc) {
    List<ClassicCertification> list = new ArrayList<>();
    Elements rows = doc.select(CERT_SELECTOR);

    for (Element row : rows) {
      String area = CommonUtil.trimToEmpty(row.select("th").text());
      Elements tds = row.select("td");

      if (CommonUtil.hasText(area) && tds.size() >= 2) {
        list.add(ClassicCertification.builder()
            .area(area)
            .requiredCount(CommonUtil.trimToEmpty(tds.get(0).text()))
            .certifiedCount(CommonUtil.trimToEmpty(tds.get(1).text()))
            .build());
      }
    }

    return list;
  }

  /**
   * 인증 시험 현황 파싱
   */
  private List<ClassicExamRecord> parseExamRecords(Document doc) {
    List<ClassicExamRecord> list = new ArrayList<>();
    Elements rows = doc.select(EXAM_SELECTOR);

    for (Element row : rows) {
      List<String> cols = row.select("td").eachText();

      if (cols.size() >= 6) {
        list.add(ClassicExamRecord.builder()
            .semester(getColumnValue(cols, 0))
            .area(getColumnValue(cols, 1))
            .bookTitle(getColumnValue(cols, 2))
            .examDate(getColumnValue(cols, 3))
            .score(getColumnValue(cols, 4))
            .passStatus(getColumnValue(cols, 5))
            .build());
      }
    }

    return list;
  }

  /**
   * 과목 대체 인증 현황 파싱
   */
  private List<ClassicSubjectRecord> parseSubjectSubstitutions(Document doc) {
    List<ClassicSubjectRecord> list = new ArrayList<>();
    Elements rows = doc.select(SUBJECT_SUB_SELECTOR);

    for (Element row : rows) {
      List<String> cols = row.select("td").eachText();

      if (cols.size() >= 5) {
        list.add(ClassicSubjectRecord.builder()
            .semester(getColumnValue(cols, 0))
            .subjectName(getColumnValue(cols, 1))
            .area(getColumnValue(cols, 2))
            .bookTitle(getColumnValue(cols, 3))
            .completion(getColumnValue(cols, 4))
            .build());
      }
    }

    return list;
  }

  /**
   * 대회 인증 현황 파싱
   */
  private List<ClassicContestRecord> parseContestRecords(Document doc) {
    List<ClassicContestRecord> list = new ArrayList<>();
    Elements rows = doc.select(CONTEST_SELECTOR);

    for (Element row : rows) {
      List<String> cols = row.select("td").eachText();

      if (cols.size() >= 4) {
        list.add(ClassicContestRecord.builder()
            .semester(getColumnValue(cols, 0))
            .contestName(getColumnValue(cols, 1))
            .area(getColumnValue(cols, 2))
            .bookTitle(getColumnValue(cols, 3))
            .build());
      }
    }

    return list;
  }

  /**
   * 교과연계 인증 현황 파싱
   */
  private List<ClassicSubjectRecord> parseCurriculumRecords(Document doc) {
    List<ClassicSubjectRecord> list = new ArrayList<>();
    Elements rows = doc.select(CURRICULUM_SELECTOR);

    for (Element row : rows) {
      List<String> cols = row.select("td").eachText();

      if (cols.size() >= 5) {
        list.add(ClassicSubjectRecord.builder()
            .semester(getColumnValue(cols, 0))
            .subjectName(getColumnValue(cols, 1))
            .area(getColumnValue(cols, 2))
            .bookTitle(getColumnValue(cols, 3))
            .completion(getColumnValue(cols, 4))
            .build());
      }
    }

    return list;
  }

  /** 리스트에서 안전하게 컬럼 값 추출 */
  private String getColumnValue(List<String> cols, int index) {
    if (cols == null || index < 0 || index >= cols.size()) {
      return "";
    }
    return CommonUtil.trimToEmpty(cols.get(index));
  }
}
