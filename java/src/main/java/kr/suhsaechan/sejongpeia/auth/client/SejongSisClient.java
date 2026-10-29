package kr.suhsaechan.sejongpeia.auth.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import kr.suhsaechan.sejongpeia.auth.model.SejongSisSchoolFields;
import kr.suhsaechan.sejongpeia.auth.model.SisRunValues;
import kr.suhsaechan.sejongpeia.auth.parser.SejongSisParser;
import kr.suhsaechan.sejongpeia.common.config.SejongAuthProperties;
import kr.suhsaechan.sejongpeia.common.exception.SejongAuthErrorCode;
import kr.suhsaechan.sejongpeia.common.exception.SejongAuthException;
import kr.suhsaechan.sejongpeia.common.http.SejongHttpSupport;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 세종대학교 학사정보시스템(SIS) HTTP 클라이언트: sjpt SSO, initUserInfo 조회 (학생 연락처)
 * 단독 인증은 자체 로그인, 통합 인증은 포털 클라이언트가 연 세션을 이어서 사용
 */
@Slf4j
public class SejongSisClient {

  // 포털 로그인 후 SSO 대상: 학사정보시스템 SSO 진입점
  static final String SJPT_RETURN_URL = "sjpt.sejong.ac.kr/main/view/Login/doSsoLogin.do?p=";
  // 스킴 붙인 SSO 주소 (SJPT_RETURN_URL 에서 파생)
  static final String SJPT_SSO_URL = "https://" + SJPT_RETURN_URL;
  static final String SJPT_INIT_USER_INFO_URL = "https://sjpt.sejong.ac.kr/main/sys/UserInfo/initUserInfo.do";
  static final String SJPT_ORIGIN = "https://sjpt.sejong.ac.kr";
  // 학사일정조회 화면 (브라우저가 화면을 열 때 보내는 실행 파라미터와 같은 값): 메뉴 목록 > 화면 열기(initUserRole) > 목록(doList)
  static final String SJPT_MENU_LIST_URL = "https://sjpt.sejong.ac.kr/main/view/Menu/doListUserMenuListLeft.do";
  static final String SJPT_USER_ROLE_URL = "https://sjpt.sejong.ac.kr/main/sys/UserRole/initUserRole.do";
  static final String SJPT_SCHEDULE_LIST_URL = "https://sjpt.sejong.ac.kr/sch/sch/sud/SudBaseScheSelfServQ/doList.do";
  static final String SCHEDULE_PROGRAM_KEY = "SELF_STUDSELF_SUB_20SELF_MENU_10SudBaseScheSelfServQ";
  static final String SCHEDULE_SYSTEM_KEY = "SCH";
  static final String MENU_SYSTEM_DIV = "SCH";
  private static final String SIS_JSON_CONTENT_TYPE = "application/json; charset=\"UTF-8\"";

  private final SejongAuthProperties properties;
  private final OkHttpClient baseClient;
  private final SejongSisParser sisParser = new SejongSisParser();
  private final ObjectMapper objectMapper = new ObjectMapper();

  public SejongSisClient(SejongAuthProperties properties) {
    this(properties, SejongHttpSupport.buildBaseClient(properties));
  }

  // 테스트: 가짜 응답 클라이언트 주입
  SejongSisClient(SejongAuthProperties properties, OkHttpClient baseClient) {
    this.properties = properties;
    this.baseClient = baseClient;
  }

  /**
   * 세종포털 로그인 후 학사정보시스템에서 사용자 정보 JSON 반환
   *
   * @param studentId 학번
   * @param password 비밀번호
   * @return initUserInfo.do API 응답 JSON
   * @throws SejongAuthException 인증 실패 시
   */
  public String authenticateAndFetchJson(String studentId, String password) {
    return SejongPortalHttpSupport.runPortalStep("SIS authentication", () -> {
      OkHttpClient sessionClient = SejongPortalHttpSupport.newSessionClient(baseClient);
      SejongPortalHttpSupport.loginToPortal(sessionClient, properties, studentId, password, SJPT_RETURN_URL);
      return fetchUserInfoWithSessionClient(sessionClient);
    });
  }

  /**
   * 로그인된 세션 > sjpt SSO > initUserInfo JSON (통합 인증: 포털 로그인 1회로 연락처 조회)
   *
   * @throws SejongAuthException 세션 거부(401)·조회 실패·연결 실패
   */
  public String fetchInitUserInfoJson(SejongPortalSession session) {
    return SejongPortalHttpSupport.runPortalStep("SIS lookup", () -> fetchUserInfoWithSessionClient(session.sessionClient()));
  }

