"""학사 기준 정보·강의·학과 교육과정 (SIS 시간표 + 교육과정 조회): 요청 조립과 행 > 모델 변환"""

import urllib.parse
from typing import Callable, Dict, List

from ..common.errors import ErrorCode, SejongAuthError
from .fetcher import PublicDataFetcher, pairs_of
from .models import CourseType, CurriculumCourse, Department, Lecture, OrganizationCategory, SelectiveDomain, Term
from .row_reader import extra_of, fields_of, object_of, rows_of

# 주소·요청 키·고정 값의 원본은 spec/contract.json 의 publicData.sisTimetable·curriculum
SIS_OPEN_TIME_URL = "https://sjpt.sejong.ac.kr/sch/sch/ext/SueOpenTimeQ/"
SIS_CONTENT_TYPE = 'application/json; charset="UTF-8"'
SIS_SEARCH_KEY = "dm_search"
SIS_ONLOAD_ENDPOINT = "doOnload.do"
SIS_TERM_LIST_ENDPOINT = "doYearsmtlist.do"
SIS_DEPARTMENT_LIST_ENDPOINT = "doLesndeptgroup.do"
SIS_LECTURE_LIST_ENDPOINT = "doList.do"
# 강좌 조회는 나머지 필터 키가 모두 빈 값으로 있어야 학교가 받아들임 (하나라도 빠지면 _SUBMIT_ERROR_)
LECTURE_EMPTY_FILTER_KEYS = ("SMT_NM", "CURI_TYPE_CD", "SLT_DOMAIN_CD", "CURI_NO", "CLASS", "SLT_DEPT_CD", "PLAN_REG_YN",
                             "CORS_SCHE_GRP_CD")
# 학과 교육과정 과목 1건: deptNo 는 SIS 학과 코드(DEPT_CD)와 같은 값, Accept: application/json 이 없으면 404
CURRICULUM_COURSE_URL = "https://www.sejong.ac.kr/curriculum/getCurriculumOneKor.do"
CURRICULUM_QUERY_KEYS = ("curiNo", "deptNo")

# Lecture 생성자 인자 순서와 같음
LECTURE_FIELD_NAMES = (
    "CURI_NO", "CLASS", "CURI_NM", "CURI_TYPE_CD", "CURI_TYPE_CD_NM", "SLT_DOMAIN_CD", "SLT_DOMAIN_CD_NM", "CDT",
    "TM_NUM", "TOT_TM_NUM", "LESN_TIME", "LESN_ROOM", "LESN_EMP", "STUDENT_YEAR", "CURI_LANG_CD", "CURI_LANG_NM",
    "DEPT_CD", "SCH_DEPT_ALIAS", "MANAGE_DEPT_NM", "REMARK", "YEAR", "SMT_CD",
)
# CurriculumCourse 생성자 인자 순서와 같음 (remark 는 교과목 개요 본문이라 overview 로 받음)
CURRICULUM_COURSE_FIELD_NAMES = (
    "deptCd", "deptNm", "orgnClsfCd", "year", "curiNo", "curiNm", "curiTypeCd", "curiTypeNm", "cdt",
    "lecTmNum", "exeTmNum", "curiTm", "stuYearCd", "smtDanCd", "smtDanNm", "remark",
)


def _to_organization_category(row):
    row = object_of(row)
    return OrganizationCategory(*fields_of(row, "COM_CD", "CD_NM", "CD_NM_ENG"),
                                extra_of(row, {"COM_CD", "CD_NM", "CD_NM_ENG"}))


def _to_term(row):
    row = object_of(row)
    return Term(*fields_of(row, "YEAR", "SMT_CD", "YEAR_SMT_CD", "YEAR_SMT_NM"),
                extra_of(row, {"YEAR", "SMT_CD", "YEAR_SMT_CD", "YEAR_SMT_NM"}))


def _to_department(row):
    row = object_of(row)
    names = ("DEPT_CD", "DEPT_LEVEL_CD", "DEPT_L_CD", "DEPT_L_PRINT", "DEPT_M_CD", "DEPT_M_PRINT",
             "DEPT_S_CD", "DEPT_S_PRINT", "DEPT_NM_ENG")
    return Department(*fields_of(row, *names), extra_of(row, set(names)))


def _to_course_type(row):
    row = object_of(row)
    names = ("CURI_TYPE_CD", "CURI_TYPE_CD_NM", "ORGN_CLSF_CD")
    return CourseType(*fields_of(row, *names), extra_of(row, set(names)))


