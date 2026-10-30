"""공개 데이터 요청 공통: 요청 > JSON 검증, 메모리 캐시, 실패 격리 (모든 소스가 공유)
상류 실패는 마지막 캐시(만료 포함) 또는 빈 값, 같은 키는 한 번에 한 조회
"""

import json
import logging
import threading
import time
import urllib.parse
from typing import Any, Callable, Dict, Optional, Tuple

from ..common.transport import USER_AGENT, UrllibSession, create_ssl_context
from .models import DormitoryNotices

LOGGER = logging.getLogger(__name__)

# 주소·요청 키·고정 값의 원본은 spec/contract.json 의 publicData (tests/test_contract.py 가 일치 검사)
JSON_ACCEPT = "application/json"
FORM_CONTENT_TYPE = "application/x-www-form-urlencoded; charset=UTF-8"
FORM_REQUESTED_WITH = "XMLHttpRequest"
SUBMIT_ERROR_MARKER = "_SUBMIT_ERROR_"


def pairs_of(keys: Tuple[str, ...], *values: Any) -> Dict[str, Any]:
    """요청 키 목록 + 값 > 보낼 순서 그대로의 키-값 쌍"""
    return dict(zip(keys, values))


class PublicDataFetcher:
    """요청 1건 > 변환 > 캐시 저장, 실패는 마지막 캐시 또는 빈 값 (세션·시계는 테스트에서 주입)"""

    def __init__(self, timeout_seconds: float, cache_ttl_seconds: float, ssl_verification: bool,
                 failure_cache_seconds: float, cache_max_entries: int,
                 session_factory: Optional[Callable[[], Any]] = None, clock: Optional[Callable[[], float]] = None):
        self.timeout_seconds = timeout_seconds
        self.cache_ttl_seconds = cache_ttl_seconds
        self.ssl_verification = ssl_verification
        self.failure_cache_seconds = failure_cache_seconds
        self.cache_max_entries = cache_max_entries
        self._session_factory = session_factory or self._create_default_session
        self._clock = clock or time.time
        self._session: Any = None
        self._cache: Dict[Tuple, Tuple[float, Any, bool]] = {}  # 키 > (저장 시각, 값, 실패 기억 여부), 오래된 순
        self._key_locks: Dict[Tuple, threading.Lock] = {}
        self._guard_lock = threading.Lock()

    def fetch(self, cache_key: Tuple, method: str, url: str, payload: Optional[Dict], content_type: Optional[str],
              convert: Callable[[Any], Any], empty_value: Any) -> Any:
        """요청 1건 > 변환 (캐시와 실패 격리는 cached_call)"""
        return self.cached_call(cache_key, lambda: convert(self.request_json(method, url, payload, content_type)),
                                empty_value)

    def cached_call(self, cache_key: Tuple, load_value: Callable[[], Any], empty_value: Any) -> Any:
        """캐시 확인 > 조회 > 캐시 저장, 상류 실패는 마지막 캐시(만료 포함) 또는 빈 값 (같은 키는 한 번에 한 조회)"""
        with self._lock_for(cache_key):
            cached = self._cache.get(cache_key)
            if cached is not None:
                stored_at, cached_value, is_failure = cached
                valid_seconds = self.failure_cache_seconds if is_failure else self.cache_ttl_seconds
                if self._clock() - stored_at < valid_seconds:  # 성공 캐시 또는 실패 기억이 유효하면 요청 없음
                    return self._copy_of(cached_value)
            try:
                value = load_value()
            except Exception as error:  # 상류 실패 전부 격리 (서버 내부 정보 로그 금지, 예외 종류만)
                LOGGER.warning("[sejong public data] upstream failed (%s) : %s", cache_key[0], type(error).__name__)
                if cached is not None and not cached[2]:
                    return self._copy_of(cached[1])  # 만료된 캐시 값
                self._store(cache_key, empty_value, True)  # 실패 기억 (캐시 값 없을 때만)
                return self._copy_of(empty_value)
            self._store(cache_key, value, False)
            return self._copy_of(value)

    def request_json(self, method: str, url: str, payload: Optional[Dict], content_type: Optional[str]) -> Any:
        """요청 1건 > JSON (2xx 아님·JSON 아님·_SUBMIT_ERROR_ 는 예외)"""
        headers = {"Accept": JSON_ACCEPT}
        data = None
        if content_type == FORM_CONTENT_TYPE:
            data = urllib.parse.urlencode(payload).encode("utf-8")
            headers["X-Requested-With"] = FORM_REQUESTED_WITH
        elif content_type:
            data = json.dumps(payload, ensure_ascii=False).encode("utf-8")  # SIS JSON 본문
        if content_type:
            headers["Content-Type"] = content_type
        response = self._get_session().request(method, url, data=data, headers=headers)
        if not response.is_successful:
            raise ValueError(f"http status {response.status}")
        # happydorm 은 text/html 로 JSON 응답 > 항상 본문을 JSON 으로 파싱
        body = json.loads(response.text())
        if isinstance(body, dict) and SUBMIT_ERROR_MARKER in body:
            raise ValueError("school reported a submit error")
        return body

    def _store(self, cache_key: Tuple, value: Any, is_failure: bool) -> None:
        """캐시 저장 (상한 초과 시 가장 오래된 항목과 그 Lock 제거)"""
        with self._guard_lock:
            self._cache.pop(cache_key, None)  # 갱신 항목을 가장 최신 위치로
            while len(self._cache) >= max(self.cache_max_entries, 1):
                oldest_key = next(iter(self._cache))
                del self._cache[oldest_key]
                self._key_locks.pop(oldest_key, None)
            self._cache[cache_key] = (self._clock(), value, is_failure)

    def _get_session(self) -> Any:
        if self._session is None:
            self._session = self._session_factory()
        return self._session

    def _create_default_session(self) -> Any:
        return UrllibSession(create_ssl_context(self.ssl_verification), self.timeout_seconds, USER_AGENT)

    def _lock_for(self, cache_key: Tuple) -> threading.Lock:
        with self._guard_lock:
            return self._key_locks.setdefault(cache_key, threading.Lock())

    @staticmethod
    def _copy_of(value: Any) -> Any:
        """호출자가 캐시를 바꾸지 못하게 목록 복사 (모델은 불변)"""
        if isinstance(value, list):
            return list(value)
        if isinstance(value, DormitoryNotices):
            return DormitoryNotices(value.total, list(value.pinned), list(value.items))
        return value
