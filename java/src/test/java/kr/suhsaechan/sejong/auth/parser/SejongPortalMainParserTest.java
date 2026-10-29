package kr.suhsaechan.sejong.auth.parser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 포털 메인 소속 표기 > 상위 소속명: 보수적 규칙 (틀린 값보다 빈 값, 예외 없음)
 */
class SejongPortalMainParserTest {

  private static final Path FIXTURE_DIRECTORY = Path.of("..", "spec", "fixtures");

  private final SejongPortalMainParser parser = new SejongPortalMainParser();

  private String portalMainHtml(String affiliation) {
    return "<div class=\"top_myinfo\"><div class=\"box\"><div class=\"text01\">"
        + "<span class=\"txt1\">홍길동</span><span class=\"txt4\">" + affiliation + "</span></div></div></div>";
  }

  @Test
  void 합성_포털_메인_fixture가_기대값과_같다() throws Exception {
    JsonNode expected = new ObjectMapper().readTree(Files.readString(
        FIXTURE_DIRECTORY.resolve("expected_portal_main.json"), StandardCharsets.UTF_8));
    String html = Files.readString(FIXTURE_DIRECTORY.resolve("portal_main.html"), StandardCharsets.UTF_8);

    assertEquals(expected.get("parentUnitName").asText(), parser.parseParentUnitName(html, expected.get("major").asText()));
  }

  @ParameterizedTest(name = "[{index}] {0} / 학과 {1} > {2}")
  @CsvSource(delimiter = '|', nullValues = "NULL", value = {
      "공과대학 컴퓨터공학과 | 컴퓨터공학과 | 공과대학",
      "공과대학 컴퓨터 공학과 | 컴퓨터 공학과 | 공과대학",
      "College of Engineering Computer Science | Computer Science | College of Engineering",
      "컴퓨터공학과 | 컴퓨터공학과 | ''",
      "컴퓨터공학과 | '' | ''",
      "공과대학 컴퓨터공학 과 | 컴퓨터공학과 | ''",
      "공과대학 컴퓨터공학과 | 컴퓨터공학부 | 공과대학",
      "공과대학 컴퓨터 공학과 | 소프트웨어학과 | ''",
      "College of Engineering Computer Science | '' | ''",
      "공과대학 학과 이름 길다 | 컴퓨터공학과 | ''",
      "'   ' | 컴퓨터공학과 | ''",
  })
  void 소속_표기_규칙(String affiliation, String major, String expected) {
    assertEquals(expected, parser.parseParentUnitName(portalMainHtml(affiliation), major));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "",
      "<html><body>교직원 포털 화면: 내 정보 영역 없음</body></html>",
      "<div class=\"text01\"><span class=\"txt4\">공과대학 컴퓨터공학과</span></div>",
      "<div class=\"top_myinfo\"><span class=\"txt1\">홍길동</span></div>",
  })
  void 요소가_없으면_예외없이_빈_문자열(String html) {
    assertEquals("", parser.parseParentUnitName(html, "컴퓨터공학과"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"<div class=top_myinfo><span class='txt4'>공과대학 <b>컴퓨터공학과", "<<<>>><div", "\u0000\u0001"})
  void 깨진_HTML도_예외를_던지지_않는다(String html) {
    assertDoesNotThrow(() -> parser.parseParentUnitName(html, "컴퓨터공학과"));
  }

  @Test
  void null_입력은_빈_문자열() {
    assertEquals("", parser.parseParentUnitName(null, null));
  }
}
