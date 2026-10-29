"""공개 데이터 조회(로그인 없음): 학사 기준 정보, 강의, 학사일정, 기숙사, 식당, 캠퍼스, 비교과
요청 > JSON 검증 > 모델 변환 > 캐시, 상류 실패는 마지막 캐시 또는 빈 값 (spec/PUBLIC_DATA.md)
"""

import json
import logging
import threading
import time
import urllib.parse
from typing import Any, Callable, Dict, List, Optional, Tuple

from .errors import ErrorCode, SejongAuthError
from .public_data_models import (
    CafeteriaMenuItem, CalendarEvent, CampusPlace, CourseType, Department, DormitoryMenuDay,
    DormitoryNotice, DormitoryNotices, ExtracurricularEvent, Lecture, OrganizationCategory,
    SelectiveDomain, Term,
)
from .transport import USER_AGENT, create_ssl_context

LOGGER = logging.getLogger(__name__)

SIS_OPEN_TIME_URL = "https://sjpt.sejong.ac.kr/sch/sch/ext/SueOpenTimeQ/"
ACADEMIC_CALENDAR_URL = "https://www.sejong.ac.kr/kor/academics/academic-calendar.do"
CAFETERIA_URL = "https://www.sejong.ac.kr/kor/unilife/cafeteria-info.do"
CAMPUS_MAP_URL = "https://www.sejong.ac.kr/kor/intro/campus-map.do"
DORMITORY_MENU_URL = "https://happydorm.sejong.ac.kr/food/indexFoodList.do"
DORMITORY_NOTICE_URL = "https://happydorm.sejong.ac.kr/bbs/getBbsList.do"
EXTRACURRICULAR_EVENTS_URL = "https://do.sejong.ac.kr/ko/process/schedule/getEvents"

SIS_CONTENT_TYPE = 'application/json; charset="UTF-8"'
FORM_CONTENT_TYPE = "application/x-www-form-urlencoded; charset=UTF-8"
SUBMIT_ERROR_MARKER = "_SUBMIT_ERROR_"

# extra 제외 필드: 교직원 이름 등 개인 식별, 서버 내부 정보
EXTRA_DENYLIST = frozenset({
    "CD_ALIAS1", "CD_ALIAS2", "CD_ALIAS3", "CD_ALIAS1_ENG", "CD_ALIAS2_ENG", "CD_ALIAS3_ENG",
    "regname", "regid", "STACK_TRACE",
})


def _text(value: Any) -> str:
    """null > 빈 문자열, 그 외 문자열"""
    return "" if value is None else str(value)


def _extra_of(row: Dict[str, Any], used_fields: set) -> Dict[str, str]:
    """매핑에 쓰지 않은 필드 원문 (null·빈 값·제외 목록 필드 뺌)"""
    return {key: _text(value) for key, value in sorted(row.items())
            if key not in used_fields and key not in EXTRA_DENYLIST and value not in (None, "")}


def _dataset(body: Any, dataset_name: str) -> List[Dict[str, Any]]:
    """응답에서 기대 배열 추출 (없거나 배열이 아니면 KeyError·TypeError > 실패 격리)"""
    rows = body[dataset_name]
    if not isinstance(rows, list):
        raise TypeError("dataset is not a list")
    return rows


# ---- 행 > 모델 변환 (필드 대응은 spec/PUBLIC_DATA.md)
def _to_organization_category(row):
    return OrganizationCategory(row["COM_CD"], row["CD_NM"], _text(row.get("CD_NM_ENG")),
                                _extra_of(row, {"COM_CD", "CD_NM", "CD_NM_ENG"}))


def _to_term(row):
    return Term(row["YEAR"], row["SMT_CD"], row["YEAR_SMT_CD"], row["YEAR_SMT_NM"],
                _extra_of(row, {"YEAR", "SMT_CD", "YEAR_SMT_CD", "YEAR_SMT_NM"}))


