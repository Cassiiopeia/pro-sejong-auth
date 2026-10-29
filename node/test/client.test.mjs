// 클라이언트 흐름: 입력 검증, 401, SIS 실패 무시, 타임아웃, 재시도 (네트워크 없음)
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { ErrorCode, SejongAuthClient, SejongAuthError } from '../dist/esm/index.js';
import { TimeoutError } from '../dist/esm/transport.js';
import {
  CLASSIC_SSO, CLASSIC_STATUS, LOGIN, PORTAL_MAIN, SIS_SSO, SIS_USER_INFO, fakeSessionFactory, ok, readFixture, status,
} from './helpers.mjs';

const dhcHtml = readFixture('dhc_status.html');
const sisJson = readFixture('sis_user_info.json');
const portalMainHtml = readFixture('portal_main.html');

function clientWith(responsesByUrlPrefix, options = {}) {
  const { factory, sessions } = fakeSessionFactory(responsesByUrlPrefix);
  return { client: new SejongAuthClient({ ...options, _sessionFactory: factory }), sessions };
}

async function rejectsWithCode(promise, code) {
  await assert.rejects(promise, (error) => error instanceof SejongAuthError && error.code === code);
}

test('emptyInputRaisesInvalidInputWithoutNetwork', async () => {
  for (const [studentId, password] of [['', 'pw'], ['21011234', ''], ['   ', 'pw'], [undefined, 'pw']]) {
    const { client, sessions } = clientWith({});
    await rejectsWithCode(client.authenticate(studentId, password), ErrorCode.INVALID_INPUT);
    assert.equal(sessions.length, 0);
  }
});

test('wrongPassword401RaisesAuthenticationFailed', async () => {
  const { client } = clientWith({ [LOGIN]: [ok()], [CLASSIC_SSO]: [ok()], [CLASSIC_STATUS]: [status(401)] });
  await rejectsWithCode(client.authenticateDhc('21011234', 'wrong'), ErrorCode.AUTHENTICATION_FAILED);
});

test('integratedSisFailureReturnsDhcWithNullContacts', async () => {
  const { client } = clientWith({
    [LOGIN]: [ok()], [CLASSIC_SSO]: [ok()], [CLASSIC_STATUS]: [ok(dhcHtml)], [SIS_SSO]: [ok()], [SIS_USER_INFO]: [status(500)],
  });
  const result = await client.authenticate('21011234', 'pw');
  assert.equal(result.success, true);
  assert.equal(result.name, '홍길동');
  assert.deepEqual([result.email, result.phoneNumber, result.englishName, result.userCategory], [null, null, null, null]);
});

test('integratedSuccessMergesDhcAndSis', async () => {
  const { client, sessions } = clientWith({
    [LOGIN]: [ok()], [CLASSIC_SSO]: [ok()], [CLASSIC_STATUS]: [ok(dhcHtml)], [SIS_SSO]: [ok()], [SIS_USER_INFO]: [ok(sisJson)],
  });
  const result = await client.authenticate('21011234', 'pw');
  // 포털 로그인 1회, 세션 1개로 DHC·SIS 모두 조회
  assert.equal(sessions.length, 1);
  assert.equal(sessions[0].requestedUrls.filter((url) => url === LOGIN).length, 1);
  assert.equal(result.grade, '3');
  assert.equal(result.classicReading.certifications[2].area, '동·서양의 문학 (3권)');
  assert.equal(result.phoneNumber, '010-1234-5678');
  assert.equal(result.userCategory, '학부생');
  assert.equal(result.classicReading.completedSemesters, '6 학기');
  assert.equal(result.classicReading.certificationStatus, '아니오');
  assert.ok(result.authenticatedAt instanceof Date);
});

test('timeoutRaisesConnectionTimeout', async () => {
  const { client } = clientWith({ [LOGIN]: [new TimeoutError('timed out')] }, { maxRetry: 3 });
  await rejectsWithCode(client.authenticateDhc('21011234', 'pw'), ErrorCode.CONNECTION_TIMEOUT);
});

test('retryExhaustedRaisesConnectionFailed', async () => {
  const { client, sessions } = clientWith({ [LOGIN]: [status(500)] }, { maxRetry: 3 });
  await rejectsWithCode(client.authenticateDhc('21011234', 'pw'), ErrorCode.CONNECTION_FAILED);
  assert.equal(sessions[0].requestedUrls.filter((url) => url === LOGIN).length, 3);
});

test('retryThenSuccess', async () => {
  const { client } = clientWith({ [LOGIN]: [status(503), ok()], [CLASSIC_SSO]: [ok()], [CLASSIC_STATUS]: [ok(dhcHtml)] });
  assert.equal((await client.authenticateDhc('21011234', 'pw')).studentId, '21011234');
});

