// 학사 기준 정보·강의·학과 교육과정 (SIS 시간표 + 교육과정 조회): 요청 조립과 행 > 모델 변환
import { ErrorCode, SejongAuthError } from '../common/errors.js';
import { pairsOf, type PublicDataFetcher } from './fetcher.js';
import type { CourseType, CurriculumCourse, Department, Lecture, OrganizationCategory, SelectiveDomain, Term } from './models.js';
import type { CurriculumCoursesQuery, DepartmentsQuery, LecturesQuery } from './queries.js';
import { extraOf, rowsOf, toText, type JsonObject, type SchoolRow } from './rowReader.js';

// 주소·요청 키·고정 값의 원본은 spec/contract.json 의 publicData.sisTimetable·curriculum
export const SIS_BASE_URL = 'https://sjpt.sejong.ac.kr/sch/sch/ext/SueOpenTimeQ/';
export const SIS_CONTENT_TYPE = 'application/json; charset="UTF-8"';
export const SIS_SEARCH_KEY = 'dm_search';
export const SIS_ONLOAD_ENDPOINT = 'doOnload.do';
export const SIS_TERM_LIST_ENDPOINT = 'doYearsmtlist.do';
export const SIS_DEPARTMENT_LIST_ENDPOINT = 'doLesndeptgroup.do';
export const SIS_LECTURE_LIST_ENDPOINT = 'doList.do';
// 강좌 조회는 나머지 필터 키가 모두 빈 값으로 있어야 학교가 받아들임 (하나라도 빠지면 _SUBMIT_ERROR_)
export const LECTURE_EMPTY_FILTER_KEYS = ['SMT_NM', 'CURI_TYPE_CD', 'SLT_DOMAIN_CD', 'CURI_NO', 'CLASS', 'SLT_DEPT_CD', 'PLAN_REG_YN', 'CORS_SCHE_GRP_CD'];
const LECTURE_EMPTY_FILTERS = Object.fromEntries(LECTURE_EMPTY_FILTER_KEYS.map((key) => [key, '']));
// 학과 교육과정 과목 1건: deptNo 는 SIS 학과 코드(DEPT_CD)와 같은 값
export const CURRICULUM_COURSE_URL = 'https://www.sejong.ac.kr/curriculum/getCurriculumOneKor.do';
export const CURRICULUM_QUERY_KEYS = ['curiNo', 'deptNo'];

// remark 는 교과목 개요 본문이라 overview 로 받음 (CurriculumCourse 필드 순서와 별개, 매핑에 쓴 키 목록)
const CURRICULUM_COURSE_USED_KEYS = ['deptCd', 'deptNm', 'orgnClsfCd', 'year', 'curiNo', 'curiNm', 'curiTypeCd', 'curiTypeNm', 'cdt',
  'lecTmNum', 'exeTmNum', 'curiTm', 'stuYearCd', 'smtDanCd', 'smtDanNm', 'remark'];
const DEPARTMENT_USED_KEYS = ['DEPT_CD', 'DEPT_LEVEL_CD', 'DEPT_L_CD', 'DEPT_L_PRINT', 'DEPT_M_CD', 'DEPT_M_PRINT', 'DEPT_S_CD', 'DEPT_S_PRINT', 'DEPT_NM_ENG'];
const LECTURE_USED_KEYS = ['CURI_NO', 'CLASS', 'CURI_NM', 'CURI_TYPE_CD', 'CURI_TYPE_CD_NM', 'SLT_DOMAIN_CD', 'SLT_DOMAIN_CD_NM', 'CDT', 'TM_NUM', 'TOT_TM_NUM',
  'LESN_TIME', 'LESN_ROOM', 'LESN_EMP', 'STUDENT_YEAR', 'CURI_LANG_CD', 'CURI_LANG_NM', 'DEPT_CD', 'SCH_DEPT_ALIAS', 'MANAGE_DEPT_NM', 'REMARK', 'YEAR', 'SMT_CD'];

const toOrganizationCategory = (row: SchoolRow): OrganizationCategory => ({
  code: toText(row['COM_CD']), name: toText(row['CD_NM']), nameEn: toText(row.CD_NM_ENG), extra: extraOf(row, ['COM_CD', 'CD_NM', 'CD_NM_ENG']),
});

