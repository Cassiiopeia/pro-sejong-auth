#!/usr/bin/env python3
"""공개 데이터 소스 9곳 형태 점검 (개설강좌, 학과 교육과정, 학사일정, 기숙사 식단·공지, 학식, 캠퍼스맵, 비교과) - 로그인·계정 불필요

  실제 호출:   python3 scripts/probe-public-data.py          (요청 9건, 1초 간격)
  오프라인:    python3 scripts/probe-public-data.py --offline (spec/fixtures/public_data 로 검사 로직만 확인)
  종료 코드: 0 정상, 1 형태 변경 감지 (spec/PUBLIC_DATA.md 와 세 언어 구현을 함께 고칠 것)
네트워크 오류·시간 초과는 SKIP 으로 출력만 하고 실패로 보지 않음 (라이브러리는 실패를 격리하므로 형태 변경만 잡음)
출력은 건수와 누락 필드 이름만
"""

import json
import sys
import time
import urllib.parse
from pathlib import Path
from typing import Callable, Dict, List, Optional, Tuple

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "python" / "src"))

import sejongpeia.public_data.client as public_data_module  # noqa: E402
from sejongpeia.common.transport import USER_AGENT, UrllibSession, create_ssl_context  # noqa: E402

REQUEST_INTERVAL_SECONDS = 1
TIMEOUT_SECONDS = 60  # 비교과 일정이 느림 (수십 초)
FIXTURE_DIRECTORY = ROOT / "spec" / "fixtures" / "public_data"
SAMPLE_PLACE_ID = "1"  # 학식 식당 번호 (비어 있으면 빈 결과도 정상)
# 학과 교육과정 점검용 학과 코드·과목 번호 (교육과정에서 빠지면 빈 결과, 형태 점검은 건너뜀)
SAMPLE_CURRICULUM_DEPARTMENT_CODE = "3513"
SAMPLE_CURRICULUM_COURSE_NUMBER = "009912"

# 소스별 (컨테이너 키 경로, 행 필수 필드, 빈 결과 허용 여부) - spec/PUBLIC_DATA.md 표와 동일하게 유지
DORMITORY_DAY_FIELDS = tuple(f"{prefix}{day}" for day in range(1, 8)
                             for prefix in ("fo_date", "fo_menu_mor", "fo_menu_lun", "fo_menu_eve"))  # fo_sub_menu 는 일부 요일에 없을 수 있어 제외
SOURCES: Dict[str, Tuple[str, Tuple[str, ...], bool]] = {
    "lectures": ("dl_main", ("CURI_NO", "CLASS", "CURI_NM", "LESN_TIME", "LESN_EMP", "DEPT_CD"), True),
    "curriculum_courses": ("items", ("deptCd", "curiNo", "curiNm", "cdt", "stuYearCd", "smtDanCd", "remark"), True),
    "calendar": ("data", ("collDiv", "scmSubject", "frDt", "toDt"), True),
    "graduate_calendar": ("items", ("collDiv", "scmSubject", "frDt", "toDt"), True),
    "dormitory_menu": ("root", DORMITORY_DAY_FIELDS, False),
    "dormitory_notices": ("root", ("topList", "list", "totalCount"), False),
    # buildingName, location: cafeteria_places 가 식당 정보로 읽는 필드
    "cafeteria": ("items", ("placeId", "placeName", "buildingName", "location", "menuName", "price"), True),
    "campus_places": ("items", ("id", "placeName", "latitude", "longitude", "placeType"), False),
    "extracurricular_events": ("events", ("id", "title", "start", "end", "category_title", "data"), True),
}
NOTICE_ROW_FIELDS = ("seq", "subject", "regdate")


