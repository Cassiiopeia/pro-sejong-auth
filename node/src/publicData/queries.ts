// 공개 데이터 메서드 인자 객체 (Node 는 인자가 많은 메서드를 객체 하나로 받음, spec/api-index.json 의 parameters 와 같음)

export interface DepartmentsQuery {
  /** 기관 구분 코드 */
  categoryCode: string;
  /** 연도 4자리 (예: 2026) */
  year: string;
  /** 학기 코드 (10 1학기, 11 하계, 20 2학기, 21 동계) */
  semesterCode: string;
}
export interface LecturesQuery extends DepartmentsQuery {
  /** 학과 코드 (departments 결과의 code), 비우면 전체 */
  departmentCode?: string;
  /** 과목명 검색어 (앞뒤 공백 제거) */
  courseName?: string;
  /** 교수명 검색어 (앞뒤 공백 제거) */
  instructorName?: string;
}
export interface CurriculumCoursesQuery {
  /** 학과 코드 (departments 결과의 code) */
  departmentCode: string;
  /** 학수번호 (lectures 결과의 courseNumber) */
  courseNumber: string;
}
export interface CalendarQuery {
  /** 시작일 yyyy-MM-dd */
  start: string;
  /** 종료일 yyyy-MM-dd */
  end: string;
  /** 일정 분류 (기본 0001 학부, 0002~0007 대학원 계열) */
  categoryCode?: string;
}
export interface GraduateCalendarQuery {
  /** 대학원 사이트 이름 (graduate, gsi, mba, pub, tourgrad, edu, arts 등) */
  siteId: string;
  /** 연도 */
  year: number | string;
  /** 월 1~12 */
  month: number | string;
}
export interface DormitoryNoticesQuery {
  /** 쪽 번호 (1부터, 기본 1) */
  page?: number;
  /** 쪽당 건수 (기본 10) */
  rows?: number;
  /** 제목 검색어 */
  keyword?: string;
  /** 기숙사 위치 코드 (기본 SJ) */
  location?: string;
}
export interface CafeteriaPlacesQuery {
  /** 조회할 식당 번호 상한 (1~50, 기본 10) */
  maxPlaceId?: number;
}
export interface ExtracurricularEventsQuery {
  /** 시작일 yyyy-MM-dd */
  start: string;
  /** 종료일 yyyy-MM-dd */
  end: string;
  /** 비교과 분류 (학교 값 그대로), 비우면 전체 */
  category?: string;
}