const toTerm = (row: SchoolRow): Term => ({
  year: toText(row['YEAR']), semesterCode: toText(row['SMT_CD']), code: toText(row['YEAR_SMT_CD']),
  name: toText(row['YEAR_SMT_NM']), extra: extraOf(row, ['YEAR', 'SMT_CD', 'YEAR_SMT_CD', 'YEAR_SMT_NM']),
});

const toDepartment = (row: SchoolRow): Department => ({
  code: toText(row['DEPT_CD']), level: toText(row['DEPT_LEVEL_CD']),
  collegeCode: toText(row.DEPT_L_CD), collegeName: toText(row.DEPT_L_PRINT),
  unitCode: toText(row.DEPT_M_CD), unitName: toText(row.DEPT_M_PRINT),
  majorCode: toText(row.DEPT_S_CD), majorName: toText(row.DEPT_S_PRINT), nameEn: toText(row.DEPT_NM_ENG),
  extra: extraOf(row, DEPARTMENT_USED_KEYS),
});

const toCourseType = (row: SchoolRow): CourseType => ({
  code: toText(row['CURI_TYPE_CD']), name: toText(row['CURI_TYPE_CD_NM']), categoryCode: toText(row.ORGN_CLSF_CD),
  extra: extraOf(row, ['CURI_TYPE_CD', 'CURI_TYPE_CD_NM', 'ORGN_CLSF_CD']),
});

const toSelectiveDomain = (row: SchoolRow): SelectiveDomain => ({
  code: toText(row['COM_CD']), name: toText(row['CD_NM']), nameEn: toText(row.CD_NM_ENG), extra: extraOf(row, ['COM_CD', 'CD_NM', 'CD_NM_ENG']),
});

const toLecture = (row: SchoolRow): Lecture => ({
  courseNumber: toText(row['CURI_NO']), section: toText(row['CLASS']), name: toText(row['CURI_NM']),
  courseTypeCode: toText(row.CURI_TYPE_CD), courseTypeName: toText(row.CURI_TYPE_CD_NM),
  domainCode: toText(row.SLT_DOMAIN_CD), domainName: toText(row.SLT_DOMAIN_CD_NM),
  credits: toText(row.CDT), hours: toText(row.TM_NUM), totalHours: toText(row.TOT_TM_NUM), schedule: toText(row.LESN_TIME),
  room: toText(row.LESN_ROOM), instructor: toText(row.LESN_EMP), gradeLevel: toText(row.STUDENT_YEAR),
  languageCode: toText(row.CURI_LANG_CD), languageName: toText(row.CURI_LANG_NM),
  departmentCode: toText(row.DEPT_CD), departmentName: toText(row.SCH_DEPT_ALIAS), managingDepartment: toText(row.MANAGE_DEPT_NM),
  remark: toText(row.REMARK), year: toText(row.YEAR), semesterCode: toText(row.SMT_CD), extra: extraOf(row, LECTURE_USED_KEYS),
});

const toCurriculumCourse = (row: SchoolRow): CurriculumCourse => ({
  departmentCode: toText(row['deptCd']), departmentName: toText(row['deptNm']), categoryCode: toText(row['orgnClsfCd']), year: toText(row['year']),
  courseNumber: toText(row['curiNo']), name: toText(row['curiNm']), courseTypeCode: toText(row['curiTypeCd']), courseTypeName: toText(row['curiTypeNm']),
  credits: toText(row['cdt']), lectureHours: toText(row['lecTmNum']), practiceHours: toText(row['exeTmNum']), totalHours: toText(row['curiTm']),
  gradeLevel: toText(row['stuYearCd']), semesterCode: toText(row['smtDanCd']), semesterName: toText(row['smtDanNm']), overview: toText(row['remark']),
  extra: extraOf(row, CURRICULUM_COURSE_USED_KEYS),
});

const termSearchFields = (categoryCode: string, year: string, semesterCode: string): JsonObject =>
  ({ ORGN_CLSF_CD: categoryCode, YEAR: year, SMT_CD: semesterCode, YEAR_SMT: `${year}/${semesterCode}` });

