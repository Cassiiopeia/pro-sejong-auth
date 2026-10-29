"""클라이언트 흐름: 입력 검증, 401, SIS 실패 무시, 타임아웃, 재시도 (네트워크 없음)"""

import pytest
from conftest import FakeSession, ok, status

from sejongpeia import ErrorCode, SejongAuthClient, SejongAuthError

LOGIN = "https://portal.sejong.ac.kr/jsp/login/login_action.jsp"
CLASSIC_SSO = "https://classic.sejong.ac.kr/_custom/sejong/sso/sso-return.jsp"
CLASSIC_STATUS = "https://classic.sejong.ac.kr/classic/reading/status.do"
SIS_SSO = "https://sjpt.sejong.ac.kr/main/view/Login/doSsoLogin.do"
SIS_USER_INFO = "https://sjpt.sejong.ac.kr/main/sys/UserInfo/initUserInfo.do"
PORTAL_MAIN = "https://portal.sejong.ac.kr/user/index.do"


def client_with(responses_by_url_prefix, **options):
    sessions = []

    def session_factory():
        session = FakeSession(responses_by_url_prefix)
        sessions.append(session)
        return session

    return SejongAuthClient(_session_factory=session_factory, **options), sessions


@pytest.mark.parametrize("student_id,password", [("", "pw"), ("21011234", ""), ("   ", "pw"), (None, "pw")])
def test_empty_input_raises_invalid_input_without_network(student_id, password):
    client, sessions = client_with({})

    with pytest.raises(SejongAuthError) as error:
        client.authenticate(student_id, password)

    assert error.value.code == ErrorCode.INVALID_INPUT
    assert sessions == []


def test_wrong_password_401_raises_authentication_failed():
    client, _ = client_with({LOGIN: [ok()], CLASSIC_SSO: [ok()], CLASSIC_STATUS: [status(401)]})

    with pytest.raises(SejongAuthError) as error:
        client.authenticate_dhc("21011234", "wrong")

    assert error.value.code == ErrorCode.AUTHENTICATION_FAILED


def test_integrated_sis_failure_returns_dhc_with_null_contacts(dhc_html):
    client, _ = client_with({
        LOGIN: [ok()], CLASSIC_SSO: [ok()], CLASSIC_STATUS: [ok(dhc_html)],
        SIS_SSO: [ok()], SIS_USER_INFO: [status(500)],
    })

    result = client.authenticate("21011234", "pw")

    assert result.success is True
    assert result.name == "홍길동"
    assert result.major == "컴퓨터공학과"
    assert (result.email, result.phone_number, result.english_name, result.user_category) == (None, None, None, None)


def test_integrated_success_merges_dhc_and_sis(dhc_html, sis_json):
    client, sessions = client_with({
        LOGIN: [ok()], CLASSIC_SSO: [ok()], CLASSIC_STATUS: [ok(dhc_html)],
        SIS_SSO: [ok()], SIS_USER_INFO: [ok(sis_json)],
    })

    result = client.authenticate("21011234", "pw")

    # 포털 로그인 1회, 세션 1개로 DHC·SIS 모두 조회
    assert len(sessions) == 1
    assert sessions[0].requested_urls.count(LOGIN) == 1
    assert result.grade == "3"
    assert result.classic_reading.certifications[2].area == "동·서양의 문학 (3권)"
    assert result.phone_number == "010-1234-5678"
    assert result.english_name == "Hong Gildong"
    assert result.user_category == "학부생"
    assert result.classic_reading.completed_semesters == "6 학기"
    assert result.classic_reading.certification_status == "아니오"


def test_timeout_raises_connection_timeout():
    client, _ = client_with({LOGIN: [TimeoutError("timed out")]}, max_retry=3)

    with pytest.raises(SejongAuthError) as error:
        client.authenticate_dhc("21011234", "pw")

    assert error.value.code == ErrorCode.CONNECTION_TIMEOUT


def test_retry_exhausted_raises_connection_failed():
    client, sessions = client_with({LOGIN: [status(500)]}, max_retry=3)

    with pytest.raises(SejongAuthError) as error:
        client.authenticate_dhc("21011234", "pw")

    assert error.value.code == ErrorCode.CONNECTION_FAILED
    assert sessions[0].requested_urls.count(LOGIN) == 3


def test_retry_then_success(dhc_html):
    client, _ = client_with({LOGIN: [status(503), ok()], CLASSIC_SSO: [ok()], CLASSIC_STATUS: [ok(dhc_html)]})

    assert client.authenticate_dhc("21011234", "pw").student_id == "21011234"


def test_sis_result_has_empty_grade_and_status_and_raw_json(sis_json):
    client, _ = client_with({LOGIN: [ok()], SIS_SSO: [ok()], SIS_USER_INFO: [ok(sis_json)]})

    result = client.authenticate_sis("21011234", "pw", include_raw=True)

    assert (result.grade, result.status) == ("", "")
    assert result.email == "student@example.com"
    assert result.raw_json == sis_json


def test_school_codes_always_and_extra_only_when_requested(dhc_html, sis_json):
    routes = {LOGIN: [ok()], CLASSIC_SSO: [ok()], CLASSIC_STATUS: [ok(dhc_html)], SIS_SSO: [ok()], SIS_USER_INFO: [ok(sis_json)]}
    sis_result = client_with(routes)[0].authenticate_sis("21011234", "pw")
    integrated_result = client_with(routes)[0].authenticate("21011234", "pw", include_extra=True)

    assert (sis_result.department_code, sis_result.organization_classification_code, sis_result.extra) == ("1234567", "20", None)
    assert integrated_result.department_code == "1234567"
    assert integrated_result.extra["dm_UserInfoSch.GRADE_SCOPE_CD"] == "S1"


