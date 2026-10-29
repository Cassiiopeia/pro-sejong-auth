package kr.suhsaechan.sejongpeia.service;

import java.time.LocalDateTime;
import kr.suhsaechan.sejongpeia.client.SejongPortalClient;
import kr.suhsaechan.sejongpeia.client.SejongPortalSession;
import kr.suhsaechan.sejongpeia.client.SejongSisClient;
import kr.suhsaechan.sejongpeia.exception.SejongAuthErrorCode;
import kr.suhsaechan.sejongpeia.exception.SejongAuthException;
import kr.suhsaechan.sejongpeia.model.SejongAuthResult;
import kr.suhsaechan.sejongpeia.model.SejongDhcAuthResult;
import kr.suhsaechan.sejongpeia.model.SejongSisAuthResult;
import kr.suhsaechan.sejongpeia.model.SejongStudentInfo;
import kr.suhsaechan.sejongpeia.parser.SejongClassicReadingParser;
import kr.suhsaechan.sejongpeia.parser.SejongPortalMainParser;
import kr.suhsaechan.sejongpeia.parser.SejongSisParser;
import kr.suhsaechan.sejongpeia.parser.SejongStudentInfoParser;
import kr.suhsaechan.sejongpeia.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 세종대학교 인증 엔진: 통합 인증, DHC(대양휴머니티칼리지) 인증, SIS(학사정보시스템) 인증
 * 로그에는 학번만 남김 (이름·연락처 제외)
 */
@Slf4j
public class SuhSejongAuthEngine {

  private final SejongPortalClient portalClient;
  private final SejongStudentInfoParser studentInfoParser;
  private final SejongClassicReadingParser classicReadingParser;
  private final SejongSisClient sisClient;
  private final SejongSisParser sisParser;
  private final SejongPortalMainParser portalMainParser;

  public SuhSejongAuthEngine(
      SejongPortalClient portalClient,
      SejongStudentInfoParser studentInfoParser,
      SejongClassicReadingParser classicReadingParser,
      SejongSisClient sisClient,
      SejongSisParser sisParser) {
    this(portalClient, studentInfoParser, classicReadingParser, sisClient, sisParser, new SejongPortalMainParser());
  }

  public SuhSejongAuthEngine(
      SejongPortalClient portalClient,
      SejongStudentInfoParser studentInfoParser,
      SejongClassicReadingParser classicReadingParser,
      SejongSisClient sisClient,
      SejongSisParser sisParser,
      SejongPortalMainParser portalMainParser) {
    this.portalClient = portalClient;
    this.studentInfoParser = studentInfoParser;
    this.classicReadingParser = classicReadingParser;
    this.sisClient = sisClient;
    this.sisParser = sisParser;
    this.portalMainParser = portalMainParser;
  }

  /**
   * 세종대학교 통합 인증: 포털 로그인 1회 > DHC(학생 정보·고전독서) > 같은 세션으로 SIS(연락처), 포털 메인(상위 소속)
   *
   * @param studentId 학번
   * @param password 비밀번호
   * @return 통합 인증 결과 (학생정보 + 고전독서 + 연락처)
   * @throws SejongAuthException 인증 실패 시
   */
  public SejongAuthResult authenticate(String studentId, String password) {
    return authenticateIntegrated(studentId, password, false);
  }

  /**
   * 통합 인증 (받은 원문 포함): authenticate 와 같고 DHC 원본 HTML, SIS 원본 JSON 을 결과에 담음 (추가 요청 없음)
   */
  public SejongAuthResult authenticateWithRaw(String studentId, String password) {
    return authenticateIntegrated(studentId, password, true);
  }

