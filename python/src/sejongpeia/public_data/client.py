"""공개 데이터 조회(로그인 없음) 진입점: 학사 기준 정보, 강의, 학과 교육과정, 학사일정, 기숙사, 식당, 캠퍼스, 비교과
공개 메서드 15개는 출처별 모듈(*_source.py)로 위임, 캐시·실패 격리·전송은 fetcher.py (spec/PUBLIC_DATA.md)
"""

from typing import Any, Callable, List, Optional

from .academic_source import AcademicSource
from .cafeteria_source import CAFETERIA_DEFAULT_MAX_PLACE_ID, CafeteriaSource
from .calendar_source import DEFAULT_CALENDAR_CATEGORY_CODE, CalendarSource
from .campus_source import CampusSource
from .dormitory_source import DEFAULT_DORMITORY_LOCATION, DormitorySource
from .extracurricular_source import ExtracurricularSource
from .fetcher import PublicDataFetcher
from .models import (
    CafeteriaMenuItem, CafeteriaPlace, CalendarEvent, CampusPlace, CourseType, CurriculumCourse, Department, DormitoryMenuDay,
    DormitoryNotices, ExtracurricularEvent, Lecture, OrganizationCategory, SelectiveDomain, Term,
)


class SejongPublicData:
    """공개 데이터 클라이언트(로그인 없음): 학사 코드, 강의, 학과 교육과정, 학사일정, 기숙사, 학식, 캠퍼스, 비교과 15개 메서드
    요청 > JSON 검증 > 모델 변환 > 메모리 캐시, 같은 인자의 동시 호출은 요청 1건 공유
    값 규칙: 학교 원문 문자열, 없으면 빈 문자열 (null 없음), 쓰지 않은 학교 필드는 extra
    실패 격리: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 값, 잘못된 입력만 INVALID_INPUT 예외 (spec/PUBLIC_DATA.md)
    """

    def __init__(self, timeout_seconds: float = 10, cache_ttl_seconds: float = 86400, ssl_verification: bool = True,
                 failure_cache_seconds: float = 60, cache_max_entries: int = 500,
                 _session_factory: Optional[Callable[[], Any]] = None, _clock: Optional[Callable[[], float]] = None):
        """공개 데이터 클라이언트 설정 (인자 이름 끝의 _seconds 는 초)
        인자 timeout_seconds: 요청 제한 시간 (기본 10)
        인자 cache_ttl_seconds: 성공 값 캐시 유효 시간 (기본 86400)
        인자 ssl_verification: 인증서 검증 (기본 True)
        인자 failure_cache_seconds: 캐시 없는 실패를 기억하는 시간 (기본 60)
        인자 cache_max_entries: 캐시 최대 항목 수 (기본 500, 초과 시 가장 오래된 항목 제거)
        (밑줄로 시작하는 인자는 테스트용)
        """
        self.timeout_seconds = timeout_seconds
        self.cache_ttl_seconds = cache_ttl_seconds
        self.ssl_verification = ssl_verification
        self.failure_cache_seconds = failure_cache_seconds
        self.cache_max_entries = cache_max_entries
        self._fetcher = PublicDataFetcher(timeout_seconds, cache_ttl_seconds, ssl_verification, failure_cache_seconds,
                                          cache_max_entries, _session_factory, _clock)
        self._academic = AcademicSource(self._fetcher)
        self._calendar = CalendarSource(self._fetcher)
        self._dormitory = DormitorySource(self._fetcher)
        self._cafeteria = CafeteriaSource(self._fetcher)
        self._campus = CampusSource(self._fetcher)
        self._extracurricular = ExtracurricularSource(self._fetcher)

    def organization_categories(self) -> List[OrganizationCategory]:
        """기관 구분 목록 (학부 20, 대학원 30 등)
        인자: 없음
        반환: OrganizationCategory 목록
        실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
        캐시: 인자별 cache_ttl_seconds (기본 86400), 요청 1건
        """
        return self._academic.organization_categories()

    def terms(self, category_code: str) -> List[Term]:
        """기관 구분의 학기 목록: 기관 구분 코드 > Term 목록
        인자 category_code: 기관 구분 코드 (organization_categories 결과의 code, 예: 20 학부)
        반환: Term 목록
        실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
        캐시: 인자별 cache_ttl_seconds (기본 86400), 요청 1건
        """
        return self._academic.terms(category_code)

    def departments(self, category_code: str, year: str, semester_code: str) -> List[Department]:
        """학기의 학과 목록: 기관 구분·연도·학기 코드 > Department 목록
        인자 category_code: 기관 구분 코드
        인자 year: 연도 4자리 (예: 2026)
        인자 semester_code: 학기 코드 (10 1학기, 11 하계, 20 2학기, 21 동계)
        반환: Department 목록
        실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
        캐시: 인자별 cache_ttl_seconds (기본 86400), 요청 1건
        """
        return self._academic.departments(category_code, year, semester_code)

    def course_types(self) -> List[CourseType]:
        """이수 구분 목록 (공필, 전필 등)
        인자: 없음
        반환: CourseType 목록
        실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
        캐시: 인자별 cache_ttl_seconds (기본 86400), 요청 1건
        """
        return self._academic.course_types()

    def selective_domains(self) -> List[SelectiveDomain]:
        """선택 교양 영역 목록
        인자: 없음
        반환: SelectiveDomain 목록
        실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
        캐시: 인자별 cache_ttl_seconds (기본 86400), 요청 1건
        """
        return self._academic.selective_domains()

    def lectures(self, category_code: str, year: str, semester_code: str, department_code: str = "",
                 course_name: str = "", instructor_name: str = "") -> List[Lecture]:
        """개설 강좌 검색: 학과 코드·과목명·교수명은 선택, 모두 비우면 그 학기 전체 (학부 약 2,800행·2.4MB, spec/PUBLIC_DATA.md)
        인자 category_code: 기관 구분 코드
        인자 year: 연도 4자리
        인자 semester_code: 학기 코드 (10, 11, 20, 21)
        인자 department_code: 학과 코드 (departments 결과의 code), 비우면 전체
        인자 course_name: 과목명 검색어 (앞뒤 공백 제거)
        인자 instructor_name: 교수명 검색어 (앞뒤 공백 제거)
        반환: Lecture 목록
        실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
        캐시: 인자별 cache_ttl_seconds (기본 86400), 요청 1건
        """
        return self._academic.lectures(category_code, year, semester_code, department_code, course_name, instructor_name)

    def curriculum_courses(self, department_code: str, course_number: str) -> List[CurriculumCourse]:
        """학과 교육과정의 과목 1건 (학년·학기·이수구분·학점·교과목 개요)
        인자 department_code: 학과 코드 (departments 결과의 code), 필수
        인자 course_number: 학수번호 (lectures 결과의 course_number), 필수
        반환: CurriculumCourse 목록 (그 학과 교육과정에 없는 과목은 빈 목록, 정상)
        실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록, 학과 코드나 학수번호가 비면 INVALID_INPUT (요청 없음)
        캐시: 인자별 cache_ttl_seconds (기본 86400), 요청 1건
        """
        return self._academic.curriculum_courses(department_code, course_number)

    def calendar(self, start: str, end: str, category_code: str = DEFAULT_CALENDAR_CATEGORY_CODE) -> List[CalendarEvent]:
        """학부·대학원 홈페이지 학사일정: 기간·분류 > CalendarEvent 목록
        인자 start: 시작일 yyyy-MM-dd
        인자 end: 종료일 yyyy-MM-dd
        인자 category_code: 일정 분류 (0001 학부, 0002~0007 대학원 계열)
        반환: CalendarEvent 목록
        실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
        캐시: 인자별 cache_ttl_seconds (기본 86400), 요청 1건
        """
        return self._calendar.calendar(start, end, category_code)

    def graduate_calendar(self, site_id: str, year: int, month: int) -> List[CalendarEvent]:
        """대학원 사이트별 한 달 학사일정 (날짜는 MM.dd 형태)
        인자 site_id: 대학원 사이트 이름 (graduate, gsi, mba, pub, tourgrad, edu, arts 등, 소문자·숫자·하이픈)
        인자 year: 연도 (예: 2026)
        인자 month: 월 1~12
        반환: CalendarEvent 목록 (빈 달은 빈 목록, 정상)
        실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록, site_id 형태가 틀리면 INVALID_INPUT (요청 없음)
        캐시: 인자별 cache_ttl_seconds (기본 86400), 요청 1건
        """
        return self._calendar.graduate_calendar(site_id, year, month)

    def dormitory_menu(self, location: str = DEFAULT_DORMITORY_LOCATION) -> List[DormitoryMenuDay]:
        """기숙사 일주일 식단 (7일: 조식·중식·석식·대체)
        인자 location: 기숙사 위치 코드 (기본 SJ, 다른 값은 확인하지 않음)
        반환: DormitoryMenuDay 목록 (7개)
        실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
        캐시: 인자별 cache_ttl_seconds (기본 86400), 요청 1건
        """
        return self._dormitory.dormitory_menu(location)

    def dormitory_notices(self, page: int = 1, rows: int = 10, keyword: str = "", location: str = DEFAULT_DORMITORY_LOCATION) -> DormitoryNotices:
        """기숙사 공지: 고정 공지와 일반 공지, 전체 건수 (작성자 정보는 반환하지 않음)
        인자 page: 쪽 번호 (1부터)
        인자 rows: 쪽당 건수
        인자 keyword: 제목 검색어
        인자 location: 기숙사 위치 코드
        반환: DormitoryNotices (실패하면 total 0과 빈 목록, 결과 없음도 total 0)
        실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
        캐시: 인자별 cache_ttl_seconds (기본 86400), 요청 1건
        """
        return self._dormitory.dormitory_notices(page, rows, keyword, location)

    def cafeteria_menu(self, place_id: str) -> List[CafeteriaMenuItem]:
        """학생식당 한 곳의 메뉴
        인자 place_id: 학교의 식당 번호 (cafeteria_places 결과의 place_id), 빈 목록이 번호 오류의 증거는 아님
        반환: CafeteriaMenuItem 목록
        실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
        캐시: 인자별 cache_ttl_seconds (기본 86400), 요청 1건
        """
        return self._cafeteria.cafeteria_menu(place_id)

    def cafeteria_places(self, max_place_id: int = CAFETERIA_DEFAULT_MAX_PLACE_ID) -> List[CafeteriaPlace]:
        """학식 식당 목록 (번호, 이름, 건물, 위치): 번호 1~max_place_id의 메뉴 응답에서 모음, 메뉴가 없는 식당은 빠짐
        인자 max_place_id: 조회할 식당 번호 상한 (1~50, 기본 10)
        반환: CafeteriaPlace 목록 (id 순서)
        실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록, 하나라도 실패하면 목록 전체를 실패로 보고 일부만 캐시하지 않음, 범위 밖이거나 정수가 아니면 INVALID_INPUT (요청 없음)
        캐시: max_place_id 별 cache_ttl_seconds, 요청은 번호만큼 차례로 (기본 10건, 최대 50건)
        """
        return self._cafeteria.cafeteria_places(max_place_id)

    def campus_places(self, place_type: str = "") -> List[CampusPlace]:
        """캠퍼스 지도의 장소 (이름, 위치 위도·경도, 안내)
        인자 place_type: 장소 종류 (학교 값 그대로), 비우면 전체
        반환: CampusPlace 목록
        실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
        캐시: 인자별 cache_ttl_seconds (기본 86400), 요청 1건
        """
        return self._campus.campus_places(place_type)

    def extracurricular_events(self, start: str, end: str, category: str = "") -> List[ExtracurricularEvent]:
        """비교과 프로그램 일정: 기간·분류 > ExtracurricularEvent 목록
        인자 start: 시작일 yyyy-MM-dd
        인자 end: 종료일 yyyy-MM-dd
        인자 category: 비교과 분류 (학교 값 그대로), 비우면 전체
        반환: ExtracurricularEvent 목록
        실패: 학교 쪽 문제는 예외 없이 마지막 캐시 또는 빈 목록
        캐시: 인자별 cache_ttl_seconds (기본 86400), 요청 1건
        """
        return self._extracurricular.extracurricular_events(start, end, category)