  /**
   * 포털 로그인 (rtUrl sjpt) > 로그인된 세션 (학사 업무 기간처럼 같은 세션에서 여러 SIS 요청을 이어갈 때)
   *
   * @throws SejongAuthException 틀린 비밀번호·잠금·연결 실패
   */
  public SejongPortalSession openSession(String studentId, String password) {
    return SejongPortalHttpSupport.runPortalStep("SIS portal login", () -> {
      OkHttpClient sessionClient = SejongPortalHttpSupport.newSessionClient(baseClient);
      SejongPortalHttpSupport.loginToPortal(sessionClient, properties, studentId, password, SJPT_RETURN_URL);
      return new SejongPortalSession(sessionClient);
    });
  }

  /**
   * 학사일정조회: 메뉴 목록 > 화면 열기 > 목록 JSON (요청 실패 시 재시도 없이 중단)
   *
   * @param session SSO·initUserInfo 를 마친 SIS 세션
   * @param runValues initUserInfo 응답의 실행 파라미터 원천값
   * @param schoolFields initUserInfo 응답의 조직·사용자 구분·메뉴 코드
   * @param year 연도 4자리
   * @param month 1~12 (빈 문자열은 연간 전체)
   * @return doList 응답 JSON
   * @throws SejongAuthException 세션 종료(SESSION_ERROR)·SIS 오류·조회 실패·연결 실패
   */
  public String fetchBusinessPeriodsJson(SejongPortalSession session, SisRunValues runValues,
      SejongSisSchoolFields schoolFields, String year, String month) {
    return SejongPortalHttpSupport.runPortalStep("SIS schedule", () -> {
      OkHttpClient client = session.sessionClient();
      // 메뉴 목록 (서버 세션에 메뉴 권한을 올림, 없으면 화면 열기가 시스템 에러)
      Map<String, String> baseValues = new LinkedHashMap<>();
      baseValues.put("_runIntgUsrNo", runValues.userNumber());
      baseValues.put("_runPgLoginDt", runValues.loginDate());
      baseValues.put("_runningSejong", runValues.sessionKey());
      Map<String, Object> menuRequest = Map.of("dm_ReqLeftMenu", orderedMap(
          "MENU_SYS_ID", schoolFields.menuSystemId(), "SYSTEM_DIV", MENU_SYSTEM_DIV, "MENU_SYS_NM", schoolFields.menuSystemName()));
      postSisJson(client, SJPT_MENU_LIST_URL, baseValues, menuRequest, "SIS menu list");

      // 화면 열기 (본문 = 실행 파라미터 + pbForceLog) > 응답의 화면 로그인 일시를 목록 요청에 사용
      Map<String, String> screenValues = new LinkedHashMap<>();
      screenValues.put("_runPgmKey", SCHEDULE_PROGRAM_KEY);
      screenValues.put("_runSysKey", SCHEDULE_SYSTEM_KEY);
      screenValues.putAll(baseValues);
      Map<String, String> roleValues = new LinkedHashMap<>();
      roleValues.put("pbForceLog", "false");
      roleValues.putAll(screenValues);
      String roleJson = postSisJson(client, SJPT_USER_ROLE_URL, roleValues, roleValues, "SIS screen open");

      screenValues.put("_runPgLoginDt", sisParser.parseScreenLoginDate(roleJson));
      Map<String, Object> listRequest = Map.of("dm_search", orderedMap(
          "ORGN_CLSF_CD", schoolFields.organizationClassificationCode(), "YEAR", year, "SMT_CD", "", "SMT_NM", "",
          "USER_CLSFM_CD", schoolFields.userClassificationMiddleCode(), "MONTH", month));
      return postSisJson(client, SJPT_SCHEDULE_LIST_URL, screenValues, listRequest, "SIS schedule list");
    });
  }