def find_source_problems(source_name: str, body: object) -> List[str]:
    """응답 1건 점검: 오류 객체 > 컨테이너 키 > 행 존재 > 필수 필드 (+ 기숙사 공지는 목록 행과 총 건수)"""
    container_key, required_fields, is_empty_allowed = SOURCES[source_name]
    if not isinstance(body, dict):
        return [f"{source_name}: body is not an object"]
    if "_SUBMIT_ERROR_" in body:
        # STACK_TRACE 는 내부 정보라 출력하지 않음
        return [f"{source_name}: _SUBMIT_ERROR_"]
    rows = body.get(container_key)
    if source_name == "graduate_calendar":
        # success 플래그 확인, items 는 JSON 문자열이라 한 번 더 해석
        if body.get("success") is not True:
            return [f"{source_name}: success is not true"]
        try:
            rows = json.loads(rows) if isinstance(rows, str) else rows
        except ValueError:
            return [f"{source_name}: items is not JSON"]
    if not isinstance(rows, list):
        return [f"{source_name}: container {container_key} missing (keys={sorted(body)[:6]})"]
    if not rows:
        return [] if is_empty_allowed else [f"{source_name}: no rows"]
    first_row = rows[0]
    if not isinstance(first_row, dict):
        return [f"{source_name}: first row is not an object"]
    missing_fields = [field for field in required_fields if field not in first_row]
    problems = [f"{source_name}: first row lacks {missing_fields}"] if missing_fields else []
    if source_name == "dormitory_notices" and not missing_fields:
        problems += find_notice_problems(first_row)
    return problems


def find_notice_problems(block: dict) -> List[str]:
    """기숙사 공지 추가 점검: 목록 행 필수 필드, 총 건수 cnt"""
    problems = []
    for list_key in ("topList", "list"):
        rows = block[list_key]
        if isinstance(rows, list) and rows and isinstance(rows[0], dict):
            missing_fields = [field for field in NOTICE_ROW_FIELDS if field not in rows[0]]
            if missing_fields:
                problems.append(f"dormitory_notices: {list_key} row lacks {missing_fields}")
    total_rows = block["totalCount"]
    if not (isinstance(total_rows, list) and total_rows and isinstance(total_rows[0], dict) and "cnt" in total_rows[0]):
        problems.append("dormitory_notices: totalCount.cnt missing")
    return problems


def request_json(session: UrllibSession, method: str, url: str, form: Optional[dict] = None) -> object:
    """요청 1건 > JSON (본문은 항상 JSON 으로 파싱: 기숙사는 text/html 로 응답)"""
    headers = {"Accept": "application/json"}
    data = None
    if form is not None:
        data = urllib.parse.urlencode(form).encode("utf-8")
        headers["Content-Type"] = public_data_module.FORM_CONTENT_TYPE
        headers["X-Requested-With"] = "XMLHttpRequest"
    response = session.request(method, url, data=data, headers=headers)
    if not response.is_successful:
        raise RuntimeError(f"HTTP {response.status}")
    time.sleep(REQUEST_INTERVAL_SECONDS)
    return json.loads(response.body)


def lectures_request(session: UrllibSession) -> object:
    """강의 검색: 필터 키 전체를 빈 값으로 포함 (하나라도 빠지면 _SUBMIT_ERROR_)"""
    search = {**public_data_module.LECTURE_EMPTY_FILTERS, "ORGN_CLSF_CD": "20", "YEAR": "2026", "SMT_CD": "20",
              "YEAR_SMT": "2026/20", "DEPT_CD": "", "CURI_NM": "영어", "EMP_NM": "", "LANG_CHK": "KOR"}
    response = session.request(
        "POST", public_data_module.SIS_OPEN_TIME_URL + "doList.do",
        data=json.dumps({"dm_search": search}, ensure_ascii=False).encode("utf-8"),
        headers={"Content-Type": public_data_module.SIS_CONTENT_TYPE, "Accept": "application/json"})
    if not response.is_successful:
        raise RuntimeError(f"HTTP {response.status}")
    time.sleep(REQUEST_INTERVAL_SECONDS)
    return json.loads(response.body)


