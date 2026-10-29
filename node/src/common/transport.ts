// HTTP 세션: 인증 1회당 쿠키 저장소 1개, 포털 레거시 TLS, 리다이렉트 자동 (Node 기본 모듈만)
import * as https from 'node:https';

import { VERSION } from './version.js';

// 포털은 TLS_RSA_WITH_AES_256_CBC_SHA 만 제공 > 기본 cipher 목록에 추가 (spec/PORTAL_FLOW.md)
export const PORTAL_CIPHERS = 'DEFAULT:AES256-SHA:@SECLEVEL=1';
// 모든 요청이 쓰는 식별 User-Agent (기본 에이전트는 포털 방화벽이 차단)
export const USER_AGENT = `pro-sejongpeia-node/${VERSION}`;
const REDIRECT_STATUS_CODES = new Set([301, 302, 303, 307, 308]);
const MAX_REDIRECTS = 10;

export interface HttpResponse {
  status: number;
  body: Buffer;
}

export interface RequestOptions {
  body?: string;
  headers?: Record<string, string>;
}

export interface Session {
  request(method: string, url: string, options?: RequestOptions): Promise<HttpResponse>;
}

/** 요청 제한 시간 초과 (클라이언트가 CONNECTION_TIMEOUT 등으로 바꿈) */
export class TimeoutError extends Error {
  constructor(message = 'Request timed out') {
    super(message);
    this.name = 'TimeoutError';
  }
}

interface StoredCookie { value: string; domain: string; isHostOnly: boolean; path: string }

/** 최소 쿠키 저장소: Domain·Path 속성 반영, 만료는 인증 1회 안에서 무시 */
class CookieJar {
  private readonly cookies = new Map<string, StoredCookie>();

  store(setCookieHeaders: string[] | undefined, requestUrl: URL): void {
    for (const header of setCookieHeaders ?? []) {
      const [pair, ...attributes] = header.split(';');
      const separatorIndex = pair.indexOf('=');
      if (separatorIndex <= 0) continue;
      const name = pair.slice(0, separatorIndex).trim();
      const attributeValue = (key: string) =>
        attributes.map((attribute) => attribute.trim()).find((attribute) => attribute.toLowerCase().startsWith(`${key}=`))?.split('=')[1];
      const domainAttribute = attributeValue('domain')?.replace(/^\./, '').toLowerCase();
      const domain = domainAttribute || requestUrl.hostname;
      this.cookies.set(`${domain}|${name}`, {
        value: pair.slice(separatorIndex + 1).trim(),
        domain,
        isHostOnly: !domainAttribute,
        path: attributeValue('path') || '/',
      });
    }
  }

  headerFor(url: URL): string {
    const host = url.hostname.toLowerCase();
    return [...this.cookies.entries()]
      .filter(([, cookie]) => (cookie.isHostOnly ? host === cookie.domain : host === cookie.domain || host.endsWith(`.${cookie.domain}`)))
      .filter(([, cookie]) => url.pathname.startsWith(cookie.path))
      .map(([key, cookie]) => `${key.split('|')[1]}=${cookie.value}`)
      .join('; ');
  }
}

/** 요청 1건: 4xx·5xx 도 응답으로 돌려줌, 리다이렉트는 GET 으로 따라감, 시간 초과는 TimeoutError */
export class NodeHttpsSession implements Session {
  private readonly cookieJar = new CookieJar();
  private readonly agent: https.Agent;

  constructor(private readonly timeoutSeconds: number, sslVerification: boolean, private readonly userAgent: string) {
    this.agent = new https.Agent({ ciphers: PORTAL_CIPHERS, rejectUnauthorized: sslVerification, keepAlive: false });
  }

  async request(method: string, url: string, options: RequestOptions = {}): Promise<HttpResponse> {
    let currentMethod = method;
    let currentUrl = new URL(url);
    let currentOptions = options;
    for (let redirectCount = 0; ; redirectCount += 1) {
      const response = await this.send(currentMethod, currentUrl, currentOptions);
      if (!REDIRECT_STATUS_CODES.has(response.status) || !response.location || redirectCount >= MAX_REDIRECTS) {
        return { status: response.status, body: response.body };
      }
      currentUrl = new URL(response.location, currentUrl);
      if (response.status !== 307 && response.status !== 308) {
        currentMethod = 'GET';
        currentOptions = {};
      }
    }
  }

  private send(method: string, url: URL, options: RequestOptions): Promise<HttpResponse & { location?: string }> {
    return new Promise((resolve, reject) => {
      // 포털은 User-Agent 없으면 SSO 이후 404, 기본 에이전트는 방화벽 차단
      const headers: Record<string, string> = { 'User-Agent': this.userAgent, ...options.headers };
      const cookieHeader = this.cookieJar.headerFor(url);
      if (cookieHeader) headers.Cookie = headers.Cookie ? `${headers.Cookie}; ${cookieHeader}` : cookieHeader;
      if (options.body !== undefined) headers['Content-Length'] = String(Buffer.byteLength(options.body));

      const request = https.request(url, { method, headers, agent: this.agent }, (response) => {
        this.cookieJar.store(response.headers['set-cookie'], url);
        const chunks: Buffer[] = [];
        response.on('data', (chunk: Buffer) => chunks.push(chunk));
        response.on('end', () => resolve({
          status: response.statusCode ?? 0,
          body: Buffer.concat(chunks),
          location: response.headers.location,
        }));
        response.on('error', reject);
      });
      request.setTimeout(this.timeoutSeconds * 1000, () => request.destroy(new TimeoutError()));
      request.on('error', reject);
      if (options.body !== undefined) request.write(options.body);
      request.end();
    });
  }
}
