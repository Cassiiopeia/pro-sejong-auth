"""포털 메인 소속 표기 파싱: span.txt4 "상위 소속 학과" > 상위 소속명 (규칙: spec/PORTAL_FLOW.md)
학년도마다 바뀌는 포털 표기 원문 그대로 통과, 값 목록·코드표 검증 없음
"""

import re
from html.parser import HTMLParser
from typing import List

# 포털 메인 화면 클래스명 (바꾸면 상위 소속명이 빈 문자열이 됨)
CLASS_MY_INFO_BOX = "top_myinfo"
CLASS_AFFILIATION = "txt4"

_WHITESPACE = re.compile(r"\s+")


class _AffiliationParser(HTMLParser):
    """div.top_myinfo 안 첫 span.txt4 글자 수집"""

    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.text_parts: List[str] = []
        self._my_info_div_depth = 0
        self._is_in_affiliation = False
        self._is_affiliation_read = False

    def handle_starttag(self, tag, attrs):
        classes = (dict(attrs).get("class") or "").split()
        if tag == "div":
            if self._my_info_div_depth:
                self._my_info_div_depth += 1
            elif CLASS_MY_INFO_BOX in classes:
                self._my_info_div_depth = 1
        elif tag == "span" and self._my_info_div_depth and CLASS_AFFILIATION in classes and not self._is_affiliation_read:
            self._is_in_affiliation = True

    def handle_endtag(self, tag):
        if tag == "div" and self._my_info_div_depth:
            self._my_info_div_depth -= 1
        elif tag == "span" and self._is_in_affiliation:
            self._is_in_affiliation = False
            self._is_affiliation_read = True

    def handle_data(self, data):
        if self._is_in_affiliation:
            self.text_parts.append(data)


def _read_affiliation_text(html: str) -> str:
    parser = _AffiliationParser()
    parser.feed(html)
    parser.close()
    return _WHITESPACE.sub(" ", "".join(parser.text_parts)).strip()


def parse_parent_unit_name(html: str, major: str) -> str:
    """소속 표기 > 상위 소속명 (규칙 3개: 학과가 끝에 일치, 아니면 토큰 2개일 때만 앞 토큰, 그 외 빈 문자열, 예외 없음)"""
    try:
        affiliation = _read_affiliation_text(html or "")
    except Exception:  # 틀린 값보다 빈 값 > 파싱 오류도 빈 문자열
        return ""
    known_major = _WHITESPACE.sub(" ", major or "").strip()
    if not affiliation or affiliation == known_major:
        return ""
    if known_major and affiliation.endswith(" " + known_major):
        return affiliation[: -len(known_major)].strip()
    tokens = affiliation.split(" ")
    return tokens[0] if len(tokens) == 2 else ""
