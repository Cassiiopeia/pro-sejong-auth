"""비교과 프로그램 일정 (요청 조립과 행 > 모델 변환)"""

from typing import List

from .fetcher import FORM_CONTENT_TYPE, PublicDataFetcher, pairs_of
from .models import ExtracurricularEvent
from .row_reader import fields_of, object_of, rows_of

# 주소·요청 키·고정 값의 원본은 spec/contract.json 의 publicData.extracurricular
EXTRACURRICULAR_EVENTS_URL = "https://do.sejong.ac.kr/ko/process/schedule/getEvents"
EXTRACURRICULAR_FORM_KEYS = ("cid", "category", "start", "end", "start_year", "end_year")
EXTRACURRICULAR_CID = "eco"


def _to_extracurricular_event(row):
    row = object_of(row)
    detail = object_of(row.get("data"))
    return ExtracurricularEvent(*fields_of(row, "id", "title", "start", "end", "category_title"),
                                *fields_of(detail, "url", "description", "location"))


def _year_of(date_text: str) -> str:
    """yyyy-mm-dd > yyyy"""
    return str(date_text)[:4]


class ExtracurricularSource:
    """비교과 프로그램 일정"""

    def __init__(self, fetcher: PublicDataFetcher):
        self._fetcher = fetcher

    def extracurricular_events(self, start: str, end: str, category: str = "") -> List[ExtracurricularEvent]:
        """기간·분류 > 비교과 일정 목록"""
        form = pairs_of(EXTRACURRICULAR_FORM_KEYS, EXTRACURRICULAR_CID, category, start, end, _year_of(start), _year_of(end))
        return self._fetcher.fetch(("extracurricular_events", start, end, category), "POST", EXTRACURRICULAR_EVENTS_URL,
                                   form, FORM_CONTENT_TYPE,
                                   lambda body: [_to_extracurricular_event(row) for row in rows_of(body, "events")], [])
