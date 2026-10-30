"""학식: 식당 한 곳의 메뉴, 식당 목록 (식당 목록 API가 없어 번호별 메뉴 응답에서 모음)"""

import urllib.parse
from typing import Any, Dict, List

from ..common.errors import ErrorCode, SejongAuthError
from .fetcher import PublicDataFetcher, pairs_of
from .models import CafeteriaMenuItem, CafeteriaPlace
from .row_reader import extra_of, fields_of, rows_of

# 주소·요청 키·고정 값의 원본은 spec/contract.json 의 publicData.cafeteria
CAFETERIA_URL = "https://www.sejong.ac.kr/kor/unilife/cafeteria-info.do"
CAFETERIA_QUERY_KEYS = ("mode", "placeId")
CAFETERIA_MENU_MODE = "getMenuList"
# 학교에 식당 목록 API가 없어 번호 1부터 차례로 조회해 식당을 모음 (학교 화면도 번호를 스크립트에 고정해 씀, spec/PUBLIC_DATA.md)
CAFETERIA_DEFAULT_MAX_PLACE_ID = 10
# 호출자가 올릴 수 있는 상한 (한 번 갱신에 최대 이만큼 요청하므로 학교 서버 부담을 막기 위해 제한)
CAFETERIA_LIMIT_MAX_PLACE_ID = 50
# 메뉴 응답 행의 알려진 필드 (extra 에는 이 밖의 필드만)
CAFETERIA_MENU_FIELD_NAMES = {"placeId", "placeName", "buildingName", "location", "menuDate", "day", "mealTypeId",
                              "mealName", "menuName", "price"}


def _to_cafeteria_menu_item(row):
    return CafeteriaMenuItem(*fields_of(row, "buildingName", "location", "placeId", "placeName", "menuDate", "day",
                                        "mealTypeId", "mealName", "menuName", "price"))


def _to_cafeteria_places(bodies_by_place_id: Dict[str, Any]) -> List[CafeteriaPlace]:
    """번호별 메뉴 응답 > 식당 목록 (메뉴가 있는 번호만, 첫 행의 식당 정보, 같은 식당 번호는 한 번만)"""
    places: Dict[str, CafeteriaPlace] = {}
    for requested_place_id, body in bodies_by_place_id.items():
        first_row = next((row for row in rows_of(body, "items") if isinstance(row, dict)), None)
        if first_row is None:
            continue
        place_id, name, building_name, location = fields_of(first_row, "placeId", "placeName", "buildingName", "location")
        place_id = place_id or requested_place_id
        places.setdefault(place_id, CafeteriaPlace(place_id, name, building_name, location,
                                                   extra_of(first_row, CAFETERIA_MENU_FIELD_NAMES)))
    return list(places.values())


def _menu_url_of(place_id: str) -> str:
    query = urllib.parse.urlencode(pairs_of(CAFETERIA_QUERY_KEYS, CAFETERIA_MENU_MODE, place_id))
    return f"{CAFETERIA_URL}?{query}"


class CafeteriaSource:
    """학식 메뉴와 식당 목록"""

    def __init__(self, fetcher: PublicDataFetcher):
        self._fetcher = fetcher

    def cafeteria_menu(self, place_id: str) -> List[CafeteriaMenuItem]:
        """식당 번호 > 메뉴 목록"""
        return self._fetcher.fetch(("cafeteria_menu", place_id), "GET", _menu_url_of(place_id), None, None,
                                   lambda body: [_to_cafeteria_menu_item(row) for row in rows_of(body, "items")], [])

    def cafeteria_places(self, max_place_id: int = CAFETERIA_DEFAULT_MAX_PLACE_ID) -> List[CafeteriaPlace]:
        """번호 1~max_place_id 메뉴 응답 > 식당 목록 (범위·정수 검증 후 요청, 틀리면 INVALID_INPUT)"""
        if isinstance(max_place_id, bool) or not isinstance(max_place_id, int) or not 1 <= max_place_id <= CAFETERIA_LIMIT_MAX_PLACE_ID:
            raise SejongAuthError(ErrorCode.INVALID_INPUT, f"Max place id must be an integer from 1 to {CAFETERIA_LIMIT_MAX_PLACE_ID}")

        def load_places() -> List[CafeteriaPlace]:
            bodies_by_place_id = {}
            for place_id in (str(number) for number in range(1, max_place_id + 1)):  # 순서대로 한 건씩, 병렬 요청 없음
                bodies_by_place_id[place_id] = self._fetcher.request_json("GET", _menu_url_of(place_id), None, None)
            return _to_cafeteria_places(bodies_by_place_id)
        return self._fetcher.cached_call(("cafeteria_places", max_place_id), load_places, [])
