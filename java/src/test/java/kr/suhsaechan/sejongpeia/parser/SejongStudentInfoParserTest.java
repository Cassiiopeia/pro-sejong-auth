package kr.suhsaechan.sejongpeia.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import kr.suhsaechan.sejongpeia.exception.SejongAuthErrorCode;
import kr.suhsaechan.sejongpeia.exception.SejongAuthException;
import kr.suhsaechan.sejongpeia.model.SejongStudentInfo;
import org.junit.jupiter.api.Test;

/**
 * 사용자 정보 표 파싱: 한 번에 5개 값, 개별 메서드와 같은 결과, 표 없으면 PARSE_ERROR
 */
class SejongStudentInfoParserTest {

  // 고전독서인증현황 페이지의 사용자 정보 표 구조 (예시값)
  private static final String SAMPLE_HTML = """
      <div class="b-con-box">
        <h4 class="b-h4-tit01">사용자 정보</h4>
        <table class="b-board-table"><tbody>
          <tr><th>학과명</th><td>컴퓨터공학과</td></tr>
          <tr><th>학번</th><td>21011234</td></tr>
          <tr><th>이름</th><td>홍길동</td></tr>
          <tr><th>학년</th><td>3</td></tr>
          <tr><th>사용자 상태</th><td>재학</td></tr>
        </tbody></table>
      </div>
      """;

  private final SejongStudentInfoParser parser = new SejongStudentInfoParser();

  @Test
  void 사용자정보표를_한번에_파싱한다() {
    SejongStudentInfo studentInfo = parser.parseStudentInfo(SAMPLE_HTML);

    assertEquals("컴퓨터공학과", studentInfo.major());
    assertEquals("21011234", studentInfo.studentId());
    assertEquals("홍길동", studentInfo.name());
    assertEquals("3", studentInfo.grade());
    assertEquals("재학", studentInfo.status());
  }

  @Test
  void 개별_파싱_메서드와_결과가_같다() {
    SejongStudentInfo studentInfo = parser.parseStudentInfo(SAMPLE_HTML);

    assertEquals(parser.parseMajor(SAMPLE_HTML), studentInfo.major());
    assertEquals(parser.parseStudentId(SAMPLE_HTML), studentInfo.studentId());
    assertEquals(parser.parseName(SAMPLE_HTML), studentInfo.name());
    assertEquals(parser.parseGrade(SAMPLE_HTML), studentInfo.grade());
    assertEquals(parser.parseStatus(SAMPLE_HTML), studentInfo.status());
  }

  @Test
  void 사용자정보표가_없으면_PARSE_ERROR() {
    SejongAuthException exception = assertThrows(SejongAuthException.class,
        () -> parser.parseStudentInfo("<html><body>로그인 페이지</body></html>"));

    assertEquals(SejongAuthErrorCode.PARSE_ERROR, exception.getErrorCode());
  }
}
