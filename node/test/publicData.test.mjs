// 공개 데이터: spec/fixtures/public_data 기대값 일치, 요청 형태, 캐시, 실패 격리 (학교 서버 호출 없음)
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { SejongPublicData } from '../dist/esm/publicData.js';
import { ErrorCode, SejongAuthError } from '../dist/esm/errors.js';
import { readFixture } from './helpers.mjs';

const readPublicFixture = (fileName) => readFixture(`public_data/${fileName}`);
const toCamelCase = (name) => name.replace(/_([a-z])/g, (_, letter) => letter.toUpperCase());
// expected(snake_case) > camelCase, extra 안의 키는 학교 원문이라 변환하지 않음
const camelizeKeys = (value) => {
  if (Array.isArray(value)) return value.map(camelizeKeys);
  if (value === null || typeof value !== 'object') return value;
  return Object.fromEntries(Object.entries(value).map(([key, inner]) => [toCamelCase(key), key === 'extra' ? inner : camelizeKeys(inner)]));
};
const expected = camelizeKeys(JSON.parse(readPublicFixture('expected_public_data.json')));

const SIS = 'https://sjpt.sejong.ac.kr/sch/sch/ext/SueOpenTimeQ/';
const HOMEPAGE = 'https://www.sejong.ac.kr/kor/';
const DORMITORY = 'https://happydorm.sejong.ac.kr/';

// URL 앞부분 > fixture 파일명 (SIS doOnload 는 세 소스가 공유)
const fixtureNameByUrlPrefix = {
  [`${SIS}doOnload.do`]: 'reference_onload.json',
  [`${SIS}doYearsmtlist.do`]: 'reference_terms.json',
  [`${SIS}doLesndeptgroup.do`]: 'reference_departments.json',
  [`${SIS}doList.do`]: 'lectures.json',
  'https://www.sejong.ac.kr/curriculum/getCurriculumOneKor.do': 'curriculum_course.json',
  [`${HOMEPAGE}academics/academic-calendar.do`]: 'calendar.json',
  'https://graduate.sejong.ac.kr/': 'graduate_calendar.json',
  [`${DORMITORY}food/indexFoodList.do`]: 'dormitory_menu.json',
  [`${DORMITORY}bbs/getBbsList.do`]: 'dormitory_notices.json',
  [`${HOMEPAGE}unilife/cafeteria-info.do`]: 'cafeteria.json',
  [`${HOMEPAGE}intro/campus-map.do`]: 'campus_places.json',
  'https://do.sejong.ac.kr/ko/process/schedule/getEvents': 'extracurricular_events.json',
};

// 요청 기록 + 응답 교체 가능한 가짜 세션 (outcome: { status, body } 또는 Error)
function createPublicData({ options = {}, outcome, delayMilliseconds = 0 } = {}) {
  const requests = [];
  const clock = { now: 1_000_000 };
  const session = {
    async request(method, url, requestOptions = {}) {
      requests.push({ method, url, headers: requestOptions.headers, body: requestOptions.body });
      if (delayMilliseconds) await new Promise((resolve) => setTimeout(resolve, delayMilliseconds));
      const current = typeof outcome === 'function' ? outcome() : outcome;
      if (current instanceof Error) throw current;
      if (current) return { status: current.status, body: Buffer.from(current.body, 'utf8') };
      const prefix = Object.keys(fixtureNameByUrlPrefix).find((candidate) => url.startsWith(candidate));
      return { status: 200, body: Buffer.from(readPublicFixture(fixtureNameByUrlPrefix[prefix]), 'utf8') };
    },
  };
  const publicData = new SejongPublicData({ ...options, _sessionFactory: () => session, _nowMilliseconds: () => clock.now });
  return { publicData, requests, clock };
}

const TERM = { categoryCode: '20', year: '2099', semesterCode: '20' };
const callBySource = {
  organizationCategories: (p) => p.organizationCategories(),
  terms: (p) => p.terms('20'),
  departments: (p) => p.departments(TERM),
  courseTypes: (p) => p.courseTypes(),
  selectiveDomains: (p) => p.selectiveDomains(),
  lectures: (p) => p.lectures({ ...TERM, departmentCode: '9101' }),
  curriculumCourses: (p) => p.curriculumCourses({ departmentCode: '9101', courseNumber: '900001' }),
  calendar: (p) => p.calendar({ start: '2099-10-01', end: '2099-10-31' }),
  graduateCalendar: (p) => p.graduateCalendar({ siteId: 'graduate', year: 2099, month: 10 }),
  dormitoryMenu: (p) => p.dormitoryMenu(),
  dormitoryNotices: (p) => p.dormitoryNotices(),
  cafeteriaMenu: (p) => p.cafeteriaMenu(1),
  campusPlaces: (p) => p.campusPlaces(),
  extracurricularEvents: (p) => p.extracurricularEvents({ start: '2099-10-01', end: '2099-10-31' }),
};

