"""포털 메인 소속 표기 > 상위 소속명: 보수적 규칙 (틀린 값보다 빈 값, 예외 없음)"""

import pytest
from conftest import read_expected, read_fixture

from sejongpeia.portal_main_parser import parse_parent_unit_name


def portal_main_html(affiliation: str) -> str:
    return (f'<div class="top_myinfo"><div class="box"><div class="text01">'
            f'<span class="txt1">홍길동</span><span class="txt4">{affiliation}</span></div></div></div>')


def test_parse_portal_main_fixture():
    expected = read_expected("expected_portal_main.json")

    assert parse_parent_unit_name(read_fixture("portal_main.html"), expected["major"]) == expected["parentUnitName"]


@pytest.mark.parametrize("affiliation,major,expected", [
    ("공과대학 컴퓨터공학과", "컴퓨터공학과", "공과대학"),  # 학부생 정상
    ("공과대학 컴퓨터 공학과", "컴퓨터 공학과", "공과대학"),  # 학과 이름 안에 공백
    ("College of Engineering Computer Science", "Computer Science", "College of Engineering"),  # 영문 표기
    ("컴퓨터공학과", "컴퓨터공학과", ""),  # 상위 소속 없이 학과만
    ("컴퓨터공학과", "", ""),  # 단일 토큰
    ("공과대학 컴퓨터공학 과", "컴퓨터공학과", ""),  # 학과 표기 불일치 + 3토큰 > 추측 안 함
    ("공과대학 컴퓨터공학과", "컴퓨터공학부", "공과대학"),  # 학과 표기 불일치 + 2토큰
    ("공과대학 컴퓨터 공학과", "소프트웨어학과", ""),  # 학과 표기 불일치 + 3토큰
    ("College of Engineering Computer Science", "", ""),  # 영문 + 학과 모름 > 4토큰 추측 안 함
    ("공과대학 학과 이름 길다", "컴퓨터공학과", ""),  # 토큰 3개 이상
    ("   ", "컴퓨터공학과", ""),  # 빈 표기
])
def test_parse_parent_unit_name_rules(affiliation, major, expected):
    assert parse_parent_unit_name(portal_main_html(affiliation), major) == expected


@pytest.mark.parametrize("html", [
    "",
    "<html><body>교직원 포털 화면: 내 정보 영역 없음</body></html>",
    '<div class="text01"><span class="txt4">공과대학 컴퓨터공학과</span></div>',  # top_myinfo 밖의 txt4
    '<div class="top_myinfo"><span class="txt1">홍길동</span></div>',  # txt4 없음
])
def test_missing_or_broken_structure_is_empty_string_without_exception(html):
    assert parse_parent_unit_name(html, "컴퓨터공학과") == ""


@pytest.mark.parametrize("html", ["<div class=top_myinfo><span class='txt4'>공과대학 <b>컴퓨터공학과", "<<<>>><div", "\x00\x01"])
def test_broken_html_never_raises(html):
    assert isinstance(parse_parent_unit_name(html, "컴퓨터공학과"), str)


def test_none_input_is_empty_string():
    assert parse_parent_unit_name(None, None) == ""
