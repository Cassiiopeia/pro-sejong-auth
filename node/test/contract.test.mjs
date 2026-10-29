// 세 언어 공통 값: spec/contract.json <-> Node 상수 (어긋나면 실패)
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import * as client from '../dist/esm/client.js';
import * as parsers from '../dist/esm/parsers.js';
import * as transport from '../dist/esm/transport.js';
import { DEFAULT_MESSAGE_BY_CODE } from '../dist/esm/errors.js';

const contract = JSON.parse(readFileSync(new URL('../../spec/contract.json', import.meta.url), 'utf8'));

test('portalValuesMatchContract', () => {
  assert.equal(client.PORTAL_LOGIN_URL, contract.portal.loginUrl);
  assert.equal(client.PORTAL_REFERER, contract.portal.referer);
  assert.equal(client.PORTAL_DEFAULT_COOKIE, contract.portal.defaultCookie);
  assert.equal(parsers.PORTAL_LOGIN_ACCEPTED, contract.portal.loginAcceptedResult);
  assert.deepEqual({ ...parsers.PORTAL_LOGIN_ERROR_CODE_BY_RESULT }, contract.portal.loginErrorCodeByResult);
});

test('portalMainValuesMatchContract', () => {
  assert.equal(client.PORTAL_MAIN_URL, contract.portalMain.url);
  assert.equal(client.PORTAL_LOGIN_PAGE_MARKER, contract.portalMain.loginPageMarker);
});

test('classicAndSisValuesMatchContract', () => {
  assert.equal(client.CLASSIC_RETURN_URL, contract.classic.returnUrl);
  assert.equal(client.CLASSIC_SSO_URL, contract.classic.ssoUrl);
  assert.equal(client.CLASSIC_STATUS_URL, contract.classic.statusUrl);
  assert.equal(client.SIS_RETURN_URL, contract.sis.returnUrl);
  assert.equal(client.SIS_SSO_URL, contract.sis.ssoUrl);
  assert.equal(client.SIS_USER_INFO_URL, contract.sis.userInfoUrl);
  assert.equal(client.SIS_ORIGIN, contract.sis.origin);
  assert.equal(parsers.SIS_SUBMIT_ERROR_KEY, contract.sis.submitErrorKey);
  assert.equal(parsers.SIS_LOGGED_OUT_ERROR_TYPE, contract.sis.loggedOutErrorType);
  assert.equal(parsers.SIS_EXTRA_EXCLUDED_KEY_PATTERN, contract.sis.extraExcludedKeyPattern);
});

test('sisScheduleValuesMatchContract', () => {
  const schedule = contract.sisSchedule;
  assert.deepEqual(
    [client.SIS_MENU_LIST_URL, client.SIS_USER_ROLE_URL, client.SIS_SCHEDULE_LIST_URL, client.SIS_SCHEDULE_PROGRAM_KEY, client.SIS_SCHEDULE_SYSTEM_KEY, client.SIS_MENU_SYSTEM_DIV],
    [schedule.menuListUrl, schedule.userRoleUrl, schedule.listUrl, schedule.programKey, schedule.systemKey, schedule.menuSystemDiv]);
});

test('tlsAndErrorMessagesMatchContract', () => {
  assert.equal(transport.PORTAL_CIPHERS, contract.opensslCiphers);
  assert.deepEqual({ ...DEFAULT_MESSAGE_BY_CODE }, contract.errorMessageByCode);
});
