// 세종대 로그인 API: 학번·비밀번호로 세종대학교 포털(SSO) 인증
//   import { authenticate } from 'pro-sejongpeia';
//   const result = await authenticate('21011234', 'password');
import { SejongAuthClient } from './auth/client.js';
import type { RawOption, SejongAuthResult, SejongDhcAuthResult, SejongSisAuthResult, SisBusinessPeriod } from './auth/models.js';

export { SejongAuthClient, type SejongAuthClientOptions } from './auth/client.js';
export { ErrorCode, SejongAuthError } from './common/errors.js';
export type * from './auth/models.js';
export { SejongPublicData, type SejongPublicDataOptions } from './publicData/client.js';
export type * from './publicData/models.js';
export { VERSION } from './common/version.js';

const defaultClient = new SejongAuthClient();

/**
 * 통합 인증 (기본 설정: 10초, 재시도 3회, 인증서 검증): SejongAuthClient.authenticate 와 같음
 * 실패: SejongAuthError(code: ErrorCode), 틀린 비밀번호는 재시도 없이 즉시 실패 (계정 잠금 방지)
 * @param studentId 학번 (빈 값이면 INVALID_INPUT)
 * @param password 포털 비밀번호 (저장·로그 금지, 빈 값이면 INVALID_INPUT)
 * @param options RawOption: includeRaw 면 받은 원문 포함 (디버깅용), includeExtra 면 SIS 나머지 필드를 extra 로 포함 (둘 다 기본 false)
 * @returns SejongAuthResult
 */
export const authenticate = (studentId: string, password: string, options?: RawOption): Promise<SejongAuthResult> =>
  defaultClient.authenticate(studentId, password, options);

/**
 * SIS 학사 업무 기간 (로그인 필요): SejongAuthClient.fetchSisBusinessPeriods 와 같음
 * 실패: SejongAuthError(code: ErrorCode), 틀린 비밀번호는 재시도 없이 즉시 실패 (계정 잠금 방지)
 * @param studentId 학번 (빈 값이면 INVALID_INPUT)
 * @param password 포털 비밀번호 (저장·로그 금지, 빈 값이면 INVALID_INPUT)
 * @param query { year, month }: 연도 4자리, 월 1~12 (비우면 연간 전체)
 * @returns SisBusinessPeriod 목록
 */
export const fetchSisBusinessPeriods = (
  studentId: string, password: string, query: { year: number | string; month?: number | string },
): Promise<SisBusinessPeriod[]> => defaultClient.fetchSisBusinessPeriods(studentId, password, query);

/**
 * DHC 인증: 학생 정보 + 고전독서 (SejongAuthClient.authenticateDhc 와 같음)
 * 실패: SejongAuthError(code: ErrorCode), 틀린 비밀번호는 재시도 없이 즉시 실패 (계정 잠금 방지)
 * @param studentId 학번 (빈 값이면 INVALID_INPUT)
 * @param password 포털 비밀번호 (저장·로그 금지, 빈 값이면 INVALID_INPUT)
 * @param options RawOption: includeRaw 면 원본 HTML 포함
 * @returns SejongDhcAuthResult
 */
export const authenticateDhc = (studentId: string, password: string, options?: RawOption): Promise<SejongDhcAuthResult> =>
  defaultClient.authenticateDhc(studentId, password, options);

/**
 * SIS 인증: 학생 정보 + 연락처 (SejongAuthClient.authenticateSis 와 같음)
 * 실패: SejongAuthError(code: ErrorCode), 틀린 비밀번호는 재시도 없이 즉시 실패 (계정 잠금 방지)
 * @param studentId 학번 (빈 값이면 INVALID_INPUT)
 * @param password 포털 비밀번호 (저장·로그 금지, 빈 값이면 INVALID_INPUT)
 * @param options RawOption: includeRaw 면 받은 원문 포함 (디버깅용), includeExtra 면 SIS 나머지 필드를 extra 로 포함 (둘 다 기본 false)
 * @returns SejongSisAuthResult
 */
export const authenticateSis = (studentId: string, password: string, options?: RawOption): Promise<SejongSisAuthResult> =>
  defaultClient.authenticateSis(studentId, password, options);