def _to_department(row):
    used_fields = {"DEPT_CD", "DEPT_LEVEL_CD", "DEPT_L_CD", "DEPT_L_PRINT", "DEPT_M_CD", "DEPT_M_PRINT",
                   "DEPT_S_CD", "DEPT_S_PRINT", "DEPT_NM_ENG"}
    return Department(row["DEPT_CD"], row["DEPT_LEVEL_CD"], _text(row["DEPT_L_CD"]), _text(row["DEPT_L_PRINT"]),
                      _text(row["DEPT_M_CD"]), _text(row["DEPT_M_PRINT"]), _text(row["DEPT_S_CD"]),
                      _text(row["DEPT_S_PRINT"]), _text(row["DEPT_NM_ENG"]), _extra_of(row, used_fields))


def _to_course_type(row):
    return CourseType(row["CURI_TYPE_CD"], row["CURI_TYPE_CD_NM"], _text(row["ORGN_CLSF_CD"]),
                      _extra_of(row, {"CURI_TYPE_CD", "CURI_TYPE_CD_NM", "ORGN_CLSF_CD"}))


def _to_selective_domain(row):
    return SelectiveDomain(row["COM_CD"], row["CD_NM"], _text(row.get("CD_NM_ENG")),
                           _extra_of(row, {"COM_CD", "CD_NM", "CD_NM_ENG"}))


LECTURE_USED_FIELDS = {
    "CURI_NO", "CLASS", "CURI_NM", "CURI_TYPE_CD", "CURI_TYPE_CD_NM", "SLT_DOMAIN_CD", "SLT_DOMAIN_CD_NM", "CDT",
    "TM_NUM", "TOT_TM_NUM", "LESN_TIME", "LESN_ROOM", "LESN_EMP", "STUDENT_YEAR", "CURI_LANG_CD", "CURI_LANG_NM",
    "DEPT_CD", "SCH_DEPT_ALIAS", "MANAGE_DEPT_NM", "REMARK", "YEAR", "SMT_CD",
}


def _to_lecture(row):
    return Lecture(
        row["CURI_NO"], row["CLASS"], row["CURI_NM"], _text(row["CURI_TYPE_CD"]), _text(row["CURI_TYPE_CD_NM"]),
        _text(row["SLT_DOMAIN_CD"]), _text(row["SLT_DOMAIN_CD_NM"]), _text(row["CDT"]), _text(row["TM_NUM"]),
        _text(row["TOT_TM_NUM"]), _text(row["LESN_TIME"]), _text(row["LESN_ROOM"]), _text(row["LESN_EMP"]),
        _text(row["STUDENT_YEAR"]), _text(row["CURI_LANG_CD"]), _text(row["CURI_LANG_NM"]), _text(row["DEPT_CD"]),
        _text(row["SCH_DEPT_ALIAS"]), _text(row["MANAGE_DEPT_NM"]), _text(row["REMARK"]), _text(row["YEAR"]),
        _text(row["SMT_CD"]), _extra_of(row, LECTURE_USED_FIELDS))


def _to_calendar_event(row):
    used_fields = {"scmYear", "scmMonth", "collDiv", "collDivNm", "scmSubject", "scmSubjectEng", "frDt", "toDt"}
    return CalendarEvent(_text(row["scmYear"]), _text(row["scmMonth"]), row["collDiv"], _text(row["collDivNm"]),
                         _text(row["scmSubject"]), _text(row["scmSubjectEng"]), row["frDt"], row["toDt"],
                         _extra_of(row, used_fields))


def _to_dormitory_menu_days(body) -> List[DormitoryMenuDay]:
    """root[0] 의 fo_*1..7 > 7일치 (날짜 없으면 KeyError)"""
    row = body["root"][0]
    return [DormitoryMenuDay(row[f"fo_date{day}"], _text(row[f"fo_menu_mor{day}"]), _text(row[f"fo_menu_lun{day}"]),
                             _text(row[f"fo_menu_eve{day}"]), _text(row[f"fo_sub_menu{day}"])) for day in range(1, 8)]


def _to_dormitory_notice(row):
    # 작성자 필드(regname, regid)는 읽지 않음
    return DormitoryNotice(_text(row["seq"]), _text(row["subject"]), _text(row["regdate"]),
                           int(row["visitcnt"] or 0), _text(row["file_name1"]))


def _to_dormitory_notices(body) -> DormitoryNotices:
    block = body["root"][0]
    return DormitoryNotices(int(block["totalCount"][0]["cnt"]), [_to_dormitory_notice(row) for row in block["topList"]],
                            [_to_dormitory_notice(row) for row in block["list"]])