// SIS 시간표 코드 5종, 개설 강좌, 학과 교육과정 과목
export class AcademicSource {
  constructor(private readonly fetcher: PublicDataFetcher) {}

  // 기관 구분 목록
  organizationCategories(): Promise<OrganizationCategory[]> {
    return this.fetchSisDataset('organizationCategories', SIS_ONLOAD_ENDPOINT, {}, 'dl_code_orgn', toOrganizationCategory);
  }

  // 기관 구분 코드 > 학기 목록
  terms(categoryCode: string): Promise<Term[]> {
    return this.fetchSisDataset('terms', SIS_TERM_LIST_ENDPOINT, { ORGN_CLSF_CD: categoryCode }, 'dl_code_yearSmt', toTerm, [categoryCode]);
  }

  // 기관 구분·연도·학기 코드 > 학과 목록
  departments({ categoryCode, year, semesterCode }: DepartmentsQuery): Promise<Department[]> {
    return this.fetchSisDataset('departments', SIS_DEPARTMENT_LIST_ENDPOINT, termSearchFields(categoryCode, year, semesterCode), 'dl_code_dept',
      toDepartment, [categoryCode, year, semesterCode]);
  }

  // 이수 구분 목록
  courseTypes(): Promise<CourseType[]> {
    return this.fetchSisDataset('courseTypes', SIS_ONLOAD_ENDPOINT, {}, 'dl_code_curiType', toCourseType);
  }

  // 선택 교양 영역 목록
  selectiveDomains(): Promise<SelectiveDomain[]> {
    return this.fetchSisDataset('selectiveDomains', SIS_ONLOAD_ENDPOINT, {}, 'dl_code_sltDomain', toSelectiveDomain);
  }

  // 학기 개설 강좌 (학과·과목명·교수명은 앞뒤 공백 제거, 모두 비우면 그 학기 전체)
  lectures(query: LecturesQuery): Promise<Lecture[]> {
    const { categoryCode, year, semesterCode, departmentCode = '', courseName = '', instructorName = '' } = query;
    const searchFields = { ...LECTURE_EMPTY_FILTERS, ...termSearchFields(categoryCode, year, semesterCode), DEPT_CD: departmentCode.trim(), CURI_NM: courseName.trim(), EMP_NM: instructorName.trim(), LANG_CHK: 'KOR' };
    return this.fetchSisDataset('lectures', SIS_LECTURE_LIST_ENDPOINT, searchFields, 'dl_main', toLecture,
      [categoryCode, year, semesterCode, departmentCode, courseName, instructorName]);
  }

  // 학과 코드·학수번호 > 교육과정 과목 (입력 검증 후 요청, 빈 입력은 INVALID_INPUT)
  curriculumCourses({ departmentCode, courseNumber }: CurriculumCoursesQuery): Promise<CurriculumCourse[]> {
    if (!String(departmentCode ?? '').trim() || !String(courseNumber ?? '').trim()) {
      throw new SejongAuthError(ErrorCode.INVALID_INPUT, 'Department code and course number are required.');
    }
    const url = `${CURRICULUM_COURSE_URL}?${pairsOf(CURRICULUM_QUERY_KEYS, courseNumber, departmentCode)}`;
    return this.fetcher.fetchCached('curriculumCourses', [departmentCode, courseNumber], () => [], () => this.fetcher.requestJson('GET', url), (body) =>
      rowsOf(body, 'items').map(toCurriculumCourse));
  }

  // SIS 데이터셋 1건 조회 > 행마다 매핑
  private fetchSisDataset<T>(
    methodName: string, endpoint: string, searchFields: JsonObject, datasetName: string, mapRow: (row: SchoolRow) => T, cacheArguments: unknown[] = [],
  ): Promise<T[]> {
    const json = JSON.stringify({ [SIS_SEARCH_KEY]: searchFields });
    return this.fetcher.fetchCached(methodName, cacheArguments, () => [] as T[],
      () => this.fetcher.requestJson('POST', SIS_BASE_URL + endpoint, { json, contentType: SIS_CONTENT_TYPE }),
      (body) => rowsOf(body, datasetName).map(mapRow));
  }
}
