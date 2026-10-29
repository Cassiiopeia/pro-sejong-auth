// 랜딩 페이지 문구와 공통 상수: 컴포넌트는 언어별 문구를 여기서만 읽는다 (단일 원본)
export type LandingLocale = 'ko' | 'en';

export const REPOSITORY_URL = 'https://github.com/Cassiiopeia/pro-sejongpeia';

// 체험 폼이 호출하는 데모 서버: 라이브러리로 포털에 한 번 로그인하고 결과만 돌려줌
export const DEMO_AUTH_API_URL = 'https://lab.suhsaechan.kr/api/sejong-auth/authenticate';
export const DEMO_SERVER_HOME_URL = 'https://lab.suhsaechan.kr/';
export const DEMO_REQUEST_TIMEOUT_MILLISECONDS = 30000;

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

interface FeatureCard {
  title: string;
  body: string;
  methods: string;
  link: string;
}
interface HardPartCard {
  title: string;
  body: string;
  link: string;
}
interface MatrixRow {
  feature: string;
  detail: string;
}

export interface LandingCopy {
  hero: { eyebrow: string; titleLine: string; titleEmphasis: string; lead: string; tryDemo: string; quickStart: string; github: string };
  firstCall: { title: string; lead: string; authLabel: string; publicLabel: string; note: string; passwordPlaceholder: string };
  demo: {
    title: string;
    lead: string;
    studentIdLabel: string;
    studentIdPlaceholder: string;
    passwordLabel: string;
    passwordPlaceholder: string;
    authTypeLabel: string;
    authIntegrated: string;
    authIntegratedDescription: string;
    authDhc: string;
    authDhcDescription: string;
    authSis: string;
    authSisDescription: string;
    submit: string;
    submitting: string;
    privacyBefore: string;
    privacyServerName: string;
    privacyAfter: string;
    resultTitle: string;
    maskToggle: string;
    tabTable: string;
    tabJson: string;
    resultEmpty: string;
    success: string;
    failure: string;
    classicTitle: string;
    rowLabels: Record<string, string>;
    inputRequired: string;
    networkError: string;
    timeoutError: string;
    errorMeanings: Record<string, string>;
  };
  features: { title: string; lead: string; cards: FeatureCard[] };
  matrix: { title: string; lead: string; featureHeader: string; supported: string; note: string; rows: MatrixRow[] };
  hardParts: { title: string; lead: string; cards: HardPartCard[] };
  closing: { title: string; lead: string; docs: string; stability: string; contribute: string; disclaimer: string };
}

