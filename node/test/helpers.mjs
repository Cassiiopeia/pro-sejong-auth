// 테스트 공통: spec/fixtures 읽기, 네트워크 없는 가짜 세션
import { readFileSync } from 'node:fs';

const FIXTURE_DIRECTORY = new URL('../../spec/fixtures/', import.meta.url);

export const readFixture = (fileName) => readFileSync(new URL(fileName, FIXTURE_DIRECTORY), 'utf8');
export const readExpected = (fileName) => JSON.parse(readFixture(fileName));

export const ok = (body = '') => ({ status: 200, body: Buffer.from(body, 'utf8') });
export const status = (code) => ({ status: code, body: Buffer.alloc(0) });

export const LOGIN = 'https://portal.sejong.ac.kr/jsp/login/login_action.jsp';
export const CLASSIC_SSO = 'https://classic.sejong.ac.kr/_custom/sejong/sso/sso-return.jsp';
export const CLASSIC_STATUS = 'https://classic.sejong.ac.kr/classic/reading/status.do';
export const SIS_SSO = 'https://sjpt.sejong.ac.kr/main/view/Login/doSsoLogin.do';
export const SIS_USER_INFO = 'https://sjpt.sejong.ac.kr/main/sys/UserInfo/initUserInfo.do';
export const PORTAL_MAIN = 'https://portal.sejong.ac.kr/user/index.do';

// URL 앞부분 > 응답 목록(차례로 소비, 마지막은 반복). 값이 Error 면 reject
export function fakeSessionFactory(responsesByUrlPrefix) {
  const sessions = [];
  const factory = () => {
    const queues = Object.fromEntries(Object.entries(responsesByUrlPrefix).map(([prefix, list]) => [prefix, [...list]]));
    const session = {
      requestedUrls: [],
      async request(method, url) {
        session.requestedUrls.push(url);
        const prefix = Object.keys(queues).find((candidate) => url.startsWith(candidate));
        if (!prefix) throw new Error(`unexpected request: ${method} ${url}`);
        const queue = queues[prefix];
        const outcome = queue.length > 1 ? queue.shift() : queue[0];
        if (outcome instanceof Error) throw outcome;
        return outcome;
      },
    };
    sessions.push(session);
    return session;
  };
  return { factory, sessions };
}
