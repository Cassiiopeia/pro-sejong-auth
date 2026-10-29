"""에러 코드·예외: Java SejongAuthErrorCode 와 같은 이름 (spec/RESULT_SCHEMA.md)"""

from enum import Enum


class ErrorCode(str, Enum):
    """인증 실패 사유 코드 (값 = Java enum 이름)"""

    INVALID_INPUT = "INVALID_INPUT"
    AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED"
    CONNECTION_FAILED = "CONNECTION_FAILED"
    CONNECTION_TIMEOUT = "CONNECTION_TIMEOUT"
    SESSION_ERROR = "SESSION_ERROR"
    DATA_FETCH_FAILED = "DATA_FETCH_FAILED"
    PARSE_ERROR = "PARSE_ERROR"
    SSL_CONFIGURATION_ERROR = "SSL_CONFIGURATION_ERROR"
    ACCOUNT_LOCKED = "ACCOUNT_LOCKED"
    ACCOUNT_UNAVAILABLE = "ACCOUNT_UNAVAILABLE"


# 코드별 기본 메시지 (Java 메시지와 같은 영어 문구)
DEFAULT_MESSAGE_BY_CODE = {
    ErrorCode.INVALID_INPUT: "Invalid input.",
    ErrorCode.AUTHENTICATION_FAILED: "Sejong University portal authentication failed. Check the student ID and password.",
    ErrorCode.CONNECTION_FAILED: "Cannot connect to the Sejong University portal.",
    ErrorCode.CONNECTION_TIMEOUT: "Connection to the Sejong University portal timed out.",
    ErrorCode.SESSION_ERROR: "An error occurred while handling the portal session.",
    ErrorCode.DATA_FETCH_FAILED: "Failed to fetch student information.",
    ErrorCode.PARSE_ERROR: "Failed to parse student information.",
    ErrorCode.SSL_CONFIGURATION_ERROR: "Failed to configure SSL.",
    ErrorCode.ACCOUNT_LOCKED: "The portal account is locked after too many wrong passwords. "
    "Reset the password at https://portal.sejong.ac.kr/jsp/inquiry/pwinquiry.jsp",
    ErrorCode.ACCOUNT_UNAVAILABLE: "The portal account cannot sign in (use period ended or blocked by the administrator).",
}


class SejongAuthError(Exception):
    """세종 인증 실패 (code 로 분기, 메시지에 비밀번호·개인정보 없음)"""

    def __init__(self, code: ErrorCode, message: str = ""):
        self.code = code
        self.message = message or DEFAULT_MESSAGE_BY_CODE[code]
        super().__init__(f"{code.value}: {self.message}")
