"""세종대 포털 인증 클라이언트: 입력 검증 > 포털 로그인(재시도) > SSO > 데이터 조회 > 파싱 (흐름: spec/PORTAL_FLOW.md)"""

import base64
import json
import logging
import ssl
import urllib.parse
from datetime import datetime
from typing import Callable, Optional

from . import __version__
from .errors import ErrorCode, SejongAuthError
from .models import SejongAuthResult, SejongDhcAuthResult, SejongSisAuthResult
from .parsers import ensure_portal_login_accepted, ensure_sis_logged_in, parse_classic_reading, parse_sis_user_info, parse_student_info
from .portal_main_parser import parse_parent_unit_name
from .transport import USER_AGENT, HttpResponse, UrllibSession, create_ssl_context

# 라이브러리 로거: 출력 방식은 사용하는 앱이 설정
LOGGER = logging.getLogger("sejongpeia")

PORTAL_LOGIN_URL = "https://portal.sejong.ac.kr/jsp/login/login_action.jsp"
PORTAL_REFERER = "https://portal.sejong.ac.kr"
# 포털이 요구하는 초기 쿠키 (없으면 로그인 폼 거부)
PORTAL_DEFAULT_COOKIE = "chknos=false"

# 로그인 후 SSO 대상 (rtUrl)
CLASSIC_RETURN_URL = "classic.sejong.ac.kr"
SIS_RETURN_URL = "sjpt.sejong.ac.kr/main/view/Login/doSsoLogin.do?p="

CLASSIC_SSO_URL = "https://classic.sejong.ac.kr/_custom/sejong/sso/sso-return.jsp?returnUrl=https://classic.sejong.ac.kr/classic/index.do"
CLASSIC_STATUS_URL = "https://classic.sejong.ac.kr/classic/reading/status.do"
# 포털 메인 (소속 표기 조회), 세션 만료 시 돌아오는 로그인 페이지는 form 대상 문구로 판별
PORTAL_MAIN_URL = "https://portal.sejong.ac.kr/user/index.do"
PORTAL_LOGIN_PAGE_MARKER = "login_action.jsp"
SIS_SSO_URL = "https://sjpt.sejong.ac.kr/main/view/Login/doSsoLogin.do?p="
SIS_USER_INFO_URL = "https://sjpt.sejong.ac.kr/main/sys/UserInfo/initUserInfo.do"
SIS_ORIGIN = "https://sjpt.sejong.ac.kr"
# 브라우저 초기 호출과 같은 빈 addParam (JSON > URL 폼 인코딩 > Base64)
SIS_EMPTY_ADD_PARAM = base64.b64encode(
    urllib.parse.quote_plus('{"_runIntgUsrNo":"","_runPgLoginDt":"","_runningSejong":""}').encode("utf-8")
).decode("ascii")

HTTP_UNAUTHORIZED = 401


