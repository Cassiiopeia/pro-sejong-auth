"""포털 응답 파싱: DHC 고전독서 페이지 HTML, SIS initUserInfo JSON (규칙: spec/PORTAL_FLOW.md)"""

import json
import re
from html.parser import HTMLParser
from typing import Dict, List, Optional

from .errors import ErrorCode, SejongAuthError
from .models import (
    ClassicCertification,
    ClassicContestRecord,
    ClassicExamRecord,
    ClassicSubjectRecord,
    SejongClassicReading,
    SejongSisUserInfo,
    SejongStudentInfo,
)

# 섹션 제목 (포털 화면 문구 그대로, 바꾸면 파싱 실패)
SECTION_USER_INFO = "사용자 정보"
SECTION_CERTIFICATIONS = "영역별 인증현황"
SECTION_EXAMS = "인증 시험 현황"
SECTION_SUBJECT_SUBSTITUTIONS = "과목 대체 인증 현황"
SECTION_CONTESTS = "대회 인증 현황"
SECTION_CURRICULUM = "교과연계 인증 현황"

# 사용자 정보 표 라벨
LABEL_MAJOR = "학과명"
LABEL_STUDENT_ID = "학번"
LABEL_NAME = "이름"
LABEL_GRADE = "학년"
LABEL_STATUS = "사용자 상태"

_WHITESPACE = re.compile(r"\s+")


class _Cell:
    def __init__(self, tag: str):
        self.tag = tag
        self.text_parts: List[str] = []

    @property
    def text(self) -> str:
        return _WHITESPACE.sub(" ", "".join(self.text_parts)).strip()


class _Section:
    def __init__(self):
        self.title = ""
        self.rows: List[List[_Cell]] = []


class _BoardTableParser(HTMLParser):
    """div.b-con-box 섹션 > h4.b-h4-tit01 제목 + table.b-board-table tbody 행·칸 수집"""

    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.sections: List[_Section] = []
        self._section: Optional[_Section] = None
        self._section_div_depth = 0
        self._is_in_title = False
        self._is_in_board_table = False
        self._is_in_tbody = False
        self._row: Optional[List[_Cell]] = None
        self._cell: Optional[_Cell] = None

    def handle_starttag(self, tag, attrs):
        classes = (dict(attrs).get("class") or "").split()
        if tag == "div":
            if self._section is not None:
                self._section_div_depth += 1
            elif "b-con-box" in classes:
                self._section = _Section()
                self._section_div_depth = 1
            return
        if self._section is None:
            return
        if tag == "h4" and "b-h4-tit01" in classes:
            self._is_in_title = True
        elif tag == "table" and "b-board-table" in classes:
            self._is_in_board_table = True
        elif tag == "tbody" and self._is_in_board_table:
            self._is_in_tbody = True
        elif tag == "tr" and self._is_in_tbody:
            self._row = []
        elif tag in ("th", "td") and self._row is not None:
            self._cell = _Cell(tag)

    def handle_endtag(self, tag):
        if tag == "div" and self._section is not None:
            self._section_div_depth -= 1
            if self._section_div_depth == 0:
                self.sections.append(self._section)
                self._section = None
        elif tag == "h4":
            self._is_in_title = False
        elif tag == "table":
            self._is_in_board_table = False
            self._is_in_tbody = False
        elif tag == "tbody":
            self._is_in_tbody = False
        elif tag == "tr" and self._row is not None:
            self._section.rows.append(self._row)
            self._row = None
        elif tag in ("th", "td") and self._cell is not None:
            self._row.append(self._cell)
            self._cell = None

    def handle_data(self, data):
        if self._is_in_title and self._section is not None:
            self._section.title += data
        elif self._cell is not None:
            self._cell.text_parts.append(data)


def _read_sections(html: str) -> List[_Section]:
    try:
        parser = _BoardTableParser()
        parser.feed(html)
        parser.close()
        return parser.sections
    except Exception as error:  # HTMLParser 내부 오류 > PARSE_ERROR
        raise SejongAuthError(ErrorCode.PARSE_ERROR) from error


def _find_rows(sections: List[_Section], title: str) -> List[List[_Cell]]:
    for section in sections:
        if title in section.title:
            return section.rows
    return []


def _td_texts(row: List[_Cell]) -> List[str]:
    return [cell.text for cell in row if cell.tag == "td"]


def _th_text(row: List[_Cell]) -> str:
    return " ".join(cell.text for cell in row if cell.tag == "th").strip()


def parse_student_info(html: str) -> SejongStudentInfo:
    """사용자 정보 표 > 학과, 학번, 이름, 학년, 상태 (표 없으면 PARSE_ERROR)"""
    value_by_label: Dict[str, str] = {}
    for row in _find_rows(_read_sections(html), SECTION_USER_INFO):
        label = _th_text(row)
        if label:
            value_by_label[label] = " ".join(_td_texts(row)).strip()
    if not value_by_label:
        raise SejongAuthError(ErrorCode.PARSE_ERROR, "Student info table not found")
    return SejongStudentInfo(
        major=value_by_label.get(LABEL_MAJOR, ""),
        student_id=value_by_label.get(LABEL_STUDENT_ID, ""),
        name=value_by_label.get(LABEL_NAME, ""),
        grade=value_by_label.get(LABEL_GRADE, ""),
        status=value_by_label.get(LABEL_STATUS, ""),
    )


