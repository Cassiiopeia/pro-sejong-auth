package kr.suhsaechan.sejongpeia.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.suhsaechan.sejongpeia.exception.SejongAuthErrorCode;
import kr.suhsaechan.sejongpeia.exception.SejongAuthException;
import kr.suhsaechan.sejongpeia.model.SejongSisSchoolFields;
import kr.suhsaechan.sejongpeia.model.SisBusinessPeriod;
import kr.suhsaechan.sejongpeia.model.SisRunValues;
import kr.suhsaechan.sejongpeia.util.CommonUtil;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * 세종대학교 학사정보시스템(SIS) 응답 파서
 * - initUserInfo.do API 응답 JSON 파싱
 */
@Slf4j
public class SejongSisParser {

  private final ObjectMapper objectMapper = new ObjectMapper();

  // SIS 오류 응답: 200 + {"_SUBMIT_ERROR_": {"ERRTYPE": ...}}, 로그인 세션 없음(틀린 비밀번호) = LOGOUT
  static final String SUBMIT_ERROR_KEY = "_SUBMIT_ERROR_";
  static final String LOGGED_OUT_ERROR_TYPE = "LOGOUT";

  // SIS 데이터셋 조회 순서 (같은 키는 앞 데이터셋 값 우선)
  private static final List<String> DATASET_NAMES = List.of("dm_UserInfoSch", "dm_UserInfoGam", "dm_UserInfo");
  // 학과 코드 기준은 DEPT_CD, 없으면 DEPT_NO
  private static final List<String> DEPARTMENT_CODE_KEYS = List.of("DEPT_CD", "DEPT_NO");
  // 기존 필드가 읽는 키 (extra 에서 제외)
  private static final Set<String> NAMED_FIELD_KEYS = Set.of(
      "DEPT_CD", "DEPT_NO", "ORGN_CLSF_CD", "STATUS_DIV_CD", "USER_CLSF_CD", "USER_CLSF_L_CD", "USER_CLSF_M_CD",
      "RPSN_ROLE_CD", "RPSN_ROLE_CD_NM", "MENU_SYS_ID", "MENU_SYS_NM", "INTG_LOCALE",
      "INTG_USR_NO", "INTG_USR_NM", "INTG_ENG_NM", "DEPT_NM", "USER_EMAIL", "USER_PHONE_NO1", "USER_PHONE_NO2",
      "USER_PHONE_NO3", "NM_ENG", "USER_CLSF_CD_NM");
  // extra 제외 키 (대소문자 무시): 세션값, 접속 IP, 비밀번호·계정 상태, 주민번호, 계좌, 주소, 직원 이름(ALIAS), 연락처, 생년월일 (spec/contract.json sis.extraExcludedKeyPattern)
  static final String EXTRA_EXCLUDED_KEY_PATTERN =
      "(PWD|PASS|JUMIN|RRN|RESI|SSN|BANK|ACNT|ACCT|ACCOUNT|ADDR|ZIP|POST|ALIAS|SESS|TOKEN|SECRET|BIRTH|BRTH|VALID"
          + "|PHONE|TEL|MOBILE|MAIL|LOGIN|RUNNING|(^|_)IP($|_)|_YN$)";
  private static final Pattern EXTRA_EXCLUDED_KEY = Pattern.compile(EXTRA_EXCLUDED_KEY_PATTERN, Pattern.CASE_INSENSITIVE);

  /**
   * JSON에서 학교 제공 코드·구분과 extra 파싱 (코드는 데이터셋 순서로 처음 찾은 값, 없으면 빈 문자열)
   *
   * @param json initUserInfo.do API 응답 JSON
   * @throws SejongAuthException 파싱 실패 시
   */
  public SejongSisSchoolFields parseSchoolFields(String json) {
    JsonNode root = readRoot(json);
    return new SejongSisSchoolFields(
        findCode(root, DEPARTMENT_CODE_KEYS), findCode(root, List.of("ORGN_CLSF_CD")), findCode(root, List.of("STATUS_DIV_CD")),
        findCode(root, List.of("USER_CLSF_CD")), findCode(root, List.of("USER_CLSF_L_CD")), findCode(root, List.of("USER_CLSF_M_CD")),
        findCode(root, List.of("RPSN_ROLE_CD")), findCode(root, List.of("RPSN_ROLE_CD_NM")),
        findCode(root, List.of("MENU_SYS_ID")), findCode(root, List.of("MENU_SYS_NM")), findCode(root, List.of("INTG_LOCALE")),
        collectExtra(root));
  }

