"""공개 데이터 조회(로그인 없음): 학사 기준 정보, 강의, 학과 교육과정, 학사일정, 기숙사, 식당, 캠퍼스, 비교과
요청 > JSON 검증 > 모델 변환 > 캐시, 상류 실패는 마지막 캐시 또는 빈 값 (spec/PUBLIC_DATA.md)
"""

import json
import logging
import re
import threading
import time
import urllib.parse
from decimal import Decimal
from typing import Any, Callable, Dict, List, Optional, Tuple

from ..common.errors import ErrorCode, SejongAuthError
from .models import (
    CafeteriaMenuItem, CafeteriaPlace, CalendarEvent, CampusPlace, CourseType, CurriculumCourse, Department, DormitoryMenuDay,
    DormitoryNotice, DormitoryNotices, ExtracurricularEvent, Lecture, OrganizationCategory,
    SelectiveDomain, Term,
)
from ..common.transport import USER_AGENT, UrllibSession, create_ssl_context

LOGGER = logging.getLogger(__name__)

SIS_OPEN_TIME_URL = "https://sjpt.sejong.ac.kr/sch/sch/ext/SueOpenTimeQ/"
# 학과 교육과정 과목 1건: deptNo 는 SIS 학과 코드(DEPT_CD)와 같은 값, Accept: application/json 이 없으면 404
CURRICULUM_COURSE_URL = "https://www.sejong.ac.kr/curriculum/getCurriculumOneKor.do"
ACADEMIC_CALENDAR_URL = "https://www.sejong.ac.kr/kor/academics/academic-calendar.do"
# 대학원 사이트별 학사일정: {site_id} 는 graduate, gsi, mba 같은 사이트 식별자 (코드표 없이 호출자가 전달)
GRADUATE_CALENDAR_URL = "https://{site_id}.sejong.ac.kr/_custom/sejong/_app/calendar-graduate/proc/getCalendarData.jsp"
# 사이트 식별자 허용 형태 (호스트 이름에 들어가므로 소문자·숫자·하이픈만)
GRADUATE_SITE_ID_PATTERN = re.compile(r"[a-z0-9][a-z0-9-]{0,30}")
CAFETERIA_URL = "https://www.sejong.ac.kr/kor/unilife/cafeteria-info.do"
# 학교에 식당 목록 API가 없어 번호 1~10을 차례로 조회해 식당을 모음 (학교 화면도 번호를 스크립트에 고정해 씀, spec/PUBLIC_DATA.md)
CAFETERIA_PROBE_PLACE_IDS = tuple(str(place_id) for place_id in range(1, 11))
CAMPUS_MAP_URL = "https://www.sejong.ac.kr/kor/intro/campus-map.do"
DORMITORY_MENU_URL = "https://happydorm.sejong.ac.kr/food/indexFoodList.do"
DORMITORY_NOTICE_URL = "https://happydorm.sejong.ac.kr/bbs/getBbsList.do"
EXTRACURRICULAR_EVENTS_URL = "https://do.sejong.ac.kr/ko/process/schedule/getEvents"

SIS_CONTENT_TYPE = 'application/json; charset="UTF-8"'
# 강좌 조회는 나머지 필터 키가 모두 빈 값으로 있어야 학교가 받아들임 (하나라도 빠지면 _SUBMIT_ERROR_)
LECTURE_EMPTY_FILTERS = {key: "" for key in (
    "SMT_NM", "CURI_TYPE_CD", "SLT_DOMAIN_CD", "CURI_NO", "CLASS", "SLT_DEPT_CD", "PLAN_REG_YN", "CORS_SCHE_GRP_CD")}
FORM_CONTENT_TYPE = "application/x-www-form-urlencoded; charset=UTF-8"
SUBMIT_ERROR_MARKER = "_SUBMIT_ERROR_"

