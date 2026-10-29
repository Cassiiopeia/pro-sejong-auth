package kr.suhsaechan.sejongpeia.client;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import kr.suhsaechan.sejongpeia.config.SejongAuthProperties;
import kr.suhsaechan.sejongpeia.exception.SejongAuthErrorCode;
import kr.suhsaechan.sejongpeia.exception.SejongAuthException;
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
  static final String SJPT_SSO_URL = "https://sjpt.sejong.ac.kr/main/view/Login/doSsoLogin.do?p=";
  static final String SJPT_INIT_USER_INFO_URL = "https://sjpt.sejong.ac.kr/main/sys/UserInfo/initUserInfo.do";
  static final String SJPT_ORIGIN = "https://sjpt.sejong.ac.kr";

  private final SejongAuthProperties properties;
  private final OkHttpClient baseClient;

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
    return SejongHttpSupport.runPortalStep("SIS authentication", () -> {
      OkHttpClient sessionClient = SejongHttpSupport.newSessionClient(baseClient);
      SejongHttpSupport.loginToPortal(sessionClient, properties, studentId, password, SJPT_RETURN_URL);
      return fetchUserInfoWithSessionClient(sessionClient);
    });
  }

  /**
   * 로그인된 세션 > sjpt SSO > initUserInfo JSON (통합 인증: 포털 로그인 1회로 연락처 조회)
   *
   * @throws SejongAuthException 세션 거부(401)·조회 실패·연결 실패
   */
  public String fetchInitUserInfoJson(SejongPortalSession session) {
    return SejongHttpSupport.runPortalStep("SIS lookup", () -> fetchUserInfoWithSessionClient(session.sessionClient()));
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
        .header("Referer", SejongHttpSupport.PORTAL_REFERER)
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