  /**
   * JSON에서 요청 실행 파라미터 원천값 파싱 (하나라도 비면 PARSE_ERROR)
   */
  public SisRunValues parseRunValues(String json) {
    JsonNode root = readRoot(json);
    JsonNode userInfo = root.path("dm_UserInfo");
    if (userInfo.isMissingNode()) {
      throw new SejongAuthException(SejongAuthErrorCode.PARSE_ERROR, "dm_UserInfo field not found");
    }
    String loginDate = getTextValue(root.path("dm_UserInfoGam"), "LOGIN_DT").trim();
    if (loginDate.isEmpty()) {
      loginDate = getTextValue(root.path("dm_UserInfoSch"), "LOGIN_DT").trim();
    }
    SisRunValues runValues = new SisRunValues(getTextValue(userInfo, "INTG_USR_NO").trim(), loginDate,
        getTextValue(userInfo, "RUNNING_SEJONG").trim());
    if (runValues.userNumber().isEmpty() || runValues.loginDate().isEmpty() || runValues.sessionKey().isEmpty()) {
      throw new SejongAuthException(SejongAuthErrorCode.PARSE_ERROR, "SIS run values not found");
    }
    return runValues;
  }

  /**
   * initUserRole 응답에서 화면 로그인 일시 dm_UserRole.PG_LOGIN_DT 파싱 (없으면 PARSE_ERROR)
   */
  public String parseScreenLoginDate(String json) {
    String screenLoginDate = getTextValue(readRoot(json).path("dm_UserRole"), "PG_LOGIN_DT").trim();
    if (screenLoginDate.isEmpty()) {
      throw new SejongAuthException(SejongAuthErrorCode.PARSE_ERROR, "dm_UserRole.PG_LOGIN_DT not found");
    }
    return screenLoginDate;
  }

  // 학사일정 행에서 이름 붙여 읽는 필드 (나머지는 extra)
  private static final Set<String> BUSINESS_PERIOD_FIELD_KEYS = Set.of(
      "PERIOD", "START_DT", "TO_DT", "SCHEDULE", "CORS_SCHE_GRP_CD_NM", "TARGET_AUTH", "SYS_NM", "YEAR_SMT", "TMS", "ORGN_CLSF_CD", "PRT_ORD");

  /**
   * 학사일정조회 doList 응답 > 업무 기간 목록 (dl_mainList 가 배열이 아니면 PARSE_ERROR, 이상한 행도 버리지 않음)
   */
  public List<SisBusinessPeriod> parseBusinessPeriods(String json) {
    JsonNode rows = readRoot(json).path("dl_mainList");
    if (!rows.isArray()) {
      throw new SejongAuthException(SejongAuthErrorCode.PARSE_ERROR, "dl_mainList not found");
    }
    List<SisBusinessPeriod> periods = new java.util.ArrayList<>();
    for (JsonNode row : rows) {
      Map<String, String> extra = new LinkedHashMap<>();
      row.fields().forEachRemaining(entry -> {
        boolean isScalar = entry.getValue().isTextual() || entry.getValue().isNumber();
        if (isScalar && !BUSINESS_PERIOD_FIELD_KEYS.contains(entry.getKey()) && !EXTRA_EXCLUDED_KEY.matcher(entry.getKey()).find()) {
          extra.put(entry.getKey(), entry.getValue().asText());
        }
      });
      periods.add(new SisBusinessPeriod(getTextValue(row, "PERIOD"), getTextValue(row, "START_DT"), getTextValue(row, "TO_DT"),
          getTextValue(row, "SCHEDULE"), getTextValue(row, "CORS_SCHE_GRP_CD_NM"), getTextValue(row, "TARGET_AUTH"),
          getTextValue(row, "SYS_NM"), getTextValue(row, "YEAR_SMT"), getTextValue(row, "TMS"), getTextValue(row, "ORGN_CLSF_CD"),
          getTextValue(row, "PRT_ORD"), java.util.Collections.unmodifiableMap(extra)));
    }
    return List.copyOf(periods);
  }

