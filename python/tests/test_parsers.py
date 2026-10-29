"""spec/fixtures 파싱 결과 == 기대값 (Java, Node 와 같은 기준)"""

import re

import pytest
from conftest import read_expected, read_fixture

from sejong_auth.errors import ErrorCode, SejongAuthError
from sejong_auth.parsers import ensure_portal_login_accepted, parse_classic_reading, parse_sis_user_info, parse_student_info


def to_snake_case(name: str) -> str:
    return re.sub(r"(?<!^)(?=[A-Z])", "_", name).lower()


def snake_case_keys(value):
    if isinstance(value, dict):
        return {to_snake_case(key): snake_case_keys(item) for key, item in value.items()}
    if isinstance(value, list):
        return [snake_case_keys(item) for item in value]
    return value


def test_parse_dhc_fixture(dhc_html):
    expected = snake_case_keys(read_expected("expected_dhc.json"))

    assert parse_student_info(dhc_html).to_dict() == expected["student_info"]
    assert parse_classic_reading(dhc_html).to_dict() == expected["classic_reading"]


def test_parse_sis_fixture(sis_json):
    expected = snake_case_keys(read_expected("expected_sis.json"))

    assert parse_sis_user_info(sis_json).to_dict() == expected


def test_parse_classic_reading_user_info_values():
    html = ('<div class="b-con-box"><h4 class="b-h4-tit01">사용자 정보</h4><table class="b-board-table"><tbody>'
            '<tr><th>이수 학기</th><td>8 학기</td></tr><tr><th>인증완료 학기</th><td>6 학기</td></tr>'
            '<tr><th>인증여부</th><td>아니오(고전특강 대상자) 2099년도 1학기 대체이수</td></tr></tbody></table></div>')

    reading = parse_classic_reading(html)

    assert (reading.completed_semesters, reading.certified_semesters) == ("8 학기", "6 학기")
    assert reading.certification_status == "아니오(고전특강 대상자) 2099년도 1학기 대체이수"


def test_parse_classic_reading_missing_user_info_labels_are_empty():
    html = ('<div class="b-con-box"><h4 class="b-h4-tit01">사용자 정보</h4><table class="b-board-table"><tbody>'
            '<tr><th>학년</th><td>3</td></tr></tbody></table></div>')

    reading = parse_classic_reading(html)

    assert (reading.completed_semesters, reading.certified_semesters, reading.certification_status) == ("", "", "")


def test_parse_sis_user_category_missing_is_empty():
    sis_json = '{"dm_UserInfo": {"INTG_USR_NO": "21011234"}, "dm_UserInfoSch": {}}'

    assert parse_sis_user_info(sis_json).user_category == ""


def test_decodes_common_named_entities():
    # Node·Jsoup 과 같은 기대값
    html = ('<div class="b-con-box"><h4 class="b-h4-tit01">사용자 정보</h4><table class="b-board-table"><tbody>'
            '<tr><th>학과명</th><td>&lsquo;컴퓨터&rsquo; &ldquo;공학&rdquo;&hellip; &ndash;&mdash;</td></tr></tbody></table></div>')
    assert parse_student_info(html).major == "‘컴퓨터’ “공학”… –—"


# 포털 로그인 판정값 > 에러 코드 (포털 화면과 같은 분기, spec/fixtures/expected_portal_login.json)
PORTAL_LOGIN_CODE_BY_RESULT = {key: value for key, value in read_expected("expected_portal_login.json").items() if not key.startswith("_")}


def portal_login_body(result_value: str) -> str:
    return read_fixture("portal_login_erridpwd.html").replace("var result = 'erridpwd'", f"var result = '{result_value}'")


@pytest.mark.parametrize("result_value,expected_code", sorted(PORTAL_LOGIN_CODE_BY_RESULT.items()))
def test_portal_login_result_matches_portal_screen(result_value, expected_code):
    body = portal_login_body(result_value)
    if expected_code is None:
        ensure_portal_login_accepted(body)
        return
    with pytest.raises(SejongAuthError) as error:
        ensure_portal_login_accepted(body)
    assert error.value.code.value == expected_code


def test_portal_login_real_responses():
    ensure_portal_login_accepted(read_fixture("portal_login_ok.html"))
    with pytest.raises(SejongAuthError) as error:
        ensure_portal_login_accepted(read_fixture("portal_login_erridpwd.html"))
    assert error.value.code == ErrorCode.AUTHENTICATION_FAILED


def test_portal_login_locked_message_points_to_password_reset():
    with pytest.raises(SejongAuthError) as error:
        ensure_portal_login_accepted(portal_login_body("pwdNeedChg"))
    assert "https://portal.sejong.ac.kr/jsp/inquiry/pwinquiry.jsp" in error.value.message


def test_portal_login_without_result_keeps_legacy_check():
    ensure_portal_login_accepted("")
    ensure_portal_login_accepted("<html>changed portal page</html>")
