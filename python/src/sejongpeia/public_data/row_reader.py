"""학교 응답 행 읽기: 값 > 문자열·숫자 변환, 매핑에 안 쓴 필드 > extra, 응답 컨테이너 꺼내기 (모든 소스가 공유)"""

import json
from decimal import Decimal
from typing import Any, Dict, List

# extra 제외 필드: 교직원 이름 등 개인 식별, 서버 내부 정보
EXTRA_DENYLIST = frozenset({
    "CD_ALIAS1", "CD_ALIAS2", "CD_ALIAS3", "CD_ALIAS1_ENG", "CD_ALIAS2_ENG", "CD_ALIAS3_ENG",
    "regname", "regid", "STACK_TRACE",
})


def text_of(value: Any) -> str:
    """스칼라 > 문자열 (null > 빈 문자열, bool > true/false, 숫자 > 평범한 십진 표기, dict·list > 압축 JSON)"""
    if value is None:
        return ""
    if isinstance(value, bool):
        return "true" if value else "false"
    if isinstance(value, float):
        return str(int(value)) if value.is_integer() else format(Decimal(repr(value)), "f")
    if isinstance(value, (dict, list)):
        return json.dumps(value, ensure_ascii=False, separators=(",", ":"))
    return str(value)


def integer_of(value: Any) -> int:
    """숫자로 읽을 수 없으면 0"""
    try:
        return int(float(value))
    except (TypeError, ValueError, OverflowError):
        return 0


def decimal_of(value: Any) -> float:
    """숫자로 읽을 수 없으면 0.0"""
    try:
        return float(value)
    except (TypeError, ValueError):
        return 0.0


def object_of(value: Any) -> Dict[str, Any]:
    """객체가 아니면 빈 객체 (이상한 행도 버리지 않음)"""
    return value if isinstance(value, dict) else {}


def fields_of(row: Any, *names: str) -> List[str]:
    """행에서 필드 여러 개를 문자열로 (없거나 null 이면 빈 문자열)"""
    row = object_of(row)
    return [text_of(row.get(name)) for name in names]


def extra_of(row: Dict[str, Any], used_fields: set) -> Dict[str, str]:
    """매핑에 쓰지 않은 필드 (null·빈 값·제외 목록 필드 뺌, 비스칼라는 압축 JSON)"""
    return {key: text_of(value) for key, value in sorted(row.items())
            if key not in used_fields and key not in EXTRA_DENYLIST and value not in (None, "")}


def rows_of(body: Any, container_name: str) -> List[Any]:
    """응답에서 기대 배열 추출 (없거나 배열이 아니면 KeyError·TypeError > 실패 격리)"""
    rows = body[container_name]
    if not isinstance(rows, list):
        raise TypeError("container is not a list")
    return rows


def first_root_row(body: Any) -> Any:
    """기숙사 응답은 항상 root 한 행 (빈 root 는 비정상 응답이라 실패)"""
    rows = rows_of(body, "root")
    if not rows:
        raise ValueError("empty root")
    return rows[0]