def test_dhc_raw_html_only_when_requested(dhc_html):
    client, _ = client_with({LOGIN: [ok()], CLASSIC_SSO: [ok()], CLASSIC_STATUS: [ok(dhc_html)]})

    assert client.authenticate_dhc("21011234", "pw").raw_html is None
    assert client.authenticate_dhc("21011234", "pw", include_raw=True).raw_html == dhc_html


def test_integrated_raw_only_when_requested(dhc_html, sis_json):
    routes = {LOGIN: [ok()], CLASSIC_SSO: [ok()], CLASSIC_STATUS: [ok(dhc_html)], SIS_SSO: [ok()], SIS_USER_INFO: [ok(sis_json)]}
    client, _ = client_with(routes)
    default_result = client.authenticate("21011234", "pw")
    client, _ = client_with(routes)
    raw_result = client.authenticate("21011234", "pw", include_raw=True)

    assert (default_result.raw_html, default_result.raw_json) == (None, None)
    assert (raw_result.raw_html, raw_result.raw_json) == (dhc_html, sis_json)
    # 켜도 기존 필드는 같음
    assert (raw_result.name, raw_result.email, raw_result.grade) == (default_result.name, default_result.email, default_result.grade)


def test_error_message_never_contains_password():
    client, _ = client_with({LOGIN: [OSError("connection reset")]})

    with pytest.raises(SejongAuthError) as error:
        client.authenticate_dhc("21011234", "secret-password-value")

    assert error.value.code == ErrorCode.CONNECTION_FAILED
    assert "secret-password-value" not in str(error.value)


def test_sis_wrong_password_submit_error_raises_authentication_failed():
    # 틀린 비밀번호: initUserInfo 가 200 + _SUBMIT_ERROR_(ERRTYPE LOGOUT) 를 돌려줌
    from conftest import read_fixture
    client, _ = client_with({LOGIN: [ok()], SIS_SSO: [ok()], SIS_USER_INFO: [ok(read_fixture("sis_submit_error_logout.json"))]})

    with pytest.raises(SejongAuthError) as error:
        client.authenticate_sis("21011234", "wrong")

    assert error.value.code == ErrorCode.AUTHENTICATION_FAILED


def test_wrong_password_login_result_stops_before_sso():
    # 포털 판정 erridpwd > SSO·데이터 요청 없이 즉시 실패 (불필요한 요청 0)
    from conftest import read_fixture
    client, sessions = client_with({LOGIN: [ok(read_fixture("portal_login_erridpwd.html"))]})

    with pytest.raises(SejongAuthError) as error:
        client.authenticate("21011234", "wrong")

    assert error.value.code == ErrorCode.AUTHENTICATION_FAILED
    assert sessions[0].requested_urls == [LOGIN]


def test_locked_account_raises_account_locked_without_retry():
    from conftest import read_fixture
    locked_body = read_fixture("portal_login_erridpwd.html").replace("'erridpwd'", "'pwdNeedChg'")
    client, sessions = client_with({LOGIN: [ok(locked_body)]})

    with pytest.raises(SejongAuthError) as error:
        client.authenticate_sis("21011234", "pw")

    assert error.value.code == ErrorCode.ACCOUNT_LOCKED
    assert sessions[0].requested_urls == [LOGIN]


def test_integrated_includes_parent_unit_name_from_portal_main(dhc_html, sis_json, portal_main_html):
    client, sessions = client_with({
        LOGIN: [ok()], CLASSIC_SSO: [ok()], CLASSIC_STATUS: [ok(dhc_html)],
        SIS_SSO: [ok()], SIS_USER_INFO: [ok(sis_json)], PORTAL_MAIN: [ok(portal_main_html)],
    })

    result = client.authenticate("21011234", "pw")

    assert result.parent_unit_name == "공과대학"
    assert sessions[0].requested_urls.count(PORTAL_MAIN) == 1
    assert sessions[0].requested_urls.count(LOGIN) == 1


@pytest.mark.parametrize("portal_main_outcome", [
    TimeoutError("timed out"),
    OSError("connection reset"),
    status(500),
    status(401),
    ok(""),
    ok("<html><form action='/jsp/login/login_action.jsp'>통합 로그인</form></html>"),
], ids=["timeout", "network-error", "http-500", "http-401", "empty-body", "login-page-redirect"])
def test_portal_main_call_failure_is_none_and_keeps_other_fields(dhc_html, sis_json, portal_main_outcome):
    client, sessions = client_with({
        LOGIN: [ok()], CLASSIC_SSO: [ok()], CLASSIC_STATUS: [ok(dhc_html)],
        SIS_SSO: [ok()], SIS_USER_INFO: [ok(sis_json)], PORTAL_MAIN: [portal_main_outcome],
    })

    result = client.authenticate("21011234", "pw")

    assert result.parent_unit_name is None
    assert (result.email, result.user_category) == ("student@example.com", "학부생")
    assert result.name == "홍길동"
    assert result.classic_reading.completed_semesters == "6 학기"
    # 보조 조회는 재시도 없이 1회
    assert sessions[0].requested_urls.count(PORTAL_MAIN) == 1


def test_portal_main_without_my_info_is_empty_string_not_none(dhc_html, sis_json):
    client, _ = client_with({
        LOGIN: [ok()], CLASSIC_SSO: [ok()], CLASSIC_STATUS: [ok(dhc_html)],
        SIS_SSO: [ok()], SIS_USER_INFO: [ok(sis_json)], PORTAL_MAIN: [ok("<html><body>교직원 화면</body></html>")],
    })

    assert client.authenticate("21011234", "pw").parent_unit_name == ""
