"""캠퍼스 지도의 장소 (요청 조립과 행 > 모델 변환)"""

import urllib.parse
from typing import List

from .fetcher import PublicDataFetcher, pairs_of
from .models import CampusPlace
from .row_reader import decimal_of, extra_of, fields_of, object_of, rows_of

# 주소·요청 키·고정 값의 원본은 spec/contract.json 의 publicData.campusMap
CAMPUS_MAP_URL = "https://www.sejong.ac.kr/kor/intro/campus-map.do"
CAMPUS_MAP_QUERY_KEYS = ("mode", "placeType")
CAMPUS_PLACE_LIST_MODE = "getPlaceUserList"


def _to_campus_place(row):
    row = object_of(row)
    used_fields = {"id", "placeName", "placeNameEn", "placeType", "welfareCategory", "latitude", "longitude",
                   "placeInfo", "telNum", "description"}
    identifier, name, name_en, place_type, category = fields_of(row, "id", "placeName", "placeNameEn", "placeType",
                                                                "welfareCategory")
    info, phone, description = fields_of(row, "placeInfo", "telNum", "description")
    return CampusPlace(identifier, name, name_en, place_type, category, decimal_of(row.get("latitude")),
                       decimal_of(row.get("longitude")), info, phone, description, extra_of(row, used_fields))


class CampusSource:
    """캠퍼스 지도 장소"""

    def __init__(self, fetcher: PublicDataFetcher):
        self._fetcher = fetcher

    def campus_places(self, place_type: str = "") -> List[CampusPlace]:
        """장소 종류 > 장소 목록 (비우면 전체)"""
        query = urllib.parse.urlencode(pairs_of(CAMPUS_MAP_QUERY_KEYS, CAMPUS_PLACE_LIST_MODE, place_type))
        return self._fetcher.fetch(("campus_places", place_type), "GET", f"{CAMPUS_MAP_URL}?{query}", None, None,
                                   lambda body: [_to_campus_place(row) for row in rows_of(body, "items")], [])
