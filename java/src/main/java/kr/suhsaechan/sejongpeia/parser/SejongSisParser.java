package kr.suhsaechan.sejongpeia.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.suhsaechan.sejongpeia.exception.SejongAuthErrorCode;
import kr.suhsaechan.sejongpeia.exception.SejongAuthException;
import kr.suhsaechan.sejongpeia.util.CommonUtil;
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
   * JsonNode에서 텍스트 값 추출
   */
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
