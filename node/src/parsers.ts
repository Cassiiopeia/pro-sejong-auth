// 포털 응답 파싱: DHC 고전독서 페이지 HTML, SIS initUserInfo JSON (규칙: spec/PORTAL_FLOW.md)
import { ErrorCode, SejongAuthError } from './errors.js';
import type {
  ClassicCertification, ClassicContestRecord, ClassicExamRecord, ClassicSubjectRecord,
  SejongClassicReading, SejongSisSchoolCodes, SejongSisUserInfo, SejongStudentInfo, SisBusinessPeriod,
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
const isObject = (candidate: unknown): candidate is JsonObject => typeof candidate === 'object' && candidate !== null && !Array.isArray(candidate);
const textOf = (node: unknown, fieldName: string) => {
  const fieldValue = isObject(node) ? node[fieldName] : undefined;
  return fieldValue === undefined || fieldValue === null ? '' : String(fieldValue);
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

// SIS 데이터셋 조회 순서 (같은 키는 앞 데이터셋 값 우선)
const SIS_DATASET_NAMES = ['dm_UserInfoSch', 'dm_UserInfoGam', 'dm_UserInfo'] as const;
// 결과 필드 > 후보 키 (앞 키 우선, 데이터셋 순서대로 찾음): 학과 코드 기준은 DEPT_CD, 없으면 DEPT_NO
export const SIS_CODE_KEYS_BY_FIELD: Record<keyof SejongSisSchoolCodes, readonly string[]> = {
  departmentCode: ['DEPT_CD', 'DEPT_NO'],
  organizationClassificationCode: ['ORGN_CLSF_CD'],
  enrollmentStatusCode: ['STATUS_DIV_CD'],
  userClassificationCode: ['USER_CLSF_CD'],
  userClassificationLargeCode: ['USER_CLSF_L_CD'],
  userClassificationMiddleCode: ['USER_CLSF_M_CD'],
  roleCode: ['RPSN_ROLE_CD'],
  roleName: ['RPSN_ROLE_CD_NM'],
  menuSystemId: ['MENU_SYS_ID'],
  menuSystemName: ['MENU_SYS_NM'],
  locale: ['INTG_LOCALE'],
};
// 기존 필드가 읽는 키 (extra 에서 제외)
const SIS_NAMED_FIELD_KEYS = new Set([
  ...Object.values(SIS_CODE_KEYS_BY_FIELD).flat(),
  'INTG_USR_NO', 'INTG_USR_NM', 'INTG_ENG_NM', 'DEPT_NM', 'USER_EMAIL', 'USER_PHONE_NO1', 'USER_PHONE_NO2',
  'USER_PHONE_NO3', 'NM_ENG', 'USER_CLSF_CD_NM',
]);
// extra 제외 키 (대소문자 무시): 세션값, 접속 IP, 비밀번호·계정 상태, 주민번호, 계좌, 주소, 직원 이름(ALIAS), 연락처, 생년월일 (spec/contract.json sis.extraExcludedKeyPattern)
export const SIS_EXTRA_EXCLUDED_KEY_PATTERN =
  '(PWD|PASS|JUMIN|RRN|RESI|SSN|BANK|ACNT|ACCT|ACCOUNT|ADDR|ZIP|POST|ALIAS|SESS|TOKEN|SECRET|BIRTH|BRTH|VALID' +
  '|PHONE|TEL|MOBILE|MAIL|LOGIN|RUNNING|(^|_)IP($|_)|_YN$)';
const SIS_EXTRA_EXCLUDED_KEY = new RegExp(SIS_EXTRA_EXCLUDED_KEY_PATTERN, 'i');

/** 이름 붙이지 않은 문자열·숫자 필드 > {"데이터셋.필드": 원문} (민감 키·중첩 값 제외) */
function sisExtraFields(root: JsonObject): Record<string, string> {
  const extra: Record<string, string> = {};
  for (const datasetName of SIS_DATASET_NAMES) {
    const dataset = root[datasetName];
    if (!isObject(dataset)) continue;
    for (const [key, value] of Object.entries(dataset)) {
      const isScalar = typeof value === 'string' || typeof value === 'number';
      if (SIS_NAMED_FIELD_KEYS.has(key) || !isScalar || SIS_EXTRA_EXCLUDED_KEY.test(key)) continue;
      extra[`${datasetName}.${key}`] = String(value);
    }
  }
  return extra;
}

/** 결과 필드별 코드 원문 (데이터셋 순서로 처음 찾은 값, 없으면 빈 문자열) */
function sisCodeValues(root: JsonObject): SejongSisSchoolCodes {
  const codeByField = {} as SejongSisSchoolCodes;
  for (const [fieldName, keys] of Object.entries(SIS_CODE_KEYS_BY_FIELD) as [keyof SejongSisSchoolCodes, readonly string[]][]) {
    codeByField[fieldName] = '';
    for (const key of keys) {
      const foundValue = SIS_DATASET_NAMES.map((datasetName) => textOf(root[datasetName], key).trim()).find(Boolean);
      if (foundValue) {
        codeByField[fieldName] = foundValue;
        break;
      }
    }
  }
  return codeByField;
}

/** SIS 요청 실행 파라미터 원천값 (세션 식별자 포함이라 출력·로그·결과에 넣지 않음) */
export interface SisRunValues {
  userNumber: string; // dm_UserInfo.INTG_USR_NO > _runIntgUsrNo
  loginDate: string; // dm_UserInfoGam.LOGIN_DT (없으면 Sch) > _runPgLoginDt (화면 열기 전)
  sessionKey: string; // dm_UserInfo.RUNNING_SEJONG > _runningSejong
}

/** initUserInfo JSON > 실행 파라미터 원천값 3개 (하나라도 비면 PARSE_ERROR) */
export function parseSisRunValues(jsonText: string): SisRunValues {
  const root = readSisRoot(jsonText);
  const runValues = {
    userNumber: textOf(root.dm_UserInfo, 'INTG_USR_NO').trim(),
    loginDate: textOf(root.dm_UserInfoGam, 'LOGIN_DT').trim() || textOf(root.dm_UserInfoSch, 'LOGIN_DT').trim(),
    sessionKey: textOf(root.dm_UserInfo, 'RUNNING_SEJONG').trim(),
  };
  if (!runValues.userNumber || !runValues.loginDate || !runValues.sessionKey) {
    throw new SejongAuthError(ErrorCode.PARSE_ERROR, 'SIS run values not found');
  }
  return runValues;
}

/** initUserRole 응답 > 화면 로그인 일시 dm_UserRole.PG_LOGIN_DT (없으면 PARSE_ERROR) */
export function parseSisScreenLoginDate(jsonText: string): string {
  let root: unknown;
  try {
    root = JSON.parse(jsonText);
  } catch (error) {
    throw new SejongAuthError(ErrorCode.PARSE_ERROR, undefined, { cause: error });
  }
  const screenLoginDate = textOf(isObject(root) ? root.dm_UserRole : undefined, 'PG_LOGIN_DT').trim();
  if (!screenLoginDate) throw new SejongAuthError(ErrorCode.PARSE_ERROR, 'dm_UserRole.PG_LOGIN_DT not found');
  return screenLoginDate;
}

// 학사일정 행에서 이름 붙여 읽는 필드 (나머지는 extra)
const SIS_BUSINESS_PERIOD_FIELD_NAMES = [
  'PERIOD', 'START_DT', 'TO_DT', 'SCHEDULE', 'CORS_SCHE_GRP_CD_NM', 'TARGET_AUTH', 'SYS_NM', 'YEAR_SMT', 'TMS', 'ORGN_CLSF_CD', 'PRT_ORD',
];

/** 학사일정조회 doList 응답 > 업무 기간 목록 (dl_mainList 가 배열이 아니면 PARSE_ERROR, 이상한 행도 버리지 않음) */
export function parseSisBusinessPeriods(jsonText: string): SisBusinessPeriod[] {
  let root: unknown;
  try {
    root = JSON.parse(jsonText);
  } catch (error) {
    throw new SejongAuthError(ErrorCode.PARSE_ERROR, undefined, { cause: error });
  }
  const rows = isObject(root) ? root.dl_mainList : undefined;
  if (!Array.isArray(rows)) throw new SejongAuthError(ErrorCode.PARSE_ERROR, 'dl_mainList not found');
  return rows.map((rawRow) => {
    const row: JsonObject = isObject(rawRow) ? rawRow : {};
    const extra: Record<string, string> = {};
    for (const [key, value] of Object.entries(row)) {
      const isScalar = typeof value === 'string' || typeof value === 'number';
      if (!SIS_BUSINESS_PERIOD_FIELD_NAMES.includes(key) && isScalar && !SIS_EXTRA_EXCLUDED_KEY.test(key)) extra[key] = String(value);
    }
    const [period, startDate, endDate, scheduleName, courseGroupName, target, businessName, yearSemester, timeText, categoryCode, sortOrder] =
      SIS_BUSINESS_PERIOD_FIELD_NAMES.map((name) => textOf(row, name));
    return { period, startDate, endDate, scheduleName, courseGroupName, target, businessName, yearSemester, timeText, categoryCode, sortOrder, extra };
  });
}

/** initUserInfo JSON > 루트 (dm_UserInfo 없으면 PARSE_ERROR) */
function readSisRoot(jsonText: string): JsonObject {
  let root: unknown;
  try {
    root = JSON.parse(jsonText);
  } catch (error) {
    throw new SejongAuthError(ErrorCode.PARSE_ERROR, undefined, { cause: error });
  }
  if (!isObject(root) || !isObject(root.dm_UserInfo)) throw new SejongAuthError(ErrorCode.PARSE_ERROR, 'dm_UserInfo field not found');
  return root;
}

/** initUserInfo JSON > 학번, 이름, 학과, 이메일, 전화번호, 영문 이름, 사용자 구분, 학교 제공 코드, extra (dm_UserInfo 없으면 PARSE_ERROR) */
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
    ...sisCodeValues(root),
    extra: sisExtraFields(root),
  };
}
