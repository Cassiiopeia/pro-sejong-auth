package kr.suhsaechan.sejongpeia.auth.parser;

import kr.suhsaechan.sejongpeia.common.util.CommonUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/**
 * 포털 메인 소속 표기 파서: span.txt4 "상위 소속 학과" > 상위 소속명
 * 학년도마다 바뀌는 포털 표기 원문 그대로 통과, 값 목록·코드표 검증 없음
 */
public class SejongPortalMainParser {

  // 포털 메인 화면 위치 (바꾸면 상위 소속명이 빈 문자열이 됨)
  private static final String AFFILIATION_SELECTOR = "div.top_myinfo span.txt4";
  private static final String WHITESPACE_PATTERN = "[\\s\\u00A0]+";
  private static final String TOKEN_DELIMITER = " ";
  private static final int PARENT_UNIT_AND_MAJOR_TOKEN_COUNT = 2;

  /** 소속 표기 > 상위 소속명 (규칙: 학과가 끝에 일치, 아니면 토큰 2개일 때만 앞 토큰, 그 외 빈 문자열, 예외 없음) */
  public String parseParentUnitName(String html, String major) {
    try {
      String affiliation = readAffiliationText(html);
      String knownMajor = normalizeWhitespace(major);
      if (affiliation.isEmpty() || affiliation.equals(knownMajor)) {
        return "";
      }
      if (!knownMajor.isEmpty() && affiliation.endsWith(TOKEN_DELIMITER + knownMajor)) {
        return affiliation.substring(0, affiliation.length() - knownMajor.length()).trim();
      }
      String[] tokens = affiliation.split(TOKEN_DELIMITER);
      return tokens.length == PARENT_UNIT_AND_MAJOR_TOKEN_COUNT ? tokens[0] : "";
    } catch (RuntimeException e) {
      return ""; // 틀린 값보다 빈 값 > 파싱 오류도 빈 문자열
    }
  }

  private String readAffiliationText(String html) {
    Element affiliationElement = Jsoup.parse(CommonUtil.trimToEmpty(html)).selectFirst(AFFILIATION_SELECTOR);
    return affiliationElement == null ? "" : normalizeWhitespace(affiliationElement.text());
  }

  private String normalizeWhitespace(String text) {
    return CommonUtil.trimToEmpty(text).replaceAll(WHITESPACE_PATTERN, TOKEN_DELIMITER).trim();
  }
}