for (const [sourceName, call] of Object.entries(callBySource)) {
  test(`${sourceName} matches expected_public_data`, async () => {
    const { publicData } = createPublicData();
    const snakeName = sourceName.replace(/[A-Z]/g, (letter) => `_${letter.toLowerCase()}`);
    const expectedKey = toCamelCase(snakeName);
    assert.deepEqual(await call(publicData), expected[expectedKey]);
  });
}

test('extra never contains deny list fields', async () => {
  const { publicData } = createPublicData();
  const deniedKeys = ['CD_ALIAS1', 'CD_ALIAS2', 'CD_ALIAS3', 'CD_ALIAS1_ENG', 'regname', 'regid', 'STACK_TRACE'];
  const results = [await publicData.organizationCategories(), await publicData.lectures({ ...TERM, courseName: '예시' }), await publicData.departments(TERM)];
  for (const row of results.flat()) for (const key of deniedKeys) assert.ok(!(key in row.extra), key);
  assert.deepEqual((await publicData.organizationCategories())[0].extra, { PRT_ORD: '1' });
  assert.equal(JSON.stringify(await publicData.dormitoryNotices()).includes('regname'), false);
});

test('curriculumCourses request shape and input check', async () => {
  const { publicData, requests } = createPublicData();
  await publicData.curriculumCourses({ departmentCode: '9101', courseNumber: '900001' });
  assert.equal(requests[0].method, 'GET');
  assert.equal(requests[0].url, 'https://www.sejong.ac.kr/curriculum/getCurriculumOneKor.do?curiNo=900001&deptNo=9101');
  // Accept 가 없으면 학교가 404 를 돌려줌
  assert.equal(requests[0].headers.Accept, 'application/json');
  for (const query of [{ departmentCode: '', courseNumber: '900001' }, { departmentCode: '9101', courseNumber: '  ' }]) {
    assert.throws(() => publicData.curriculumCourses(query), (error) => error instanceof SejongAuthError && error.code === ErrorCode.INVALID_INPUT);
  }
  assert.equal(requests.length, 1);
  // 그 학과 교육과정에 없는 과목은 빈 items 로 오며 정상 응답
  const empty = createPublicData({ outcome: { status: 200, body: '{"success":true,"items":[]}' } });
  assert.deepEqual(await empty.publicData.curriculumCourses({ departmentCode: '9101', courseNumber: '000000' }), []);
});

test('request shapes follow the contract', async () => {
  const { publicData, requests } = createPublicData();
  await publicData.terms('20');
  await publicData.lectures({ ...TERM, departmentCode: '9101', courseName: '과목', instructorName: '교수' });
  await publicData.calendar({ categoryCode: '0002', start: '2099-10-01', end: '2099-10-31' });
  await publicData.dormitoryMenu('SJ');
  await publicData.dormitoryNotices({ page: 2, rows: 5, keyword: '공지' });
  await publicData.cafeteriaMenu(7);
  await publicData.campusPlaces('BUILDING');
  await publicData.extracurricularEvents({ start: '2098-12-01', end: '2099-01-31', category: 'a' });
  const [terms, lectures, calendar, menu, notices, cafeteria, places, events] = requests;

  assert.equal(terms.method, 'POST');
  assert.equal(terms.url, `${SIS}doYearsmtlist.do`);
  assert.deepEqual(JSON.parse(terms.body), { dm_search: { ORGN_CLSF_CD: '20' } });
  assert.equal(terms.headers['Content-Type'], 'application/json; charset="UTF-8"');
  assert.equal(terms.headers.Accept, 'application/json');

  // 학교가 필터 키 전체를 요구함 (하나라도 빠지면 _SUBMIT_ERROR_)
  assert.deepEqual(JSON.parse(lectures.body).dm_search, {
    SMT_NM: '', CURI_TYPE_CD: '', SLT_DOMAIN_CD: '', CURI_NO: '', CLASS: '', SLT_DEPT_CD: '', PLAN_REG_YN: '', CORS_SCHE_GRP_CD: '',
    ORGN_CLSF_CD: '20', YEAR: '2099', SMT_CD: '20', YEAR_SMT: '2099/20', DEPT_CD: '9101', CURI_NM: '과목', EMP_NM: '교수', LANG_CHK: 'KOR',
  });

  assert.equal(calendar.method, 'GET');
  assert.equal(calendar.url, `${HOMEPAGE}academics/academic-calendar.do?mode=getCalendarData&start=2099-10-01&end=2099-10-31&collDiv=0002`);

  assert.equal(menu.method, 'POST');
  assert.equal(menu.url, `${DORMITORY}food/indexFoodList.do`);
  assert.equal(menu.body, 'locgbn=SJ');
  assert.equal(menu.headers['Content-Type'], 'application/x-www-form-urlencoded; charset=UTF-8');
  assert.equal(menu.headers['X-Requested-With'], 'XMLHttpRequest');

  const noticeForm = new URLSearchParams(notices.body);
  assert.deepEqual(Object.fromEntries(noticeForm), { cPage: '2', rows: '5', bbs_locgbn: 'SJ', bbs_id: 'notice', sType: 'subject', sWord: '공지' });

  assert.equal(cafeteria.url, `${HOMEPAGE}unilife/cafeteria-info.do?mode=getMenuList&placeId=7`);
  assert.equal(places.url, `${HOMEPAGE}intro/campus-map.do?mode=getPlaceUserList&placeType=BUILDING`);

  assert.equal(events.url, 'https://do.sejong.ac.kr/ko/process/schedule/getEvents');
  assert.deepEqual(Object.fromEntries(new URLSearchParams(events.body)), {
    cid: 'eco', category: 'a', start: '2098-12-01', end: '2099-01-31', start_year: '2098', end_year: '2099',
  });
});