# extra 제외 필드: 교직원 이름 등 개인 식별, 서버 내부 정보
EXTRA_DENYLIST = frozenset({
    "CD_ALIAS1", "CD_ALIAS2", "CD_ALIAS3", "CD_ALIAS1_ENG", "CD_ALIAS2_ENG", "CD_ALIAS3_ENG",
    "regname", "regid", "STACK_TRACE",
})


def _text(value: Any) -> str:
    """스칼라 > 문자열 (null > 빈 문자열, bool > true/false, 숫자 > 평범한 십진 표기, dict·list > 압축 JSON)"""
    if value is None:
        return ""
    if isinstance(value, bool):
        return "true" if value else "false"
    if isinstance(value, float):
        return str(int(value)) if value.is_integer() else format(Decimal(repr(value)), "f")
    if isinstance(value, (dict, list)):
        return json.dumps(value, ensure_ascii=False, separators=(",", ":"))
    return str(value)


def _int_of(value: Any) -> int:
    """숫자로 읽을 수 없으면 0"""
    try:
        return int(float(value))
    except (TypeError, ValueError, OverflowError):
        return 0


def _float_of(value: Any) -> float:
    """숫자로 읽을 수 없으면 0.0"""
    try:
        return float(value)
    except (TypeError, ValueError):
        return 0.0


def _object_of(value: Any) -> Dict[str, Any]:
    """객체가 아니면 빈 객체 (이상한 행도 버리지 않음)"""
    return value if isinstance(value, dict) else {}


def _fields(row: Any, *names: str) -> List[str]:
    """행에서 필드 여러 개를 문자열로 (없거나 null 이면 빈 문자열)"""
    row = _object_of(row)
    return [_text(row.get(name)) for name in names]


def _extra_of(row: Dict[str, Any], used_fields: set) -> Dict[str, str]:
    """매핑에 쓰지 않은 필드 (null·빈 값·제외 목록 필드 뺌, 비스칼라는 압축 JSON)"""
    return {key: _text(value) for key, value in sorted(row.items())
            if key not in used_fields and key not in EXTRA_DENYLIST and value not in (None, "")}


def _dataset(body: Any, dataset_name: str) -> List[Any]:
    """응답에서 기대 배열 추출 (없거나 배열이 아니면 KeyError·TypeError > 실패 격리)"""
    rows = body[dataset_name]
    if not isinstance(rows, list):
        raise TypeError("dataset is not a list")
    return rows


def _first_root_row(body: Any) -> Any:
    """기숙사 응답은 항상 root 한 행 (빈 root 는 비정상 응답이라 실패)"""
    rows = _dataset(body, "root")
    if not rows:
        raise ValueError("empty root")
    return rows[0]


# ---- 행 > 모델 변환 (필드 대응은 spec/PUBLIC_DATA.md, 모든 필드는 느슨하게 읽음)
def _to_organization_category(row):
    row = _object_of(row)
    return OrganizationCategory(*_fields(row, "COM_CD", "CD_NM", "CD_NM_ENG"),
                                _extra_of(row, {"COM_CD", "CD_NM", "CD_NM_ENG"}))


def _to_term(row):
    row = _object_of(row)
    return Term(*_fields(row, "YEAR", "SMT_CD", "YEAR_SMT_CD", "YEAR_SMT_NM"),
                _extra_of(row, {"YEAR", "SMT_CD", "YEAR_SMT_CD", "YEAR_SMT_NM"}))


def _to_department(row):
    row = _object_of(row)
    names = ("DEPT_CD", "DEPT_LEVEL_CD", "DEPT_L_CD", "DEPT_L_PRINT", "DEPT_M_CD", "DEPT_M_PRINT",
             "DEPT_S_CD", "DEPT_S_PRINT", "DEPT_NM_ENG")
    return Department(*_fields(row, *names), _extra_of(row, set(names)))


def _to_course_type(row):
    row = _object_of(row)
    names = ("CURI_TYPE_CD", "CURI_TYPE_CD_NM", "ORGN_CLSF_CD")
    return CourseType(*_fields(row, *names), _extra_of(row, set(names)))


