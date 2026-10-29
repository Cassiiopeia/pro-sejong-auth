"""공개 데이터 테스트: fixture 주입 가짜 세션 > expected_public_data.json 비교 (학교 서버 호출 없음)"""

import dataclasses
import json
import threading
import time
from urllib.parse import parse_qs, urlparse

import pytest

from sejongpeia import ErrorCode, SejongAuthError, SejongPublicData
from sejongpeia.common.transport import HttpResponse

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
    "https://www.sejong.ac.kr/curriculum/getCurriculumOneKor.do": fixture_response("curriculum_course.json"),
    "https://www.sejong.ac.kr/kor/academics/academic-calendar.do": fixture_response("calendar.json"),
    "https://graduate.sejong.ac.kr/": fixture_response("graduate_calendar.json"),
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
    "curriculum_courses": lambda c: c.curriculum_courses("9101", "900001"),
    "calendar": lambda c: c.calendar("2099-10-01", "2099-11-01"),
    "graduate_calendar": lambda c: c.graduate_calendar("graduate", 2099, 10),
    "dormitory_menu": lambda c: c.dormitory_menu(),
    "dormitory_notices": lambda c: c.dormitory_notices(),
    "cafeteria_menu": lambda c: c.cafeteria_menu("1"),
    "cafeteria_places": lambda c: c.cafeteria_places(),
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
    client.organization_categories()
    assert json.loads(session.requests[1]["data"]) == {"dm_search": {}}
    client.lectures("20", "2099", "20", course_name="예시과목")
    # 학교가 필터 키 전체를 요구함 (하나라도 빠지면 _SUBMIT_ERROR_)
    assert json.loads(session.requests[2]["data"])["dm_search"] == {
        **{key: "" for key in ("SMT_NM", "CURI_TYPE_CD", "SLT_DOMAIN_CD", "CURI_NO", "CLASS",
                               "SLT_DEPT_CD", "PLAN_REG_YN", "CORS_SCHE_GRP_CD")},
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


def test_curriculum_courses_request_and_input_check():
    client, session = create_client(ALL_SOURCES)
    client.curriculum_courses("9101", "900001")
    assert session.requests[0]["method"] == "GET"
    assert session.requests[0]["url"] == "https://www.sejong.ac.kr/curriculum/getCurriculumOneKor.do?curiNo=900001&deptNo=9101"
    # 학교가 Accept: application/json 이 없으면 404 를 돌려줌
    assert session.requests[0]["headers"]["Accept"] == "application/json"
    for department_code, course_number in (("", "900001"), ("9101", "  "), (None, "900001")):
        with pytest.raises(SejongAuthError) as error_info:
            client.curriculum_courses(department_code, course_number)
        assert error_info.value.code == ErrorCode.INVALID_INPUT
    assert len(session.requests) == 1  # 잘못된 입력은 요청 없음
    # 그 학과 교육과정에 없는 과목은 빈 items 로 오며 정상 응답
    empty_client, _ = create_client({"https://": json_response('{"success": true, "items": []}')})
    assert empty_client.curriculum_courses("9101", "000000") == []


def test_graduate_calendar_request_and_input_check():
    client, session = create_client(ALL_SOURCES)
    client.graduate_calendar("graduate", 2099, 10)
    assert session.requests[0]["method"] == "GET"
    assert session.requests[0]["url"] == ("https://graduate.sejong.ac.kr/_custom/sejong/_app/calendar-graduate/proc/"
                                          "getCalendarData.jsp?year=2099&month=10&siteId=graduate")
    for invalid_site_id in ("", "  ", "evil.example.com/x", "Graduate", None):
        with pytest.raises(SejongAuthError) as error_info:
            client.graduate_calendar(invalid_site_id, 2099, 10)
        assert error_info.value.code == ErrorCode.INVALID_INPUT
    assert len(session.requests) == 1  # 잘못된 입력은 요청 없음


def test_graduate_calendar_success_false_is_failure_and_empty_month_is_not():
    client, _ = create_client({"https://": json_response('{"success": false, "items": "[]"}')})
    assert client.graduate_calendar("graduate", 2099, 1) == []
    client, session = create_client({"https://": json_response('{"success": true, "items": "[]"}')})
    assert client.graduate_calendar("arts", 2099, 1) == []
    client.graduate_calendar("arts", 2099, 1)
    assert len(session.requests) == 1  # 빈 달은 정상 응답이라 캐시됨


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
def test_lectures_without_any_filter_lists_whole_semester_with_one_request():
    # 학교 서버는 검색 값이 모두 비어도 그 학기 전체를 돌려줌 (2026-10-30 확인)
    client, session = create_client(ALL_SOURCES)
    lectures = client.lectures("20", "2099", "20", course_name="  ")  # 공백만 있는 값은 빈 값으로 취급
    assert len(lectures) > 0
    assert len(session.requests) == 1
    search = json.loads(session.requests[0]["data"])["dm_search"]
    assert [search[key] for key in ("DEPT_CD", "CURI_NM", "EMP_NM")] == ["", "", ""]


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
    client.cafeteria_menu("1")  # 인자가 다르면 다른 키
    client.cafeteria_menu("2")
    assert len(session.requests) == 4
    client.course_types().clear()  # 호출자가 캐시를 바꾸지 못함
    assert len(client.course_types()) == 1


def test_failure_is_remembered_and_stale_cache_is_still_returned():
    clock = FakeClock()
    session = RecordingSession({"https://": OSError("down")})
    client = SejongPublicData(cache_ttl_seconds=10, failure_cache_seconds=60, _session_factory=lambda: session, _clock=clock)
    assert client.cafeteria_menu("1") == [] and client.cafeteria_menu("1") == []
    assert len(session.requests) == 1  # 실패 기억 중에는 요청 없음
    clock.now += 61
    client.cafeteria_menu("1")
    assert len(session.requests) == 2
    session.outcome_by_url_prefix = ALL_SOURCES
    clock.now += 61
    first = client.cafeteria_menu("1")
    session.outcome_by_url_prefix = {"https://": OSError("down")}
    clock.now += 1000
    assert client.cafeteria_menu("1") == first  # 만료된 캐시 값


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


# ---- 식당 목록
class CafeteriaPlaceSession:
    """식당 번호 > 메뉴 행 목록 (없는 번호는 빈 목록), 요청 기록"""

    def __init__(self, rows_by_place_id, failing_place_id=None):
        self.rows_by_place_id = rows_by_place_id
        self.failing_place_id = failing_place_id
        self.place_ids = []

    def request(self, method, url, *, data=None, headers=None):
        place_id = parse_qs(urlparse(url).query)["placeId"][0]
        self.place_ids.append(place_id)
        if place_id == self.failing_place_id:
            raise OSError("down")
        return json_response(json.dumps({"items": self.rows_by_place_id.get(place_id, [])}, ensure_ascii=False))


def cafeteria_row(place_id, place_name, **over):
    return {"placeId": int(place_id), "placeName": place_name, "buildingName": "예시관", "location": "B1층", "menuName": "예시메뉴",
            "price": 1000, **over}


def test_cafeteria_places_collects_each_place_once_in_id_order_with_bounded_requests():
    session = CafeteriaPlaceSession({"6": [cafeteria_row(6, "여섯째")], "1": [cafeteria_row(1, "첫째", mealTypeId=1), cafeteria_row(1, "첫째")],
                                     "3": [cafeteria_row(3, "셋째", operatingHours="예시")]})
    client = SejongPublicData(_session_factory=lambda: session)
    places = client.cafeteria_places()
    assert [(place.place_id, place.place_name) for place in places] == [("1", "첫째"), ("3", "셋째"), ("6", "여섯째")]
    assert places[1].extra == {"operatingHours": "예시"} and places[0].extra == {}
    assert session.place_ids == [str(number) for number in range(1, 11)]  # 순서대로 한 번씩, 10건 한도
    client.cafeteria_places()
    assert len(session.place_ids) == 10  # 캐시 유효 중에는 요청 없음


def test_cafeteria_places_one_failed_request_fails_the_whole_list_and_keeps_stale_value():
    clock = FakeClock()
    session = CafeteriaPlaceSession({"1": [cafeteria_row(1, "첫째")]})
    client = SejongPublicData(cache_ttl_seconds=10, _session_factory=lambda: session, _clock=clock)
    first = client.cafeteria_places()
    session.failing_place_id = "4"
    clock.now += 11
    assert client.cafeteria_places() == first  # 일부만 성공한 목록을 새 값으로 저장하지 않음
    fresh_client = SejongPublicData(_session_factory=lambda: session)
    assert fresh_client.cafeteria_places() == []


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


# ---- 느슨한 매핑
def test_row_with_missing_fields_survives_and_values_are_text():
    body = '{"items": [{"id": 7, "latitude": null, "meta": {"a": [1, 2]}}, {"placeName": "예시관", "latitude": "37.5", "flag": true}]}'
    client, _ = create_client({"https://": json_response(body)})
    places = client.campus_places()
    assert [place.id for place in places] == ["7", ""]
    assert places[0].latitude == 0.0 and places[0].name == ""
    assert places[0].extra == {"meta": '{"a":[1,2]}'}  # 비스칼라는 압축 JSON
    assert places[1].latitude == 37.5 and places[1].extra == {"flag": "true"}


def test_dormitory_notices_without_results_is_empty_not_failure():
    client, session = create_client({"https://": json_response('{"root": [{}]}')})
    assert to_plain(client.dormitory_notices()) == {"total": 0, "pinned": [], "items": []}
    client.dormitory_notices()
    assert len(session.requests) == 1  # 정상 응답으로 캐시됨


# ---- 캐시 상한
def test_cache_evicts_oldest_entry_and_its_lock():
    client = SejongPublicData(cache_max_entries=2, _session_factory=lambda: RecordingSession(ALL_SOURCES))
    for place_id in ("1", "2", "3"):
        client.cafeteria_menu(place_id)
    assert len(client._cache) == 2 and len(client._key_locks) == 2
    assert ("cafeteria_menu", "1") not in client._cache
