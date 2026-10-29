// 포털 메인 소속 표기 > 상위 소속명: 보수적 규칙 (틀린 값보다 빈 값, 예외 없음)
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { parseParentUnitName } from '../dist/esm/portalMainParser.js';
import { readExpected, readFixture } from './helpers.mjs';

const portalMainHtml = (affiliation) =>
  `<div class="top_myinfo"><div class="box"><div class="text01"><span class="txt1">홍길동</span><span class="txt4">${affiliation}</span></div></div></div>`;

test('parsePortalMainFixture', () => {
  const expected = readExpected('expected_portal_main.json');
  assert.equal(parseParentUnitName(readFixture('portal_main.html'), expected.major), expected.parentUnitName);
});

const RULE_CASES = [
  ['학부생 정상', '공과대학 컴퓨터공학과', '컴퓨터공학과', '공과대학'],
  ['학과 이름 안에 공백', '공과대학 컴퓨터 공학과', '컴퓨터 공학과', '공과대학'],
  ['영문 표기', 'College of Engineering Computer Science', 'Computer Science', 'College of Engineering'],
  ['상위 소속 없이 학과만', '컴퓨터공학과', '컴퓨터공학과', ''],
  ['단일 토큰', '컴퓨터공학과', '', ''],
  ['학과 표기 불일치 + 3토큰은 추측 안 함', '공과대학 컴퓨터공학 과', '컴퓨터공학과', ''],
  ['학과 표기 불일치 + 2토큰', '공과대학 컴퓨터공학과', '컴퓨터공학부', '공과대학'],
  ['학과 표기 불일치 + 3토큰 (긴 학과)', '공과대학 컴퓨터 공학과', '소프트웨어학과', ''],
  ['영문 + 학과 모름 + 4토큰은 추측 안 함', 'College of Engineering Computer Science', '', ''],
  ['토큰 3개 이상', '공과대학 학과 이름 길다', '컴퓨터공학과', ''],
  ['빈 표기', '   ', '컴퓨터공학과', ''],
];

for (const [caseName, affiliation, major, expected] of RULE_CASES) {
  test(`parentUnitNameRule_${caseName}`, () => {
    assert.equal(parseParentUnitName(portalMainHtml(affiliation), major), expected);
  });
}

test('missingOrBrokenStructureIsEmptyStringWithoutException', () => {
  for (const html of [
    '',
    '<html><body>교직원 포털 화면: 내 정보 영역 없음</body></html>',
    '<div class="text01"><span class="txt4">공과대학 컴퓨터공학과</span></div>', // top_myinfo 밖의 txt4
    '<div class="top_myinfo"><span class="txt1">홍길동</span></div>', // txt4 없음
  ]) {
    assert.equal(parseParentUnitName(html, '컴퓨터공학과'), '');
  }
});

test('brokenHtmlNeverThrows', () => {
  for (const html of ["<div class=top_myinfo><span class='txt4'>공과대학 <b>컴퓨터공학과", '<<<>>><div', '\u0000\u0001']) {
    assert.equal(typeof parseParentUnitName(html, '컴퓨터공학과'), 'string');
  }
});

test('nullInputIsEmptyString', () => {
  assert.equal(parseParentUnitName(null, null), '');
  assert.equal(parseParentUnitName(undefined, undefined), '');
});