def _to_selective_domain(row):
    row = _object_of(row)
    return SelectiveDomain(*_fields(row, "COM_CD", "CD_NM", "CD_NM_ENG"),
                           _extra_of(row, {"COM_CD", "CD_NM", "CD_NM_ENG"}))


# Lecture 생성자 인자 순서와 같음
LECTURE_FIELD_NAMES = (
    "CURI_NO", "CLASS", "CURI_NM", "CURI_TYPE_CD", "CURI_TYPE_CD_NM", "SLT_DOMAIN_CD", "SLT_DOMAIN_CD_NM", "CDT",
    "TM_NUM", "TOT_TM_NUM", "LESN_TIME", "LESN_ROOM", "LESN_EMP", "STUDENT_YEAR", "CURI_LANG_CD", "CURI_LANG_NM",
    "DEPT_CD", "SCH_DEPT_ALIAS", "MANAGE_DEPT_NM", "REMARK", "YEAR", "SMT_CD",
)


def _to_lecture(row):
    row = _object_of(row)
    return Lecture(*_fields(row, *LECTURE_FIELD_NAMES), _extra_of(row, set(LECTURE_FIELD_NAMES)))


# CurriculumCourse 생성자 인자 순서와 같음 (remark 는 교과목 개요 본문이라 overview 로 받음)
CURRICULUM_COURSE_FIELD_NAMES = (
    "deptCd", "deptNm", "orgnClsfCd", "year", "curiNo", "curiNm", "curiTypeCd", "curiTypeNm", "cdt",
    "lecTmNum", "exeTmNum", "curiTm", "stuYearCd", "smtDanCd", "smtDanNm", "remark",
)


def _to_curriculum_course(row):
    row = _object_of(row)
    return CurriculumCourse(*_fields(row, *CURRICULUM_COURSE_FIELD_NAMES),
                            _extra_of(row, set(CURRICULUM_COURSE_FIELD_NAMES)))


def _to_calendar_event(row):
    row = _object_of(row)
    names = ("scmYear", "scmMonth", "collDiv", "collDivNm", "scmSubject", "scmSubjectEng", "frDt", "toDt")
    return CalendarEvent(*_fields(row, *names), _extra_of(row, set(names)))


def _to_graduate_calendar_events(body) -> List[CalendarEvent]:
    """대학원 달력 응답 > 일정 목록 (success 거짓·없음, items 가 문자열 JSON 도 배열도 아니면 실패)"""
    if not isinstance(body, dict) or _text(body.get("success")).lower() != "true":
        raise ValueError("graduate calendar did not report success")
    items = body["items"]
    if isinstance(items, str):
        items = json.loads(items)  # items 는 JSON 문자열이라 한 번 더 해석
    if not isinstance(items, list):
        raise TypeError("items is not a list")
    return [_to_calendar_event(row) for row in items]


def _to_dormitory_menu_days(body) -> List[DormitoryMenuDay]:
    """root[0] 의 fo_*1..7 > 7일치 (root 없음·배열 아님·빈 root 는 실패)"""
    row = _object_of(_first_root_row(body))
    return [DormitoryMenuDay(*_fields(row, f"fo_date{day}", f"fo_menu_mor{day}", f"fo_menu_lun{day}",
                                      f"fo_menu_eve{day}", f"fo_sub_menu{day}")) for day in range(1, 8)]


def _to_dormitory_notice(row):
    # 작성자 필드(regname, regid)는 읽지 않음
    row = _object_of(row)
    seq, subject, regdate, file_name = _fields(row, "seq", "subject", "regdate", "file_name1")
    return DormitoryNotice(seq, subject, regdate, _int_of(row.get("visitcnt")), file_name)


def _notice_rows(block: Dict[str, Any], key: str) -> List[DormitoryNotice]:
    """목록 필드가 없거나 배열이 아니면 빈 목록 (결과 없음은 장애가 아님)"""
    rows = block.get(key)
    return [_to_dormitory_notice(row) for row in rows] if isinstance(rows, list) else []


