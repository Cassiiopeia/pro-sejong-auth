package kr.suhsaechan.sejongpeia.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * SIS 오류 응답 키: spec/contract.json <-> SejongSisParser 상수
 */
class SisContractConstantsTest {

  @Test
  void SIS_오류_키가_contract와_같다() throws Exception {
    JsonNode sis = new ObjectMapper().readTree(Path.of("..", "spec", "contract.json").toFile()).get("sis");

    assertEquals(sis.get("submitErrorKey").asText(), SejongSisParser.SUBMIT_ERROR_KEY);
    assertEquals(sis.get("loggedOutErrorType").asText(), SejongSisParser.LOGGED_OUT_ERROR_TYPE);
    assertEquals(sis.get("extraExcludedKeyPattern").asText(), SejongSisParser.EXTRA_EXCLUDED_KEY_PATTERN);
  }
}
