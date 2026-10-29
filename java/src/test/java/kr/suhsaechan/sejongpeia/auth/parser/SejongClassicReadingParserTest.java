package kr.suhsaechan.sejongpeia.auth.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import kr.suhsaechan.sejongpeia.auth.model.SejongClassicReading;
import org.junit.jupiter.api.Test;

/**
 * 고전독서 파서: 사용자 정보 표의 이수 학기, 인증완료 학기, 인증여부 (합성 HTML, 네트워크 없음)
 */
class SejongClassicReadingParserTest {

  private final SejongClassicReadingParser parser = new SejongClassicReadingParser();

  @Test
  void 사용자정보_표의_학기와_인증여부를_읽는다() {
    String html = userInfoTable("""
        <tr><th>이수 학기</th><td>8 학기</td></tr>
        <tr><th>인증완료 학기</th><td>6 학기</td></tr>
        <tr><th>인증여부</th><td>아니오(고전특강 대상자) 2099년도 1학기 대체이수</td></tr>
        """);

    SejongClassicReading reading = parser.parse(html);

    assertEquals("8 학기", reading.getCompletedSemesters());
    assertEquals("6 학기", reading.getCertifiedSemesters());
    assertEquals("아니오(고전특강 대상자) 2099년도 1학기 대체이수", reading.getCertificationStatus());
  }

  @Test
  void 라벨이_없으면_빈문자열() {
    SejongClassicReading reading = parser.parse(userInfoTable("<tr><th>학년</th><td>3</td></tr>"));

    assertEquals("", reading.getCompletedSemesters());
    assertEquals("", reading.getCertifiedSemesters());
    assertEquals("", reading.getCertificationStatus());
  }

  private String userInfoTable(String rows) {
    return "<div class=\"b-con-box\"><h4 class=\"b-h4-tit01\">사용자 정보</h4>"
        + "<table class=\"b-board-table\"><tbody>" + rows + "</tbody></table></div>";
  }
}