def _to_selective_domain(row):
    row = object_of(row)
    return SelectiveDomain(*fields_of(row, "COM_CD", "CD_NM", "CD_NM_ENG"),
                           extra_of(row, {"COM_CD", "CD_NM", "CD_NM_ENG"}))


def _to_lecture(row):
    row = object_of(row)
    return Lecture(*fields_of(row, *LECTURE_FIELD_NAMES), extra_of(row, set(LECTURE_FIELD_NAMES)))


def _to_curriculum_course(row):
    row = object_of(row)
    return CurriculumCourse(*fields_of(row, *CURRICULUM_COURSE_FIELD_NAMES),
                            extra_of(row, set(CURRICULUM_COURSE_FIELD_NAMES)))


class AcademicSource:
    """SIS 시간표 코드 5종, 개설 강좌, 학과 교육과정 과목 (요청 > 데이터셋 배열 > 모델 목록)"""

    def __init__(self, fetcher: PublicDataFetcher):
        self._fetcher = fetcher

    def organization_categories(self) -> List[OrganizationCategory]:
        """기관 구분 목록"""
        return self._sis_dataset(SIS_ONLOAD_ENDPOINT, {}, "dl_code_orgn", _to_organization_category)

    def terms(self, category_code: str) -> List[Term]:
        """기관 구분 코드 > 학기 목록"""
        return self._sis_dataset(SIS_TERM_LIST_ENDPOINT, {"ORGN_CLSF_CD": category_code}, "dl_code_yearSmt", _to_term)

    def departments(self, category_code: str, year: str, semester_code: str) -> List[Department]:
        """기관 구분·연도·학기 코드 > 학과 목록"""
        search = {"ORGN_CLSF_CD": category_code, "YEAR": year, "SMT_CD": semester_code,
                  "YEAR_SMT": f"{year}/{semester_code}"}
        return self._sis_dataset(SIS_DEPARTMENT_LIST_ENDPOINT, search, "dl_code_dept", _to_department)

    def course_types(self) -> List[CourseType]:
        """이수 구분 목록"""
        return self._sis_dataset(SIS_ONLOAD_ENDPOINT, {}, "dl_code_curiType", _to_course_type)

    def selective_domains(self) -> List[SelectiveDomain]:
        """선택 교양 영역 목록"""
        return self._sis_dataset(SIS_ONLOAD_ENDPOINT, {}, "dl_code_sltDomain", _to_selective_domain)

    def lectures(self, category_code: str, year: str, semester_code: str, department_code: str = "",
                 course_name: str = "", instructor_name: str = "") -> List[Lecture]:
        """학기 개설 강좌 (학과·과목명·교수명은 앞뒤 공백 제거, 모두 비우면 그 학기 전체)"""
        search = {**dict.fromkeys(LECTURE_EMPTY_FILTER_KEYS, ""), "ORGN_CLSF_CD": category_code, "YEAR": year,
                  "SMT_CD": semester_code, "YEAR_SMT": f"{year}/{semester_code}",
                  "DEPT_CD": str(department_code or "").strip(), "CURI_NM": str(course_name or "").strip(),
                  "EMP_NM": str(instructor_name or "").strip(), "LANG_CHK": "KOR"}
        return self._sis_dataset(SIS_LECTURE_LIST_ENDPOINT, search, "dl_main", _to_lecture)

    def curriculum_courses(self, department_code: str, course_number: str) -> List[CurriculumCourse]:
        """학과 코드·학수번호 > 교육과정 과목 (입력 검증 후 요청, 빈 입력은 INVALID_INPUT)"""
        if not str(department_code or "").strip() or not str(course_number or "").strip():
            raise SejongAuthError(ErrorCode.INVALID_INPUT, "Department code and course number are required")
        query = urllib.parse.urlencode(pairs_of(CURRICULUM_QUERY_KEYS, course_number, department_code))
        return self._fetcher.fetch(("curriculum_courses", department_code, course_number), "GET",
                                   f"{CURRICULUM_COURSE_URL}?{query}", None, None,
                                   lambda body: [_to_curriculum_course(row) for row in rows_of(body, "items")], [])

    def _sis_dataset(self, endpoint: str, search: Dict[str, str], dataset_name: str, to_model: Callable) -> List:
        """SIS 조회 한 건: {"dm_search": ...} JSON 본문 > 데이터셋 배열 > 모델 목록"""
        cache_key = ("sis", endpoint, dataset_name, tuple(sorted(search.items())))
        return self._fetcher.fetch(cache_key, "POST", SIS_OPEN_TIME_URL + endpoint, {SIS_SEARCH_KEY: search},
                                   SIS_CONTENT_TYPE, lambda body: [to_model(row) for row in rows_of(body, dataset_name)], [])
