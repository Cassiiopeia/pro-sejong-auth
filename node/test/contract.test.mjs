// 세 언어 공통 값: spec/contract.json <-> Node 상수 (어긋나면 실패)
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import * as client from '../dist/esm/auth/client.js';
import * as parsers from '../dist/esm/auth/parsers.js';
import * as transport from '../dist/esm/common/transport.js';
import { DEFAULT_MESSAGE_BY_CODE } from '../dist/esm/common/errors.js';
import * as publicData from '../dist/esm/publicData/client.js';

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

test('publicDataRequestBasicsMatchContract', () => {
  const contractValues = contract.publicData;
  assert.deepEqual(
    [publicData.JSON_ACCEPT, publicData.FORM_CONTENT_TYPE, publicData.FORM_REQUESTED_WITH, publicData.SIS_SUBMIT_ERROR_KEY],
    [contractValues.jsonAccept, contractValues.formContentType, contractValues.formRequestedWith, contract.sis.submitErrorKey]);
});

test('publicDataSisTimetableValuesMatchContract', () => {
  const { sisTimetable, curriculum } = contract.publicData;
  assert.deepEqual(
    [publicData.SIS_BASE_URL, publicData.SIS_CONTENT_TYPE, publicData.SIS_SEARCH_KEY],
    [sisTimetable.baseUrl, sisTimetable.contentType, sisTimetable.searchKey]);
  assert.deepEqual(
    [publicData.SIS_ONLOAD_ENDPOINT, publicData.SIS_TERM_LIST_ENDPOINT, publicData.SIS_DEPARTMENT_LIST_ENDPOINT, publicData.SIS_LECTURE_LIST_ENDPOINT],
    [sisTimetable.onloadEndpoint, sisTimetable.termListEndpoint, sisTimetable.departmentListEndpoint, sisTimetable.lectureListEndpoint]);
  assert.deepEqual(publicData.LECTURE_EMPTY_FILTER_KEYS, sisTimetable.lectureEmptyFilterKeys);
  assert.equal(publicData.CURRICULUM_COURSE_URL, curriculum.url);
  assert.deepEqual(publicData.CURRICULUM_QUERY_KEYS, curriculum.queryKeys);
});

test('publicDataCalendarValuesMatchContract', () => {
  const { calendar, graduateCalendar } = contract.publicData;
  assert.deepEqual(
    [publicData.ACADEMIC_CALENDAR_URL, publicData.CALENDAR_MODE, publicData.DEFAULT_CALENDAR_CATEGORY_CODE],
    [calendar.url, calendar.mode, calendar.defaultCategoryCode]);
  assert.deepEqual(publicData.CALENDAR_QUERY_KEYS, calendar.queryKeys);
  assert.equal(publicData.GRADUATE_CALENDAR_URL, graduateCalendar.urlTemplate);
  assert.deepEqual(publicData.GRADUATE_CALENDAR_QUERY_KEYS, graduateCalendar.queryKeys);
  assert.equal(publicData.GRADUATE_SITE_ID_PATTERN_SOURCE, graduateCalendar.siteIdPattern);
});

test('publicDataDormitoryValuesMatchContract', () => {
  const { dormitory } = contract.publicData;
  assert.deepEqual(
    [publicData.DORMITORY_MENU_URL, publicData.DORMITORY_NOTICE_URL, publicData.DEFAULT_DORMITORY_LOCATION,
      publicData.DORMITORY_NOTICE_BOARD_ID, publicData.DORMITORY_NOTICE_SEARCH_TYPE],
    [dormitory.menuUrl, dormitory.noticeUrl, dormitory.defaultLocation, dormitory.noticeBoardId, dormitory.noticeSearchType]);
  assert.deepEqual(publicData.DORMITORY_MENU_FORM_KEYS, dormitory.menuFormKeys);
  assert.deepEqual(publicData.DORMITORY_NOTICE_FORM_KEYS, dormitory.noticeFormKeys);
});

test('publicDataCafeteriaCampusExtracurricularValuesMatchContract', () => {
  const { cafeteria, campusMap, extracurricular } = contract.publicData;
  assert.deepEqual(
    [publicData.CAFETERIA_URL, publicData.CAFETERIA_MENU_MODE, publicData.CAFETERIA_DEFAULT_MAX_PLACE_ID, publicData.CAFETERIA_LIMIT_MAX_PLACE_ID],
    [cafeteria.url, cafeteria.menuMode, cafeteria.defaultMaxPlaceId, cafeteria.limitMaxPlaceId]);
  assert.deepEqual(publicData.CAFETERIA_QUERY_KEYS, cafeteria.queryKeys);
  assert.deepEqual([publicData.CAMPUS_MAP_URL, publicData.CAMPUS_PLACE_LIST_MODE], [campusMap.url, campusMap.mode]);
  assert.deepEqual(publicData.CAMPUS_MAP_QUERY_KEYS, campusMap.queryKeys);
  assert.deepEqual([publicData.EXTRACURRICULAR_URL, publicData.EXTRACURRICULAR_CID], [extracurricular.url, extracurricular.cid]);
  assert.deepEqual(publicData.EXTRACURRICULAR_FORM_KEYS, extracurricular.formKeys);
});
