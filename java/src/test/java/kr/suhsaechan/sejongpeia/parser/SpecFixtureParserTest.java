package kr.suhsaechan.sejongpeia.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import kr.suhsaechan.sejongpeia.model.SejongClassicReading;
import kr.suhsaechan.sejongpeia.model.SejongStudentInfo;
import org.junit.jupiter.api.Test;

/**
 * 언어 공통 샘플(spec/fixtures) 파싱 결과 == 기대값 JSON (Python, Node 테스트와 같은 기준)
 */
class SpecFixtureParserTest {

  // Gradle 테스트 작업 디렉터리: java/ > 레포 루트의 spec/fixtures
  private static final Path FIXTURE_DIRECTORY = Path.of("..", "spec", "fixtures");

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void dhc_fixture_matches_expected() throws Exception {
    String html = readFixture("dhc_status.html");
    JsonNode expected = objectMapper.readTree(readFixture("expected_dhc.json"));

    SejongStudentInfo studentInfo = new SejongStudentInfoParser().parseStudentInfo(html);
    SejongClassicReading classicReading = new SejongClassicReadingParser().parse(html);

    Map<String, Object> actual = new LinkedHashMap<>();
    actual.put("studentInfo", studentInfo);
    actual.put("classicReading", classicReading);
    assertEquals(expected, objectMapper.valueToTree(actual));
  }

  @Test
  void sis_fixture_matches_expected() throws Exception {
    String json = readFixture("sis_user_info.json");
    JsonNode expected = objectMapper.readTree(readFixture("expected_sis.json"));
    SejongSisParser parser = new SejongSisParser();

    Map<String, String> actual = new LinkedHashMap<>();
    actual.put("studentId", parser.parseStudentId(json));
    actual.put("name", parser.parseName(json));
    actual.put("major", parser.parseMajor(json));
    actual.put("email", parser.parseEmail(json));
    actual.put("phoneNumber", parser.parsePhoneNumber(json));
    actual.put("englishName", parser.parseEnglishName(json));
    actual.put("userCategory", parser.parseUserCategory(json));
    assertEquals(expected, objectMapper.valueToTree(actual));
  }

  private String readFixture(String fileName) throws Exception {
    return Files.readString(FIXTURE_DIRECTORY.resolve(fileName), StandardCharsets.UTF_8);
  }
}
