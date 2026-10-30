// 랜딩 문구: 기능 카드, 기능×언어 표, 직접 구현의 어려움 (ko/en)

import type { LandingCopy, LandingLocale } from './types';

type FeatureSectionsCopy = Pick<LandingCopy, 'features' | 'matrix' | 'hardParts'>;

export const FEATURES_COPY: Record<LandingLocale, FeatureSectionsCopy> = {
  ko: {
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
  },
  en: {
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
  },
};
