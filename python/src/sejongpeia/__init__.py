"""세종대 로그인 API: 학번·비밀번호로 세종대학교 포털(SSO) 인증

    from sejongpeia import authenticate
    result = authenticate("21011234", "password")
"""

from .auth.client import SejongAuthClient
from .auth.models import (
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
from .common.errors import ErrorCode, SejongAuthError
from .common.version import __version__
from .public_data.client import SejongPublicData
from .public_data.models import (
    CafeteriaMenuItem, CafeteriaPlace, CalendarEvent, CampusPlace, CourseType, CurriculumCourse, Department, DormitoryMenuDay,
    DormitoryNotice, DormitoryNotices, ExtracurricularEvent, Lecture, OrganizationCategory,
    SelectiveDomain, Term,
)

_default_client = SejongAuthClient()


def authenticate(student_id: str, password: str, include_raw: bool = False, include_extra: bool = False) -> SejongAuthResult:
    """통합 인증 (기본 설정: 10초, 재시도 3회, 인증서 검증): SejongAuthClient.authenticate 와 같음
    인자 student_id, password: 학번, 포털 비밀번호 (저장·로그 금지)
    인자 include_raw, include_extra: 원문 포함, SIS 나머지 필드 extra 포함
    반환: SejongAuthResult
    실패: SejongAuthError(code=ErrorCode)
    """
    return _default_client.authenticate(student_id, password, include_raw=include_raw, include_extra=include_extra)


def fetch_sis_business_periods(student_id: str, password: str, year, month=""):
    """SIS 학사 업무 기간 (로그인 필요): SejongAuthClient.fetch_sis_business_periods 와 같음
    인자 student_id, password: 학번, 포털 비밀번호 (저장·로그 금지)
    인자 year, month: 연도 4자리, 월 1~12 (비우면 연간 전체)
    반환: SisBusinessPeriod 목록
    실패: SejongAuthError(code=ErrorCode)
    """
    return _default_client.fetch_sis_business_periods(student_id, password, year, month)


def authenticate_dhc(student_id: str, password: str, include_raw: bool = False) -> SejongDhcAuthResult:
    """DHC 인증: 학생 정보 + 고전독서 (SejongAuthClient.authenticate_dhc 와 같음)
    인자 student_id, password: 학번, 포털 비밀번호 (저장·로그 금지)
    인자 include_raw: 원본 HTML 포함
    반환: SejongDhcAuthResult
    실패: SejongAuthError(code=ErrorCode)
    """
    return _default_client.authenticate_dhc(student_id, password, include_raw=include_raw)


def authenticate_sis(student_id: str, password: str, include_raw: bool = False, include_extra: bool = False) -> SejongSisAuthResult:
    """SIS 인증: 학생 정보 + 연락처 (SejongAuthClient.authenticate_sis 와 같음)
    인자 student_id, password: 학번, 포털 비밀번호 (저장·로그 금지)
    인자 include_raw, include_extra: 원본 JSON 포함, 나머지 필드 extra 포함
    반환: SejongSisAuthResult
    실패: SejongAuthError(code=ErrorCode)
    """
    return _default_client.authenticate_sis(student_id, password, include_raw=include_raw, include_extra=include_extra)


__all__ = [
    "authenticate", "authenticate_dhc", "authenticate_sis", "fetch_sis_business_periods", "SisBusinessPeriod", "SejongAuthClient",
    "SejongAuthError", "ErrorCode",
    "SejongAuthResult", "SejongDhcAuthResult", "SejongSisAuthResult", "SejongClassicReading",
    "ClassicCertification", "ClassicExamRecord", "ClassicSubjectRecord", "ClassicContestRecord",
    "SejongPublicData", "OrganizationCategory", "Term", "Department", "CourseType", "SelectiveDomain", "Lecture",
    "CalendarEvent", "DormitoryMenuDay", "DormitoryNotice", "DormitoryNotices", "CafeteriaMenuItem", "CafeteriaPlace",
    "CampusPlace", "ExtracurricularEvent", "CurriculumCourse",
    "__version__",
]
