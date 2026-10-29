// 사이트 공통 설정과 사이드바: astro.config.mjs 와 scripts/generate-llms.mjs 가 같이 읽는 단일 원본
export const REPOSITORY_URL = 'https://github.com/Cassiiopeia/pro-sejongpeia';
export const SITE_URL = 'https://cassiiopeia.github.io';
// 프로젝트 Pages는 /<레포명>/ 아래에서 서빙되므로 base를 맞춰야 에셋 경로가 깨지지 않는다.
export const BASE_PATH = '/pro-sejongpeia';
export const SITE_TITLE = 'pro-sejongpeia';
export const SITE_DESCRIPTION_KO =
  '세종대학교 통합 라이브러리: 포털 로그인(학번 인증)과 로그인 없이 쓰는 학교 데이터를 Java, Python, Node.js로 한 번에.';
export const SITE_DESCRIPTION_EN =
  'Sejong University library: portal login (student verification) and login-free school data for Java, Python and Node.js.';

// 사이드바 그룹: label 은 한국어, translations.en 은 영어. items 는 docs 폴더 기준 문서 경로 (영어는 en/ 아래 같은 경로)
export const SIDEBAR = [
  {
    label: '시작하기',
    translations: { en: 'Start' },
    items: ['start/introduction', 'start/quickstart'],
  },
  {
    label: '가이드',
    translations: { en: 'Guides' },
    items: [
      'guides/student-verification',
      'guides/department-lecture-search',
      'guides/academic-calendar',
      'guides/dormitory-cafeteria-bot',
      'guides/agents',
    ],
  },
  {
    label: '학교 데이터 레퍼런스',
    translations: { en: 'School data reference' },
    items: [
      'data/overview',
      'data/codes',
      'data/lectures',
      'data/calendar',
      'data/dormitory',
      'data/cafeteria',
      'data/campus-places',
      'data/extracurricular',
      'data/curriculum',
    ],
  },
  {
    label: '인증 레퍼런스',
    translations: { en: 'Authentication reference' },
    items: [
      'auth/methods',
      'auth/result-fields',
      'auth/error-codes',
      'auth/configuration',
      'auth/raw-and-extra',
      'auth/business-periods',
    ],
  },
  {
    label: '이해하기',
    translations: { en: 'Understand' },
    items: [
      'understand/legacy-tls',
      'understand/firewall-user-agent',
      'understand/sso-flow',
      'understand/login-verdict',
      'understand/failure-isolation-cache',
      'understand/daily-live-check',
    ],
  },
  {
    label: '기여하기',
    translations: { en: 'Contribute' },
    items: ['contribute/add-data-source', 'contribute/structure', 'contribute/issues-and-prs'],
  },
  {
    label: '더 보기',
    translations: { en: 'More' },
    items: ['api-stability', 'faq'],
  },
];
