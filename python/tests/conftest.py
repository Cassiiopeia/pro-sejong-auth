"""테스트 공통: spec/fixtures 읽기, 네트워크 없는 가짜 세션"""

import json
from pathlib import Path

import pytest

from sejongpeia.transport import HttpResponse

FIXTURE_DIRECTORY = Path(__file__).resolve().parents[2] / "spec" / "fixtures"


def read_fixture(file_name: str) -> str:
    return (FIXTURE_DIRECTORY / file_name).read_text(encoding="utf-8")


def read_expected(file_name: str) -> dict:
    return json.loads(read_fixture(file_name))


class FakeSession:
    """URL 앞부분 > 응답 목록(차례로 소비). 값이 예외 인스턴스면 raise"""

    def __init__(self, responses_by_url_prefix):
        self.responses_by_url_prefix = {prefix: list(responses) for prefix, responses in responses_by_url_prefix.items()}
        self.requested_urls = []
        self.requests = []  # (메서드, URL, 본문, 헤더)

    def request(self, method, url, *, data=None, headers=None):
        self.requested_urls.append(url)
        self.requests.append((method, url, data, headers or {}))
        for prefix, responses in self.responses_by_url_prefix.items():
            if url.startswith(prefix):
                outcome = responses.pop(0) if len(responses) > 1 else responses[0]
                if isinstance(outcome, BaseException):
                    raise outcome
                return outcome
        raise AssertionError(f"unexpected request: {method} {url}")


def ok(body: str = "") -> HttpResponse:
    return HttpResponse(status=200, body=body.encode("utf-8"))


def status(code: int) -> HttpResponse:
    return HttpResponse(status=code, body=b"")


@pytest.fixture
def dhc_html() -> str:
    return read_fixture("dhc_status.html")


@pytest.fixture
def sis_json() -> str:
    return read_fixture("sis_user_info.json")


@pytest.fixture
def portal_main_html() -> str:
    return read_fixture("portal_main.html")