test('sisResultHasEmptyGradeStatusAndRawJson', async () => {
  const { client } = clientWith({ [LOGIN]: [ok()], [SIS_SSO]: [ok()], [SIS_USER_INFO]: [ok(sisJson)] });
  const result = await client.authenticateSis('21011234', 'pw', { includeRaw: true });
  assert.deepEqual([result.grade, result.status], ['', '']);
  assert.equal(result.rawJson, sisJson);
});

test('dhcRawHtmlOnlyWhenRequested', async () => {
  const { client } = clientWith({ [LOGIN]: [ok()], [CLASSIC_SSO]: [ok()], [CLASSIC_STATUS]: [ok(dhcHtml)] });
  assert.equal((await client.authenticateDhc('21011234', 'pw')).rawHtml, null);
  assert.equal((await client.authenticateDhc('21011234', 'pw', { includeRaw: true })).rawHtml, dhcHtml);
});

test('errorMessageNeverContainsPassword', async () => {
  const { client } = clientWith({ [LOGIN]: [new Error('socket hang up')] });
  await assert.rejects(client.authenticateDhc('21011234', 'secret-password-value'), (error) =>
    error.code === ErrorCode.CONNECTION_FAILED && !String(error.message).includes('secret-password-value'));
});

test('sisWrongPasswordSubmitErrorRaisesAuthenticationFailed', async () => {
  // 틀린 비밀번호: initUserInfo 가 200 + _SUBMIT_ERROR_(ERRTYPE LOGOUT) 를 돌려줌
  const { client } = clientWith({ [LOGIN]: [ok()], [SIS_SSO]: [ok()], [SIS_USER_INFO]: [ok(readFixture('sis_submit_error_logout.json'))] });
  await rejectsWithCode(client.authenticateSis('21011234', 'wrong'), ErrorCode.AUTHENTICATION_FAILED);
});

test('wrongPasswordLoginResultStopsBeforeSso', async () => {
  // 포털 판정 erridpwd > SSO·데이터 요청 없이 즉시 실패 (불필요한 요청 0)
  const { client, sessions } = clientWith({ [LOGIN]: [ok(readFixture('portal_login_erridpwd.html'))] });
  await rejectsWithCode(client.authenticate('21011234', 'wrong'), ErrorCode.AUTHENTICATION_FAILED);
  assert.deepEqual(sessions[0].requestedUrls, [LOGIN]);
});

test('lockedAccountRaisesAccountLockedWithoutRetry', async () => {
  const lockedBody = readFixture('portal_login_erridpwd.html').replace("'erridpwd'", "'pwdNeedChg'");
  const { client, sessions } = clientWith({ [LOGIN]: [ok(lockedBody)] });
  await rejectsWithCode(client.authenticateSis('21011234', 'pw'), ErrorCode.ACCOUNT_LOCKED);
  assert.deepEqual(sessions[0].requestedUrls, [LOGIN]);
});

function integratedClientWith(portalMainOutcome) {
  return clientWith({
    [LOGIN]: [ok()], [CLASSIC_SSO]: [ok()], [CLASSIC_STATUS]: [ok(dhcHtml)], [SIS_SSO]: [ok()], [SIS_USER_INFO]: [ok(sisJson)],
    [PORTAL_MAIN]: [portalMainOutcome],
  });
}

test('integratedIncludesParentUnitNameFromPortalMain', async () => {
  const { client, sessions } = integratedClientWith(ok(portalMainHtml));
  const result = await client.authenticate('21011234', 'pw');
  assert.equal(result.parentUnitName, '공과대학');
  assert.equal(sessions[0].requestedUrls.filter((url) => url === PORTAL_MAIN).length, 1);
  assert.equal(sessions[0].requestedUrls.filter((url) => url === LOGIN).length, 1);
});

const PORTAL_MAIN_FAILURES = {
  timeout: new TimeoutError(),
  networkError: new Error('connection reset'),
  http500: status(500),
  http401: status(401),
  emptyBody: ok(''),
  loginPageRedirect: ok("<html><form action='/jsp/login/login_action.jsp'>통합 로그인</form></html>"),
};

for (const [caseName, outcome] of Object.entries(PORTAL_MAIN_FAILURES)) {
  test(`portalMainCallFailureIsNullAndKeepsOtherFields_${caseName}`, async () => {
    const { client, sessions } = integratedClientWith(outcome);
    const result = await client.authenticate('21011234', 'pw');
    assert.equal(result.parentUnitName, null);
    assert.deepEqual([result.email, result.userCategory, result.name], ['student@example.com', '학부생', '홍길동']);
    assert.equal(result.classicReading.completedSemesters, '6 학기');
    // 보조 조회는 재시도 없이 1회
    assert.equal(sessions[0].requestedUrls.filter((url) => url === PORTAL_MAIN).length, 1);
  });
}

test('portalMainWithoutMyInfoIsEmptyStringNotNull', async () => {
  const { client } = integratedClientWith(ok('<html><body>교직원 화면</body></html>'));
  assert.equal((await client.authenticate('21011234', 'pw')).parentUnitName, '');
});
