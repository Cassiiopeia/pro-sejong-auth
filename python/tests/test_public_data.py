"""공개 데이터 테스트: fixture 주입 가짜 세션 > expected_public_data.json 비교 (학교 서버 호출 없음)"""

import dataclasses
import json
import threading
import time
from urllib.parse import parse_qs, urlparse

import pytest

from sejongpeia import ErrorCode, SejongAuthError, SejongPublicData
from sejongpeia.transport import HttpResponse

from conftest import read_expected, read_fixture

EXPECTED = read_expected("public_data/expected_public_data.json")
SIS_BASE = "https://sjpt.sejong.ac.kr/sch/sch/ext/SueOpenTimeQ/"


class RecordingSession:
    """URL 앞부분 > 응답 또는 예외, 요청(메서드·URL·본문·헤더) 기록"""

    def __init__(self, outcome_by_url_prefix):
        self.outcome_by_url_prefix = outcome_by_url_prefix
        self.requests = []

    def request(self, method, url, *, data=None, headers=None):
        self.requests.append({"method": method, "url": url, "data": data, "headers": headers or {}})
        for prefix, outcome in self.outcome_by_url_prefix.items():
            if url.startswith(prefix):
                if isinstance(outcome, BaseException):
                    raise outcome
                return outcome
        raise AssertionError(f"unexpected request: {method} {url}")


class FakeClock:
    def __init__(self):
        self.now = 1000.0

    def __call__(self):
        return self.now


def json_response(text: str, status: int = 200) -> HttpResponse:
    return HttpResponse(status=status, body=text.encode("utf-8"))


def fixture_response(file_name: str) -> HttpResponse:
    return json_response(read_fixture(f"public_data/{file_name}"))


def create_client(outcome_by_url_prefix, clock=None, cache_ttl_seconds=86400):
    session = RecordingSession(outcome_by_url_prefix)
    client = SejongPublicData(cache_ttl_seconds=cache_ttl_seconds, _session_factory=lambda: session, _clock=clock)
    return client, session


def to_plain(value):
    """모델 > dict·list (expected 와 비교)"""
    if isinstance(value, list):
        return [to_plain(item) for item in value]
    return dataclasses.asdict(value)


ALL_SOURCES = {
    SIS_BASE + "doOnload.do": fixture_response("reference_onload.json"),
    SIS_BASE + "doYearsmtlist.do": fixture_response("reference_terms.json"),
    SIS_BASE + "doLesndeptgroup.do": fixture_response("reference_departments.json"),
    SIS_BASE + "doList.do": fixture_response("lectures.json"),
    "https://www.sejong.ac.kr/kor/academics/academic-calendar.do": fixture_response("calendar.json"),
    "https://happydorm.sejong.ac.kr/food/": fixture_response("dormitory_menu.json"),
    "https://happydorm.sejong.ac.kr/bbs/": fixture_response("dormitory_notices.json"),
    "https://www.sejong.ac.kr/kor/unilife/cafeteria-info.do": fixture_response("cafeteria.json"),
    "https://www.sejong.ac.kr/kor/intro/campus-map.do": fixture_response("campus_places.json"),
    "https://do.sejong.ac.kr/ko/process/schedule/getEvents": fixture_response("extracurricular_events.json"),
}

CALLS = {
    "organization_categories": lambda c: c.organization_categories(),
    "terms": lambda c: c.terms("20"),
    "departments": lambda c: c.departments("20", "2099", "20"),
    "course_types": lambda c: c.course_types(),
    "selective_domains": lambda c: c.selective_domains(),
    "lectures": lambda c: c.lectures("20", "2099", "20", department_code="9101"),
    "calendar": lambda c: c.calendar("2099-10-01", "2099-11-01"),
    "dormitory_menu": lambda c: c.dormitory_menu(),
    "dormitory_notices": lambda c: c.dormitory_notices(),
    "cafeteria_menu": lambda c: c.cafeteria_menu("1"),
    "campus_places": lambda c: c.campus_places(),
    "extracurricular_events": lambda c: c.extracurricular_events("2099-10-01", "2099-11-01"),
}


@pytest.mark.parametrize("method_name", sorted(CALLS))
def test_result_matches_expected_fixture(method_name):
    client, _ = create_client(ALL_SOURCES)
    assert to_plain(CALLS[method_name](client)) == EXPECTED[method_name]


def test_extra_never_contains_denylisted_fields():
    client, _ = create_client(ALL_SOURCES)
    forbidden = {"CD_ALIAS1", "CD_ALIAS2", "CD_ALIAS3", "CD_ALIAS1_ENG", "regname", "regid", "STACK_TRACE"}
    for category in client.organization_categories():
        assert not forbidden & set(category.extra)
    assert "가상담당자" not in json.dumps(to_plain(client.organization_categories()), ensure_ascii=False)


# ---- 요청 계약
def test_sis_request_body_and_headers():
    client, session = create_client(ALL_SOURCES)
    client.departments("20", "2099", "20")
    request = session.requests[0]
    assert request["method"] == "POST"
    assert request["url"] == SIS_BASE + "doLesndeptgroup.do"
    assert json.loads(request["data"]) == {"dm_search": {"ORGN_CLSF_CD": "20", "YEAR": "2099", "SMT_CD": "20", "YEAR_SMT": "2099/20"}}
    assert request["headers"]["Content-Type"] == 'application/json; charset="UTF-8"'
    assert request["headers"]["Accept"] == "application/json"


def test_organization_categories_request_body_is_empty_search():
    client, session = create_client(ALL_SOURCES)
    client.organization_categories()
    assert json.loads(session.requests[0]["data"]) == {"dm_search": {}}