def _to_dormitory_notices(body) -> DormitoryNotices:
    block = _object_of(_first_root_row(body))
    counts = block.get("totalCount")
    total = _int_of(_object_of(counts[0]).get("cnt")) if isinstance(counts, list) and counts else 0
    return DormitoryNotices(total, _notice_rows(block, "topList"), _notice_rows(block, "list"))


def _to_cafeteria_menu_item(row):
    return CafeteriaMenuItem(*_fields(row, "buildingName", "location", "placeId", "placeName", "menuDate", "day",
                                      "mealTypeId", "mealName", "menuName", "price"))


# 메뉴 응답 행의 알려진 필드 (extra 에는 이 밖의 필드만)
CAFETERIA_MENU_FIELD_NAMES = {"placeId", "placeName", "buildingName", "location", "menuDate", "day", "mealTypeId",
                              "mealName", "menuName", "price"}


def _to_cafeteria_places(bodies_by_place_id: Dict[str, Any]) -> List[CafeteriaPlace]:
    """번호별 메뉴 응답 > 식당 목록 (메뉴가 있는 번호만, 첫 행의 식당 정보, 같은 식당 번호는 한 번만)"""
    places: Dict[str, CafeteriaPlace] = {}
    for requested_place_id, body in bodies_by_place_id.items():
        first_row = next((row for row in _dataset(body, "items") if isinstance(row, dict)), None)
        if first_row is None:
            continue
        place_id, name, building_name, location = _fields(first_row, "placeId", "placeName", "buildingName", "location")
        place_id = place_id or requested_place_id
        places.setdefault(place_id, CafeteriaPlace(place_id, name, building_name, location,
                                                   _extra_of(first_row, CAFETERIA_MENU_FIELD_NAMES)))
    return list(places.values())


def _to_campus_place(row):
    row = _object_of(row)
    used_fields = {"id", "placeName", "placeNameEn", "placeType", "welfareCategory", "latitude", "longitude",
                   "placeInfo", "telNum", "description"}
    identifier, name, name_en, place_type, category = _fields(row, "id", "placeName", "placeNameEn", "placeType",
                                                              "welfareCategory")
    info, phone, description = _fields(row, "placeInfo", "telNum", "description")
    return CampusPlace(identifier, name, name_en, place_type, category, _float_of(row.get("latitude")),
                       _float_of(row.get("longitude")), info, phone, description, _extra_of(row, used_fields))


def _to_extracurricular_event(row):
    row = _object_of(row)
    detail = _object_of(row.get("data"))
    return ExtracurricularEvent(*_fields(row, "id", "title", "start", "end", "category_title"),
                                *_fields(detail, "url", "description", "location"))


def _year_of(date_text: str) -> str:
    """yyyy-mm-dd > yyyy"""
    return str(date_text)[:4]


