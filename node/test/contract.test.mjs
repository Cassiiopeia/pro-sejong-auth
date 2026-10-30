// 세 언어 공통 값: spec/contract.json <-> Node 상수 (어긋나면 실패)
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import * as client from '../dist/esm/auth/client.js';
import * as parsers from '../dist/esm/auth/parsers.js';
import * as transport from '../dist/esm/common/transport.js';
import { DEFAULT_MESSAGE_BY_CODE } from '../dist/esm/common/errors.js';
import * as academicSource from '../dist/esm/publicData/academicSource.js';
import * as cafeteriaSource from '../dist/esm/publicData/cafeteriaSource.js';
import * as calendarSource from '../dist/esm/publicData/calendarSource.js';
import * as campusSource from '../dist/esm/publicData/campusSource.js';
import * as dormitorySource from '../dist/esm/publicData/dormitorySource.js';
import * as extracurricularSource from '../dist/esm/publicData/extracurricularSource.js';
import * as publicDataFetcher from '../dist/esm/publicData/fetcher.js';

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
    [publicDataFetcher.JSON_ACCEPT, publicDataFetcher.FORM_CONTENT_TYPE, publicDataFetcher.FORM_REQUESTED_WITH, publicDataFetcher.SUBMIT_ERROR_KEY],
    [contractValues.jsonAccept, contractValues.formContentType, contractValues.formRequestedWith, contract.sis.submitErrorKey]);
});

test('publicDataSisTimetableValuesMatchContract', () => {
  const { sisTimetable, curriculum } = contract.publicData;
  assert.deepEqual(
    [academicSource.SIS_BASE_URL, academicSource.SIS_CONTENT_TYPE, academicSource.SIS_SEARCH_KEY],
    [sisTimetable.baseUrl, sisTimetable.contentType, sisTimetable.searchKey]);
  assert.deepEqual(
    [academicSource.SIS_ONLOAD_ENDPOINT, academicSource.SIS_TERM_LIST_ENDPOINT, academicSource.SIS_DEPARTMENT_LIST_ENDPOINT, academicSource.SIS_LECTURE_LIST_ENDPOINT],
    [sisTimetable.onloadEndpoint, sisTimetable.termListEndpoint, sisTimetable.departmentListEndpoint, sisTimetable.lectureListEndpoint]);
  assert.deepEqual(academicSource.LECTURE_EMPTY_FILTER_KEYS, sisTimetable.lectureEmptyFilterKeys);
  assert.equal(academicSource.CURRICULUM_COURSE_URL, curriculum.url);
  assert.deepEqual(academicSource.CURRICULUM_QUERY_KEYS, curriculum.queryKeys);
});

test('publicDataCalendarValuesMatchContract', () => {
  const { calendar, graduateCalendar } = contract.publicData;
  assert.deepEqual(
    [calendarSource.ACADEMIC_CALENDAR_URL, calendarSource.CALENDAR_MODE, calendarSource.DEFAULT_CALENDAR_CATEGORY_CODE],
    [calendar.url, calendar.mode, calendar.defaultCategoryCode]);
  assert.deepEqual(calendarSource.CALENDAR_QUERY_KEYS, calendar.queryKeys);
  assert.equal(calendarSource.GRADUATE_CALENDAR_URL, graduateCalendar.urlTemplate);
  assert.deepEqual(calendarSource.GRADUATE_CALENDAR_QUERY_KEYS, graduateCalendar.queryKeys);
  assert.equal(calendarSource.GRADUATE_SITE_ID_PATTERN_SOURCE, graduateCalendar.siteIdPattern);
});

test('publicDataDormitoryValuesMatchContract', () => {
  const { dormitory } = contract.publicData;
  assert.deepEqual(
    [dormitorySource.DORMITORY_MENU_URL, dormitorySource.DORMITORY_NOTICE_URL, dormitorySource.DEFAULT_DORMITORY_LOCATION,
      dormitorySource.DORMITORY_NOTICE_BOARD_ID, dormitorySource.DORMITORY_NOTICE_SEARCH_TYPE],
    [dormitory.menuUrl, dormitory.noticeUrl, dormitory.defaultLocation, dormitory.noticeBoardId, dormitory.noticeSearchType]);
  assert.deepEqual(dormitorySource.DORMITORY_MENU_FORM_KEYS, dormitory.menuFormKeys);
  assert.deepEqual(dormitorySource.DORMITORY_NOTICE_FORM_KEYS, dormitory.noticeFormKeys);
});

test('publicDataCafeteriaCampusExtracurricularValuesMatchContract', () => {
  const { cafeteria, campusMap, extracurricular } = contract.publicData;
  assert.deepEqual(
    [cafeteriaSource.CAFETERIA_URL, cafeteriaSource.CAFETERIA_MENU_MODE, cafeteriaSource.CAFETERIA_DEFAULT_MAX_PLACE_ID, cafeteriaSource.CAFETERIA_LIMIT_MAX_PLACE_ID],
    [cafeteria.url, cafeteria.menuMode, cafeteria.defaultMaxPlaceId, cafeteria.limitMaxPlaceId]);
  assert.deepEqual(cafeteriaSource.CAFETERIA_QUERY_KEYS, cafeteria.queryKeys);
  assert.deepEqual([campusSource.CAMPUS_MAP_URL, campusSource.CAMPUS_PLACE_LIST_MODE], [campusMap.url, campusMap.mode]);
  assert.deepEqual(campusSource.CAMPUS_MAP_QUERY_KEYS, campusMap.queryKeys);
  assert.deepEqual([extracurricularSource.EXTRACURRICULAR_URL, extracurricularSource.EXTRACURRICULAR_CID], [extracurricular.url, extracurricular.cid]);
  assert.deepEqual(extracurricularSource.EXTRACURRICULAR_FORM_KEYS, extracurricular.formKeys);
});
