// 결과 타입: Java 결과 클래스와 같은 필드 이름 (spec/RESULT_SCHEMA.md)

export interface ClassicCertification {
  area: string;
  requiredCount: string;
  certifiedCount: string;
}

export interface ClassicExamRecord {
  semester: string;
  area: string;
  bookTitle: string;
  examDate: string;
  score: string;
  passStatus: string;
}

export interface ClassicSubjectRecord {
  semester: string;
  subjectName: string;
  area: string;
  bookTitle: string;
  completion: string;
}

export interface ClassicContestRecord {
  semester: string;
  contestName: string;
  area: string;
  bookTitle: string;
}

/** 고전독서 인증 현황: 영역별 인증, 시험, 과목 대체, 대회, 교과연계, 이수·인증 학기, 인증여부 */
export interface SejongClassicReading {
  // 사용자 정보 표 원문 (예: "8 학기", 대체이수 사유가 붙는 자유 텍스트), 없으면 빈 문자열
  completedSemesters: string;
  certifiedSemesters: string;
  certificationStatus: string;
  certifications: ClassicCertification[];
  examRecords: ClassicExamRecord[];
  subjectSubstitutions: ClassicSubjectRecord[];
  contestRecords: ClassicContestRecord[];
  curriculumRecords: ClassicSubjectRecord[];
}

/** DHC 사용자 정보 표 (없는 값은 빈 문자열) */
export interface SejongStudentInfo {
  major: string;
  studentId: string;
  name: string;
  grade: string;
  status: string;
}

/** SIS 학교 제공 코드·구분 원문 (없으면 빈 문자열, 이름 매핑·값 목록 없음) */
export interface SejongSisSchoolCodes {
  departmentCode: string;
  organizationClassificationCode: string;
  enrollmentStatusCode: string;
  userClassificationCode: string;
  userClassificationLargeCode: string;
  userClassificationMiddleCode: string;
  roleCode: string;
  roleName: string;
  menuSystemId: string;
  menuSystemName: string;
  locale: string;
}

/** SIS initUserInfo 값 (userCategory: 사용자 구분, 예: 학부생, extra: 이름 붙이지 않은 나머지 학교 제공 필드 {"데이터셋.필드": 원문}, 민감 키 제외) */
export interface SejongSisUserInfo extends SejongSisSchoolCodes {
  studentId: string;
  name: string;
  major: string;
  email: string;
  phoneNumber: string;
  englishName: string;
  userCategory: string;
  extra: Record<string, string>;
}

/** 통합 인증 결과 (연락처·영문 이름·사용자 구분은 SIS 실패 시, 상위 소속명은 포털 메인 조회 실패 시 null) */
export interface SejongAuthResult extends SejongStudentInfo, Partial<Record<keyof SejongSisSchoolCodes, string | null>> {
  success: true;
  classicReading: SejongClassicReading;
  email: string | null;
  phoneNumber: string | null;
  englishName: string | null;
  userCategory: string | null;
  /**
   * 학과의 상위 소속 이름(단과대학 또는 학부): 포털 화면 표기에서 추출한 부가 정보, 학적 구분에 따라 빈 문자열일 수 있음, 인증 판정에 쓰지 말 것
   * 학년도마다 바뀌는 포털 표기 원문, 값 목록으로 분기하지 말 것
   */
  parentUnitName: string | null;
  authenticatedAt: Date;
  // includeRaw 일 때만: DHC 원문 HTML, SIS 원문 JSON (SIS 조회 실패 시 null), 세션값·접속 IP 포함이라 외부 전달 금지
  rawHtml: string | null;
  rawJson: string | null;
  // SIS 조회 실패 시 위 코드 필드는 null, extra 는 includeExtra 일 때만
  extra: Record<string, string> | null;
}

/** DHC 인증 결과 (rawHtml 은 includeRaw 일 때만) */
export interface SejongDhcAuthResult extends SejongStudentInfo {
  success: true;
  classicReading: SejongClassicReading;
  authenticatedAt: Date;
  rawHtml: string | null;
}

/** SIS 인증 결과 (grade·status 는 SIS 미제공 > 빈 문자열, rawJson 은 includeRaw 일 때만) */
export interface SejongSisAuthResult extends Omit<SejongSisUserInfo, 'extra'> {
  success: true;
  grade: '';
  status: '';
  authenticatedAt: Date;
  rawJson: string | null;
  extra: Record<string, string> | null; // includeExtra 일 때만
}

export interface RawOption {
  includeRaw?: boolean;
  /** SIS 나머지 학교 제공 필드를 extra 로 포함 (기본 false) */
  includeExtra?: boolean;
}
