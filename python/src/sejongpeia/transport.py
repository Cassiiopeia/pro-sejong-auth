"""HTTP 세션: 인증 1회당 쿠키 저장소 1개, 포털 레거시 TLS, 리다이렉트 자동 (표준 라이브러리만)"""

import http.cookiejar
import socket
import ssl
import urllib.error
import urllib.request
from dataclasses import dataclass
from typing import Dict, Optional

# 포털은 TLS_RSA_WITH_AES_256_CBC_SHA 만 제공 > 기본 cipher 목록에 추가 (spec/PORTAL_FLOW.md)
PORTAL_CIPHERS = "DEFAULT:AES256-SHA:@SECLEVEL=1"


@dataclass(frozen=True)
class HttpResponse:
    status: int
    body: bytes

    @property
    def is_successful(self) -> bool:
        return 200 <= self.status < 300

    def text(self) -> str:
        return self.body.decode("utf-8", errors="replace")


def create_ssl_context(ssl_verification: bool) -> ssl.SSLContext:
    """포털 cipher 허용 컨텍스트 (ssl_verification=False 면 인증서·호스트 검증 해제)"""
    context = ssl.create_default_context()
    context.set_ciphers(PORTAL_CIPHERS)
    if not ssl_verification:
        context.check_hostname = False
        context.verify_mode = ssl.CERT_NONE
    return context


class UrllibSession:
    """요청 1건: 상태 코드가 4xx·5xx 여도 예외 대신 HttpResponse, 시간 초과는 TimeoutError"""

    def __init__(self, ssl_context: ssl.SSLContext, timeout_seconds: float, user_agent: str):
        self._timeout_seconds = timeout_seconds
        self._opener = urllib.request.build_opener(
            urllib.request.HTTPSHandler(context=ssl_context),
            urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()),
        )
        # 기본 Python-urllib 에이전트는 포털 방화벽이 차단
        self._opener.addheaders = [("User-Agent", user_agent)]

    def request(self, method: str, url: str, *, data: Optional[bytes] = None,
                headers: Optional[Dict[str, str]] = None) -> HttpResponse:
        request = urllib.request.Request(url, data=data, headers=headers or {}, method=method)
        try:
            with self._opener.open(request, timeout=self._timeout_seconds) as response:
                return HttpResponse(status=response.status, body=response.read())
        except urllib.error.HTTPError as error:
            with error:
                return HttpResponse(status=error.code, body=error.read() or b"")
        except urllib.error.URLError as error:
            if isinstance(error.reason, (socket.timeout, TimeoutError)):
                raise TimeoutError(str(error.reason)) from error
            raise OSError(str(error.reason)) from error
        except socket.timeout as error:
            raise TimeoutError(str(error)) from error
