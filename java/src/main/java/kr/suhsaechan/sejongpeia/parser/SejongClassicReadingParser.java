package kr.suhsaechan.sejongpeia.parser;

import java.util.ArrayList;
import java.util.List;
import kr.suhsaechan.sejongpeia.exception.SejongAuthErrorCode;
import kr.suhsaechan.sejongpeia.exception.SejongAuthException;
import kr.suhsaechan.sejongpeia.model.ClassicCertification;
import kr.suhsaechan.sejongpeia.model.ClassicContestRecord;
import kr.suhsaechan.sejongpeia.model.ClassicExamRecord;
import kr.suhsaechan.sejongpeia.model.ClassicSubjectRecord;
import kr.suhsaechan.sejongpeia.model.SejongClassicReading;
import kr.suhsaechan.sejongpeia.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * 세종대학교 고전독서 인증 정보 파서
 * - 영역별 인증현황, 시험현황, 대체인증, 대회, 교과연계 파싱
 */
@Slf4j
public class SejongClassicReadingParser {

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

  /**
   * HTML에서 고전독서 전체 정보 파싱
   *
   * @param html 고전독서인증현황 페이지 HTML
   * @return 고전독서 인증 정보
   * @throws SejongAuthException 파싱 실패 시
   */
  public SejongClassicReading parse(String html) {
    try {
      Document doc = Jsoup.parse(html);

      SejongClassicReading reading = SejongClassicReading.builder()
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

  /**
   * 리스트에서 안전하게 컬럼 값 추출
   *
   * @param cols 컬럼 리스트
   * @param index 인덱스
   * @return trim된 값 또는 빈 문자열
   */
  private String getColumnValue(List<String> cols, int index) {
    if (cols == null || index < 0 || index >= cols.size()) {
      return "";
    }
    return CommonUtil.trimToEmpty(cols.get(index));
  }
}
