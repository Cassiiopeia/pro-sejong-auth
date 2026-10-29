"""세 언어 공통 값: spec/contract.json <-> Python 상수 (어긋나면 실패)"""

import json

from conftest import FIXTURE_DIRECTORY

from sejongpeia.auth import client, parsers
from sejongpeia.common import transport
from sejongpeia.common.errors import DEFAULT_MESSAGE_BY_CODE

CONTRACT = json.loads((FIXTURE_DIRECTORY.parent / "contract.json").read_text(encoding="utf-8"))


def test_portal_values_match_contract():
    portal = CONTRACT["portal"]
    assert client.PORTAL_LOGIN_URL == portal["loginUrl"]
    assert client.PORTAL_REFERER == portal["referer"]
    assert client.PORTAL_DEFAULT_COOKIE == portal["defaultCookie"]
    assert parsers.PORTAL_LOGIN_ACCEPTED == portal["loginAcceptedResult"]
    assert {result: code.value for result, code in parsers.PORTAL_LOGIN_ERROR_CODE_BY_RESULT.items()} \
        == portal["loginErrorCodeByResult"]


def test_portal_main_values_match_contract():
    portal_main = CONTRACT["portalMain"]
    assert (client.PORTAL_MAIN_URL, client.PORTAL_LOGIN_PAGE_MARKER) == (portal_main["url"], portal_main["loginPageMarker"])


def test_classic_and_sis_values_match_contract():
    classic, sis = CONTRACT["classic"], CONTRACT["sis"]
    assert (client.CLASSIC_RETURN_URL, client.CLASSIC_SSO_URL, client.CLASSIC_STATUS_URL) \
        == (classic["returnUrl"], classic["ssoUrl"], classic["statusUrl"])
    assert (client.SIS_RETURN_URL, client.SIS_SSO_URL, client.SIS_USER_INFO_URL, client.SIS_ORIGIN) \
        == (sis["returnUrl"], sis["ssoUrl"], sis["userInfoUrl"], sis["origin"])
    assert (parsers.SIS_SUBMIT_ERROR_KEY, parsers.SIS_LOGGED_OUT_ERROR_TYPE) \
        == (sis["submitErrorKey"], sis["loggedOutErrorType"])
    assert parsers.SIS_EXTRA_EXCLUDED_KEY_PATTERN == sis["extraExcludedKeyPattern"]


def test_sis_schedule_values_match_contract():
    schedule = CONTRACT["sisSchedule"]
    assert (client.SIS_MENU_LIST_URL, client.SIS_USER_ROLE_URL, client.SIS_SCHEDULE_LIST_URL, client.SIS_SCHEDULE_PROGRAM_KEY,
            client.SIS_SCHEDULE_SYSTEM_KEY, client.SIS_MENU_SYSTEM_DIV) \
        == (schedule["menuListUrl"], schedule["userRoleUrl"], schedule["listUrl"], schedule["programKey"], schedule["systemKey"],
            schedule["menuSystemDiv"])


def test_tls_and_error_messages_match_contract():
    assert transport.PORTAL_CIPHERS == CONTRACT["opensslCiphers"]
    assert {code.value: message for code, message in DEFAULT_MESSAGE_BY_CODE.items()} == CONTRACT["errorMessageByCode"]
