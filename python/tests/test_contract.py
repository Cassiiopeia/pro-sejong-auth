"""세 언어 공통 값: spec/contract.json <-> Python 상수 (어긋나면 실패)"""

import json

from conftest import FIXTURE_DIRECTORY

from sejongpeia.auth import client, parsers
from sejongpeia.common import transport
from sejongpeia.common.errors import DEFAULT_MESSAGE_BY_CODE
from sejongpeia.public_data import (
    academic_source, cafeteria_source, calendar_source, campus_source, dormitory_source, extracurricular_source, fetcher,
)

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


def test_public_data_request_basics_match_contract():
    contract = CONTRACT["publicData"]
    assert (fetcher.JSON_ACCEPT, fetcher.FORM_CONTENT_TYPE, fetcher.FORM_REQUESTED_WITH) \
        == (contract["jsonAccept"], contract["formContentType"], contract["formRequestedWith"])
    assert fetcher.SUBMIT_ERROR_MARKER == CONTRACT["sis"]["submitErrorKey"]


def test_public_data_sis_timetable_values_match_contract():
    sis_timetable, curriculum = CONTRACT["publicData"]["sisTimetable"], CONTRACT["publicData"]["curriculum"]
    assert (academic_source.SIS_OPEN_TIME_URL, academic_source.SIS_CONTENT_TYPE, academic_source.SIS_SEARCH_KEY) \
        == (sis_timetable["baseUrl"], sis_timetable["contentType"], sis_timetable["searchKey"])
    assert (academic_source.SIS_ONLOAD_ENDPOINT, academic_source.SIS_TERM_LIST_ENDPOINT, academic_source.SIS_DEPARTMENT_LIST_ENDPOINT,
            academic_source.SIS_LECTURE_LIST_ENDPOINT) \
        == (sis_timetable["onloadEndpoint"], sis_timetable["termListEndpoint"], sis_timetable["departmentListEndpoint"],
            sis_timetable["lectureListEndpoint"])
    assert list(academic_source.LECTURE_EMPTY_FILTER_KEYS) == sis_timetable["lectureEmptyFilterKeys"]
    assert academic_source.CURRICULUM_COURSE_URL == curriculum["url"]
    assert list(academic_source.CURRICULUM_QUERY_KEYS) == curriculum["queryKeys"]


def test_public_data_calendar_values_match_contract():
    calendar, graduate_calendar = CONTRACT["publicData"]["calendar"], CONTRACT["publicData"]["graduateCalendar"]
    assert (calendar_source.ACADEMIC_CALENDAR_URL, calendar_source.CALENDAR_MODE, calendar_source.DEFAULT_CALENDAR_CATEGORY_CODE) \
        == (calendar["url"], calendar["mode"], calendar["defaultCategoryCode"])
    assert list(calendar_source.CALENDAR_QUERY_KEYS) == calendar["queryKeys"]
    assert calendar_source.GRADUATE_CALENDAR_URL == graduate_calendar["urlTemplate"]
    assert list(calendar_source.GRADUATE_CALENDAR_QUERY_KEYS) == graduate_calendar["queryKeys"]
    assert calendar_source.GRADUATE_SITE_ID_PATTERN.pattern == graduate_calendar["siteIdPattern"]


def test_public_data_dormitory_values_match_contract():
    dormitory = CONTRACT["publicData"]["dormitory"]
    assert (dormitory_source.DORMITORY_MENU_URL, dormitory_source.DORMITORY_NOTICE_URL, dormitory_source.DEFAULT_DORMITORY_LOCATION) \
        == (dormitory["menuUrl"], dormitory["noticeUrl"], dormitory["defaultLocation"])
    assert (dormitory_source.DORMITORY_NOTICE_BOARD_ID, dormitory_source.DORMITORY_NOTICE_SEARCH_TYPE) \
        == (dormitory["noticeBoardId"], dormitory["noticeSearchType"])
    assert list(dormitory_source.DORMITORY_MENU_FORM_KEYS) == dormitory["menuFormKeys"]
    assert list(dormitory_source.DORMITORY_NOTICE_FORM_KEYS) == dormitory["noticeFormKeys"]


def test_public_data_cafeteria_campus_extracurricular_values_match_contract():
    cafeteria, campus_map, extracurricular = (CONTRACT["publicData"][name] for name in ("cafeteria", "campusMap", "extracurricular"))
    assert (cafeteria_source.CAFETERIA_URL, cafeteria_source.CAFETERIA_MENU_MODE, cafeteria_source.CAFETERIA_DEFAULT_MAX_PLACE_ID,
            cafeteria_source.CAFETERIA_LIMIT_MAX_PLACE_ID) \
        == (cafeteria["url"], cafeteria["menuMode"], cafeteria["defaultMaxPlaceId"], cafeteria["limitMaxPlaceId"])
    assert list(cafeteria_source.CAFETERIA_QUERY_KEYS) == cafeteria["queryKeys"]
    assert (campus_source.CAMPUS_MAP_URL, campus_source.CAMPUS_PLACE_LIST_MODE) == (campus_map["url"], campus_map["mode"])
    assert list(campus_source.CAMPUS_MAP_QUERY_KEYS) == campus_map["queryKeys"]
    assert (extracurricular_source.EXTRACURRICULAR_EVENTS_URL, extracurricular_source.EXTRACURRICULAR_CID) \
        == (extracurricular["url"], extracurricular["cid"])
    assert list(extracurricular_source.EXTRACURRICULAR_FORM_KEYS) == extracurricular["formKeys"]
