// 세종대 포털 인증 클라이언트: 입력 검증 > 포털 로그인(재시도) > SSO > 데이터 조회 > 파싱 (흐름: spec/PORTAL_FLOW.md)
import { ErrorCode, SejongAuthError } from './errors.js';
import type { RawOption, SejongAuthResult, SejongSisSchoolCodes, SejongSisUserInfo, SejongDhcAuthResult, SejongSisAuthResult } from './models.js';
import { SIS_CODE_KEYS_BY_FIELD, ensurePortalLoginAccepted, ensureSisLoggedIn, parseClassicReading, parseSisUserInfo, parseStudentInfo } from './parsers.js';
import { parseParentUnitName } from './portalMainParser.js';
import { NodeHttpsSession, TimeoutError, USER_AGENT, type HttpResponse, type RequestOptions, type Session } from './transport.js';

export const PORTAL_LOGIN_URL = 'https://portal.sejong.ac.kr/jsp/login/login_action.jsp';
export const PORTAL_REFERER = 'https://portal.sejong.ac.kr';
// 포털이 요구하는 초기 쿠키 (없으면 로그인 폼 거부)
export const PORTAL_DEFAULT_COOKIE = 'chknos=false';

// 로그인 후 SSO 대상 (rtUrl)
export const CLASSIC_RETURN_URL = 'classic.sejong.ac.kr';
export const SIS_RETURN_URL = 'sjpt.sejong.ac.kr/main/view/Login/doSsoLogin.do?p=';

export const CLASSIC_SSO_URL = 'https://classic.sejong.ac.kr/_custom/sejong/sso/sso-return.jsp?returnUrl=https://classic.sejong.ac.kr/classic/index.do';
export const CLASSIC_STATUS_URL = 'https://classic.sejong.ac.kr/classic/reading/status.do';
// 포털 메인 (소속 표기 조회), 세션 만료 시 돌아오는 로그인 페이지는 form 대상 문구로 판별
export const PORTAL_MAIN_URL = 'https://portal.sejong.ac.kr/user/index.do';
export const PORTAL_LOGIN_PAGE_MARKER = 'login_action.jsp';
export const SIS_SSO_URL = 'https://sjpt.sejong.ac.kr/main/view/Login/doSsoLogin.do?p=';
export const SIS_USER_INFO_URL = 'https://sjpt.sejong.ac.kr/main/sys/UserInfo/initUserInfo.do';
export const SIS_ORIGIN = 'https://sjpt.sejong.ac.kr';
// 브라우저 초기 호출과 같은 빈 addParam (JSON > URL 폼 인코딩 > Base64)
const SIS_EMPTY_ADD_PARAM = Buffer.from(
  new URLSearchParams({ value: '{"_runIntgUsrNo":"","_runPgLoginDt":"","_runningSejong":""}' }).toString().slice('value='.length),
  'utf8',
).toString('base64');

export { USER_AGENT };
const HTTP_UNAUTHORIZED = 401;

export interface SejongAuthClientOptions {
  /** 요청 제한 시간 (초, 기본 10) */
  timeoutSeconds?: number;
  /** 포털 로그인 재시도 횟수 (기본 3) */
  maxRetry?: number;
  /** 인증서 검증 (기본 true) */
  sslVerification?: boolean;
  /** @internal 테스트용 세션 주입 */
  _sessionFactory?: () => Session;
}

const isSuccessful = (response: HttpResponse) => response.status >= 200 && response.status < 300;

/** SIS 사용자 정보 > 결과에 붙일 코드 필드 (extra 는 요청했을 때만) */
function sisSchoolCodesOf(userInfo: SejongSisUserInfo, includeExtra: boolean) {
  const codeByField = {} as SejongSisSchoolCodes;
  for (const fieldName of Object.keys(SIS_CODE_KEYS_BY_FIELD) as (keyof SejongSisSchoolCodes)[]) codeByField[fieldName] = userInfo[fieldName];
  return { ...codeByField, extra: includeExtra ? { ...userInfo.extra } : null };
}

/** 세종대 포털 인증 */
export class SejongAuthClient {
  readonly timeoutSeconds: number;
  readonly maxRetry: number;
  readonly sslVerification: boolean;
  private readonly sessionFactory: () => Session;

  constructor({ timeoutSeconds = 10, maxRetry = 3, sslVerification = true, _sessionFactory }: SejongAuthClientOptions = {}) {
    this.timeoutSeconds = timeoutSeconds;
    this.maxRetry = maxRetry;
    this.sslVerification = sslVerification;
    this.sessionFactory = _sessionFactory ?? (() => new NodeHttpsSession(this.timeoutSeconds, this.sslVerification, USER_AGENT));
  }

