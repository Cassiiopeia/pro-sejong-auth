package kr.suhsaechan.sejongpeia.client;

import java.io.IOException;
import kr.suhsaechan.sejongpeia.config.SejongAuthProperties;
import kr.suhsaechan.sejongpeia.exception.SejongAuthErrorCode;
import kr.suhsaechan.sejongpeia.exception.SejongAuthException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 세종대학교 포털 HTTP 클라이언트: 포털 로그인(세션 생성), 고전독서 SSO, 고전독서인증현황 페이지 조회
 * 공용 OkHttpClient 1개 (연결 풀 공유) + 인증마다 새 쿠키 저장소
 */
@Slf4j
public class SejongPortalClient {

  // 포털 로그인 후 SSO 대상: 고전독서 사이트
  static final String CLASSIC_RETURN_URL = "classic.sejong.ac.kr";

  // 포털 메인 (소속 표기 조회), 세션 만료 시 돌아오는 로그인 페이지는 form 대상 문구로 판별
  static final String PORTAL_MAIN_URL = "https://portal.sejong.ac.kr/user/index.do";
  static final String PORTAL_LOGIN_PAGE_MARKER = "login_action.jsp";

  private final SejongAuthProperties properties;
  private final OkHttpClient baseClient;

  public SejongPortalClient(SejongAuthProperties properties) {
    this(properties, SejongHttpSupport.buildBaseClient(properties));
  }

  // 테스트: 가짜 응답 클라이언트 주입
  SejongPortalClient(SejongAuthProperties properties, OkHttpClient baseClient) {
    this.properties = properties;
    this.baseClient = baseClient;
  }

  /**
   * 세종포털 로그인 후 고전독서인증현황 페이지 HTML 반환
   *
   * @param studentId 학번
   * @param password 비밀번호
   * @return 고전독서인증현황 페이지 HTML
   * @throws SejongAuthException 인증 실패 시
   */
  public String authenticateAndFetchHtml(String studentId, String password) {
    return fetchClassicStatusHtml(openSession(studentId, password));
  }

  /**
   * 포털 로그인 > 로그인된 세션 (rtUrl 고전독서, 같은 세션으로 학사정보시스템 SSO 도 가능)
   *
   * @throws SejongAuthException 틀린 비밀번호·잠금·연결 실패
   */
  public SejongPortalSession openSession(String studentId, String password) {
    return SejongHttpSupport.runPortalStep("portal login", () -> {
      OkHttpClient sessionClient = SejongHttpSupport.newSessionClient(baseClient);
      SejongHttpSupport.loginToPortal(sessionClient, properties, studentId, password, CLASSIC_RETURN_URL);
      return new SejongPortalSession(sessionClient);
    });
  }

  /**
   * 로그인된 세션 > 고전독서 SSO > 고전독서인증현황 페이지 HTML
   *
   * @throws SejongAuthException 세션 만료(401)·조회 실패·연결 실패
   */
  public String fetchClassicStatusHtml(SejongPortalSession session) {
    return SejongHttpSupport.runPortalStep("classic reading lookup", () -> {
      handleSsoRedirect(session.sessionClient());
      return fetchClassicStatusPage(session.sessionClient());
    });
  }

  /**
   * 로그인된 세션 > 포털 메인 HTML (보조 조회: 재시도 없음, 기존 타임아웃 그대로)
   *
   * @throws SejongAuthException 비 2xx·빈 본문·로그인 페이지(세션 만료)·연결 실패 (사유는 detail, 응답 본문은 담지 않음)
   */
  public String fetchPortalMainHtml(SejongPortalSession session) {
    Request request = new Request.Builder()
        .url(PORTAL_MAIN_URL)
        .get()
        .header("Referer", SejongHttpSupport.PORTAL_REFERER)
        .build();

    try (Response response = session.sessionClient().newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new SejongAuthException(SejongAuthErrorCode.DATA_FETCH_FAILED, "http " + response.code());
      }
      String html = response.body() == null ? "" : response.body().string();
      if (html.isEmpty()) {
        throw new SejongAuthException(SejongAuthErrorCode.DATA_FETCH_FAILED, "empty body");
      }
      if (html.contains(PORTAL_LOGIN_PAGE_MARKER)) {
        throw new SejongAuthException(SejongAuthErrorCode.SESSION_ERROR, "login page returned (session expired)");
      }
      return html;
    } catch (IOException e) {
      throw new SejongAuthException(SejongAuthErrorCode.CONNECTION_FAILED, "request failed : " + e.getClass().getSimpleName());
    }
  }

  /**
   * SSO 리다이렉트 처리
   */
  private void handleSsoRedirect(OkHttpClient client) throws IOException {
    Request ssoRequest = new Request.Builder()
        .url(properties.getSsoRedirectUrl())
        .get()
        .build();

    try (Response ssoResponse = client.newCall(ssoRequest).execute()) {
      if (!ssoResponse.isSuccessful()) {
        throw new SejongAuthException(SejongAuthErrorCode.SESSION_ERROR,
            "SSO redirect failed : " + ssoResponse.code());
      }
      log.debug("[sejong auth] SSO redirect done : code={}", ssoResponse.code());
    }
  }

  /**
   * 고전독서인증현황 페이지 HTML 가져오기
   * - 401 응답: 로그인 실패 (학번/비밀번호 불일치)
   * - 그 외 실패: 데이터 조회 실패
   */
  private String fetchClassicStatusPage(OkHttpClient client) throws IOException {
    Request request = new Request.Builder()
        .url(properties.getClassicStatusUrl())
        .get()
        .build();

    try (Response response = client.newCall(request).execute()) {
      int responseCode = response.code();

      // 401 Unauthorized: 로그인 실패 (학번/비밀번호 불일치)
      if (responseCode == 401) {
        log.warn("[sejong auth] portal authentication rejected 401 : student ID or password mismatch");
        throw new SejongAuthException(SejongAuthErrorCode.AUTHENTICATION_FAILED);
      }

      // 그 외 실패 응답
      if (response.body() == null || responseCode != 200) {
        throw new SejongAuthException(SejongAuthErrorCode.DATA_FETCH_FAILED,
            "Classic reading page request failed : " + responseCode);
      }

      String html = response.body().string();
      log.debug("[sejong auth] classic reading page loaded : code={}, htmlLength={}", responseCode, html.length());
      return html;
    }
  }
}