def build_live_requests(session: UrllibSession) -> Dict[str, Callable[[], object]]:
    module = public_data_module
    month_query = {"start": "2026-10-01", "end": "2026-10-31"}
    calendar_query = urllib.parse.urlencode({"mode": "getCalendarData", **month_query, "collDiv": "0001"})
    cafeteria_query = urllib.parse.urlencode({"mode": "getMenuList", "placeId": SAMPLE_PLACE_ID})
    campus_query = urllib.parse.urlencode({"mode": "getPlaceUserList", "placeType": ""})
    curriculum_query = urllib.parse.urlencode({"curiNo": SAMPLE_CURRICULUM_COURSE_NUMBER, "deptNo": SAMPLE_CURRICULUM_DEPARTMENT_CODE})
    graduate_query = urllib.parse.urlencode({"year": "2026", "month": "10", "siteId": "graduate"})
    return {
        "lectures": lambda: lectures_request(session),
        "curriculum_courses": lambda: request_json(session, "GET", f"{module.CURRICULUM_COURSE_URL}?{curriculum_query}"),
        "calendar": lambda: request_json(session, "GET", f"{module.ACADEMIC_CALENDAR_URL}?{calendar_query}"),
        "graduate_calendar": lambda: request_json(
            session, "GET", f"{module.GRADUATE_CALENDAR_URL.format(site_id='graduate')}?{graduate_query}"),
        "dormitory_menu": lambda: request_json(session, "POST", module.DORMITORY_MENU_URL, {"locgbn": "SJ"}),
        "dormitory_notices": lambda: request_json(session, "POST", module.DORMITORY_NOTICE_URL, {
            "cPage": 1, "rows": 3, "bbs_locgbn": "SJ", "bbs_id": "notice", "sType": "subject", "sWord": ""}),
        "cafeteria": lambda: request_json(session, "GET", f"{module.CAFETERIA_URL}?{cafeteria_query}"),
        "campus_places": lambda: request_json(session, "GET", f"{module.CAMPUS_MAP_URL}?{campus_query}"),
        "extracurricular_events": lambda: request_json(session, "POST", module.EXTRACURRICULAR_EVENTS_URL, {
            "cid": "eco", "category": "", **month_query, "start_year": "2026", "end_year": "2026"}),
    }


def _count_graduate_calendar_items(body: object) -> int:
    """대학원 학사일정 items(JSON 문자열) 건수, 깨져 있으면 0 (형태 문제는 find_source_problems 가 보고)"""
    items = body.get("items") if isinstance(body, dict) else None
    if not isinstance(items, str):
        return 0
    try:
        return len(json.loads(items))
    except ValueError:
        return 0


def load_fixture_bodies() -> Dict[str, object]:
    file_by_source = {"lectures": "lectures", "curriculum_courses": "curriculum_course", "calendar": "calendar", "graduate_calendar": "graduate_calendar", "dormitory_menu": "dormitory_menu",
                      "dormitory_notices": "dormitory_notices", "cafeteria": "cafeteria",
                      "campus_places": "campus_places", "extracurricular_events": "extracurricular_events"}
    return {name: json.loads((FIXTURE_DIRECTORY / f"{file}.json").read_text(encoding="utf-8"))
            for name, file in file_by_source.items()}


def main(argv: Optional[List[str]] = None) -> int:
    is_offline = "--offline" in (argv if argv is not None else sys.argv[1:])
    problems: List[str] = []
    if is_offline:
        bodies = load_fixture_bodies()
        for source_name, body in bodies.items():
            problems += find_source_problems(source_name, body)
        print(f"[probe-public] mode=offline sources={len(bodies)}")
    else:
        session = UrllibSession(create_ssl_context(True), TIMEOUT_SECONDS, USER_AGENT)
        for source_name, send_request in build_live_requests(session).items():
            try:
                body = send_request()
            except (OSError, RuntimeError, ValueError, TimeoutError) as error:
                print(f"[probe-public] SKIP {source_name}: {type(error).__name__}")
                continue
            source_problems = find_source_problems(source_name, body)
            row_count = len(body.get(SOURCES[source_name][0]) or []) if isinstance(body, dict) else 0
            if source_name == "graduate_calendar":
                row_count = _count_graduate_calendar_items(body)
            print(f"[probe-public] {source_name}: rows={row_count} {'DRIFT' if source_problems else 'ok'}")
            problems += source_problems
    for problem in problems:
        print(f"[probe-public] DRIFT {problem}")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
