"""공개 데이터 모델: 학교 응답 필드 > 라이브러리 필드 (spec/PUBLIC_DATA.md, 불변 dataclass)

extra: 매핑에 쓰지 않은 학교 필드 문자열 (비스칼라는 압축 JSON, 빈 값·개인 식별·서버 내부 필드 제외)
"""

from dataclasses import dataclass, field
from typing import Dict, List


@dataclass(frozen=True)
class OrganizationCategory:
    code: str
    name: str
    name_en: str
    extra: Dict[str, str] = field(default_factory=dict)


@dataclass(frozen=True)
class Term:
    year: str
    semester_code: str
    code: str
    name: str
    extra: Dict[str, str] = field(default_factory=dict)


@dataclass(frozen=True)
class Department:
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
    code: str
    name: str
    category_code: str
    extra: Dict[str, str] = field(default_factory=dict)


@dataclass(frozen=True)
class SelectiveDomain:
    code: str
    name: str
    name_en: str
    extra: Dict[str, str] = field(default_factory=dict)


@dataclass(frozen=True)
class Lecture:
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
    date: str
    breakfast: str
    lunch: str
    dinner: str
    alternative: str


@dataclass(frozen=True)
class DormitoryNotice:
    id: str
    title: str
    date: str
    views: int
    file_name: str


@dataclass(frozen=True)
class DormitoryNotices:
    total: int
    pinned: List[DormitoryNotice] = field(default_factory=list)
    items: List[DormitoryNotice] = field(default_factory=list)


@dataclass(frozen=True)
class CafeteriaMenuItem:
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
    place_id: str
    place_name: str
    building_name: str
    location: str
    extra: Dict[str, str] = field(default_factory=dict)


@dataclass(frozen=True)
class CampusPlace:
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
    id: str
    title: str
    start: str
    end: str
    category_title: str
    url: str
    description: str
    location: str