  private SejongAuthResult authenticateIntegrated(String studentId, String password, boolean shouldIncludeRaw) {
    validateCredentials(studentId, password);
    log.info("[sejong auth] integrated auth started : studentId={}", studentId);

    // 1. 포털 로그인 1회 + DHC에서 학생 정보·고전독서 (실패 시 예외)
    SejongPortalSession session = portalClient.openSession(studentId, password);
    SejongDhcAuthResult dhcResult = buildDhcResult(portalClient.fetchClassicStatusHtml(session), shouldIncludeRaw);

    // 2. 같은 세션으로 SIS 연락처 3개, 영어 이름, 사용자 구분 (실패해도 DHC 정보만으로 결과 반환)
    String email = null;
    String phoneNumber = null;
    String englishName = null;
    String userCategory = null;
    String sisJson = null;
    try {
      String json = sisClient.fetchInitUserInfoJson(session);
      sisJson = json;
      sisParser.ensureLoggedIn(json);
      email = sisParser.parseEmail(json);
      phoneNumber = sisParser.parsePhoneNumber(json);
      englishName = sisParser.parseEnglishName(json);
      userCategory = sisParser.parseUserCategory(json);
    } catch (Exception e) {
      log.warn("[sejong auth] SIS contact lookup failed (ignored) : {}", e.getMessage());
    }

    // 3. 같은 세션으로 포털 메인 1회 (보조 조회: 실패 시 null)
    String parentUnitName = fetchParentUnitName(session, dhcResult.getMajor());

    // 4. 결과 합치기
    SejongAuthResult integratedResult = SejongAuthResult.builder()
        .success(true)
        .major(dhcResult.getMajor())
        .studentId(dhcResult.getStudentId())
        .name(dhcResult.getName())
        .grade(dhcResult.getGrade())
        .status(dhcResult.getStatus())
        .classicReading(dhcResult.getClassicReading())
        .email(email)
        .phoneNumber(phoneNumber)
        .englishName(englishName)
        .userCategory(userCategory)
        .parentUnitName(parentUnitName)
        .authenticatedAt(LocalDateTime.now())
        .rawHtml(dhcResult.getRawHtml())
        .rawJson(shouldIncludeRaw ? sisJson : null)
        .build();

    log.info("[sejong auth] integrated auth completed : studentId={}", studentId);
    return integratedResult;
  }

  /**
   * 포털 메인 1회 조회 > 상위 소속명 (조회 실패 null, 표기 없음 빈 문자열, 통합 인증을 막지 않음)
   */
  private String fetchParentUnitName(SejongPortalSession session, String major) {
    try {
      return portalMainParser.parseParentUnitName(portalClient.fetchPortalMainHtml(session), major);
    } catch (SejongAuthException e) {
      log.warn("[portal main] skipped : {}", e.getDetail());
      return null;
    }
  }

  /**
   * DHC 인증: classic.sejong.ac.kr 조회 > 학생 정보 + 고전독서
   *
   * @param studentId 학번
   * @param password 비밀번호
   * @return DHC 인증 결과 (학생정보 + 고전독서)
   * @throws SejongAuthException 인증 실패 시
   */
  public SejongDhcAuthResult authenticateWithDHC(String studentId, String password) {
    return authenticateDhc(studentId, password, false);
  }

  /**
   * SIS 인증: sjpt.sejong.ac.kr 조회 > 학생 정보 + 연락처
   *
   * @param studentId 학번
   * @param password 비밀번호
   * @return SIS 인증 결과 (학생정보 + 연락처)
   * @throws SejongAuthException 인증 실패 시
   */
  public SejongSisAuthResult authenticateWithSIS(String studentId, String password) {
    return authenticateSis(studentId, password, false);
  }

  /**
   * DHC 인증 (디버깅용 원본 HTML 포함)
   *
   * @param studentId 학번
   * @param password 비밀번호
   * @return DHC 인증 결과 (원본 HTML 포함)
   * @throws SejongAuthException 인증 실패 시
   */
  public SejongDhcAuthResult authenticateWithDHCRaw(String studentId, String password) {
    return authenticateDhc(studentId, password, true);
  }

