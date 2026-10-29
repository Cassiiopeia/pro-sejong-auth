"""결과 모델: Java 결과 클래스와 같은 필드 (snake_case, spec/RESULT_SCHEMA.md)"""

from dataclasses import asdict, dataclass, field
from datetime import datetime
from typing import List, Optional


@dataclass(frozen=True)
class ClassicCertification:
    area: str
    required_count: str
    certified_count: str


@dataclass(frozen=True)
class ClassicExamRecord:
    semester: str
    area: str
    book_title: str
    exam_date: str
    score: str
    pass_status: str


@dataclass(frozen=True)
class ClassicSubjectRecord:
    semester: str
    subject_name: str
    area: str
    book_title: str
    completion: str


@dataclass(frozen=True)
class ClassicContestRecord:
    semester: str
    contest_name: str
    area: str
    book_title: str


@dataclass(frozen=True)
class SejongClassicReading:
    """고전독서 인증 현황: 영역별 인증, 시험, 과목 대체, 대회, 교과연계, 이수·인증 학기, 인증여부"""

    certifications: List[ClassicCertification] = field(default_factory=list)
    exam_records: List[ClassicExamRecord] = field(default_factory=list)
    subject_substitutions: List[ClassicSubjectRecord] = field(default_factory=list)
    contest_records: List[ClassicContestRecord] = field(default_factory=list)
    curriculum_records: List[ClassicSubjectRecord] = field(default_factory=list)
    # 사용자 정보 표 원문 (예: "8 학기", "6 학기", 대체이수 사유가 붙는 자유 텍스트), 없으면 빈 문자열
    completed_semesters: str = ""
    certified_semesters: str = ""
    certification_status: str = ""

    def to_dict(self) -> dict:
        return asdict(self)


@dataclass(frozen=True)
class SejongStudentInfo:
    """DHC 사용자 정보 표: 학과, 학번, 이름, 학년, 학적 상태 (없으면 빈 문자열)"""

    major: str
    student_id: str
    name: str
    grade: str
    status: str

    def to_dict(self) -> dict:
        return asdict(self)


@dataclass(frozen=True)
class SejongSisUserInfo:
    """SIS initUserInfo: 학번, 이름, 학과, 이메일, 전화번호, 영문 이름, 사용자 구분"""

    student_id: str
    name: str
    major: str
    email: str
    phone_number: str
    english_name: str
    user_category: str

    def to_dict(self) -> dict:
        return asdict(self)


@dataclass(frozen=True)
class SejongAuthResult:
    """통합 인증 결과 (연락처·영문 이름·사용자 구분은 SIS 실패 시, 상위 소속명은 포털 메인 조회 실패 시 None)"""

    success: bool
    student_id: str
    name: str
    major: str
    grade: str
    status: str
    classic_reading: SejongClassicReading
    email: Optional[str]
    phone_number: Optional[str]
    english_name: Optional[str]
    user_category: Optional[str]
    # 학과의 상위 소속 이름(단과대학 또는 학부): 포털 화면 표기에서 추출한 부가 정보, 학적 구분에 따라 빈 문자열일 수 있음, 인증 판정에 쓰지 말 것
    # 학년도마다 바뀌는 포털 표기 원문, 값 목록으로 분기하지 말 것
    parent_unit_name: Optional[str]
    authenticated_at: datetime
    # include_raw=True 일 때만: DHC 원문 HTML, SIS 원문 JSON (SIS 조회 실패 시 None), 세션값·접속 IP 포함이라 외부 전달 금지
    raw_html: Optional[str] = None
    raw_json: Optional[str] = None


@dataclass(frozen=True)
class SejongDhcAuthResult:
    """DHC 인증 결과 (raw_html 은 include_raw=True 일 때만)"""

    success: bool
    student_id: str
    name: str
    major: str
    grade: str
    status: str
    classic_reading: SejongClassicReading
    authenticated_at: datetime
    raw_html: Optional[str] = None


@dataclass(frozen=True)
class SejongSisAuthResult:
    """SIS 인증 결과 (학년·상태는 SIS 미제공 > 빈 문자열, raw_json 은 include_raw=True 일 때만)"""

    success: bool
    student_id: str
    name: str
    major: str
    grade: str
    status: str
    email: str
    phone_number: str
    english_name: str
    user_category: str
    authenticated_at: datetime
    raw_json: Optional[str] = None
