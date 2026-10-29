package kr.suhsaechan.sejong.auth.parser;

import java.util.HashMap;
import java.util.Map;
import kr.suhsaechan.sejong.auth.exception.SejongAuthErrorCode;
import kr.suhsaechan.sejong.auth.exception.SejongAuthException;
import kr.suhsaechan.sejong.auth.model.SejongStudentInfo;
import kr.suhsaechan.sejong.auth.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * 세종대학교 학생 기본 정보 파서
 * - 고전독서인증현황 페이지에서 사용자 정보 테이블 파싱
 */
@Slf4j
public class SejongStudentInfoParser {

  private static final String USER_INFO_SELECTOR =
      ".b-con-box:has(h4.b-h4-tit01:contains(사용자 정보)) table.b-board-table tbody tr";

  // 사용자 정보 표 라벨 (포털 화면 문구 그대로, 바꾸면 파싱 실패)
  private static final String LABEL_MAJOR = "학과명";
  private static final String LABEL_STUDENT_ID = "학번";
  private static final String LABEL_NAME = "이름";
  private static final String LABEL_GRADE = "학년";
  private static final String LABEL_STATUS = "사용자 상태";

  /**
   * 사용자 정보 표 1회 파싱 > 학과, 학번, 이름, 학년, 상태
   *
   * @param html 고전독서인증현황 페이지 HTML
   * @return 학생 기본 정보 (없는 값은 빈 문자열)
   * @throws SejongAuthException 표를 찾지 못하거나 파싱 실패 시
   */
  public SejongStudentInfo parseStudentInfo(String html) {
    Map<String, String> valueByLabel = parseTableData(html);
    return new SejongStudentInfo(
        findLabelValue(valueByLabel, LABEL_MAJOR),
        findLabelValue(valueByLabel, LABEL_STUDENT_ID),
        findLabelValue(valueByLabel, LABEL_NAME),
        findLabelValue(valueByLabel, LABEL_GRADE),
        findLabelValue(valueByLabel, LABEL_STATUS));
  }

  /**
   * HTML에서 학과명 파싱
   *
   * @param html 고전독서인증현황 페이지 HTML
   * @return 학과명
   * @throws SejongAuthException 파싱 실패 시
   */
  public String parseMajor(String html) {
    return findLabelValue(parseTableData(html), LABEL_MAJOR);
  }

  /**
   * HTML에서 학번 파싱
   *
   * @param html 고전독서인증현황 페이지 HTML
   * @return 학번
   * @throws SejongAuthException 파싱 실패 시
   */
  public String parseStudentId(String html) {
    return findLabelValue(parseTableData(html), LABEL_STUDENT_ID);
  }

  /**
   * HTML에서 이름 파싱
   *
   * @param html 고전독서인증현황 페이지 HTML
   * @return 이름
   * @throws SejongAuthException 파싱 실패 시
   */
  public String parseName(String html) {
    return findLabelValue(parseTableData(html), LABEL_NAME);
  }

  /**
   * HTML에서 학년 파싱
   *
   * @param html 고전독서인증현황 페이지 HTML
   * @return 학년
   * @throws SejongAuthException 파싱 실패 시
   */
  public String parseGrade(String html) {
    return findLabelValue(parseTableData(html), LABEL_GRADE);
  }

  /**
   * HTML에서 재학 상태 파싱
   *
   * @param html 고전독서인증현황 페이지 HTML
   * @return 재학 상태
   * @throws SejongAuthException 파싱 실패 시
   */
  public String parseStatus(String html) {
    return findLabelValue(parseTableData(html), LABEL_STATUS);
  }

  /**
   * HTML에서 테이블 데이터 파싱 (내부 캐싱용)
   */
  private Map<String, String> parseTableData(String html) {
    try {
      Document doc = Jsoup.parse(html);
      Map<String, String> valueByLabel = extractTableData(doc);

      if (valueByLabel.isEmpty()) {
        throw new SejongAuthException(SejongAuthErrorCode.PARSE_ERROR,
            "Student info table not found");
      }

      return valueByLabel;
    } catch (SejongAuthException e) {
      throw e;
    } catch (Exception e) {
      throw new SejongAuthException(SejongAuthErrorCode.PARSE_ERROR, e);
    }
  }

  /**
   * 테이블에서 라벨-값 쌍 추출
   */
  private String findLabelValue(Map<String, String> valueByLabel, String label) {
    return CommonUtil.defaultIfBlank(valueByLabel.get(label), "");
  }

  private Map<String, String> extractTableData(Document doc) {
    Map<String, String> valueByLabel = new HashMap<>();

    doc.select(USER_INFO_SELECTOR).forEach(row -> {
      String label = CommonUtil.trimToEmpty(row.select("th").text());
      String labelValue = CommonUtil.trimToEmpty(row.select("td").text());
      if (CommonUtil.hasText(label)) {
        valueByLabel.put(label, labelValue);
      }
    });

    return valueByLabel;
  }
}
