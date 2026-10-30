"""기숙사: 일주일 식단, 공지 (요청 조립과 행 > 모델 변환, 작성자 필드는 읽지 않음)"""

from typing import Any, Dict, List

from .fetcher import FORM_CONTENT_TYPE, PublicDataFetcher, pairs_of
from .models import DormitoryMenuDay, DormitoryNotice, DormitoryNotices
from .row_reader import first_root_row, fields_of, integer_of, object_of

# 주소·요청 키·고정 값의 원본은 spec/contract.json 의 publicData.dormitory
DORMITORY_MENU_URL = "https://happydorm.sejong.ac.kr/food/indexFoodList.do"
DORMITORY_MENU_FORM_KEYS = ("locgbn",)
DORMITORY_NOTICE_URL = "https://happydorm.sejong.ac.kr/bbs/getBbsList.do"
DORMITORY_NOTICE_FORM_KEYS = ("cPage", "rows", "bbs_locgbn", "bbs_id", "sType", "sWord")
DORMITORY_NOTICE_BOARD_ID = "notice"
DORMITORY_NOTICE_SEARCH_TYPE = "subject"  # 제목 검색
DEFAULT_DORMITORY_LOCATION = "SJ"
DORMITORY_DAYS_PER_WEEK = 7


def _to_dormitory_menu_days(body) -> List[DormitoryMenuDay]:
    """root[0] 의 fo_*1..7 > 7일치 (root 없음·배열 아님·빈 root 는 실패)"""
    row = object_of(first_root_row(body))
    return [DormitoryMenuDay(*fields_of(row, f"fo_date{day}", f"fo_menu_mor{day}", f"fo_menu_lun{day}",
                                        f"fo_menu_eve{day}", f"fo_sub_menu{day}"))
            for day in range(1, DORMITORY_DAYS_PER_WEEK + 1)]


def _to_dormitory_notice(row):
    # 작성자 필드(regname, regid)는 읽지 않음
    row = object_of(row)
    seq, subject, regdate, file_name = fields_of(row, "seq", "subject", "regdate", "file_name1")
    return DormitoryNotice(seq, subject, regdate, integer_of(row.get("visitcnt")), file_name)


def _notice_rows(block: Dict[str, Any], key: str) -> List[DormitoryNotice]:
    """목록 필드가 없거나 배열이 아니면 빈 목록 (결과 없음은 장애가 아님)"""
    rows = block.get(key)
    return [_to_dormitory_notice(row) for row in rows] if isinstance(rows, list) else []


def _to_dormitory_notices(body) -> DormitoryNotices:
    block = object_of(first_root_row(body))
    counts = block.get("totalCount")
    total = integer_of(object_of(counts[0]).get("cnt")) if isinstance(counts, list) and counts else 0
    return DormitoryNotices(total, _notice_rows(block, "topList"), _notice_rows(block, "list"))


class DormitorySource:
    """기숙사 식단과 공지"""

    def __init__(self, fetcher: PublicDataFetcher):
        self._fetcher = fetcher

    def dormitory_menu(self, location: str = DEFAULT_DORMITORY_LOCATION) -> List[DormitoryMenuDay]:
        """위치 코드 > 일주일 식단 7일"""
        return self._fetcher.fetch(("dormitory_menu", location), "POST", DORMITORY_MENU_URL,
                                   pairs_of(DORMITORY_MENU_FORM_KEYS, location), FORM_CONTENT_TYPE,
                                   _to_dormitory_menu_days, [])

    def dormitory_notices(self, page: int = 1, rows: int = 10, keyword: str = "",
                          location: str = DEFAULT_DORMITORY_LOCATION) -> DormitoryNotices:
        """쪽·건수·제목 검색어·위치 > 고정 공지와 일반 공지, 전체 건수"""
        form = pairs_of(DORMITORY_NOTICE_FORM_KEYS, page, rows, location, DORMITORY_NOTICE_BOARD_ID,
                        DORMITORY_NOTICE_SEARCH_TYPE, keyword)
        return self._fetcher.fetch(("dormitory_notices", page, rows, keyword, location), "POST", DORMITORY_NOTICE_URL,
                                   form, FORM_CONTENT_TYPE, _to_dormitory_notices,
                                   DormitoryNotices(total=0, pinned=[], items=[]))
