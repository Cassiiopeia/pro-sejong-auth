// 에러 코드·예외: Java SejongAuthErrorCode 와 같은 이름 (spec/RESULT_SCHEMA.md)

export const ErrorCode = {
  INVALID_INPUT: 'INVALID_INPUT',
  AUTHENTICATION_FAILED: 'AUTHENTICATION_FAILED',
  CONNECTION_FAILED: 'CONNECTION_FAILED',
  CONNECTION_TIMEOUT: 'CONNECTION_TIMEOUT',
  SESSION_ERROR: 'SESSION_ERROR',
  DATA_FETCH_FAILED: 'DATA_FETCH_FAILED',
  PARSE_ERROR: 'PARSE_ERROR',
  SSL_CONFIGURATION_ERROR: 'SSL_CONFIGURATION_ERROR',
  ACCOUNT_LOCKED: 'ACCOUNT_LOCKED',
  ACCOUNT_UNAVAILABLE: 'ACCOUNT_UNAVAILABLE',
} as const;

export type ErrorCode = (typeof ErrorCode)[keyof typeof ErrorCode];

// 코드별 기본 메시지 (Java 메시지와 같은 영어 문구)
export const DEFAULT_MESSAGE_BY_CODE: Record<ErrorCode, string> = {
  INVALID_INPUT: 'Invalid input.',
  AUTHENTICATION_FAILED: 'Sejong University portal authentication failed. Check the student ID and password.',
  CONNECTION_FAILED: 'Cannot connect to the Sejong University portal.',
  CONNECTION_TIMEOUT: 'Connection to the Sejong University portal timed out.',
  SESSION_ERROR: 'An error occurred while handling the portal session.',
  DATA_FETCH_FAILED: 'Failed to fetch student information.',
  PARSE_ERROR: 'Failed to parse student information.',
  SSL_CONFIGURATION_ERROR: 'Failed to configure SSL.',
  ACCOUNT_LOCKED: 'The portal account is locked after too many wrong passwords. '
    + 'Reset the password at https://portal.sejong.ac.kr/jsp/inquiry/pwinquiry.jsp',
  ACCOUNT_UNAVAILABLE: 'The portal account cannot sign in (use period ended or blocked by the administrator).',
};

/** 세종 인증 실패 (code 로 분기, 메시지에 비밀번호·개인정보 없음) */
export class SejongAuthError extends Error {
  readonly code: ErrorCode;

  constructor(code: ErrorCode, message?: string, options?: { cause?: unknown }) {
    super(`${code}: ${message || DEFAULT_MESSAGE_BY_CODE[code]}`);
    this.name = 'SejongAuthError';
    this.code = code;
    if (options?.cause !== undefined) {
      Object.defineProperty(this, 'cause', { value: options.cause, enumerable: false });
    }
  }
}
