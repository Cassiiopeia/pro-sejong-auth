// 포털 응답 파싱: DHC 고전독서 페이지 HTML, SIS initUserInfo JSON (규칙: spec/PORTAL_FLOW.md)
import { ErrorCode, SejongAuthError } from './errors.js';
import type {
  ClassicCertification, ClassicContestRecord, ClassicExamRecord, ClassicSubjectRecord,
  SejongClassicReading, SejongSisUserInfo, SejongStudentInfo,
} from './models.js';

// 섹션 제목 (포털 화면 문구 그대로, 바꾸면 파싱 실패)
const SECTION_USER_INFO = '사용자 정보';
const SECTION_CERTIFICATIONS = '영역별 인증현황';
const SECTION_EXAMS = '인증 시험 현황';
const SECTION_SUBJECT_SUBSTITUTIONS = '과목 대체 인증 현황';
const SECTION_CONTESTS = '대회 인증 현황';
const SECTION_CURRICULUM = '교과연계 인증 현황';

// 사용자 정보 표 라벨
const LABEL_MAJOR = '학과명';
const LABEL_STUDENT_ID = '학번';
const LABEL_NAME = '이름';
const LABEL_GRADE = '학년';
const LABEL_STATUS = '사용자 상태';
const LABEL_COMPLETED_SEMESTERS = '이수 학기';
const LABEL_CERTIFIED_SEMESTERS = '인증완료 학기';
const LABEL_CERTIFICATION_STATUS = '인증여부';

// 주석 | 태그(닫힘 여부, 이름, 속성) | 글자
export const TOKEN_PATTERN = /<!--[\s\S]*?-->|<(\/?)([a-zA-Z][a-zA-Z0-9]*)([^>]*)>|([^<]+)/g;
const RAW_TEXT_TAGS = new Set(['script', 'style']);
// 포털 표에 나올 수 있는 이름 엔티티 (그 외 이름은 원문 유지)
const NAMED_ENTITIES: Record<string, string> = {
  amp: '&', lt: '<', gt: '>', quot: '"', apos: "'", nbsp: ' ', middot: '·',
  lsquo: '‘', rsquo: '’', ldquo: '“', rdquo: '”', hellip: '…', ndash: '–', mdash: '—',
  laquo: '«', raquo: '»', bull: '•', copy: '©', reg: '®', trade: '™', times: '×', divide: '÷',
};
const MAX_CODE_POINT = 0x10ffff;

// 숫자 엔티티 > 문자 (범위 밖·서로게이트는 원문 유지)
function decodeCodePoint(entity: string, codePoint: number): string {
  const isSurrogate = codePoint >= 0xd800 && codePoint <= 0xdfff;
  return Number.isInteger(codePoint) && codePoint <= MAX_CODE_POINT && !isSurrogate ? String.fromCodePoint(codePoint) : entity;
}

interface Cell { tag: 'th' | 'td'; text: string }
interface Section { title: string; rows: Cell[][] }

function decodeEntities(text: string): string {
  return text.replace(/&(#x[0-9a-fA-F]+|#[0-9]+|[a-zA-Z]+);/g, (entity, body: string) => {
    if (body.startsWith('#x')) return decodeCodePoint(entity, parseInt(body.slice(2), 16));
    if (body.startsWith('#')) return decodeCodePoint(entity, parseInt(body.slice(1), 10));
    return NAMED_ENTITIES[body] ?? entity;
  });
}

export const normalizeText = (text: string) => decodeEntities(text).replace(/\s+/g, ' ').trim();

export function hasClass(attributes: string, className: string): boolean {
  const match = /class\s*=\s*["']([^"']*)["']/i.exec(attributes);
  return !!match && match[1].split(/\s+/).includes(className);
}