def test_lectures_request_body():
    client, session = create_client(ALL_SOURCES)
    client.lectures("20", "2099", "20", course_name="예시과목")
    assert json.loads(session.requests[0]["data"])["dm_search"] == {
        "ORGN_CLSF_CD": "20", "YEAR": "2099", "SMT_CD": "20", "YEAR_SMT": "2099/20",
        "DEPT_CD": "", "CURI_NM": "예시과목", "EMP_NM": "", "LANG_CHK": "KOR"}


def test_get_urls():
    client, session = create_client(ALL_SOURCES)
    client.calendar("2099-10-01", "2099-11-01", category_code="0002")
    client.cafeteria_menu("7")
    client.campus_places("BUILDING")
    queries = [parse_qs(urlparse(request["url"]).query) for request in session.requests]
    assert all(request["method"] == "GET" and request["data"] is None for request in session.requests)
    assert queries[0] == {"mode": ["getCalendarData"], "start": ["2099-10-01"], "end": ["2099-11-01"], "collDiv": ["0002"]}
    assert queries[1] == {"mode": ["getMenuList"], "placeId": ["7"]}
    assert queries[2] == {"mode": ["getPlaceUserList"], "placeType": ["BUILDING"]}
    assert session.requests[0]["url"].startswith("https://www.sejong.ac.kr/kor/academics/academic-calendar.do?")


def test_form_requests():
    client, session = create_client(ALL_SOURCES)
    client.dormitory_menu("SJ")
    client.dormitory_notices(page=2, rows=5, keyword="공지")
    client.extracurricular_events("2098-12-20", "2099-01-10", category="특강")
    forms = [parse_qs(request["data"].decode("utf-8")) for request in session.requests]
    assert forms[0] == {"locgbn": ["SJ"]}
    assert forms[1] == {"cPage": ["2"], "rows": ["5"], "bbs_locgbn": ["SJ"], "bbs_id": ["notice"], "sType": ["subject"], "sWord": ["공지"]}
    assert forms[2] == {"cid": ["eco"], "category": ["특강"], "start": ["2098-12-20"], "end": ["2099-01-10"],
                        "start_year": ["2098"], "end_year": ["2099"]}
    for request in session.requests:
        assert request["method"] == "POST"
        assert request["headers"]["Content-Type"] == "application/x-www-form-urlencoded; charset=UTF-8"
        assert request["headers"]["X-Requested-With"] == "XMLHttpRequest"


# ---- 입력 검증
def test_lectures_requires_at_least_one_filter():
    client, session = create_client(ALL_SOURCES)
    with pytest.raises(SejongAuthError) as error_info:
        client.lectures("20", "2099", "20", department_code=" ", course_name="", instructor_name="")
    assert error_info.value.code == ErrorCode.INVALID_INPUT
    assert session.requests == []


# ---- 캐시
def test_fresh_cache_skips_request_and_expired_cache_refreshes():
    clock = FakeClock()
    client, session = create_client(ALL_SOURCES, clock=clock, cache_ttl_seconds=100)
    client.course_types()
    clock.now += 99
    client.course_types()
    assert len(session.requests) == 1
    clock.now += 2
    client.course_types()
    assert len(session.requests) == 2


def test_cache_key_includes_arguments():
    client, session = create_client(ALL_SOURCES)
    client.cafeteria_menu("1")
    client.cafeteria_menu("2")
    assert len(session.requests) == 2


def test_cached_list_is_not_shared_with_caller():
    client, _ = create_client(ALL_SOURCES)
    client.course_types().clear()
    assert len(client.course_types()) == 1


def test_expired_cache_is_returned_when_refresh_fails():
    clock = FakeClock()
    session = RecordingSession(ALL_SOURCES)
    client = SejongPublicData(cache_ttl_seconds=10, _session_factory=lambda: session, _clock=clock)
    first = client.cafeteria_menu("1")
    session.outcome_by_url_prefix = {"https://www.sejong.ac.kr/": OSError("down")}
    clock.now += 1000
    assert client.cafeteria_menu("1") == first


def test_concurrent_calls_share_one_request():
    class SlowSession(RecordingSession):
        def request(self, method, url, *, data=None, headers=None):
            time.sleep(0.2)
            return super().request(method, url, data=data, headers=headers)

    session = SlowSession(ALL_SOURCES)
    client = SejongPublicData(_session_factory=lambda: session)
    results = []
    threads = [threading.Thread(target=lambda: results.append(client.course_types())) for _ in range(5)]
    for thread in threads:
        thread.start()
    for thread in threads:
        thread.join()
    assert len(session.requests) == 1
    assert len(results) == 5


# ---- 실패 격리
FAILURES = {
    "connection_error": OSError("refused"),
    "timeout": TimeoutError("slow"),
    "server_error": json_response("{}", status=500),
    "not_json": json_response("<html>maintenance</html>"),
    "submit_error": json_response('{"_SUBMIT_ERROR_": {"ERRTYPE": "X", "STACK_TRACE": "secret internals"}}'),
    "missing_key": json_response('{"unexpected": []}'),
    "wrong_shape": json_response('{"items": "no", "data": 1, "events": null, "root": []}'),
}


@pytest.mark.parametrize("failure_name", sorted(FAILURES))
@pytest.mark.parametrize("method_name", sorted(CALLS))
def test_upstream_failure_returns_empty_value(method_name, failure_name, caplog):
    outcome = FAILURES[failure_name]
    client, _ = create_client({"https://": outcome})
    result = CALLS[method_name](client)
    if method_name == "dormitory_notices":
        assert to_plain(result) == {"total": 0, "pinned": [], "items": []}
    else:
        assert result == []
    assert "secret internals" not in caplog.text