  /**
   * 후보 키 순서 > 데이터셋 순서로 처음 찾은 비어 있지 않은 값
   */
  private String findCode(JsonNode root, List<String> keys) {
    for (String key : keys) {
      for (String datasetName : DATASET_NAMES) {
        String value = getTextValue(root.path(datasetName), key).trim();
        if (!value.isEmpty()) {
          return value;
        }
      }
    }
    return "";
  }

  /**
   * 이름 붙이지 않은 문자열·숫자 필드 > {"데이터셋.필드": 원문} (민감 키·중첩 값 제외)
   */
  private Map<String, String> collectExtra(JsonNode root) {
    Map<String, String> extra = new LinkedHashMap<>();
    for (String datasetName : DATASET_NAMES) {
      JsonNode dataset = root.path(datasetName);
      if (!dataset.isObject()) {
        continue;
      }
      dataset.fields().forEachRemaining(entry -> {
        JsonNode value = entry.getValue();
        boolean isScalar = value.isTextual() || (value.isNumber());
        if (isScalar && !NAMED_FIELD_KEYS.contains(entry.getKey()) && !EXTRA_EXCLUDED_KEY.matcher(entry.getKey()).find()) {
          extra.put(datasetName + "." + entry.getKey(), value.asText());
        }
      });
    }
    return extra;
  }

  /**
   * initUserInfo 오류 응답 판정: LOGOUT > AUTHENTICATION_FAILED, 그 외 오류 > DATA_FETCH_FAILED (JSON 아니면 통과)
   *
   * @param json initUserInfo.do API 응답 JSON
   * @throws SejongAuthException 오류 응답일 때
   */
  public void ensureLoggedIn(String json) {
    JsonNode submitError;
    try {
      submitError = objectMapper.readTree(json).path(SUBMIT_ERROR_KEY);
    } catch (Exception e) {
      return; // JSON 아님 > 파싱 단계에서 PARSE_ERROR
    }
    if (submitError.isMissingNode() || !submitError.isObject()) {
      return;
    }
    String errorType = getTextValue(submitError, "ERRTYPE");
    if (LOGGED_OUT_ERROR_TYPE.equals(errorType)) {
      throw new SejongAuthException(SejongAuthErrorCode.AUTHENTICATION_FAILED);
    }
    throw new SejongAuthException(SejongAuthErrorCode.DATA_FETCH_FAILED, "SIS returned an error : " + errorType);
  }

  /**
   * JSON에서 학과명 파싱
   *
   * @param json initUserInfo.do API 응답 JSON
   * @return 학과명
   * @throws SejongAuthException 파싱 실패 시
   */
  public String parseMajor(String json) {
    JsonNode root = readRoot(json);
    JsonNode dmUserInfoGam = root.path("dm_UserInfoGam");
    JsonNode dmUserInfoSch = root.path("dm_UserInfoSch");

    String major = getTextValue(dmUserInfoGam, "DEPT_NM");
    if (CommonUtil.isBlank(major)) {
      major = getTextValue(dmUserInfoSch, "DEPT_NM");
    }
    return CommonUtil.defaultIfBlank(major, "");
  }

  /**
   * JSON에서 학번 파싱
   *
   * @param json initUserInfo.do API 응답 JSON
   * @return 학번
   * @throws SejongAuthException 파싱 실패 시
   */
  public String parseStudentId(String json) {
    JsonNode root = readRoot(json);
    JsonNode dmUserInfo = root.path("dm_UserInfo");

    if (dmUserInfo.isMissingNode()) {
      throw new SejongAuthException(SejongAuthErrorCode.PARSE_ERROR,
          "dm_UserInfo field not found");
    }

    String studentId = getTextValue(dmUserInfo, "INTG_USR_NO");
    return CommonUtil.defaultIfBlank(studentId, "");
  }

  /**
   * JSON에서 이름 파싱
   *
   * @param json initUserInfo.do API 응답 JSON
   * @return 이름
   * @throws SejongAuthException 파싱 실패 시
   */
  public String parseName(String json) {
    JsonNode root = readRoot(json);
    JsonNode dmUserInfo = root.path("dm_UserInfo");

    String name = getTextValue(dmUserInfo, "INTG_USR_NM");
    return CommonUtil.defaultIfBlank(name, "");
  }