  /** 통합 인증: 포털 로그인 1회 > DHC 학생 정보·고전독서 > 같은 세션으로 SIS 연락처, 포털 메인 상위 소속 (보조 조회 실패는 무시하고 null, includeRaw 면 받은 원문, includeExtra 면 SIS 나머지 필드 extra) */
  async authenticate(studentId: string, password: string, { includeRaw = false, includeExtra = false }: RawOption = {}): Promise<SejongAuthResult> {
    validateCredentials(studentId, password);
    const session = this.sessionFactory();
    await this.login(session, studentId, password, CLASSIC_RETURN_URL);
    const dhcResult = await this.fetchDhcWithSession(session, includeRaw);

    let contact: { email: string | null; phoneNumber: string | null; englishName: string | null; userCategory: string | null } =
      { email: null, phoneNumber: null, englishName: null, userCategory: null };
    let sisJson: string | null = null;
    let schoolCodes: Partial<SejongSisSchoolCodes> & { extra: Record<string, string> | null } = { extra: null };
    try {
      sisJson = await this.fetchSisJsonWithSession(session);
      const sisUserInfo = parseSisUserInfo(sisJson);
      contact = {
        email: sisUserInfo.email, phoneNumber: sisUserInfo.phoneNumber, englishName: sisUserInfo.englishName,
        userCategory: sisUserInfo.userCategory,
      };
      schoolCodes = sisSchoolCodesOf(sisUserInfo, includeExtra);
    } catch {
      // SIS 는 연락처 보조 조회 > 어떤 실패든 통합 결과를 막지 않음 (Java 와 동일)
    }

    const parentUnitName = await this.fetchParentUnitName(session, dhcResult.major);

    const { rawHtml, ...dhcFields } = dhcResult;
    return {
      ...dhcFields, ...contact, parentUnitName, authenticatedAt: new Date(),
      rawHtml, rawJson: includeRaw ? sisJson : null, ...schoolCodes,
    };
  }

  /** DHC 인증: 학생 정보 + 고전독서 (includeRaw 면 원본 HTML 포함) */
  async authenticateDhc(studentId: string, password: string, { includeRaw = false }: RawOption = {}): Promise<SejongDhcAuthResult> {
    validateCredentials(studentId, password);
    return this.fetchDhc(studentId, password, includeRaw);
  }

  /** SIS 인증: 학생 정보 + 연락처 + 학교 제공 코드 (grade·status 빈 문자열, includeRaw 면 원본 JSON, includeExtra 면 나머지 필드 extra) */
  async authenticateSis(studentId: string, password: string, { includeRaw = false, includeExtra = false }: RawOption = {}): Promise<SejongSisAuthResult> {
    validateCredentials(studentId, password);
    const jsonText = await this.fetchSisJson(studentId, password);
    const userInfo = parseSisUserInfo(jsonText);
    return {
      success: true, ...userInfo, grade: '', status: '',
      authenticatedAt: new Date(), rawJson: includeRaw ? jsonText : null, extra: includeExtra ? userInfo.extra : null,
    };
  }

  /** 포털 로그인 > DHC 조회 */
  private async fetchDhc(studentId: string, password: string, shouldIncludeRawHtml: boolean): Promise<SejongDhcAuthResult> {
    const session = this.sessionFactory();
    await this.login(session, studentId, password, CLASSIC_RETURN_URL);
    return this.fetchDhcWithSession(session, shouldIncludeRawHtml);
  }

  /** 로그인된 세션 > classic SSO > 고전독서 페이지 > 학생 정보·고전독서 파싱 */
  private async fetchDhcWithSession(session: Session, shouldIncludeRawHtml: boolean): Promise<SejongDhcAuthResult> {
    expectSessionStep(await call(session, 'GET', CLASSIC_SSO_URL), 'SSO redirect failed');
    const html = expectData(await call(session, 'GET', CLASSIC_STATUS_URL), 'Classic reading page request failed');
    return {
      success: true, ...parseStudentInfo(html), classicReading: parseClassicReading(html),
      authenticatedAt: new Date(), rawHtml: shouldIncludeRawHtml ? html : null,
    };
  }

  /** 같은 세션으로 포털 메인 1회 조회 > 상위 소속명 (재시도 없음, 호출 실패·세션 만료·빈 본문은 null, 요소 없음은 빈 문자열) */
  private async fetchParentUnitName(session: Session, major: string): Promise<string | null> {
    try {
      const response = await call(session, 'GET', PORTAL_MAIN_URL, { headers: { Referer: PORTAL_REFERER } });
      if (!isSuccessful(response) || response.body.length === 0) return null;
      const html = response.body.toString('utf8');
      if (html.includes(PORTAL_LOGIN_PAGE_MARKER)) return null;
      return parseParentUnitName(html, major);
    } catch {
      return null; // 부가 정보 > 어떤 실패든 통합 결과를 막지 않음
    }
  }

