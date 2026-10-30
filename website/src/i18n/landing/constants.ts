// 랜딩 공통 상수: 저장소 주소, 데모 서버, 패키지 배지, 데모 에러 코드
export const REPOSITORY_URL = 'https://github.com/Cassiiopeia/pro-sejongpeia';

// 체험 폼이 호출하는 데모 서버: 라이브러리로 포털에 한 번 로그인하고 결과만 돌려줌
export const DEMO_AUTH_API_URL = 'https://lab.suhsaechan.kr/api/sejong-auth/authenticate';
export const DEMO_SERVER_HOME_URL = 'https://lab.suhsaechan.kr/';
export const DEMO_REQUEST_TIMEOUT_MILLISECONDS = 30000;

// 최신 버전 칩이 읽는 파일 (저장소 main 의 version.yml 이 단일 원본)
export const VERSION_FILE_URL = 'https://raw.githubusercontent.com/Cassiiopeia/pro-sejongpeia/main/version.yml';

// 배지: 레지스트리 세 곳 모두 고정 배지 (외부 조회형은 느리거나 오래된 버전을 보여 줌), 버전은 README 맨 위 최신 버전 줄이 안내
export const PACKAGE_BADGES = [
  {
    name: 'Maven Central',
    imageUrl: 'https://raw.githubusercontent.com/Cassiiopeia/pro-sejongpeia/main/docs/assets/badges/maven-central.svg',
    linkUrl: 'https://central.sonatype.com/artifact/kr.suhsaechan/sejongpeia',
  },
  {
    name: 'PyPI',
    imageUrl: 'https://raw.githubusercontent.com/Cassiiopeia/pro-sejongpeia/main/docs/assets/badges/pypi.svg',
    linkUrl: 'https://pypi.org/project/pro-sejongpeia/',
  },
  {
    name: 'npm',
    imageUrl: 'https://raw.githubusercontent.com/Cassiiopeia/pro-sejongpeia/main/docs/assets/badges/npm.svg',
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
