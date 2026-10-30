// 학교 응답 행 읽기: 값 > 문자열·숫자 변환, 매핑에 안 쓴 필드 > extra, 응답 컨테이너 꺼내기 (모든 소스가 공유)
import type { ExtraFields } from './models.js';

export type JsonObject = Record<string, unknown>;
export type SchoolRow = JsonObject;

// 결과 extra 에서 제외: 교직원 이름 등 개인 식별, 서버 내부 정보
const EXTRA_DENYLIST = new Set(['CD_ALIAS1', 'CD_ALIAS2', 'CD_ALIAS3', 'CD_ALIAS1_ENG', 'CD_ALIAS2_ENG', 'CD_ALIAS3_ENG', 'regname', 'regid', 'STACK_TRACE']);

// 학교 값 > 문자열 (null 은 빈 문자열, 객체·배열은 공백 없는 JSON)
export const toText = (value: unknown): string => {
  if (value === null || value === undefined) return '';
  return typeof value === 'object' ? JSON.stringify(value) : String(value);
};

// 학교 값 > 숫자 (비었거나 숫자가 아니면 0)
export const toNumber = (value: unknown): number => {
  const parsed = typeof value === 'number' ? value : Number.parseFloat(toText(value));
  return Number.isFinite(parsed) ? parsed : 0;
};

export const toCount = (value: unknown): number => Number.parseInt(toText(value), 10) || 0;

// 매핑에 쓰지 않은 필드를 원문 문자열로 (빈 값과 제외 목록 필드는 뺌)
export function extraOf(row: SchoolRow, usedKeys: string[]): ExtraFields {
  const used = new Set(usedKeys);
  const extra: ExtraFields = {};
  for (const key of Object.keys(row).sort()) {
    const value = row[key];
    if (used.has(key) || EXTRA_DENYLIST.has(key) || value === null || value === undefined || value === '') continue;
    extra[key] = toText(value);
  }
  return extra;
}

export const isObject = (value: unknown): value is JsonObject => typeof value === 'object' && value !== null && !Array.isArray(value);

// 행이 객체가 아니면 빈 행으로 (행은 버리지 않음)
export const asObject = (value: unknown): JsonObject => (isObject(value) ? value : {});

// 컨테이너가 배열이 아니면 실패
export function asRows(value: unknown): SchoolRow[] {
  if (!Array.isArray(value)) throw new Error('expected array');
  return value.map(asObject);
}

// 없거나 배열이 아니면 빈 목록 (결과 없음)
export const asRowsOrEmpty = (value: unknown): SchoolRow[] => (Array.isArray(value) ? value.map(asObject) : []);

export const rowsOf = (body: JsonObject, key: string): SchoolRow[] => asRows(body[key]);