  /**
   * SIS 인증 (디버깅용 원본 JSON 포함)
   *
   * @param studentId 학번
   * @param password 비밀번호
   * @return SIS 인증 결과 (원본 JSON 포함)
   * @throws SejongAuthException 인증 실패 시
   */
  public SejongSisAuthResult authenticateWithSISRaw(String studentId, String password) {
    return authenticateSis(studentId, password, true);
  }

  /**
   * DHC 인증: 입력 검증 > 포털 로그인·페이지 조회 > 결과 조립
   */
  private SejongDhcAuthResult authenticateDhc(String studentId, String password, boolean shouldIncludeRawHtml) {
    validateCredentials(studentId, password);
    log.info("[sejong auth] DHC auth started : studentId={}, raw={}", studentId, shouldIncludeRawHtml);

    SejongDhcAuthResult dhcResult = buildDhcResult(portalClient.authenticateAndFetchHtml(studentId, password), shouldIncludeRawHtml);

    log.info("[sejong auth] DHC auth completed : studentId={}", studentId);
    return dhcResult;
  }

  /**
   * SIS 인증: 입력 검증 > 포털 로그인·API 조회 > 결과 조립
   */
  private SejongSisAuthResult authenticateSis(String studentId, String password, boolean shouldIncludeRawJson) {
    validateCredentials(studentId, password);
    log.info("[sejong auth] SIS auth started : studentId={}, raw={}", studentId, shouldIncludeRawJson);

    String json = sisClient.authenticateAndFetchJson(studentId, password);
    // 틀린 비밀번호는 401 이 아니라 200 + _SUBMIT_ERROR_(LOGOUT)
    sisParser.ensureLoggedIn(json);
    SejongSisAuthResult sisResult = buildSisResult(json, shouldIncludeRawJson);

    log.info("[sejong auth] SIS auth completed : studentId={}", studentId);
    return sisResult;
  }

  /**
   * 고전독서인증현황 HTML > DHC 결과 (학생 정보 표 1회 파싱 + 고전독서)
   */
  private SejongDhcAuthResult buildDhcResult(String html, boolean shouldIncludeRawHtml) {
    SejongStudentInfo studentInfo = studentInfoParser.parseStudentInfo(html);
    return SejongDhcAuthResult.builder()
        .success(true)
        .major(studentInfo.major())
        .studentId(studentInfo.studentId())
        .name(studentInfo.name())
        .grade(studentInfo.grade())
        .status(studentInfo.status())
        .classicReading(classicReadingParser.parse(html))
        .authenticatedAt(LocalDateTime.now())
        .rawHtml(shouldIncludeRawHtml ? html : null)
        .build();
  }

  /**
   * initUserInfo JSON > SIS 결과 (학년·상태는 SIS 미제공 > 빈 문자열)
   */
  private SejongSisAuthResult buildSisResult(String json, boolean shouldIncludeRawJson) {
    return SejongSisAuthResult.builder()
        .success(true)
        .major(sisParser.parseMajor(json))
        .studentId(sisParser.parseStudentId(json))
        .name(sisParser.parseName(json))
        .grade("")
        .status("")
        .email(sisParser.parseEmail(json))
        .phoneNumber(sisParser.parsePhoneNumber(json))
        .englishName(sisParser.parseEnglishName(json))
        .userCategory(sisParser.parseUserCategory(json))
        .authenticatedAt(LocalDateTime.now())
        .rawJson(shouldIncludeRawJson ? json : null)
        .build();
  }

  /**
   * 학번·비밀번호 빈 값 검증
   *
   * @param studentId 학번
   * @param password 비밀번호
   * @throws SejongAuthException 유효하지 않은 입력값
   */
  private void validateCredentials(String studentId, String password) {
    if (CommonUtil.isBlank(studentId)) {
      throw new SejongAuthException(SejongAuthErrorCode.INVALID_INPUT, "Student ID is empty");
    }
    if (CommonUtil.isBlank(password)) {
      throw new SejongAuthException(SejongAuthErrorCode.INVALID_INPUT, "Password is empty");
    }
  }
}
