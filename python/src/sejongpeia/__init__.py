"""세종대 로그인 API: 학번·비밀번호로 세종대학교 포털(SSO) 인증

    from sejongpeia import authenticate
    result = authenticate("21011234", "password")
"""

from importlib.metadata import PackageNotFoundError, version

try:
    # 버전 단일 원본: pyproject.toml (projectops 가 version.yml 과 함께 갱신)
    __version__ = version("pro-sejongpeia")
except PackageNotFoundError:  # 설치 없이 소스로 실행 (테스트)
    __version__ = "0.0.0-dev"

from .client import SejongAuthClient  # noqa: E402
from .errors import ErrorCode, SejongAuthError  # noqa: E402
from .models import (  # noqa: E402
    ClassicCertification,
    ClassicContestRecord,
    ClassicExamRecord,
    ClassicSubjectRecord,
    SejongAuthResult,
    SejongClassicReading,
    SejongDhcAuthResult,
    SejongSisAuthResult,
    SisBusinessPeriod,
)
from .public_data import SejongPublicData  # noqa: E402
from .public_data_models import (  # noqa: E402
    CafeteriaMenuItem, CalendarEvent, CampusPlace, CourseType, Department, DormitoryMenuDay,
    DormitoryNotice, DormitoryNotices, ExtracurricularEvent, Lecture, OrganizationCategory,
    SelectiveDomain, Term,
)

_default_client = SejongAuthClient()


def authenticate(student_id: str, password: str, include_raw: bool = False, include_extra: bool = False) -> SejongAuthResult:
    """통합 인증 (기본 설정: 10초, 재시도 3회, 인증서 검증)"""
    return _default_client.authenticate(student_id, password, include_raw=include_raw, include_extra=include_extra)


def fetch_sis_business_periods(student_id: str, password: str, year, month=""):
    """SIS 학사 업무 기간 (로그인 필요, month 비우면 연간 전체)"""
    return _default_client.fetch_sis_business_periods(student_id, password, year, month)


def authenticate_dhc(student_id: str, password: str, include_raw: bool = False) -> SejongDhcAuthResult:
    """DHC 인증: 학생 정보 + 고전독서"""
    return _default_client.authenticate_dhc(student_id, password, include_raw=include_raw)


def authenticate_sis(student_id: str, password: str, include_raw: bool = False, include_extra: bool = False) -> SejongSisAuthResult:
    """SIS 인증: 학생 정보 + 연락처"""
    return _default_client.authenticate_sis(student_id, password, include_raw=include_raw, include_extra=include_extra)


__all__ = [
    "authenticate", "authenticate_dhc", "authenticate_sis", "fetch_sis_business_periods", "SisBusinessPeriod", "SejongAuthClient",
    "SejongAuthError", "ErrorCode",
    "SejongAuthResult", "SejongDhcAuthResult", "SejongSisAuthResult", "SejongClassicReading",
    "ClassicCertification", "ClassicExamRecord", "ClassicSubjectRecord", "ClassicContestRecord",
    "SejongPublicData", "OrganizationCategory", "Term", "Department", "CourseType", "SelectiveDomain", "Lecture",
    "CalendarEvent", "DormitoryMenuDay", "DormitoryNotice", "DormitoryNotices", "CafeteriaMenuItem",
    "CampusPlace", "ExtracurricularEvent",
    "__version__",
]
