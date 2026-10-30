"""학사일정: 학부·대학원 홈페이지 달력, 대학원 사이트별 달력 (요청 조립과 행 > 모델 변환)"""

import json
import re
import urllib.parse
from typing import List

from ..common.errors import ErrorCode, SejongAuthError
from .fetcher import PublicDataFetcher, pairs_of
from .models import CalendarEvent
from .row_reader import extra_of, fields_of, object_of, rows_of, text_of

# 주소·요청 키·고정 값의 원본은 spec/contract.json 의 publicData.calendar·graduateCalendar
ACADEMIC_CALENDAR_URL = "https://www.sejong.ac.kr/kor/academics/academic-calendar.do"
CALENDAR_QUERY_KEYS = ("mode", "start", "end", "collDiv")
CALENDAR_MODE = "getCalendarData"
DEFAULT_CALENDAR_CATEGORY_CODE = "0001"  # 학부 일정
# 대학원 사이트별 학사일정: {siteId} 는 graduate, gsi, mba 같은 사이트 식별자 (코드표 없이 호출자가 전달)
GRADUATE_CALENDAR_URL = "https://{siteId}.sejong.ac.kr/_custom/sejong/_app/calendar-graduate/proc/getCalendarData.jsp"
GRADUATE_CALENDAR_QUERY_KEYS = ("year", "month", "siteId")
# 사이트 식별자 허용 형태 (호스트 이름에 들어가므로 소문자·숫자·하이픈만)
GRADUATE_SITE_ID_PATTERN = re.compile(r"[a-z0-9][a-z0-9-]{0,30}")


def _to_calendar_event(row):
    row = object_of(row)
    names = ("scmYear", "scmMonth", "collDiv", "collDivNm", "scmSubject", "scmSubjectEng", "frDt", "toDt")
    return CalendarEvent(*fields_of(row, *names), extra_of(row, set(names)))


def _to_graduate_calendar_events(body) -> List[CalendarEvent]:
    """대학원 달력 응답 > 일정 목록 (success 거짓·없음, items 가 문자열 JSON 도 배열도 아니면 실패)"""
    if not isinstance(body, dict) or text_of(body.get("success")).lower() != "true":
        raise ValueError("graduate calendar did not report success")
    items = body["items"]
    if isinstance(items, str):
        items = json.loads(items)  # items 는 JSON 문자열이라 한 번 더 해석
    if not isinstance(items, list):
        raise TypeError("items is not a list")
    return [_to_calendar_event(row) for row in items]


class CalendarSource:
    """홈페이지 학사일정과 대학원 사이트별 학사일정"""

    def __init__(self, fetcher: PublicDataFetcher):
        self._fetcher = fetcher

    def calendar(self, start: str, end: str, category_code: str = DEFAULT_CALENDAR_CATEGORY_CODE) -> List[CalendarEvent]:
        """기간·분류 > 홈페이지 학사일정"""
        query = urllib.parse.urlencode(pairs_of(CALENDAR_QUERY_KEYS, CALENDAR_MODE, start, end, category_code))
        return self._fetcher.fetch(("calendar", category_code, start, end), "GET", f"{ACADEMIC_CALENDAR_URL}?{query}",
                                   None, None, lambda body: [_to_calendar_event(row) for row in rows_of(body, "data")], [])

    def graduate_calendar(self, site_id: str, year: int, month: int) -> List[CalendarEvent]:
        """대학원 사이트·연·월 > 한 달 학사일정 (사이트 이름 형태 검증 후 요청, 틀리면 INVALID_INPUT)"""
        if not GRADUATE_SITE_ID_PATTERN.fullmatch(str(site_id or "")):
            raise SejongAuthError(ErrorCode.INVALID_INPUT, "Graduate site id must be lowercase letters, digits and hyphens")
        query = urllib.parse.urlencode(pairs_of(GRADUATE_CALENDAR_QUERY_KEYS, year, month, site_id))
        return self._fetcher.fetch(("graduate_calendar", site_id, year, month), "GET",
                                   f"{GRADUATE_CALENDAR_URL.replace('{siteId}', site_id)}?{query}", None, None,
                                   _to_graduate_calendar_events, [])
