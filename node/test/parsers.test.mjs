// spec/fixtures 파싱 결과 == 기대값 (Java, Python 과 같은 기준)
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { ensurePortalLoginAccepted, parseClassicReading, parseSisUserInfo, parseStudentInfo } from '../dist/esm/parsers.js';
import { ErrorCode, SejongAuthError } from '../dist/esm/errors.js';
import { readExpected, readFixture } from './helpers.mjs';

test('parseDhcFixture', () => {
  const html = readFixture('dhc_status.html');
  const expected = readExpected('expected_dhc.json');

  assert.deepEqual(parseStudentInfo(html), expected.studentInfo);
  assert.deepEqual(parseClassicReading(html), expected.classicReading);
});

test('parseSisFixture', () => {
  assert.deepEqual(parseSisUserInfo(readFixture('sis_user_info.json')), readExpected('expected_sis.json'));
});

test('sisSchoolCodesPreferDepartmentCodeAndNeverExposeSensitiveFields', () => {
  const root = JSON.parse(readFixture('sis_user_info.json'));
  root.dm_UserInfoGam.DEPT_NO = 'OTHER';
  assert.equal(parseSisUserInfo(JSON.stringify(root)).departmentCode, '1234567');

  delete root.dm_UserInfoSch.DEPT_CD;
  const userInfo = parseSisUserInfo(JSON.stringify(root));
  assert.equal(userInfo.departmentCode, 'OTHER');
  for (const excluded of ['RUNNING', 'IP', 'PWD_YN', 'ID_YN', 'PASS_VALID', 'VALID_FLAG', 'CD_ALIAS', 'RESI', 'ADDR', 'BANK']) {
    assert.ok(!Object.keys(userInfo.extra).join(' ').includes(excluded), excluded);
  }
  const sensitiveValues = ['synthetic-session-value', '203.0.113.1', 'synthetic staff name', '000000-0000000'];
  assert.ok(Object.values(userInfo.extra).every((value) => !sensitiveValues.includes(value)));
});

test('parsesClassicReadingUserInfoValues', () => {
  const html = `<div class="b-con-box"><h4 class="b-h4-tit01">사용자 정보</h4><table class="b-board-table"><tbody>
    <tr><th>이수 학기</th><td>8 학기</td></tr><tr><th>인증완료 학기</th><td>6 학기</td></tr>
    <tr><th>인증여부</th><td>아니오(고전특강 대상자) 2099년도 1학기 대체이수</td></tr></tbody></table></div>`;
  const reading = parseClassicReading(html);
  assert.deepEqual([reading.completedSemesters, reading.certifiedSemesters], ['8 학기', '6 학기']);
  assert.equal(reading.certificationStatus, '아니오(고전특강 대상자) 2099년도 1학기 대체이수');
});

test('classicReadingUserInfoValuesAreEmptyWhenLabelsMissing', () => {
  const html = `<div class="b-con-box"><h4 class="b-h4-tit01">사용자 정보</h4><table class="b-board-table"><tbody>
    <tr><th>학년</th><td>3</td></tr></tbody></table></div>`;
  const reading = parseClassicReading(html);
  assert.deepEqual([reading.completedSemesters, reading.certifiedSemesters, reading.certificationStatus], ['', '', '']);
});

test('sisUserCategoryIsEmptyWhenMissing', () => {
  assert.equal(parseSisUserInfo('{"dm_UserInfo":{"INTG_USR_NO":"21011234"},"dm_UserInfoSch":{}}').userCategory, '');
});

test('decodesCommonNamedEntitiesAndIgnoresInvalidCodePoints', () => {
  // Python html.parser·Jsoup 과 같은 디코딩 (책 제목의 따옴표·말줄임표), 범위 밖 코드는 원문 유지
  const html = `<div class="b-con-box"><h4 class="b-h4-tit01">사용자 정보</h4><table class="b-board-table"><tbody>
    <tr><th>학과명</th><td>&lsquo;컴퓨터&rsquo; &ldquo;공학&rdquo;&hellip; &ndash;&mdash;&#x110000;</td></tr></tbody></table></div>`;
  assert.equal(parseStudentInfo(html).major, '‘컴퓨터’ “공학”… –—&#x110000;');
});

// 포털 로그인 판정값 > 에러 코드 (포털 화면과 같은 분기, spec/fixtures/expected_portal_login.json)
const portalLoginCodeByResult = Object.entries(readExpected('expected_portal_login.json')).filter(([key]) => !key.startsWith('_'));
const portalLoginBody = (resultValue) => readFixture('portal_login_erridpwd.html').replace("var result = 'erridpwd'", `var result = '${resultValue}'`);
const throwsWithCode = (action, code) => assert.throws(action, (error) => error instanceof SejongAuthError && error.code === code);

test('portalLoginResultMatchesPortalScreen', () => {
  for (const [resultValue, expectedCode] of portalLoginCodeByResult) {
    const body = portalLoginBody(resultValue);
    if (expectedCode === null) ensurePortalLoginAccepted(body);
    else throwsWithCode(() => ensurePortalLoginAccepted(body), expectedCode);
  }
});

test('portalLoginRealResponses', () => {
  ensurePortalLoginAccepted(readFixture('portal_login_ok.html'));
  throwsWithCode(() => ensurePortalLoginAccepted(readFixture('portal_login_erridpwd.html')), ErrorCode.AUTHENTICATION_FAILED);
});

test('portalLoginLockedMessagePointsToPasswordReset', () => {
  assert.throws(() => ensurePortalLoginAccepted(portalLoginBody('pwdNeedChg')), /https:\/\/portal\.sejong\.ac\.kr\/jsp\/inquiry\/pwinquiry\.jsp/);
});

test('portalLoginWithoutResultKeepsLegacyCheck', () => {
  ensurePortalLoginAccepted('');
  ensurePortalLoginAccepted('<html>changed portal page</html>');
});