class SejongPublicData:
    """공개 데이터 클라이언트: 메서드 15개, 메서드·인자별 메모리 캐시, 동시 호출 합치기(같은 키 1회 요청)"""

    def __init__(self, timeout_seconds: float = 10, cache_ttl_seconds: float = 86400, ssl_verification: bool = True,
                 failure_cache_seconds: float = 60, cache_max_entries: int = 500,
                 _session_factory: Optional[Callable[[], Any]] = None, _clock: Optional[Callable[[], float]] = None):
        self.timeout_seconds = timeout_seconds
        self.cache_ttl_seconds = cache_ttl_seconds
        self.ssl_verification = ssl_verification
        self.failure_cache_seconds = failure_cache_seconds
        self.cache_max_entries = cache_max_entries
        self._session_factory = _session_factory or self._create_default_session
        self._clock = _clock or time.time
        self._session: Any = None
        self._cache: Dict[Tuple, Tuple[float, Any, bool]] = {}  # 키 > (저장 시각, 값, 실패 기억 여부), 오래된 순
        self._key_locks: Dict[Tuple, threading.Lock] = {}
        self._guard_lock = threading.Lock()

    # ---- 공개 메서드
    def organization_categories(self) -> List[OrganizationCategory]:
        return self._sis_dataset("doOnload.do", {}, "dl_code_orgn", _to_organization_category)

    def terms(self, category_code: str) -> List[Term]:
        return self._sis_dataset("doYearsmtlist.do", {"ORGN_CLSF_CD": category_code}, "dl_code_yearSmt", _to_term)

    def departments(self, category_code: str, year: str, semester_code: str) -> List[Department]:
        search = {"ORGN_CLSF_CD": category_code, "YEAR": year, "SMT_CD": semester_code,
                  "YEAR_SMT": f"{year}/{semester_code}"}
        return self._sis_dataset("doLesndeptgroup.do", search, "dl_code_dept", _to_department)

    def course_types(self) -> List[CourseType]:
        return self._sis_dataset("doOnload.do", {}, "dl_code_curiType", _to_course_type)

    def selective_domains(self) -> List[SelectiveDomain]:
        return self._sis_dataset("doOnload.do", {}, "dl_code_sltDomain", _to_selective_domain)

    def lectures(self, category_code: str, year: str, semester_code: str, department_code: str = "",
                 course_name: str = "", instructor_name: str = "") -> List[Lecture]:
        """강의 검색: 학과 코드·과목명·교수명은 선택, 모두 비우면 그 학기 전체 (학부 약 2,800행·2.4MB, spec/PUBLIC_DATA.md)"""
        search = {**LECTURE_EMPTY_FILTERS, "ORGN_CLSF_CD": category_code, "YEAR": year, "SMT_CD": semester_code,
                  "YEAR_SMT": f"{year}/{semester_code}", "DEPT_CD": str(department_code or "").strip(),
                  "CURI_NM": str(course_name or "").strip(), "EMP_NM": str(instructor_name or "").strip(), "LANG_CHK": "KOR"}
        return self._sis_dataset("doList.do", search, "dl_main", _to_lecture)

    def curriculum_courses(self, department_code: str, course_number: str) -> List[CurriculumCourse]:
        """학과 교육과정의 과목 1건 (학년·학기·이수구분·학점·교과목 개요), 그 학과 교육과정에 없으면 빈 목록
        학과 코드·과목 번호 필수 (없으면 INVALID_INPUT)"""
        if not str(department_code or "").strip() or not str(course_number or "").strip():
            raise SejongAuthError(ErrorCode.INVALID_INPUT, "Department code and course number are required")
        query = urllib.parse.urlencode({"curiNo": course_number, "deptNo": department_code})
        return self._fetch(("curriculum_courses", department_code, course_number), "GET",
                           f"{CURRICULUM_COURSE_URL}?{query}", None, None,
                           lambda body: [_to_curriculum_course(row) for row in _dataset(body, "items")], [])

    def calendar(self, start: str, end: str, category_code: str = "0001") -> List[CalendarEvent]:
        query = urllib.parse.urlencode({"mode": "getCalendarData", "start": start, "end": end, "collDiv": category_code})
        return self._fetch(("calendar", category_code, start, end), "GET", f"{ACADEMIC_CALENDAR_URL}?{query}", None, None,
                           lambda body: [_to_calendar_event(row) for row in _dataset(body, "data")], [])

    def graduate_calendar(self, site_id: str, year: int, month: int) -> List[CalendarEvent]:
        """대학원 사이트 학사일정 (site_id: graduate, gsi, mba, pub, tourgrad, edu, arts 등, 형태가 틀리면 INVALID_INPUT)"""
        if not GRADUATE_SITE_ID_PATTERN.fullmatch(str(site_id or "")):
            raise SejongAuthError(ErrorCode.INVALID_INPUT, "Graduate site id must be lowercase letters, digits and hyphens")
        query = urllib.parse.urlencode({"year": year, "month": month, "siteId": site_id})
        return self._fetch(("graduate_calendar", site_id, year, month), "GET",
                           f"{GRADUATE_CALENDAR_URL.format(site_id=site_id)}?{query}", None, None,
                           _to_graduate_calendar_events, [])

    def dormitory_menu(self, location: str = "SJ") -> List[DormitoryMenuDay]:
        return self._fetch(("dormitory_menu", location), "POST", DORMITORY_MENU_URL, {"locgbn": location}, FORM_CONTENT_TYPE,
                           _to_dormitory_menu_days, [])

    def dormitory_notices(self, page: int = 1, rows: int = 10, keyword: str = "", location: str = "SJ") -> DormitoryNotices:
        form = {"cPage": page, "rows": rows, "bbs_locgbn": location, "bbs_id": "notice", "sType": "subject", "sWord": keyword}
        return self._fetch(("dormitory_notices", page, rows, keyword, location), "POST", DORMITORY_NOTICE_URL, form,
                           FORM_CONTENT_TYPE, _to_dormitory_notices, DormitoryNotices(total=0, pinned=[], items=[]))

    def cafeteria_menu(self, place_id: str) -> List[CafeteriaMenuItem]:
        query = urllib.parse.urlencode({"mode": "getMenuList", "placeId": place_id})
        return self._fetch(("cafeteria_menu", place_id), "GET", f"{CAFETERIA_URL}?{query}", None, None,
                           lambda body: [_to_cafeteria_menu_item(row) for row in _dataset(body, "items")], [])

    def cafeteria_places(self) -> List[CafeteriaPlace]:
        """학식 식당 목록 (번호, 이름, 건물, 위치): 번호 1~10의 메뉴 응답에서 모음, 메뉴가 없는 식당은 빠짐
        하나라도 실패하면 목록 전체를 실패로 보고 마지막 캐시 또는 빈 목록 (일부만 하루 캐시하지 않음)"""
        def load_places() -> List[CafeteriaPlace]:
            bodies_by_place_id = {}
            for place_id in CAFETERIA_PROBE_PLACE_IDS:  # 순서대로 한 건씩, 병렬 요청 없음
                query = urllib.parse.urlencode({"mode": "getMenuList", "placeId": place_id})
                bodies_by_place_id[place_id] = self._request_json("GET", f"{CAFETERIA_URL}?{query}", None, None)
            return _to_cafeteria_places(bodies_by_place_id)
        return self._cached_call(("cafeteria_places",), load_places, [])

    def campus_places(self, place_type: str = "") -> List[CampusPlace]:
        query = urllib.parse.urlencode({"mode": "getPlaceUserList", "placeType": place_type})
        return self._fetch(("campus_places", place_type), "GET", f"{CAMPUS_MAP_URL}?{query}", None, None,
                           lambda body: [_to_campus_place(row) for row in _dataset(body, "items")], [])

    def extracurricular_events(self, start: str, end: str, category: str = "") -> List[ExtracurricularEvent]:
        form = {"cid": "eco", "category": category, "start": start, "end": end,
                "start_year": _year_of(start), "end_year": _year_of(end)}
        return self._fetch(("extracurricular_events", start, end, category), "POST", EXTRACURRICULAR_EVENTS_URL, form,
                           FORM_CONTENT_TYPE,
                           lambda body: [_to_extracurricular_event(row) for row in _dataset(body, "events")], [])

    # ---- 공통 헬퍼
    def _sis_dataset(self, endpoint: str, search: Dict[str, str], dataset_name: str, to_model: Callable) -> List:
        """SIS 조회 한 건: {"dm_search": ...} JSON 본문 > 데이터셋 배열 > 모델 목록"""
        cache_key = ("sis", endpoint, dataset_name, tuple(sorted(search.items())))
        return self._fetch(cache_key, "POST", SIS_OPEN_TIME_URL + endpoint, {"dm_search": search}, SIS_CONTENT_TYPE,
                           lambda body: [to_model(row) for row in _dataset(body, dataset_name)], [])

    def _fetch(self, cache_key: Tuple, method: str, url: str, payload: Optional[Dict], content_type: Optional[str],
               convert: Callable[[Any], Any], empty_value: Any) -> Any:
        """요청 1건 > 변환 (캐시와 실패 격리는 _cached_call)"""
        return self._cached_call(cache_key, lambda: convert(self._request_json(method, url, payload, content_type)),
                                 empty_value)

    def _cached_call(self, cache_key: Tuple, load_value: Callable[[], Any], empty_value: Any) -> Any:
        """캐시 확인 > 조회 > 캐시 저장, 상류 실패는 마지막 캐시(만료 포함) 또는 빈 값 (같은 키는 한 번에 한 조회)"""
        with self._lock_for(cache_key):
            cached = self._cache.get(cache_key)
            if cached is not None:
                stored_at, cached_value, is_failure = cached
                valid_seconds = self.failure_cache_seconds if is_failure else self.cache_ttl_seconds
                if self._clock() - stored_at < valid_seconds:  # 성공 캐시 또는 실패 기억이 유효하면 요청 없음
                    return self._copy_of(cached_value)
            try:
                value = load_value()
            except Exception as error:  # 상류 실패 전부 격리 (서버 내부 정보 로그 금지, 예외 종류만)
                LOGGER.warning("[sejong public data] upstream failed (%s) : %s", cache_key[0], type(error).__name__)
                if cached is not None and not cached[2]:
                    return self._copy_of(cached[1])  # 만료된 캐시 값
                self._store(cache_key, empty_value, True)  # 실패 기억 (캐시 값 없을 때만)
                return self._copy_of(empty_value)
            self._store(cache_key, value, False)
            return self._copy_of(value)

    def _store(self, cache_key: Tuple, value: Any, is_failure: bool) -> None:
        """캐시 저장 (상한 초과 시 가장 오래된 항목과 그 Lock 제거)"""
        with self._guard_lock:
            self._cache.pop(cache_key, None)  # 갱신 항목을 가장 최신 위치로
            while len(self._cache) >= max(self.cache_max_entries, 1):
                oldest_key = next(iter(self._cache))
                del self._cache[oldest_key]
                self._key_locks.pop(oldest_key, None)
            self._cache[cache_key] = (self._clock(), value, is_failure)

    def _request_json(self, method: str, url: str, payload: Optional[Dict], content_type: Optional[str]) -> Any:
        """요청 1건 > JSON (2xx 아님·JSON 아님·_SUBMIT_ERROR_ 는 예외)"""
        headers = {"Accept": "application/json"}
        data = None
        if content_type == SIS_CONTENT_TYPE:
            data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        elif content_type == FORM_CONTENT_TYPE:
            data = urllib.parse.urlencode(payload).encode("utf-8")
            headers["X-Requested-With"] = "XMLHttpRequest"
        if content_type:
            headers["Content-Type"] = content_type
        response = self._get_session().request(method, url, data=data, headers=headers)
        if not response.is_successful:
            raise ValueError(f"http status {response.status}")
        # happydorm 은 text/html 로 JSON 응답 > 항상 본문을 JSON 으로 파싱
        body = json.loads(response.text())
        if isinstance(body, dict) and SUBMIT_ERROR_MARKER in body:
            raise ValueError("school reported a submit error")
        return body

    def _get_session(self) -> Any:
        if self._session is None:
            self._session = self._session_factory()
        return self._session

    def _create_default_session(self) -> Any:
        return UrllibSession(create_ssl_context(self.ssl_verification), self.timeout_seconds, USER_AGENT)

    def _lock_for(self, cache_key: Tuple) -> threading.Lock:
        with self._guard_lock:
            return self._key_locks.setdefault(cache_key, threading.Lock())

    @staticmethod
    def _copy_of(value: Any) -> Any:
        """호출자가 캐시를 바꾸지 못하게 목록 복사 (모델은 불변)"""
        if isinstance(value, list):
            return list(value)
        if isinstance(value, DormitoryNotices):
            return DormitoryNotices(value.total, list(value.pinned), list(value.items))
        return value
