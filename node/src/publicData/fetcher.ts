// 공개 데이터 요청 공통: 요청 > JSON 검증, 메모리 캐시, 실패 격리 (모든 소스가 공유)
// 같은 키 동시 호출은 요청 1건 공유, 실패는 마지막 캐시(만료 포함) 또는 빈 값
import { NodeHttpsSession, USER_AGENT, type Session } from '../common/transport.js';
import { isObject, type JsonObject } from './rowReader.js';

// 주소·요청 키·고정 값의 원본은 spec/contract.json 의 publicData (test/contract.test.mjs 가 일치 검사)
export const JSON_ACCEPT = 'application/json';
export const FORM_CONTENT_TYPE = 'application/x-www-form-urlencoded; charset=UTF-8';
export const FORM_REQUESTED_WITH = 'XMLHttpRequest';
export const SUBMIT_ERROR_KEY = '_SUBMIT_ERROR_';

// GET 도 Accept 가 없으면 학교(교육과정)가 404 를 돌려줌
const JSON_ACCEPT_HEADERS = { Accept: JSON_ACCEPT };
const FORM_HEADERS = { 'Content-Type': FORM_CONTENT_TYPE, 'X-Requested-With': FORM_REQUESTED_WITH };

// 요청 본문: 폼 또는 JSON 문자열 (없으면 GET)
export type RequestBody = { form: URLSearchParams } | { json: string; contentType: string };

// isFailure: 캐시 없이 실패해 빈 값을 잠시 기억하는 항목
interface CacheEntry { value: unknown; storedAtMilliseconds: number; isFailure: boolean }

export interface PublicDataFetcherOptions {
  timeoutSeconds: number;
  cacheTtlSeconds: number;
  failureCacheSeconds: number;
  cacheMaxEntries: number;
  sslVerification: boolean;
  sessionFactory?: () => Session;
  nowMilliseconds?: () => number;
}

// 요청 키 목록 + 값 > 보낼 순서 그대로의 키-값 쌍
export const pairsOf = (keys: string[], ...values: string[]): URLSearchParams =>
  new URLSearchParams(keys.map((key, index): [string, string] => [key, values[index]]));

export class PublicDataFetcher {
  private readonly cacheTtlSeconds: number;
  private readonly failureCacheSeconds: number;
  private readonly cacheMaxEntries: number;
  private readonly sessionFactory: () => Session;
  private readonly nowMilliseconds: () => number;
  private session?: Session;
  private readonly cacheByKey = new Map<string, CacheEntry>();
  private readonly pendingByKey = new Map<string, Promise<unknown>>();

  constructor(options: PublicDataFetcherOptions) {
    this.cacheTtlSeconds = options.cacheTtlSeconds;
    this.failureCacheSeconds = options.failureCacheSeconds;
    this.cacheMaxEntries = options.cacheMaxEntries;
    this.sessionFactory = options.sessionFactory ?? (() => new NodeHttpsSession(options.timeoutSeconds, options.sslVerification, USER_AGENT));
    this.nowMilliseconds = options.nowMilliseconds ?? Date.now;
  }

  // 캐시 확인 > 같은 키 진행 중 요청 공유 > 요청·파싱 > 캐시 저장, 실패 시 마지막 캐시 또는 빈 값(캐시 없으면 잠시 기억)
  fetchCached<T>(methodName: string, cacheArguments: unknown[], createEmptyValue: () => T, request: () => Promise<JsonObject>, parse: (body: JsonObject) => T): Promise<T> {
    const cacheKey = `${methodName}|${JSON.stringify(cacheArguments)}`;
    const cachedEntry = this.cacheByKey.get(cacheKey);
    if (cachedEntry) {
      const validSeconds = cachedEntry.isFailure ? this.failureCacheSeconds : this.cacheTtlSeconds;
      if (this.nowMilliseconds() - cachedEntry.storedAtMilliseconds < validSeconds * 1000) {
        return Promise.resolve(structuredClone(cachedEntry.value) as T);
      }
    }
    const pendingRequest = this.pendingByKey.get(cacheKey);
    if (pendingRequest) return pendingRequest.then((value) => structuredClone(value) as T);

    // 만료된 실패 기억은 마지막 값이 아님
    const lastValueEntry = cachedEntry && !cachedEntry.isFailure ? cachedEntry : undefined;
    let newRequest: Promise<T> | undefined;
    newRequest = (async (): Promise<T> => {
      try {
        const value = parse(await request());
        this.storeEntry(cacheKey, { value, storedAtMilliseconds: this.nowMilliseconds(), isFailure: false });
        return value;
      } catch {
        // 업스트림 실패는 내부 정보 없이 마지막 캐시(만료 포함) 또는 빈 값
        if (lastValueEntry) return lastValueEntry.value as T;
        const emptyValue = createEmptyValue();
        this.storeEntry(cacheKey, { value: emptyValue, storedAtMilliseconds: this.nowMilliseconds(), isFailure: true });
        return emptyValue;
      } finally {
        if (this.pendingByKey.get(cacheKey) === newRequest) this.pendingByKey.delete(cacheKey);
      }
    })();
    this.pendingByKey.set(cacheKey, newRequest);
    return newRequest.then((value) => structuredClone(value) as T);
  }

  // 요청 1건: 2xx 아님, JSON 아님, _SUBMIT_ERROR_ 는 예외 (호출부가 실패 격리)
  async requestJson(method: string, url: string, body?: RequestBody): Promise<JsonObject> {
    this.session ??= this.sessionFactory();
    const response = await this.session.request(method, url, {
      headers: body === undefined ? JSON_ACCEPT_HEADERS : 'form' in body ? FORM_HEADERS : { 'Content-Type': body.contentType, Accept: JSON_ACCEPT },
      body: body === undefined ? undefined : 'form' in body ? body.form.toString() : body.json,
    });
    if (response.status < 200 || response.status >= 300) throw new Error(`status ${response.status}`);
    // happydorm 은 JSON 을 text/html 로 보내므로 항상 본문을 JSON 으로 파싱
    const parsed: unknown = JSON.parse(response.body.toString('utf8'));
    if (!isObject(parsed)) throw new Error('expected object');
    if (SUBMIT_ERROR_KEY in parsed) throw new Error('school reported an error');
    return parsed;
  }

  // 캐시 저장 (가득 차면 가장 오래된 항목과 그 진행 중 요청 제거)
  private storeEntry(cacheKey: string, entry: CacheEntry): void {
    this.cacheByKey.delete(cacheKey);
    this.cacheByKey.set(cacheKey, entry);
    while (this.cacheByKey.size > this.cacheMaxEntries) {
      const oldestKey = this.cacheByKey.keys().next().value as string;
      this.cacheByKey.delete(oldestKey);
      this.pendingByKey.delete(oldestKey);
    }
  }
}