  /**
   * JSON에서 이메일 파싱
   *
   * @param json initUserInfo.do API 응답 JSON
   * @return 이메일 주소
   * @throws SejongAuthException 파싱 실패 시
   */
  public String parseEmail(String json) {
    JsonNode root = readRoot(json);
    JsonNode dmUserInfoGam = root.path("dm_UserInfoGam");
    String email = getTextValue(dmUserInfoGam, "USER_EMAIL");
    return CommonUtil.defaultIfBlank(email, "");
  }

  /**
   * JSON에서 전화번호 파싱
   *
   * @param json initUserInfo.do API 응답 JSON
   * @return 전화번호 (010-1234-5678 형식)
   * @throws SejongAuthException 파싱 실패 시
   */
  public String parsePhoneNumber(String json) {
    JsonNode root = readRoot(json);
    JsonNode dmUserInfoGam = root.path("dm_UserInfoGam");
    String phoneNumber = buildPhoneNumber(dmUserInfoGam);
    return CommonUtil.defaultIfBlank(phoneNumber, "");
  }

  /**
   * JSON에서 영어 이름 파싱
   *
   * @param json initUserInfo.do API 응답 JSON
   * @return 영어 이름
   * @throws SejongAuthException 파싱 실패 시
   */
  public String parseEnglishName(String json) {
    JsonNode root = readRoot(json);
    JsonNode dmUserInfo = root.path("dm_UserInfo");
    JsonNode dmUserInfoSch = root.path("dm_UserInfoSch");

    String englishName = getTextValue(dmUserInfo, "INTG_ENG_NM");
    if (CommonUtil.isBlank(englishName)) {
      englishName = getTextValue(dmUserInfoSch, "NM_ENG");
    }
    return CommonUtil.defaultIfBlank(englishName, "");
  }

  /**
   * JSON에서 사용자 구분 파싱 (dm_UserInfoSch.USER_CLSF_CD_NM, 예: 학부생)
   *
   * @param json initUserInfo.do API 응답 JSON
   * @return 사용자 구분 (없으면 빈 문자열)
   * @throws SejongAuthException 파싱 실패 시
   */
  public String parseUserCategory(String json) {
    JsonNode dmUserInfoSch = readRoot(json).path("dm_UserInfoSch");
    return CommonUtil.defaultIfBlank(getTextValue(dmUserInfoSch, "USER_CLSF_CD_NM"), "");
  }

  /**
   * JSON 문자열 > 루트 노드 (형식 오류는 PARSE_ERROR)
   */
  private JsonNode readRoot(String json) {
    try {
      return objectMapper.readTree(json);
    } catch (Exception e) {
      throw new SejongAuthException(SejongAuthErrorCode.PARSE_ERROR, e);
    }
  }

  /**
   * JsonNode 필드 > 텍스트 (노드·필드 없음 또는 null은 빈 문자열)
   */
  private String getTextValue(JsonNode node, String fieldName) {
    if (node == null || node.isMissingNode()) {
      return "";
    }
    JsonNode field = node.path(fieldName);
    if (field.isMissingNode() || field.isNull()) {
      return "";
    }
    return field.asText("");
  }

  /**
   * 전화번호 조합 (NO1-NO2-NO3)
   */
  private String buildPhoneNumber(JsonNode dmUserInfoGam) {
    if (dmUserInfoGam == null || dmUserInfoGam.isMissingNode()) {
      return "";
    }

    String no1 = getTextValue(dmUserInfoGam, "USER_PHONE_NO1");
    String no2 = getTextValue(dmUserInfoGam, "USER_PHONE_NO2");
    String no3 = getTextValue(dmUserInfoGam, "USER_PHONE_NO3");

    // 모두 비어있으면 빈 문자열
    if (CommonUtil.isBlank(no1) && CommonUtil.isBlank(no2) && CommonUtil.isBlank(no3)) {
      return "";
    }

    // 조합
    StringBuilder sb = new StringBuilder();
    if (CommonUtil.hasText(no1)) {
      sb.append(no1);
    }
    if (CommonUtil.hasText(no2)) {
      if (sb.length() > 0) sb.append("-");
      sb.append(no2);
    }
    if (CommonUtil.hasText(no3)) {
      if (sb.length() > 0) sb.append("-");
      sb.append(no3);
    }

    return sb.toString();
  }
}