test('graduateCalendar request, input check and success flag', async () => {
  const { publicData, requests } = createPublicData();
  await publicData.graduateCalendar({ siteId: 'graduate', year: 2099, month: 10 });
  assert.equal(requests[0].method, 'GET');
  assert.equal(requests[0].url, 'https://graduate.sejong.ac.kr/_custom/sejong/_app/calendar-graduate/proc/getCalendarData.jsp?year=2099&month=10&siteId=graduate');
  for (const siteId of ['', '  ', 'evil.example.com/x', 'Graduate', undefined]) {
    assert.throws(() => publicData.graduateCalendar({ siteId, year: 2099, month: 10 }), (error) => error instanceof SejongAuthError && error.code === ErrorCode.INVALID_INPUT);
  }
  assert.equal(requests.length, 1);
  const failed = createPublicData({ outcome: () => ({ status: 200, body: '{"success":false,"items":"[]"}' }) });
  assert.deepEqual(await failed.publicData.graduateCalendar({ siteId: 'graduate', year: 2099, month: 1 }), []);
  const emptyMonth = createPublicData({ outcome: () => ({ status: 200, body: '{"success":true,"items":"[]"}' }) });
  await emptyMonth.publicData.graduateCalendar({ siteId: 'arts', year: 2099, month: 1 });
  await emptyMonth.publicData.graduateCalendar({ siteId: 'arts', year: 2099, month: 1 });
  assert.equal(emptyMonth.requests.length, 1);
});

test('cache: fresh entry makes no request, expired entry refreshes', async () => {
  const { publicData, requests, clock } = createPublicData({ options: { cacheTtlSeconds: 60 } });
  await publicData.courseTypes();
  clock.now += 59_000;
  await publicData.courseTypes();
  assert.equal(requests.length, 1);
  clock.now += 2_000;
  await publicData.courseTypes();
  assert.equal(requests.length, 2);
  await publicData.terms('20');
  await publicData.terms('30');
  assert.equal(requests.length, 4);
  // 호출자가 결과를 바꿔도 캐시는 그대로
  (await publicData.courseTypes())[0].name = 'changed';
  assert.equal((await publicData.courseTypes())[0].name, expected.courseTypes[0].name);
});

test('failure returns expired cache, or remembers failure briefly without cache', async () => {
  let currentOutcome;
  const { publicData, requests, clock } = createPublicData({ options: { cacheTtlSeconds: 10, failureCacheSeconds: 30 }, outcome: () => currentOutcome });
  const first = await publicData.cafeteriaMenu(1);
  clock.now += 20_000;
  currentOutcome = new Error('connection reset');
  assert.deepEqual(await publicData.cafeteriaMenu(1), first);
  assert.equal(requests.length, 2);
  // 캐시 없는 실패: 기억 시간 동안 요청 없이 빈 값, 지나면 재시도
  assert.deepEqual(await publicData.cafeteriaMenu(2), []);
  assert.deepEqual(await publicData.cafeteriaMenu(2), []);
  assert.equal(requests.length, 3);
  clock.now += 31_000;
  currentOutcome = undefined;
  assert.equal((await publicData.cafeteriaMenu(2)).length > 0, true);
  assert.equal(requests.length, 4);
});