  /** 포털 로그인 (rtUrl sjpt) > SIS 조회 */
  private async fetchSisJson(studentId: string, password: string): Promise<string> {
    const session = this.sessionFactory();
    await this.login(session, studentId, password, SIS_RETURN_URL);
    return this.fetchSisJsonWithSession(session);
  }

  /** 로그인된 세션 > sjpt SSO > initUserInfo JSON */
  private async fetchSisJsonWithSession(session: Session): Promise<string> {
    expectSessionStep(await call(session, 'GET', SIS_SSO_URL, { headers: { Referer: PORTAL_REFERER } }), 'SIS SSO page request failed');
    const response = await call(session, 'POST', `${SIS_USER_INFO_URL}?addParam=${SIS_EMPTY_ADD_PARAM}`, {
      body: '{}',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json; charset=UTF-8',
        Referer: SIS_SSO_URL,
        Origin: SIS_ORIGIN,
      },
    });
    const jsonText = expectData(response, 'initUserInfo API request failed');
    // 틀린 비밀번호는 401 이 아니라 200 + _SUBMIT_ERROR_(LOGOUT)
    ensureSisLoggedIn(jsonText);
    return jsonText;
  }

  /** 포털 로그인 (실패 응답·시간 초과 시 maxRetry 회까지) > 포털 판정값 확인 (틀린 비밀번호·잠금은 재시도 없이 즉시 실패) */
  private async login(session: Session, studentId: string, password: string, returnUrl: string): Promise<void> {
    const body = new URLSearchParams({ mainLogin: 'N', rtUrl: returnUrl, id: studentId, password }).toString();
    const headers = { 'Content-Type': 'application/x-www-form-urlencoded', Referer: PORTAL_REFERER, Cookie: PORTAL_DEFAULT_COOKIE };
    for (let attempt = 1; attempt <= this.maxRetry; attempt += 1) {
      try {
        const response = await callRaw(session, 'POST', PORTAL_LOGIN_URL, { body, headers });
        if (isSuccessful(response)) {
          ensurePortalLoginAccepted(response.body.toString('utf8'));
          return;
        }
      } catch (error) {
        if (!(error instanceof TimeoutError)) throw error;
        if (attempt === this.maxRetry) throw new SejongAuthError(ErrorCode.CONNECTION_TIMEOUT, undefined, { cause: error });
      }
    }
    throw new SejongAuthError(ErrorCode.CONNECTION_FAILED, `Max retry count exceeded : ${this.maxRetry}`);
  }
}

/** 요청 (TimeoutError 는 그대로, 그 외 네트워크·TLS 오류 > CONNECTION_FAILED) */
async function callRaw(session: Session, method: string, url: string, options?: RequestOptions): Promise<HttpResponse> {
  try {
    return await session.request(method, url, options);
  } catch (error) {
    if (error instanceof TimeoutError || error instanceof SejongAuthError) throw error;
    throw new SejongAuthError(ErrorCode.CONNECTION_FAILED, undefined, { cause: error });
  }
}

/** 로그인 외 요청 (시간 초과도 CONNECTION_FAILED) */
async function call(session: Session, method: string, url: string, options?: RequestOptions): Promise<HttpResponse> {
  try {
    return await callRaw(session, method, url, options);
  } catch (error) {
    if (error instanceof TimeoutError) throw new SejongAuthError(ErrorCode.CONNECTION_FAILED, undefined, { cause: error });
    throw error;
  }
}

function expectSessionStep(response: HttpResponse, failureMessage: string): void {
  if (!isSuccessful(response)) throw new SejongAuthError(ErrorCode.SESSION_ERROR, `${failureMessage} : ${response.status}`);
}

/** 데이터 응답 판정: 401 > AUTHENTICATION_FAILED, 그 외 200 아님·빈 본문 > DATA_FETCH_FAILED */
function expectData(response: HttpResponse, failureMessage: string): string {
  if (response.status === HTTP_UNAUTHORIZED) throw new SejongAuthError(ErrorCode.AUTHENTICATION_FAILED);
  if (response.status !== 200 || response.body.length === 0) {
    throw new SejongAuthError(ErrorCode.DATA_FETCH_FAILED, `${failureMessage} : ${response.status}`);
  }
  return response.body.toString('utf8');
}

function validateCredentials(studentId: string | undefined, password: string | undefined): void {
  if (!studentId || !String(studentId).trim()) throw new SejongAuthError(ErrorCode.INVALID_INPUT, 'Student ID is empty');
  if (!password || !String(password).trim()) throw new SejongAuthError(ErrorCode.INVALID_INPUT, 'Password is empty');
}
