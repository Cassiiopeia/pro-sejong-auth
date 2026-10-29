package kr.suhsaechan.sejong.auth.service;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import kr.suhsaechan.sejong.auth.client.SejongPortalClient;
import kr.suhsaechan.sejong.auth.client.SejongSisClient;
import kr.suhsaechan.sejong.auth.config.SejongAuthProperties;
import kr.suhsaechan.sejong.auth.exception.SejongAuthException;
import kr.suhsaechan.sejong.auth.model.SejongAuthResult;
import kr.suhsaechan.sejong.auth.parser.SejongClassicReadingParser;
import kr.suhsaechan.sejong.auth.parser.SejongSisParser;
import kr.suhsaechan.sejong.auth.parser.SejongStudentInfoParser;
import org.junit.jupiter.api.Test;

/**
 * 세 언어 교차 검증용: Java 통합 인증 결과 > 필드별 해시 JSON 파일 (scripts/verify-live.sh 가 비교)
 * 환경변수 SEJONG_TEST_STUDENT_ID, SEJONG_TEST_PASSWORD 없으면 건너뜀
 * 틀린 비밀번호 시도는 SEJONG_TEST_INCLUDE_WRONG_PASSWORD=true 일 때만 (포털 잠금 방지, verify-live.sh 만 켬)
 */
class CrossLanguageSummaryTest {

  // 결과 파일: java/build/cross-language/java{,-wrong,-sis-wrong}.json
  private static final Path OUTPUT_DIRECTORY = Path.of("build", "cross-language");
  private static final String INCLUDE_WRONG_PASSWORD_ENV = "SEJONG_TEST_INCLUDE_WRONG_PASSWORD";

  // 키 정렬 JSON (Python json.dumps(sort_keys=True, separators=(",", ":")) 와 같은 모양)
  private final ObjectMapper canonicalMapper = new ObjectMapper()
      .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
      .configure(com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);

  @Test
  void write_live_summary_for_cross_language_comparison() throws Exception {
    String studentId = System.getenv("SEJONG_TEST_STUDENT_ID");
    String password = System.getenv("SEJONG_TEST_PASSWORD");
    assumeTrue(studentId != null && password != null, "SEJONG_TEST_* not set");

    Files.createDirectories(OUTPUT_DIRECTORY);
    Files.writeString(OUTPUT_DIRECTORY.resolve("java.json"), summarize(studentId, password), StandardCharsets.UTF_8);
    if (!"true".equals(System.getenv(INCLUDE_WRONG_PASSWORD_ENV))) {
      return;
    }
    Files.writeString(OUTPUT_DIRECTORY.resolve("java-wrong.json"), summarize(studentId, password + "-wrong"), StandardCharsets.UTF_8);
    Files.writeString(OUTPUT_DIRECTORY.resolve("java-sis-wrong.json"), summarizeSisError(studentId, password + "-wrong"), StandardCharsets.UTF_8);
  }

  private String summarize(String studentId, String password) throws Exception {
    try {
      SejongAuthResult result = createEngine().authenticate(studentId, password);
      Map<String, Object> values = new LinkedHashMap<>();
      values.put("studentId", result.getStudentId());
      values.put("name", result.getName());
      values.put("major", result.getMajor());
      values.put("grade", result.getGrade());
      values.put("status", result.getStatus());
      values.put("email", result.getEmail());
      values.put("phoneNumber", result.getPhoneNumber());
      values.put("englishName", result.getEnglishName());
      values.put("userCategory", result.getUserCategory());
      values.put("classicReading", canonicalMapper.convertValue(result.getClassicReading(), TreeMap.class));

      Map<String, String> fieldHashes = new TreeMap<>();
      for (Map.Entry<String, Object> entry : values.entrySet()) {
        fieldHashes.put(entry.getKey(), hash(canonicalMapper.writeValueAsString(entry.getValue())));
      }
      return canonicalMapper.writeValueAsString(Map.of("fields", fieldHashes));
    } catch (SejongAuthException exception) {
      return canonicalMapper.writeValueAsString(Map.of("error", exception.getErrorCode().name()));
    }
  }

  // SIS 단독 인증의 실패 코드 (성공이면 빈 객체)
  private String summarizeSisError(String studentId, String password) throws Exception {
    try {
      createEngine().authenticateWithSIS(studentId, password);
      return "{}";
    } catch (SejongAuthException exception) {
      return canonicalMapper.writeValueAsString(Map.of("error", exception.getErrorCode().name()));
    }
  }

  private SuhSejongAuthEngine createEngine() {
    SejongAuthProperties properties = new SejongAuthProperties();
    return new SuhSejongAuthEngine(new SejongPortalClient(properties), new SejongStudentInfoParser(),
        new SejongClassicReadingParser(), new SejongSisClient(properties), new SejongSisParser());
  }

  private String hash(String canonicalJson) throws Exception {
    byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonicalJson.getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(digest).substring(0, 16);
  }
}
