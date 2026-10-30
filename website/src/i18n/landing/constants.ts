// 랜딩 공통 상수: 저장소 주소, 데모 서버, 패키지 배지, 데모 에러 코드, 학교 데이터 실측 수치
export const REPOSITORY_URL = 'https://github.com/Cassiiopeia/pro-sejongpeia';

// 체험 폼이 호출하는 데모 서버: 라이브러리로 포털에 한 번 로그인하고 결과만 돌려줌
export const DEMO_AUTH_API_URL = 'https://lab.suhsaechan.kr/api/sejong-auth/authenticate';
export const DEMO_SERVER_HOME_URL = 'https://lab.suhsaechan.kr/';
export const DEMO_REQUEST_TIMEOUT_MILLISECONDS = 30000;

// 학교 데이터 실측 수치 (학부 2026학년도 2학기, 실제 학교 서버 호출 결과): README 와 배너(scripts/banner/banner.html)도 같은 값
// 값을 바꿀 때는 측정 날짜와 함께 세 곳을 같이 고친다
export const SCHOOL_DATA_SNAPSHOT = {
  measuredOn: '2026-10-30',
  departmentCount: '103',
  lectureRowCount: '2,786',
  cafeteriaCount: '5',
  campusPlaceCount: '60',
  organizationCategoryCount: '22',
};

// 배지: 패키지 레지스트리가 알려 주는 최신 버전
export const PACKAGE_BADGES = [
  {
    name: 'Maven Central',
    imageUrl: 'https://img.shields.io/maven-central/v/kr.suhsaechan/sejongpeia?label=Maven%20Central&color=C20F2F',
    linkUrl: 'https://central.sonatype.com/artifact/kr.suhsaechan/sejongpeia',
  },
  {
    name: 'PyPI',
    imageUrl: 'https://img.shields.io/pypi/v/pro-sejongpeia?label=PyPI&color=324D60',
    linkUrl: 'https://pypi.org/project/pro-sejongpeia/',
  },
  {
    name: 'npm',
    imageUrl: 'https://img.shields.io/npm/v/pro-sejongpeia?label=npm&color=324D60',
    linkUrl: 'https://www.npmjs.com/package/pro-sejongpeia',
  },
  {
    name: 'License: MIT',
    imageUrl: 'https://img.shields.io/badge/License-MIT-324D60',
    linkUrl: `${REPOSITORY_URL}/blob/main/LICENSE`,
  },
  {
    name: 'Live portal check',
    imageUrl: `${REPOSITORY_URL}/actions/workflows/PROJECT-SEJONG-LIVE-CHECK.yml/badge.svg`,
    linkUrl: `${REPOSITORY_URL}/actions/workflows/PROJECT-SEJONG-LIVE-CHECK.yml`,
  },
];

// 인증 에러 코드 10종 (spec/RESULT_SCHEMA.md) + 데모 서버 전용 TOO_MANY_ATTEMPTS
export const DEMO_ERROR_CODES = [
  'AUTHENTICATION_FAILED',
  'INVALID_INPUT',
  'CONNECTION_FAILED',
  'CONNECTION_TIMEOUT',
  'DATA_FETCH_FAILED',
  'PARSE_ERROR',
  'SESSION_ERROR',
  'SSL_CONFIGURATION_ERROR',
  'ACCOUNT_LOCKED',
  'ACCOUNT_UNAVAILABLE',
  'TOO_MANY_ATTEMPTS',
] as const;