class SejongAuthClient:
    """세종대 포털 인증 (설정: 제한 시간 초, 로그인 재시도 횟수, 인증서 검증)"""

    def __init__(self, timeout_seconds: float = 10, max_retry: int = 3, ssl_verification: bool = True,
                 _session_factory: Optional[Callable[[], object]] = None):
        self.timeout_seconds = timeout_seconds
        self.max_retry = max_retry
        self.ssl_verification = ssl_verification
        self._session_factory = _session_factory or self._create_session

    def authenticate(self, student_id: str, password: str, include_raw: bool = False) -> SejongAuthResult:
        """통합 인증: 포털 로그인 1회 > DHC 학생 정보·고전독서 > 같은 세션으로 SIS 연락처, 포털 메인 상위 소속 (보조 조회 실패는 무시하고 None, include_raw=True 면 받은 원문만 담음)"""
        self._validate_credentials(student_id, password)
        session = self._session_factory()
        self._login(session, student_id, password, CLASSIC_RETURN_URL)
        dhc_result = self._fetch_dhc_with_session(session, should_include_raw_html=include_raw)

        email = phone_number = english_name = user_category = sis_json = None
        try:
            sis_json = self._fetch_sis_json_with_session(session)
            sis_user_info = parse_sis_user_info(sis_json)
            email, phone_number, english_name = sis_user_info.email, sis_user_info.phone_number, sis_user_info.english_name
            user_category = sis_user_info.user_category
        except Exception as error:  # SIS 는 연락처 보조 조회 > 어떤 실패든 통합 결과를 막지 않음 (Java 와 동일)
            LOGGER.warning("[sejong auth] SIS contact lookup failed (ignored) : %s", type(error).__name__)
        parent_unit_name = self._fetch_parent_unit_name(session, dhc_result.major)

        return SejongAuthResult(
            success=True, student_id=dhc_result.student_id, name=dhc_result.name, major=dhc_result.major,
            grade=dhc_result.grade, status=dhc_result.status, classic_reading=dhc_result.classic_reading,
            email=email, phone_number=phone_number, english_name=english_name, user_category=user_category,
            parent_unit_name=parent_unit_name, authenticated_at=datetime.now(),
            raw_html=dhc_result.raw_html, raw_json=sis_json if include_raw else None,
        )

    def authenticate_dhc(self, student_id: str, password: str, include_raw: bool = False) -> SejongDhcAuthResult:
        """DHC 인증: 학생 정보 + 고전독서 (include_raw=True 면 원본 HTML 포함)"""
        self._validate_credentials(student_id, password)
        return self._fetch_dhc(student_id, password, should_include_raw_html=include_raw)

    def authenticate_sis(self, student_id: str, password: str, include_raw: bool = False) -> SejongSisAuthResult:
        """SIS 인증: 학생 정보 + 연락처 (학년·상태 빈 문자열, include_raw=True 면 원본 JSON 포함)"""
        self._validate_credentials(student_id, password)
        json_text = self._fetch_sis_json(student_id, password)
        user_info = parse_sis_user_info(json_text)
        return SejongSisAuthResult(
            success=True, student_id=user_info.student_id, name=user_info.name, major=user_info.major,
            grade="", status="", email=user_info.email, phone_number=user_info.phone_number,
            english_name=user_info.english_name, user_category=user_info.user_category, authenticated_at=datetime.now(),
            raw_json=json_text if include_raw else None,
        )

    def _fetch_dhc(self, student_id: str, password: str, should_include_raw_html: bool) -> SejongDhcAuthResult:
        """포털 로그인 > DHC 조회"""
        session = self._session_factory()
        self._login(session, student_id, password, CLASSIC_RETURN_URL)
        return self._fetch_dhc_with_session(session, should_include_raw_html)

    def _fetch_dhc_with_session(self, session, should_include_raw_html: bool) -> SejongDhcAuthResult:
        """로그인된 세션 > classic SSO > 고전독서 페이지 > 학생 정보·고전독서 파싱"""
        self._expect_session_step(self._call(session, "GET", CLASSIC_SSO_URL), "SSO redirect failed")
        html = self._expect_data(self._call(session, "GET", CLASSIC_STATUS_URL), "Classic reading page request failed")
        student_info = parse_student_info(html)
        return SejongDhcAuthResult(
            success=True, student_id=student_info.student_id, name=student_info.name, major=student_info.major,
            grade=student_info.grade, status=student_info.status, classic_reading=parse_classic_reading(html),
            authenticated_at=datetime.now(), raw_html=html if should_include_raw_html else None,
        )

    def _fetch_sis_json(self, student_id: str, password: str) -> str:
        """포털 로그인 (rtUrl sjpt) > SIS 조회"""
        session = self._session_factory()
        self._login(session, student_id, password, SIS_RETURN_URL)
        return self._fetch_sis_json_with_session(session)

    def _fetch_sis_json_with_session(self, session) -> str:
        """로그인된 세션 > sjpt SSO > initUserInfo JSON"""
        self._expect_session_step(
            self._call(session, "GET", SIS_SSO_URL, headers={"Referer": PORTAL_REFERER}), "SIS SSO page request failed")
        response = self._call(session, "POST", f"{SIS_USER_INFO_URL}?addParam={SIS_EMPTY_ADD_PARAM}", data=b"{}", headers={
            "Accept": "application/json",
            "Content-Type": "application/json; charset=UTF-8",
            "Referer": SIS_SSO_URL,
            "Origin": SIS_ORIGIN,
        })
        json_text = self._expect_data(response, "initUserInfo API request failed")
        # 틀린 비밀번호는 401 이 아니라 200 + _SUBMIT_ERROR_(LOGOUT)
        ensure_sis_logged_in(json_text)
        return json_text

    def _fetch_parent_unit_name(self, session, major: str) -> Optional[str]:
        """같은 세션으로 포털 메인 1회 조회 > 상위 소속명 (재시도 없음, 호출 실패·세션 만료·빈 본문은 None, 요소 없음은 빈 문자열)"""
        try:
            response = self._call(session, "GET", PORTAL_MAIN_URL, headers={"Referer": PORTAL_REFERER})
            if not response.is_successful or not response.body:
                return None
            html = response.text()
            if PORTAL_LOGIN_PAGE_MARKER in html:
                return None
            return parse_parent_unit_name(html, major)
        except Exception as error:  # 부가 정보 > 어떤 실패든 통합 결과를 막지 않음
            LOGGER.warning("[sejong auth] portal main lookup failed (ignored) : %s", type(error).__name__)
            return None

    def _login(self, session, student_id: str, password: str, return_url: str) -> None:
        """포털 로그인 (실패 응답·시간 초과 시 max_retry 회까지) > 포털 판정값 확인 (틀린 비밀번호·잠금은 재시도 없이 즉시 실패)"""
        form = urllib.parse.urlencode({"mainLogin": "N", "rtUrl": return_url, "id": student_id, "password": password})
        headers = {"Content-Type": "application/x-www-form-urlencoded", "Referer": PORTAL_REFERER, "Cookie": PORTAL_DEFAULT_COOKIE}
        for attempt in range(1, self.max_retry + 1):
            try:
                response = self._call_raw(session, "POST", PORTAL_LOGIN_URL, data=form.encode("utf-8"), headers=headers)
                if response.is_successful:
                    ensure_portal_login_accepted(response.text())
                    return
            except TimeoutError as error:
                if attempt == self.max_retry:
                    raise SejongAuthError(ErrorCode.CONNECTION_TIMEOUT) from error
        raise SejongAuthError(ErrorCode.CONNECTION_FAILED, f"Max retry count exceeded : {self.max_retry}")

    def _call(self, session, method: str, url: str, **kwargs) -> HttpResponse:
        """로그인 외 요청 (시간 초과·네트워크 오류 > CONNECTION_FAILED)"""
        try:
            return self._call_raw(session, method, url, **kwargs)
        except TimeoutError as error:
            raise SejongAuthError(ErrorCode.CONNECTION_FAILED) from error

    @staticmethod
    def _call_raw(session, method: str, url: str, **kwargs) -> HttpResponse:
        try:
            return session.request(method, url, **kwargs)
        except (TimeoutError, SejongAuthError):
            raise
        except ssl.SSLError as error:
            raise SejongAuthError(ErrorCode.CONNECTION_FAILED, "TLS handshake with the portal failed") from error
        except OSError as error:
            raise SejongAuthError(ErrorCode.CONNECTION_FAILED) from error

    @staticmethod
    def _expect_session_step(response: HttpResponse, failure_message: str) -> None:
        if not response.is_successful:
            raise SejongAuthError(ErrorCode.SESSION_ERROR, f"{failure_message} : {response.status}")

    @staticmethod
    def _expect_data(response: HttpResponse, failure_message: str) -> str:
        """데이터 응답 판정: 401 > AUTHENTICATION_FAILED, 그 외 200 아님·빈 본문 > DATA_FETCH_FAILED"""
        if response.status == HTTP_UNAUTHORIZED:
            raise SejongAuthError(ErrorCode.AUTHENTICATION_FAILED)
        if response.status != 200 or not response.body:
            raise SejongAuthError(ErrorCode.DATA_FETCH_FAILED, f"{failure_message} : {response.status}")
        return response.text()

    @staticmethod
    def _validate_credentials(student_id: Optional[str], password: Optional[str]) -> None:
        if not student_id or not str(student_id).strip():
            raise SejongAuthError(ErrorCode.INVALID_INPUT, "Student ID is empty")
        if not password or not str(password).strip():
            raise SejongAuthError(ErrorCode.INVALID_INPUT, "Password is empty")

    def _create_session(self) -> UrllibSession:
        try:
            ssl_context = create_ssl_context(self.ssl_verification)
        except ssl.SSLError as error:
            raise SejongAuthError(ErrorCode.SSL_CONFIGURATION_ERROR) from error
        return UrllibSession(ssl_context, self.timeout_seconds, USER_AGENT)