def _to_cafeteria_menu_item(row):
    return CafeteriaMenuItem(_text(row["buildingName"]), _text(row["location"]), _text(row["placeId"]),
                             _text(row["placeName"]), _text(row["menuDate"]), _text(row["day"]),
                             _text(row["mealTypeId"]), _text(row["mealName"]), _text(row["menuName"]),
                             _text(row["price"]))


def _to_campus_place(row):
    used_fields = {"id", "placeName", "placeNameEn", "placeType", "welfareCategory", "latitude", "longitude",
                   "placeInfo", "telNum", "description"}
    return CampusPlace(_text(row["id"]), _text(row["placeName"]), _text(row["placeNameEn"]), _text(row["placeType"]),
                       _text(row["welfareCategory"]), float(row["latitude"]), float(row["longitude"]),
                       _text(row["placeInfo"]), _text(row["telNum"]), _text(row["description"]),
                       _extra_of(row, used_fields))


def _to_extracurricular_event(row):
    detail = row["data"]
    return ExtracurricularEvent(_text(row["id"]), _text(row["title"]), _text(row["start"]), _text(row["end"]),
                                _text(row["category_title"]), _text(detail.get("url")),
                                _text(detail.get("description")), _text(detail.get("location")))


def _year_of(date_text: str) -> str:
    """yyyy-mm-dd > yyyy"""
    return str(date_text)[:4]


class SejongPublicData:
    """공개 데이터 클라이언트: 메서드 12개, 메서드·인자별 메모리 캐시, 동시 호출 합치기(같은 키 1회 요청)"""

    def __init__(self, timeout_seconds: float = 10, cache_ttl_seconds: float = 86400, ssl_verification: bool = True,
                 _session_factory: Optional[Callable[[], Any]] = None, _clock: Optional[Callable[[], float]] = None):
        self.timeout_seconds = timeout_seconds
        self.cache_ttl_seconds = cache_ttl_seconds
        self.ssl_verification = ssl_verification
        self._session_factory = _session_factory or self._create_default_session
        self._clock = _clock or time.time
        self._session: Any = None
        self._cache: Dict[Tuple, Tuple[float, Any]] = {}  # 키 > (저장 시각, 값)
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
        """강의 검색: 학과 코드·과목명·교수명 중 하나 이상 필수 (없으면 INVALID_INPUT)"""
        if not any(str(value or "").strip() for value in (department_code, course_name, instructor_name)):
            raise SejongAuthError(ErrorCode.INVALID_INPUT,
                                  "At least one of department code, course name, instructor name is required")
        search = {"ORGN_CLSF_CD": category_code, "YEAR": year, "SMT_CD": semester_code,
                  "YEAR_SMT": f"{year}/{semester_code}", "DEPT_CD": department_code, "CURI_NM": course_name,
                  "EMP_NM": instructor_name, "LANG_CHK": "KOR"}
        return self._sis_dataset("doList.do", search, "dl_main", _to_lecture)

    def calendar(self, start: str, end: str, category_code: str = "0001") -> List[CalendarEvent]:
        query = urllib.parse.urlencode({"mode": "getCalendarData", "start": start, "end": end, "collDiv": category_code})
        return self._fetch(("calendar", category_code, start, end), "GET", f"{ACADEMIC_CALENDAR_URL}?{query}", None, None,
                           lambda body: [_to_calendar_event(row) for row in _dataset(body, "data")], [])

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
        """캐시 확인 > 요청 > 변환 > 캐시 저장, 상류 실패는 마지막 캐시(만료 포함) 또는 빈 값 (같은 키는 한 번에 한 요청)"""
        with self._lock_for(cache_key):
            cached = self._cache.get(cache_key)
            if cached is not None and self._clock() - cached[0] < self.cache_ttl_seconds:
                return self._copy_of(cached[1])
            try:
                value = convert(self._request_json(method, url, payload, content_type))
            except Exception as error:  # 상류 실패 전부 격리 (서버 내부 정보 로그 금지, 예외 종류만)
                LOGGER.warning("[sejong public data] upstream failed (%s) : %s", cache_key[0], type(error).__name__)
                return self._copy_of(cached[1] if cached is not None else empty_value)
            self._cache[cache_key] = (self._clock(), value)
            return self._copy_of(value)

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
        from .transport import UrllibSession
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