  /**
   * SIS JSON 요청 1건 (addParam = 실행 파라미터), 세션 종료 응답은 SESSION_ERROR 로 즉시 중단
   */
  private String postSisJson(OkHttpClient client, String url, Map<String, String> addParamValues, Object body,
      String stepName) throws IOException {
    Request request = new Request.Builder()
        .url(url + "?addParam=" + encodeAddParam(objectMapper.writeValueAsString(addParamValues)))
        .post(RequestBody.create(objectMapper.writeValueAsString(body), MediaType.parse(SIS_JSON_CONTENT_TYPE)))
        .header("Accept", "application/json")
        .header("Content-Type", SIS_JSON_CONTENT_TYPE)
        .header("Referer", SJPT_SSO_URL)
        .header("Origin", SJPT_ORIGIN)
        .build();
    try (Response response = client.newCall(request).execute()) {
      if (response.code() == 401) {
        throw new SejongAuthException(SejongAuthErrorCode.AUTHENTICATION_FAILED);
      }
      if (response.body() == null || response.code() != 200) {
        throw new SejongAuthException(SejongAuthErrorCode.DATA_FETCH_FAILED, stepName + " request failed : " + response.code());
      }
      String json = response.body().string();
      if (json.isEmpty()) {
        throw new SejongAuthException(SejongAuthErrorCode.DATA_FETCH_FAILED, stepName + " : empty response body");
      }
      try {
        sisParser.ensureLoggedIn(json);
      } catch (SejongAuthException e) {
        if (e.getErrorCode() == SejongAuthErrorCode.AUTHENTICATION_FAILED) {
          throw new SejongAuthException(SejongAuthErrorCode.SESSION_ERROR, stepName + " : SIS ended the session", e);
        }
        throw new SejongAuthException(SejongAuthErrorCode.DATA_FETCH_FAILED, stepName + " : SIS returned an error", e);
      }
      return json;
    }
  }

  /**
   * 실행 파라미터 JSON > addParam (URL 폼 인코딩 > Base64)
   */
  private String encodeAddParam(String compactJson) {
    String urlEncoded = URLEncoder.encode(compactJson, StandardCharsets.UTF_8);
    return Base64.getEncoder().encodeToString(urlEncoded.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * 키·값을 번갈아 받아 넣은 순서를 지키는 맵
   */
  private static Map<String, String> orderedMap(String... keysAndValues) {
    Map<String, String> map = new LinkedHashMap<>();
    for (int index = 0; index < keysAndValues.length; index += 2) {
      map.put(keysAndValues[index], keysAndValues[index + 1]);
    }
    return map;
  }

  private String fetchUserInfoWithSessionClient(OkHttpClient client) throws IOException {
    accessSsoPage(client);
    return fetchInitUserInfo(client);
  }

  /**
   * SSO 페이지 접근
   */
  private void accessSsoPage(OkHttpClient client) throws IOException {
    Request request = new Request.Builder()
        .url(SJPT_SSO_URL)
        .get()
        .header("Referer", SejongPortalHttpSupport.PORTAL_REFERER)
        .build();

    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new SejongAuthException(SejongAuthErrorCode.SESSION_ERROR,
            "SIS SSO page request failed : " + response.code());
      }
      log.debug("[sejong auth] SIS SSO page loaded : code={}", response.code());
    }
  }

  /**
   * initUserInfo.do API 호출
   * - 401 응답: 로그인 실패 (학번/비밀번호 불일치)
   */
  private String fetchInitUserInfo(OkHttpClient client) throws IOException {
    String addParam = generateEmptyAddParam();
    String apiUrl = SJPT_INIT_USER_INFO_URL + "?addParam=" + addParam;

    RequestBody body = RequestBody.create(
        "{}",
        MediaType.parse("application/json; charset=UTF-8")
    );

    Request request = new Request.Builder()
        .url(apiUrl)
        .post(body)
        .header("Accept", "application/json")
        .header("Content-Type", "application/json; charset=UTF-8")
        .header("Referer", SJPT_SSO_URL)
        .header("Origin", SJPT_ORIGIN)
        .build();

    try (Response response = client.newCall(request).execute()) {
      int responseCode = response.code();

      // 401 Unauthorized: 로그인 실패
      if (responseCode == 401) {
        log.warn("[sejong auth] SIS authentication rejected 401 : unauthorized");
        throw new SejongAuthException(SejongAuthErrorCode.AUTHENTICATION_FAILED);
      }

      if (response.body() == null || responseCode != 200) {
        throw new SejongAuthException(SejongAuthErrorCode.DATA_FETCH_FAILED,
            "initUserInfo API request failed : " + responseCode);
      }

      String json = response.body().string();
      log.debug("[sejong auth] initUserInfo response : code={}, jsonLength={}", responseCode, json.length());
      return json;
    }
  }

  /**
   * 빈 addParam 생성 (브라우저 초기 호출 시 사용)
   */
  private String generateEmptyAddParam() {
    String json = "{\"_runIntgUsrNo\":\"\",\"_runPgLoginDt\":\"\",\"_runningSejong\":\"\"}";
    String urlEncoded = URLEncoder.encode(json, StandardCharsets.UTF_8);
    return Base64.getEncoder().encodeToString(urlEncoded.getBytes(StandardCharsets.UTF_8));
  }
}