def parse_classic_reading(html: str) -> SejongClassicReading:
    """고전독서 5개 표 > SejongClassicReading (칸 수 부족한 행은 건너뜀)"""
    sections = _read_sections(html)

    certifications = []
    for row in _find_rows(sections, SECTION_CERTIFICATIONS):
        area, counts = _th_text(row), _td_texts(row)
        if area and len(counts) >= 2:
            certifications.append(ClassicCertification(area, counts[0], counts[1]))

    def rows_with(title: str, minimum_cell_count: int) -> List[List[str]]:
        return [cells for cells in map(_td_texts, _find_rows(sections, title)) if len(cells) >= minimum_cell_count]

    return SejongClassicReading(
        certifications=certifications,
        exam_records=[ClassicExamRecord(*cells[:6]) for cells in rows_with(SECTION_EXAMS, 6)],
        subject_substitutions=[ClassicSubjectRecord(*cells[:5]) for cells in rows_with(SECTION_SUBJECT_SUBSTITUTIONS, 5)],
        contest_records=[ClassicContestRecord(*cells[:4]) for cells in rows_with(SECTION_CONTESTS, 4)],
        curriculum_records=[ClassicSubjectRecord(*cells[:5]) for cells in rows_with(SECTION_CURRICULUM, 5)],
    )


def _text(node: dict, field_name: str) -> str:
    value = node.get(field_name) if isinstance(node, dict) else None
    return "" if value is None else str(value)


# 포털 로그인 판정값: login_action.jsp 응답의 var result = '...' (포털 화면 분기와 같은 대응)
_PORTAL_LOGIN_RESULT = re.compile(r"var\s+result\s*=\s*'([^']*)'")
PORTAL_LOGIN_ACCEPTED = "OK"
PORTAL_LOGIN_ERROR_CODE_BY_RESULT = {
    "erridpwd": ErrorCode.AUTHENTICATION_FAILED,  # 아이디·비밀번호 불일치
    "Error": ErrorCode.AUTHENTICATION_FAILED,  # 아이디·비밀번호 불일치
    "pwdNeedChg": ErrorCode.ACCOUNT_LOCKED,  # 반복 실패로 잠김, 비밀번호 재설정 필요
    "invalidDt": ErrorCode.ACCOUNT_UNAVAILABLE,  # 계정 사용 기간 종료
    "invalid": ErrorCode.ACCOUNT_UNAVAILABLE,  # 관리자 요청으로 로그인 불가
}


def ensure_portal_login_accepted(html: str) -> None:
    """포털 로그인 판정: OK 통과, 알려진 실패 > 코드별 예외, 그 외 값 > SESSION_ERROR, 판정값 없음 > 통과(이후 401 판정)"""
    match = _PORTAL_LOGIN_RESULT.search(html or "")
    if match is None or match.group(1) == PORTAL_LOGIN_ACCEPTED:
        return
    result_value = match.group(1)
    error_code = PORTAL_LOGIN_ERROR_CODE_BY_RESULT.get(result_value)
    if error_code is None:
        raise SejongAuthError(ErrorCode.SESSION_ERROR, f"Portal login failed : result={result_value[:20]}")
    raise SejongAuthError(error_code)


# SIS 오류 응답: 200 + {"_SUBMIT_ERROR_": {"ERRTYPE": ...}}, 로그인 세션 없음(틀린 비밀번호) = LOGOUT
SIS_SUBMIT_ERROR_KEY = "_SUBMIT_ERROR_"
SIS_LOGGED_OUT_ERROR_TYPE = "LOGOUT"


def ensure_sis_logged_in(json_text: str) -> None:
    """initUserInfo 오류 응답 판정: LOGOUT > AUTHENTICATION_FAILED, 그 외 오류 > DATA_FETCH_FAILED"""
    try:
        root = json.loads(json_text)
    except (TypeError, ValueError):
        return  # JSON 아님 > 파싱 단계에서 PARSE_ERROR
    submit_error = root.get(SIS_SUBMIT_ERROR_KEY) if isinstance(root, dict) else None
    if not isinstance(submit_error, dict):
        return
    error_type = _text(submit_error, "ERRTYPE")
    if error_type == SIS_LOGGED_OUT_ERROR_TYPE:
        raise SejongAuthError(ErrorCode.AUTHENTICATION_FAILED)
    raise SejongAuthError(ErrorCode.DATA_FETCH_FAILED, f"SIS returned an error : {error_type}")


def parse_sis_user_info(json_text: str) -> SejongSisUserInfo:
    """initUserInfo JSON > 학번, 이름, 학과, 이메일, 전화번호, 영문 이름 (dm_UserInfo 없으면 PARSE_ERROR)"""
    try:
        root = json.loads(json_text)
    except (TypeError, ValueError) as error:
        raise SejongAuthError(ErrorCode.PARSE_ERROR) from error
    if not isinstance(root, dict) or not isinstance(root.get("dm_UserInfo"), dict):
        raise SejongAuthError(ErrorCode.PARSE_ERROR, "dm_UserInfo field not found")

    user_info = root["dm_UserInfo"]
    gam = root.get("dm_UserInfoGam") or {}
    sch = root.get("dm_UserInfoSch") or {}
    phone_parts = [_text(gam, key).strip() for key in ("USER_PHONE_NO1", "USER_PHONE_NO2", "USER_PHONE_NO3")]

    return SejongSisUserInfo(
        student_id=_text(user_info, "INTG_USR_NO"),
        name=_text(user_info, "INTG_USR_NM"),
        major=_text(gam, "DEPT_NM").strip() or _text(sch, "DEPT_NM"),
        email=_text(gam, "USER_EMAIL"),
        phone_number="-".join(part for part in phone_parts if part),
        english_name=_text(user_info, "INTG_ENG_NM").strip() or _text(sch, "NM_ENG"),
    )
