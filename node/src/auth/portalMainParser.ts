// 포털 메인 소속 표기 파싱: span.txt4 "상위 소속 학과" > 상위 소속명 (규칙: spec/PORTAL_FLOW.md)
// 학년도마다 바뀌는 포털 표기 원문 그대로 통과, 값 목록·코드표 검증 없음
import { hasClass, normalizeText, TOKEN_PATTERN } from './parsers.js';

// 포털 메인 화면 클래스명 (바꾸면 상위 소속명이 빈 문자열이 됨)
const CLASS_MY_INFO_BOX = 'top_myinfo';
const CLASS_AFFILIATION = 'txt4';

/** div.top_myinfo 안 첫 span.txt4 글자 */
function readAffiliationText(html: string): string {
  const textParts: string[] = [];
  let myInfoDivDepth = 0;
  let isInAffiliation = false;
  let isAffiliationRead = false;

  // 정규식 상태(lastIndex)를 parsers.ts 와 공유하지 않게 복사본 사용
  const tokenPattern = new RegExp(TOKEN_PATTERN.source, 'g');
  for (let token = tokenPattern.exec(html); token; token = tokenPattern.exec(html)) {
    const [, closingSlash, rawTagName, attributes, text] = token;
    if (text !== undefined) {
      if (isInAffiliation) textParts.push(text);
      continue;
    }
    if (!rawTagName) continue; // 주석
    const tagName = rawTagName.toLowerCase();
    const isClosing = closingSlash === '/';

    if (tagName === 'div') {
      if (myInfoDivDepth > 0) myInfoDivDepth += isClosing ? -1 : 1;
      else if (!isClosing && hasClass(attributes, CLASS_MY_INFO_BOX)) myInfoDivDepth = 1;
    } else if (tagName === 'span' && myInfoDivDepth > 0) {
      if (!isClosing && !isAffiliationRead && hasClass(attributes, CLASS_AFFILIATION)) isInAffiliation = true;
      else if (isClosing && isInAffiliation) { isInAffiliation = false; isAffiliationRead = true; }
    }
  }
  return normalizeText(textParts.join(''));
}

/** 소속 표기 > 상위 소속명 (규칙: 학과가 끝에 일치, 아니면 토큰 2개일 때만 앞 토큰, 그 외 빈 문자열, 예외 없음) */
export function parseParentUnitName(html: string | null | undefined, major: string | null | undefined): string {
  try {
    const affiliation = readAffiliationText(html ?? '');
    const knownMajor = normalizeText(major ?? '');
    if (!affiliation || affiliation === knownMajor) return '';
    if (knownMajor && affiliation.endsWith(` ${knownMajor}`)) return affiliation.slice(0, -knownMajor.length).trim();
    const tokens = affiliation.split(' ');
    return tokens.length === 2 ? tokens[0] : '';
  } catch {
    return ''; // 틀린 값보다 빈 값 > 파싱 오류도 빈 문자열
  }
}