export const LANDING_COPY: Record<LandingLocale, LandingCopy> = {
  ko: {
    hero: {
      eyebrow: 'Pro Sejongpeia · 세종대 개발자들의 작은 유토피아',
      titleLine: '세종대학교를 라이브러리 하나로,',
      titleEmphasis: '로그인부터 학교 데이터까지',
      lead: '학번과 비밀번호로 재학생을 확인하고, 로그인 없이 학과·강의·학사일정·기숙사·학식·캠퍼스 정보를 받습니다. Java, Python, Node.js에서 같은 메서드, 같은 결과입니다.',
      tryDemo: '바로 체험하기',
      quickStart: '빠른 시작',
      github: 'GitHub',
    },
    firstCall: {
      title: '30초면 첫 호출',
      lead: '설치하고 두 줄이면 됩니다. 아래 코드를 그대로 붙여 넣으세요.',
      authLabel: '학번 인증',
      publicLabel: '로그인 없이 학과 코드 받기',
      note: '서버에서만 호출하세요. 학번과 비밀번호를 브라우저나 앱에서 직접 보내면 제3자 코드에 노출됩니다.',
      passwordPlaceholder: '비밀번호',
    },
    demo: {
      title: '지금 바로 체험하기',
      lead: '본인 세종대 포털 계정으로 라이브러리가 실제로 무엇을 돌려주는지 확인해 보세요.',
      studentIdLabel: '학번',
      studentIdPlaceholder: '예: 21011234',
      passwordLabel: '포털 비밀번호',
      passwordPlaceholder: '세종대 포털 비밀번호',
      authTypeLabel: '조회 방식',
      authIntegrated: '통합 (추천)',
      authIntegratedDescription: '학생 정보 + 고전독서 + 연락처 전부',
      authDhc: 'DHC',
      authDhcDescription: '학생 정보 + 고전독서 인증 현황',
      authSis: 'SIS',
      authSisDescription: '학생 정보 + 이메일, 전화번호, 영문 이름',
      submit: '인증하기',
      submitting: '포털에 로그인하는 중…',
      privacyBefore: '입력값은 저장하지 않습니다. 데모 서버(',
      privacyServerName: 'lab.suhsaechan.kr',
      privacyAfter: ')가 이 라이브러리로 세종대 포털에 한 번 로그인하고 결과만 돌려줍니다.',
      resultTitle: '인증 결과',
      maskToggle: '개인정보 가리기',
      tabTable: '표',
      tabJson: 'JSON',
      resultEmpty: '왼쪽에 학번과 비밀번호를 넣고 인증하기를 누르세요.',
      success: '인증 성공',
      failure: '인증 실패',
      classicTitle: '고전독서 인증 현황',
      rowLabels: {
        authType: '조회 방식',
        studentId: '학번',
        name: '이름',
        major: '학과',
        grade: '학년',
        status: '상태',
        email: '이메일',
        phoneNumber: '전화번호',
        englishName: '영문 이름',
        authenticatedAt: '인증 시각',
      },
      inputRequired: '학번과 비밀번호를 모두 입력하세요.',
      networkError: '데모 서버에 연결하지 못했습니다. 잠시 뒤 다시 시도하세요.',
      timeoutError: '응답이 30초 안에 오지 않았습니다. 포털이 느릴 수 있으니 잠시 뒤 다시 시도하세요.',
      errorMeanings: {
        AUTHENTICATION_FAILED: '학번 또는 비밀번호가 일치하지 않음',
        INVALID_INPUT: '입력값이 비었거나 형식이 맞지 않음',
        CONNECTION_FAILED: '세종대 포털에 연결할 수 없음',
        CONNECTION_TIMEOUT: '포털 응답 시간 초과',
        DATA_FETCH_FAILED: '학생 정보 조회 실패',
        PARSE_ERROR: '포털 응답 구조 변경 등으로 파싱 실패',
        SESSION_ERROR: '포털 세션 처리 오류',
        SSL_CONFIGURATION_ERROR: 'SSL 설정 오류',
        ACCOUNT_LOCKED:
          '비밀번호를 여러 번 틀려 포털 계정이 잠김. 포털 비밀번호 찾기(https://portal.sejong.ac.kr/jsp/inquiry/pwinquiry.jsp)에서 재설정',
        ACCOUNT_UNAVAILABLE: '포털 계정으로 로그인할 수 없음 (사용 기간 종료 또는 관리자 차단)',
        TOO_MANY_ATTEMPTS: '같은 학번 3번 연속 실패로 30분 동안 데모 시도 차단 (포털 계정 잠금 방지)',
      },
    },
    features: {
      title: '무엇을 받을 수 있나요',
      lead: '로그인이 필요한 학생 인증 하나와, 로그인 없이 받는 학교 데이터 일곱 가지입니다. 카드를 누르면 메서드와 결과 필드가 나옵니다.',
      cards: [
        {
          title: '학생 인증',
          body: '재학 여부, 이름, 학과, 학년, 고전독서 인증 현황, 연락처를 한 번의 로그인으로 받습니다.',
          methods: 'authenticate',
          link: 'auth/methods/',
        },
        {
          title: '학과·강의',
          body: '학과·학기 코드와 개설강좌를 학과, 과목명, 교수명으로 찾습니다. 모두 비우면 그 학기 전체입니다.',
          methods: 'departments · lectures',
          link: 'data/lectures/',
        },
        {
          title: '학사일정',
          body: '기간을 주면 학부와 대학원 학사일정을 날짜와 제목으로 돌려줍니다.',
          methods: 'calendar · graduate_calendar',
          link: 'data/calendar/',
        },
        {
          title: '기숙사',
          body: '일주일 식단(조식·중식·석식)과 공지 목록을 받습니다.',
          methods: 'dormitory_menu · dormitory_notices',
          link: 'data/dormitory/',
        },
        {
          title: '학식',
          body: '식당 목록(번호·이름·건물·위치)과 식당별 메뉴, 가격을 받습니다.',
          methods: 'cafeteria_places · cafeteria_menu',
          link: 'data/cafeteria/',
        },
        {
          title: '캠퍼스 지도',
          body: '건물과 편의시설의 이름, 위치(위도·경도), 안내 정보를 받습니다.',
          methods: 'campus_places',
          link: 'data/campus-places/',
        },
        {
          title: '비교과',
          body: '기간별 비교과 프로그램을 분류와 함께 받습니다.',
          methods: 'extracurricular_events',
          link: 'data/extracurricular/',
        },
        {
          title: '교육과정',
          body: '학과 코드와 과목 번호로 권장 학년·학기, 이수 구분, 학점, 교과목 개요를 받습니다.',
          methods: 'curriculum_courses',
          link: 'data/curriculum/',
        },
      ],
    },
    matrix: {
      title: '세 언어가 모두 지원합니다',
      lead: '기능, 결과 필드, 에러 코드, 값 규칙이 같습니다. 어긋나면 공통 계약 테스트가 실패하도록 해 두었습니다.',
      featureHeader: '기능',
      supported: '지원',
      note: '실행 환경은 다릅니다. Java는 17 이상 (Spring Boot 3.x 자동 설정), Python은 3.9 이상, Node.js는 18 이상이며 Python과 Node.js는 외부 의존성이 없습니다. 이름 표기만 Python snake_case, Node.js와 Java camelCase로 다릅니다.',
      rows: [
        { feature: '통합 인증', detail: '학생 정보 + 고전독서 + 연락처, 로그인 1회' },
        { feature: 'DHC · SIS 개별 인증', detail: '필요한 쪽만 조회' },
        { feature: '학사 업무 기간', detail: '수강신청, 성적 조회 등 (로그인 필요)' },
        { feature: '원문 · extra 옵션', detail: '학교 원문 응답과 나머지 필드' },
        { feature: '학과 · 학기 코드', detail: '소속 분류, 학기, 학과, 이수 구분, 선택 영역' },
        { feature: '개설강좌 검색', detail: '학과 · 과목명 · 교수명' },
        { feature: '교육과정', detail: '학년 · 학기 · 학점 · 교과목 개요' },
        { feature: '학사일정', detail: '학부 · 대학원' },
        { feature: '기숙사 식단 · 공지', detail: '7일 식단, 공지 검색' },
        { feature: '학식 식당 · 메뉴', detail: '식당 목록과 메뉴, 가격' },
        { feature: '캠퍼스 장소', detail: '건물 · 편의시설, 위도 · 경도' },
        { feature: '비교과 프로그램', detail: '기간별 일정' },
        { feature: '에러 코드 10종', detail: '같은 코드, 같은 의미' },
        { feature: '메모리 캐시 · 실패 격리', detail: '공개 데이터 기본 24시간' },
      ],
    },
    hardParts: {
      title: '직접 구현하면 어려운 것들',
      lead: '포털 로그인은 눈에 보이는 것보다 훨씬 까다롭습니다. 아래를 모두 라이브러리가 대신 처리합니다. 카드를 누르면 자세한 설명이 나옵니다.',
      cards: [
        {
          title: '레거시 TLS 암호',
          body: '포털이 허용하는 유일한 암호(TLS_RSA_WITH_AES_256_CBC_SHA)를 최신 JDK, Python, Node 기본 설정이 막습니다. 언어마다 다른 방식으로 풀어 두었습니다.',
          link: 'understand/legacy-tls/',
        },
        {
          title: '방화벽과 User-Agent',
          body: '기본 라이브러리 User-Agent는 방화벽이 막고, 없으면 일부 사이트가 404를 돌려줍니다. 식별 가능한 User-Agent를 자동으로 보냅니다.',
          link: 'understand/firewall-user-agent/',
        },
        {
          title: 'SSO 쿠키와 리다이렉트',
          body: '세종대 도메인 여러 곳에 걸친 SSO 쿠키와 리다이렉트를 처리해, 로그인 한 번으로 DHC와 SIS를 모두 조회합니다.',
          link: 'understand/sso-flow/',
        },
        {
          title: '로그인 판정과 계정 보호',
          body: '200 응답 안의 스크립트 값으로 비밀번호 오류, 계정 잠금, 이용 불가를 구분합니다. 틀린 비밀번호는 재시도하지 않아 계정 잠금을 막습니다.',
          link: 'understand/login-verdict/',
        },
        {
          title: '실패 격리와 캐시',
          body: '공개 데이터는 한 곳이 실패해도 나머지가 그대로 동작하고, 마지막 캐시 값이나 빈 목록을 돌려줍니다. 같은 요청은 한 번만 보냅니다.',
          link: 'understand/failure-isolation-cache/',
        },
        {
          title: '매일 도는 실제 포털 점검',
          body: '매일 세 언어로 실제 포털에 로그인하고, 계정이 필요 없는 공개 데이터의 응답 모양도 확인합니다. 이상이 있으면 이슈가 열립니다.',
          link: 'understand/daily-live-check/',
        },
      ],
    },
    closing: {
      title: '더 알아보기',
      lead: '이 프로젝트는 세종대 학생이 만든 개인 오픈소스입니다. 하위 호환을 보장하지 않으니 버전을 고정해서 쓰세요.',
      docs: '문서 소개',
      stability: 'API 안정성',
      contribute: '기여하기',
      disclaimer:
        '이 프로젝트는 세종대학교의 공식 제품이 아니며 세종대학교와 제휴 관계가 없습니다. 세종대학교 교표의 권리는 세종대학교에 있습니다.',
    },
  },
  en: {
    hero: {
      eyebrow: 'Pro Sejongpeia · A little utopia for Sejong developers',
      titleLine: 'Sejong University in one library,',
      titleEmphasis: 'from login to school data',
      lead: 'Verify a student with a student ID and password, and get departments, courses, the academic calendar, dormitory, cafeteria and campus data without logging in. The same methods and the same results in Java, Python and Node.js.',
      tryDemo: 'Try it live',
      quickStart: 'Quick start',
      github: 'GitHub',
    },
    firstCall: {
      title: 'Your first call in 30 seconds',
      lead: 'Install it and write two lines. Paste the code below as it is.',
      authLabel: 'Student verification',
      publicLabel: 'Department codes without login',
      note: 'Call it from your server only. Sending a student ID and password straight from a browser or an app exposes them to third-party code.',
      passwordPlaceholder: 'password',
    },
    demo: {
      title: 'Try it live',
      lead: 'Sign in with your own Sejong portal account and see exactly what the library returns.',
      studentIdLabel: 'Student ID',
      studentIdPlaceholder: 'e.g. 21011234',
      passwordLabel: 'Portal password',
      passwordPlaceholder: 'Sejong portal password',
      authTypeLabel: 'Lookup',
      authIntegrated: 'Integrated (recommended)',
      authIntegratedDescription: 'Student info + classic reading + contact',
      authDhc: 'DHC',
      authDhcDescription: 'Student info + classic-reading status',
      authSis: 'SIS',
      authSisDescription: 'Student info + email, phone, English name',
      submit: 'Authenticate',
      submitting: 'Signing in to the portal…',
      privacyBefore: 'Nothing you type is stored. The demo server (',
      privacyServerName: 'lab.suhsaechan.kr',
      privacyAfter: ') uses this library to sign in to the Sejong portal once and returns the result.',
      resultTitle: 'Result',
      maskToggle: 'Hide personal info',
      tabTable: 'Table',
      tabJson: 'JSON',
      resultEmpty: 'Enter your student ID and password, then press Authenticate.',
      success: 'Authenticated',
      failure: 'Failed',
      classicTitle: 'Classic reading certification',
      rowLabels: {
        authType: 'Lookup',
        studentId: 'Student ID',
        name: 'Name',
        major: 'Major',
        grade: 'Year',
        status: 'Status',
        email: 'Email',
        phoneNumber: 'Phone',
        englishName: 'English name',
        authenticatedAt: 'Authenticated at',
      },
      inputRequired: 'Enter both your student ID and password.',
      networkError: 'Could not reach the demo server. Please try again later.',
      timeoutError: 'No response within 30 seconds. The portal may be slow; please try again.',
      errorMeanings: {
        AUTHENTICATION_FAILED: 'Student ID or password is wrong',
        INVALID_INPUT: 'Input is empty or malformed',
        CONNECTION_FAILED: 'Cannot reach the Sejong portal',
        CONNECTION_TIMEOUT: 'Portal timed out',
        DATA_FETCH_FAILED: 'Failed to fetch student info',
        PARSE_ERROR: 'Could not parse the portal response (structure changed?)',
        SESSION_ERROR: 'Portal session error',
        SSL_CONFIGURATION_ERROR: 'SSL configuration error',
        ACCOUNT_LOCKED:
          'The portal locked the account after too many wrong passwords. Reset it at https://portal.sejong.ac.kr/jsp/inquiry/pwinquiry.jsp',
        ACCOUNT_UNAVAILABLE: 'The portal account cannot sign in (use period ended or blocked by the administrator)',
        TOO_MANY_ATTEMPTS: '3 failed attempts in a row for this student ID: demo blocked for 30 minutes to protect the portal account',
      },
    },
    features: {
      title: 'What you get',
      lead: 'One login-based student verification and seven kinds of school data you get without logging in. Open a card for the methods and result fields.',
      cards: [
        {
          title: 'Student verification',
          body: 'Enrollment status, name, major, year, classic-reading certification and contact info from a single login.',
          methods: 'authenticate',
          link: 'auth/methods/',
        },
        {
          title: 'Departments and courses',
          body: 'Department and term codes, and course listings by department, course name or instructor. Leave all empty for the whole term.',
          methods: 'departments · lectures',
          link: 'data/lectures/',
        },
        {
          title: 'Academic calendar',
          body: 'Give a date range and get the undergraduate or graduate calendar with dates and titles.',
          methods: 'calendar · graduate_calendar',
          link: 'data/calendar/',
        },
        {
          title: 'Dormitory',
          body: 'The weekly menu (breakfast, lunch, dinner) and the notice list.',
          methods: 'dormitory_menu · dormitory_notices',
          link: 'data/dormitory/',
        },
        {
          title: 'Cafeterias',
          body: 'The cafeteria list (number, name, building, location), plus menus and prices per cafeteria.',
          methods: 'cafeteria_places · cafeteria_menu',
          link: 'data/cafeteria/',
        },
        {
          title: 'Campus map',
          body: 'Buildings and facilities with names, coordinates and info.',
          methods: 'campus_places',
          link: 'data/campus-places/',
        },
        {
          title: 'Extracurricular',
          body: 'Extracurricular programs for a period, with their categories.',
          methods: 'extracurricular_events',
          link: 'data/extracurricular/',
        },
        {
          title: 'Curriculum',
          body: 'Suggested year and semester, course type, credits and the course overview from a department code and course number.',
          methods: 'curriculum_courses',
          link: 'data/curriculum/',
        },
      ],
    },
    matrix: {
      title: 'All three languages support everything',
      lead: 'Features, result fields, error codes and value rules are the same. A shared contract test fails if they drift apart.',
      featureHeader: 'Feature',
      supported: 'Supported',
      note: 'Runtimes differ. Java 17+ (Spring Boot 3.x auto-configuration), Python 3.9+, Node.js 18+; Python and Node.js have no external dependencies. Only naming differs: Python snake_case, Node.js and Java camelCase.',
      rows: [
        { feature: 'Integrated verification', detail: 'Student info + classic reading + contact, one login' },
        { feature: 'DHC and SIS verification', detail: 'Look up only the side you need' },
        { feature: 'Academic business periods', detail: 'Course registration, grade lookup and more (login required)' },
        { feature: 'Raw and extra options', detail: 'The school\'s raw response and the remaining fields' },
        { feature: 'Department and term codes', detail: 'Categories, terms, departments, course types, selective domains' },
        { feature: 'Course search', detail: 'By department, course name, instructor' },
        { feature: 'Curriculum', detail: 'Year, semester, credits, course overview' },
        { feature: 'Academic calendar', detail: 'Undergraduate and graduate' },
        { feature: 'Dormitory menu and notices', detail: '7-day menu, notice search' },
        { feature: 'Cafeterias and menus', detail: 'Cafeteria list, menus, prices' },
        { feature: 'Campus places', detail: 'Buildings, facilities, coordinates' },
        { feature: 'Extracurricular programs', detail: 'Schedule by period' },
        { feature: '10 error codes', detail: 'Same codes, same meaning' },
        { feature: 'In-memory cache and failure isolation', detail: 'Public data, 24 hours by default' },
      ],
    },
    hardParts: {
      title: 'What you would otherwise build',
      lead: 'Portal login is trickier than it looks. The library handles all of this for you. Open a card for the details.',
      cards: [
        {
          title: 'Legacy TLS cipher',
          body: 'The portal accepts only one cipher (TLS_RSA_WITH_AES_256_CBC_SHA), which current JDK, Python and Node defaults block. It is re-enabled the right way in each language.',
          link: 'understand/legacy-tls/',
        },
        {
          title: 'Firewall and User-Agent',
          body: 'The firewall blocks default library User-Agents, and some sites answer 404 without one. An identifying User-Agent is sent automatically.',
          link: 'understand/firewall-user-agent/',
        },
        {
          title: 'SSO cookies and redirects',
          body: 'Handles SSO cookies and redirects across several Sejong domains, so one login serves both DHC and SIS.',
          link: 'understand/sso-flow/',
        },
        {
          title: 'Login verdict and account safety',
          body: 'Reads the verdict hidden in a 200 response to tell wrong password, locked account and unavailable account apart. A wrong password is never retried, so the account does not get locked.',
          link: 'understand/login-verdict/',
        },
        {
          title: 'Failure isolation and cache',
          body: 'One public data source failing never blocks another; you get the last cached value or an empty list. Identical requests are sent once.',
          link: 'understand/failure-isolation-cache/',
        },
        {
          title: 'A daily check against the live portal',
          body: 'Every day the three languages sign in to the real portal, and the shape of the public data responses is checked without credentials. An issue opens on any anomaly.',
          link: 'understand/daily-live-check/',
        },
      ],
    },
    closing: {
      title: 'Read on',
      lead: 'This is a personal open-source project by a Sejong student. It does not guarantee backward compatibility, so pin the version.',
      docs: 'Introduction',
      stability: 'API stability',
      contribute: 'Contribute',
      disclaimer:
        'This project is not an official product of Sejong University and is not affiliated with it. The Sejong University seal belongs to Sejong University.',
    },
  },
};
