package kr.suhsaechan.sejongpeia.publicdata;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 학교 응답 행 읽기: 값 > 문자열 변환(null 은 빈 문자열), 매핑에 안 쓴 필드 > extra
 */
final class PublicDataRowReader {

  // extra 제외 필드: 교직원 이름 등 개인 식별, 서버 내부 정보
  private static final Set<String> EXTRA_DENY_LIST = Set.of(
      "CD_ALIAS1", "CD_ALIAS2", "CD_ALIAS3", "CD_ALIAS1_ENG", "CD_ALIAS2_ENG", "CD_ALIAS3_ENG",
      "regname", "regid", "STACK_TRACE");

  private PublicDataRowReader() {
  }

  static String text(JsonNode row, String fieldName) {
    JsonNode value = row.get(fieldName);
    if (value == null || value.isNull()) {
      return "";
    }
    return value.isValueNode() ? value.asText() : value.toString();
  }

  /** 정수 변환 (빈 값·숫자 아님 > 0) */
  static int integer(JsonNode row, String fieldName) {
    String value = text(row, fieldName).trim();
    try {
      return value.isEmpty() ? 0 : (int) Double.parseDouble(value);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  static double decimal(JsonNode row, String fieldName) {
    String value = text(row, fieldName).trim();
    try {
      return value.isEmpty() ? 0.0 : Double.parseDouble(value);
    } catch (NumberFormatException e) {
      return 0.0;
    }
  }

  /** 매핑에 쓰지 않은 필드 (null·빈 값·제외 목록 필드 뺌, 키 정렬) */
  static Map<String, String> extraOf(JsonNode row, Set<String> usedFieldNames) {
    Map<String, String> extra = new TreeMap<>();
    Iterator<Map.Entry<String, JsonNode>> fields = row.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      String fieldName = field.getKey();
      if (usedFieldNames.contains(fieldName) || EXTRA_DENY_LIST.contains(fieldName)) {
        continue;
      }
      String value = text(row, fieldName);
      if (!value.isEmpty()) {
        extra.put(fieldName, value);
      }
    }
    return extra;
  }

  /** 필수 배열 꺼내기 (없거나 배열이 아니면 실패 > 호출부가 격리) */
  static JsonNode requireArray(JsonNode root, String fieldName) {
    JsonNode array = root.get(fieldName);
    if (array == null || !array.isArray()) {
      throw new IllegalStateException("expected array missing : " + fieldName);
    }
    return array;
  }
}
