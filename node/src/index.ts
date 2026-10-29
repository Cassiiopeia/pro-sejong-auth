// 세종대 로그인 API: 학번·비밀번호로 세종대학교 포털(SSO) 인증
//   import { authenticate } from 'pro-sejongpeia';
//   const result = await authenticate('21011234', 'password');
import { SejongAuthClient } from './client.js';
import type { RawOption, SejongAuthResult, SejongDhcAuthResult, SejongSisAuthResult } from './models.js';

export { SejongAuthClient, type SejongAuthClientOptions } from './client.js';
export { ErrorCode, SejongAuthError } from './errors.js';
export type * from './models.js';
export { VERSION } from './version.js';

const defaultClient = new SejongAuthClient();

/** 통합 인증 (기본 설정: 10초, 재시도 3회, 인증서 검증) */
export const authenticate = (studentId: string, password: string): Promise<SejongAuthResult> =>
  defaultClient.authenticate(studentId, password);

/** DHC 인증: 학생 정보 + 고전독서 */
export const authenticateDhc = (studentId: string, password: string, options?: RawOption): Promise<SejongDhcAuthResult> =>
  defaultClient.authenticateDhc(studentId, password, options);

/** SIS 인증: 학생 정보 + 연락처 */
export const authenticateSis = (studentId: string, password: string, options?: RawOption): Promise<SejongSisAuthResult> =>
  defaultClient.authenticateSis(studentId, password, options);