/** div.b-con-box 섹션 > h4.b-h4-tit01 제목 + table.b-board-table tbody 행·칸 */
function readSections(html: string): Section[] {
  const sections: Section[] = [];
  let section: Section | null = null;
  let sectionDivDepth = 0;
  let isInTitle = false;
  let isInBoardTable = false;
  let isInTbody = false;
  let row: Cell[] | null = null;
  let cell: { tag: 'th' | 'td'; parts: string[] } | null = null;

  TOKEN_PATTERN.lastIndex = 0;
  for (let token = TOKEN_PATTERN.exec(html); token; token = TOKEN_PATTERN.exec(html)) {
    const [, closingSlash, rawTagName, attributes, text] = token;
    if (text !== undefined) {
      if (isInTitle && section) section.title += text;
      else if (cell) cell.parts.push(text);
      continue;
    }
    if (!rawTagName) continue; // 주석
    const tagName = rawTagName.toLowerCase();
    const isClosing = closingSlash === '/';

    if (!isClosing && RAW_TEXT_TAGS.has(tagName)) {
      // script·style 안의 '<' 가 태그로 읽히지 않게 닫는 태그까지 건너뜀
      const closeIndex = html.toLowerCase().indexOf(`</${tagName}`, TOKEN_PATTERN.lastIndex);
      TOKEN_PATTERN.lastIndex = closeIndex === -1 ? html.length : closeIndex;
      continue;
    }

    if (tagName === 'div') {
      if (!isClosing && section) sectionDivDepth += 1;
      else if (!isClosing && hasClass(attributes, 'b-con-box')) { section = { title: '', rows: [] }; sectionDivDepth = 1; }
      else if (isClosing && section && --sectionDivDepth === 0) { sections.push(section); section = null; }
      continue;
    }
    if (!section) continue;

    if (!isClosing) {
      if (tagName === 'h4' && hasClass(attributes, 'b-h4-tit01')) isInTitle = true;
      else if (tagName === 'table' && hasClass(attributes, 'b-board-table')) isInBoardTable = true;
      else if (tagName === 'tbody' && isInBoardTable) isInTbody = true;
      else if (tagName === 'tr' && isInTbody) row = [];
      else if ((tagName === 'th' || tagName === 'td') && row) cell = { tag: tagName, parts: [] };
    } else if (tagName === 'h4') isInTitle = false;
    else if (tagName === 'table') { isInBoardTable = false; isInTbody = false; }
    else if (tagName === 'tbody') isInTbody = false;
    else if (tagName === 'tr' && row) { section.rows.push(row); row = null; }
    else if ((tagName === 'th' || tagName === 'td') && cell && row) {
      row.push({ tag: cell.tag, text: normalizeText(cell.parts.join('')) });
      cell = null;
    }
  }
  return sections;
}

const findRows = (sections: Section[], title: string) =>
  sections.find((section) => section.title.includes(title))?.rows ?? [];
const tdTexts = (row: Cell[]) => row.filter((cell) => cell.tag === 'td').map((cell) => cell.text);
const thText = (row: Cell[]) => row.filter((cell) => cell.tag === 'th').map((cell) => cell.text).join(' ').trim();

/** 사용자 정보 표 > {라벨: 값} */
function readUserInfoValueByLabel(sections: Section[]): Map<string, string> {
  const valueByLabel = new Map<string, string>();
  for (const row of findRows(sections, SECTION_USER_INFO)) {
    const label = thText(row);
    if (label) valueByLabel.set(label, tdTexts(row).join(' ').trim());
  }
  return valueByLabel;
}

/** 사용자 정보 표 > 학과, 학번, 이름, 학년, 상태 (표 없으면 PARSE_ERROR) */
export function parseStudentInfo(html: string): SejongStudentInfo {
  const valueByLabel = readUserInfoValueByLabel(readSections(html));
  if (valueByLabel.size === 0) throw new SejongAuthError(ErrorCode.PARSE_ERROR, 'Student info table not found');
  return {
    major: valueByLabel.get(LABEL_MAJOR) ?? '',
    studentId: valueByLabel.get(LABEL_STUDENT_ID) ?? '',
    name: valueByLabel.get(LABEL_NAME) ?? '',
    grade: valueByLabel.get(LABEL_GRADE) ?? '',
    status: valueByLabel.get(LABEL_STATUS) ?? '',
  };
}

/** 고전독서 5개 표 + 사용자 정보 표의 이수·인증 학기, 인증여부 > SejongClassicReading (칸 수 부족한 행은 건너뜀) */
export function parseClassicReading(html: string): SejongClassicReading {
  const sections = readSections(html);
  const userInfoValueByLabel = readUserInfoValueByLabel(sections);
  const rowsWith = (title: string, minimumCellCount: number) =>
    findRows(sections, title).map(tdTexts).filter((cells) => cells.length >= minimumCellCount);
  const toSubjectRecord = ([semester, subjectName, area, bookTitle, completion]: string[]): ClassicSubjectRecord =>
    ({ semester, subjectName, area, bookTitle, completion });

  const certifications: ClassicCertification[] = [];
  for (const row of findRows(sections, SECTION_CERTIFICATIONS)) {
    const area = thText(row);
    const counts = tdTexts(row);
    if (area && counts.length >= 2) certifications.push({ area, requiredCount: counts[0], certifiedCount: counts[1] });
  }

  return {
    completedSemesters: userInfoValueByLabel.get(LABEL_COMPLETED_SEMESTERS) ?? '',
    certifiedSemesters: userInfoValueByLabel.get(LABEL_CERTIFIED_SEMESTERS) ?? '',
    certificationStatus: userInfoValueByLabel.get(LABEL_CERTIFICATION_STATUS) ?? '',
    certifications,
    examRecords: rowsWith(SECTION_EXAMS, 6).map(([semester, area, bookTitle, examDate, score, passStatus]): ClassicExamRecord =>
      ({ semester, area, bookTitle, examDate, score, passStatus })),
    subjectSubstitutions: rowsWith(SECTION_SUBJECT_SUBSTITUTIONS, 5).map(toSubjectRecord),
    contestRecords: rowsWith(SECTION_CONTESTS, 4).map(([semester, contestName, area, bookTitle]): ClassicContestRecord =>
      ({ semester, contestName, area, bookTitle })),
    curriculumRecords: rowsWith(SECTION_CURRICULUM, 5).map(toSubjectRecord),
  };
}