const failureOutcomes = {
  'connection failure': () => new Error('ECONNREFUSED'),
  'server error status': () => ({ status: 500, body: '{"items":[]}' }),
  'not json': () => ({ status: 200, body: '<html>maintenance</html>' }),
  'expected key missing': () => ({ status: 200, body: '{"other":[]}' }),
  'submit error object': () => ({ status: 200, body: '{"_SUBMIT_ERROR_":{"STACK_TRACE":"internal secret"}}' }),
};

test('failure isolation: every failure kind returns an empty value', async () => {
  for (const [label, makeOutcome] of Object.entries(failureOutcomes)) {
    for (const [sourceName, call] of Object.entries(callBySource)) {
      const { publicData } = createPublicData({ outcome: makeOutcome });
      const emptyValue = sourceName === 'dormitoryNotices' ? { total: 0, pinned: [], items: [] } : [];
      assert.deepEqual(await call(publicData), emptyValue, `${label} / ${sourceName}`);
    }
  }
});

const okBody = (body) => () => ({ status: 200, body: JSON.stringify(body) });

test('rows with missing or null fields survive with empty defaults', async () => {
  const campus = createPublicData({ outcome: okBody({ items: [{ id: 1, latitude: '', longitude: null }, 'odd'] }) });
  assert.deepEqual((await campus.publicData.campusPlaces()).map((place) => [place.id, place.name, place.latitude, place.longitude]), [['1', '', 0, 0], ['', '', 0, 0]]);
  const calendar = createPublicData({ outcome: okBody({ data: [{}] }) });
  assert.deepEqual((await calendar.publicData.calendar({ start: 'a', end: 'b' }))[0].startDate, '');
});

test('dormitory notices without list parts mean no results', async () => {
  const { publicData } = createPublicData({ outcome: okBody({ root: [{}] }) });
  assert.deepEqual(await publicData.dormitoryNotices(), { total: 0, pinned: [], items: [] });
  const rows = createPublicData({ outcome: okBody({ root: [{ list: [{ seq: 3 }], totalCount: [{}] }] }) });
  assert.deepEqual(await rows.publicData.dormitoryNotices(), { total: 0, pinned: [], items: [{ id: '3', title: '', date: '', views: 0, fileName: '' }] });
});

test('extra converts scalars to text and non-scalars to compact JSON', async () => {
  const { publicData } = createPublicData({ outcome: okBody({ dl_code_orgn: [{ COM_CD: 1, CD_NM: 'n', A: 5, B: true, C: { x: 1, y: [2] }, D: [1, 2] }] }) });
  assert.deepEqual((await publicData.organizationCategories())[0].extra, { A: '5', B: 'true', C: '{"x":1,"y":[2]}', D: '[1,2]' });
});

test('cache keeps at most cacheMaxEntries and evicts the oldest', async () => {
  const { publicData, requests } = createPublicData({ options: { cacheMaxEntries: 2 } });
  await publicData.terms('1');
  await publicData.terms('2');
  await publicData.terms('3');
  assert.equal(requests.length, 3);
  await publicData.terms('3');
  assert.equal(requests.length, 3);
  await publicData.terms('1');
  assert.equal(requests.length, 4);
});

test('lectures without any filter lists the whole semester with one request', async () => {
  // 학교 서버는 검색 값이 모두 비어도 그 학기 전체를 돌려줌 (2026-10-30 확인)
  const { publicData, requests } = createPublicData();
  const lectures = await publicData.lectures({ ...TERM, courseName: '  ' }); // 공백만 있는 값은 빈 값으로 취급
  assert.ok(lectures.length > 0);
  assert.equal(requests.length, 1);
  const search = JSON.parse(requests[0].body).dm_search;
  assert.deepEqual([search.DEPT_CD, search.CURI_NM, search.EMP_NM], ['', '', '']);
});

test('concurrent calls with the same arguments share one request', async () => {
  const { publicData, requests } = createPublicData({ delayMilliseconds: 20 });
  const results = await Promise.all([publicData.campusPlaces(), publicData.campusPlaces(), publicData.campusPlaces()]);
  assert.equal(requests.length, 1);
  assert.deepEqual(results[0], expected.campusPlaces);
  assert.deepEqual(results[2], expected.campusPlaces);
});
