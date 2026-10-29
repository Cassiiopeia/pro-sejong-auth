"""공개 데이터 모델: 학교 응답 필드 > 라이브러리 필드 (spec/PUBLIC_DATA.md, 불변 dataclass)

extra: 매핑에 쓰지 않은 학교 필드 문자열 (비스칼라는 압축 JSON, 빈 값·개인 식별·서버 내부 필드 제외)
"""

from dataclasses import dataclass, field
from typing import Dict, List


@dataclass(frozen=True)
class OrganizationCategory:
    """기관 구분 1건 (학부 20, 대학원 30 등)"""

    code: str
    name: str
    name_en: str
    extra: Dict[str, str] = field(default_factory=dict)


@dataclass(frozen=True)
class Term:
    """학기 1건 (연도, 학기 코드, 이름)"""

    year: str
    semester_code: str
    code: str
    name: str
    extra: Dict[str, str] = field(default_factory=dict)


@dataclass(frozen=True)
class Department:
    """학과 1건 (코드, 단과대·학부·전공 단계별 코드와 이름, 영문 이름)"""

    code: str
    level: str
    college_code: str
    college_name: str
    unit_code: str
    unit_name: str
    major_code: str
    major_name: str
    name_en: str
    extra: Dict[str, str] = field(default_factory=dict)


@dataclass(frozen=True)
class CourseType:
    """이수 구분 1건 (공필, 전필 등)"""

    code: str
    name: str
    category_code: str
    extra: Dict[str, str] = field(default_factory=dict)


@dataclass(frozen=True)
class SelectiveDomain:
    """선택 교양 영역 1건"""

    code: str
    name: str
    name_en: str
    extra: Dict[str, str] = field(default_factory=dict)


@dataclass(frozen=True)
class Lecture:
    """개설 강좌 1건 (학수번호, 분반, 시간표, 강의실, 교수, 학점 등)"""

    course_number: str
    section: str
    name: str
    course_type_code: str
    course_type_name: str
    domain_code: str
    domain_name: str
    credits: str
    hours: str
    total_hours: str
    schedule: str
    room: str
    instructor: str
    grade_level: str
    language_code: str
    language_name: str
    department_code: str
    department_name: str
    managing_department: str
    remark: str
    year: str
    semester_code: str
    extra: Dict[str, str] = field(default_factory=dict)


@dataclass(frozen=True)
class CurriculumCourse:
    """학과 교육과정의 과목 1건 (학년, 학기, 이수구분, 학점, 교과목 개요 overview)"""

    department_code: str
    department_name: str
    category_code: str
    year: str
    course_number: str
    name: str
    course_type_code: str
    course_type_name: str
    credits: str
    lecture_hours: str
    practice_hours: str
    total_hours: str
    grade_level: str
    semester_code: str
    semester_name: str
    overview: str
    extra: Dict[str, str] = field(default_factory=dict)


@dataclass(frozen=True)
class CalendarEvent:
    """학사일정 1건 (제목, 시작일·종료일, 분류)"""

    year: str
    month: str
    category_code: str
    category_name: str
    title: str
    title_en: str
    start_date: str
    end_date: str
    extra: Dict[str, str] = field(default_factory=dict)


@dataclass(frozen=True)
class DormitoryMenuDay:
    """기숙사 식단 하루 (조식, 중식, 석식, 대체 메뉴)"""

    date: str
    breakfast: str
    lunch: str
    dinner: str
    alternative: str


@dataclass(frozen=True)
class DormitoryNotice:
    """기숙사 공지 1건 (작성자 정보 없음, views 는 정수)"""

    id: str
    title: str
    date: str
    views: int
    file_name: str


@dataclass(frozen=True)
class DormitoryNotices:
    """기숙사 공지 목록: 전체 건수 total(정수), 고정 공지 pinned, 일반 공지 items"""

    total: int
    pinned: List[DormitoryNotice] = field(default_factory=list)
    items: List[DormitoryNotice] = field(default_factory=list)


@dataclass(frozen=True)
class CafeteriaMenuItem:
    """학식 메뉴 1건 (식당, 날짜, 끼니, 메뉴 이름, 가격)"""

    building_name: str
    location: str
    place_id: str
    place_name: str
    date: str
    day: str
    meal_type_id: str
    meal_name: str
    menu_name: str
    price: str


@dataclass(frozen=True)
class CafeteriaPlace:
    """학식 식당 1곳 (번호, 이름, 건물, 위치)"""

    place_id: str
    place_name: str
    building_name: str
    location: str
    extra: Dict[str, str] = field(default_factory=dict)


@dataclass(frozen=True)
class CampusPlace:
    """캠퍼스 장소 1건 (이름, 종류, 위도·경도 숫자, 안내)"""

    id: str
    name: str
    name_en: str
    place_type: str
    category: str
    latitude: float
    longitude: float
    info: str
    phone: str
    description: str
    extra: Dict[str, str] = field(default_factory=dict)


@dataclass(frozen=True)
class ExtracurricularEvent:
    """비교과 프로그램 일정 1건 (제목, 기간, 분류, 주소, 장소)"""

    id: str
    title: str
    start: str
    end: str
    category_title: str
    url: str
    description: str
    location: str