type JsonObject = Record<string, unknown>;
const isObject = (value: unknown): value is JsonObject => typeof value === 'object' && value !== null && !Array.isArray(value);
const textOf = (node: unknown, fieldName: string) => {
  const value = isObject(node) ? node[fieldName] : undefined;
  return value === undefined || value === null ? '' : String(value);
};

// 포털 로그인 판정값: login_action.jsp 응답의 var result = '...' (포털 화면 분기와 같은 대응)
const PORTAL_LOGIN_RESULT = /var\s+result\s*=\s*'([^']*)'/;
export const PORTAL_LOGIN_ACCEPTED = 'OK';
export const PORTAL_LOGIN_ERROR_CODE_BY_RESULT: Record<string, ErrorCode> = {
  erridpwd: ErrorCode.AUTHENTICATION_FAILED, // 아이디·비밀번호 불일치
  Error: ErrorCode.AUTHENTICATION_FAILED, // 아이디·비밀번호 불일치
  pwdNeedChg: ErrorCode.ACCOUNT_LOCKED, // 반복 실패로 잠김, 비밀번호 재설정 필요
  invalidDt: ErrorCode.ACCOUNT_UNAVAILABLE, // 계정 사용 기간 종료
  invalid: ErrorCode.ACCOUNT_UNAVAILABLE, // 관리자 요청으로 로그인 불가
};

/** 포털 로그인 판정: OK 통과, 알려진 실패 > 코드별 예외, 그 외 값 > SESSION_ERROR, 판정값 없음 > 통과(이후 401 판정) */
export function ensurePortalLoginAccepted(html: string): void {
  const match = PORTAL_LOGIN_RESULT.exec(html ?? '');
  if (!match || match[1] === PORTAL_LOGIN_ACCEPTED) return;
  const resultValue = match[1];
  const errorCode = Object.prototype.hasOwnProperty.call(PORTAL_LOGIN_ERROR_CODE_BY_RESULT, resultValue) ? PORTAL_LOGIN_ERROR_CODE_BY_RESULT[resultValue] : undefined;
  if (!errorCode) throw new SejongAuthError(ErrorCode.SESSION_ERROR, `Portal login failed : result=${resultValue.slice(0, 20)}`);
  throw new SejongAuthError(errorCode);
}

// SIS 오류 응답: 200 + {"_SUBMIT_ERROR_": {"ERRTYPE": ...}}, 로그인 세션 없음(틀린 비밀번호) = LOGOUT
export const SIS_SUBMIT_ERROR_KEY = '_SUBMIT_ERROR_';
export const SIS_LOGGED_OUT_ERROR_TYPE = 'LOGOUT';

/** initUserInfo 오류 응답 판정: LOGOUT > AUTHENTICATION_FAILED, 그 외 오류 > DATA_FETCH_FAILED */
export function ensureSisLoggedIn(jsonText: string): void {
  let root: unknown;
  try {
    root = JSON.parse(jsonText);
  } catch {
    return; // JSON 아님 > 파싱 단계에서 PARSE_ERROR
  }
  const submitError = isObject(root) ? root[SIS_SUBMIT_ERROR_KEY] : undefined;
  if (!isObject(submitError)) return;
  const errorType = textOf(submitError, 'ERRTYPE');
  if (errorType === SIS_LOGGED_OUT_ERROR_TYPE) throw new SejongAuthError(ErrorCode.AUTHENTICATION_FAILED);
  throw new SejongAuthError(ErrorCode.DATA_FETCH_FAILED, `SIS returned an error : ${errorType}`);
}

/** initUserInfo JSON > 학번, 이름, 학과, 이메일, 전화번호, 영문 이름, 사용자 구분 (dm_UserInfo 없으면 PARSE_ERROR) */
export function parseSisUserInfo(jsonText: string): SejongSisUserInfo {
  let root: unknown;
  try {
    root = JSON.parse(jsonText);
  } catch (error) {
    throw new SejongAuthError(ErrorCode.PARSE_ERROR, undefined, { cause: error });
  }
  if (!isObject(root) || !isObject(root.dm_UserInfo)) {
    throw new SejongAuthError(ErrorCode.PARSE_ERROR, 'dm_UserInfo field not found');
  }
  const userInfo = root.dm_UserInfo;
  const gam = root.dm_UserInfoGam;
  const sch = root.dm_UserInfoSch;
  const phoneParts = ['USER_PHONE_NO1', 'USER_PHONE_NO2', 'USER_PHONE_NO3'].map((key) => textOf(gam, key).trim());

  return {
    studentId: textOf(userInfo, 'INTG_USR_NO'),
    name: textOf(userInfo, 'INTG_USR_NM'),
    major: textOf(gam, 'DEPT_NM').trim() || textOf(sch, 'DEPT_NM'),
    email: textOf(gam, 'USER_EMAIL'),
    phoneNumber: phoneParts.filter(Boolean).join('-'),
    englishName: textOf(userInfo, 'INTG_ENG_NM').trim() || textOf(sch, 'NM_ENG'),
    userCategory: textOf(sch, 'USER_CLSF_CD_NM'),
  };
}
