# pro-sejongpeia (Node.js)

**세종대학교 통합 라이브러리 (Node.js)** · Sejong University integrated library: portal login and public school data (세종대 로그인 API)

한국어 | [English](https://github.com/Cassiiopeia/pro-sejongpeia/blob/main/README.en.md)

학번과 포털 비밀번호로 세종대 재학생 여부를 확인하고 학과, 학년, 고전독서 인증, 연락처를 가져옵니다. 런타임 의존성 0개, TypeScript 타입 포함, ESM·CommonJS 모두 지원합니다.

로그인 없이 쓰는 학교 공개 데이터(학과 코드, 개설강좌, 학사일정 등)도 같은 패키지에서 제공합니다. 자세한 사용법은 [루트 README](https://github.com/Cassiiopeia/pro-sejongpeia#로그인-없이-쓰는-학교-데이터)를 보세요.

**[▶ 브라우저에서 바로 체험하기](https://cassiiopeia.github.io/pro-sejongpeia/#demo)** · 호출 한 번에 포털 로그인 한 번 (약 0.7초) · Java·Python 패키지와 같은 결과 · 비밀번호가 틀리면 바로 멈춤 · 매일 실제 포털로 점검

```bash
npm install pro-sejongpeia
```

```js
import { authenticate, SejongAuthError, ErrorCode } from 'pro-sejongpeia';

try {
  const result = await authenticate('21011234', '비밀번호');
  console.log(result.name, result.major, result.grade);
} catch (error) {
  if (error instanceof SejongAuthError && error.code === ErrorCode.AUTHENTICATION_FAILED) {
    console.log('학번 또는 비밀번호가 틀렸습니다');
  } else if (error instanceof SejongAuthError && error.code === ErrorCode.ACCOUNT_LOCKED) {
    console.log('포털 계정이 잠겼습니다. 포털에서 비밀번호를 재설정하세요');
  }
}
```

| 함수 | 반환 |
|---|---|
| `authenticate(id, pw, { includeRaw, includeExtra })` | 학생 정보 + 고전독서 + 연락처 (연락처 조회가 실패하면 연락처만 `null`, `includeRaw` 면 `rawHtml`·`rawJson` 원문 포함: 세션값·접속 IP가 있어 외부에 그대로 전달 금지, `includeExtra` 면 나머지 SIS 필드 `extra`. 학과 코드 `departmentCode` 등 SIS 코드 11개는 항상 포함) |
| `authenticateDhc(id, pw, { includeRaw })` | 학생 정보 + 고전독서 |
| `fetchSisBusinessPeriods(id, pw, { year, month })` | SIS 학사 업무 기간 목록 (로그인 필요, 학교 공개 학사일정과 다른 자료, month 비우면 연간 전체) |
| `authenticateSis(id, pw, { includeRaw, includeExtra })` | 학생 정보 + 이메일, 전화번호, 영문 이름 + 학교 제공 코드 11개 (`departmentCode` 등) |

### 로그인 없이 쓰는 학교 데이터

학번·비밀번호 없이 학교가 공개로 내려주는 정보를 받습니다. Java·Python 패키지와 같은 메서드, 같은 결과입니다.

| 정보 | 메서드 |
|---|---|
| 단과대·학과 코드 (소속 분류, 학기별) | `organizationCategories`, `terms`, `departments` |
| 개설강좌 (학과·과목명·교수명으로 검색, 모두 비우면 그 학기 전체) | `lectures`, `courseTypes`, `selectiveDomains` |
| 학과 교육과정의 과목 (권장 학년·학기, 학점, 교과목 개요) | `curriculumCourses` |
| 학사일정 | `calendar` |
| 대학원 학사일정 (사이트별) | `graduateCalendar` |
| 기숙사 식단·공지 | `dormitoryMenu`, `dormitoryNotices` |
| 학식 식당 목록·메뉴, 캠퍼스 장소 | `cafeteriaPlaces`, `cafeteriaMenu`, `campusPlaces` |
| 비교과 프로그램 일정 | `extracurricularEvents` |

```js
import { SejongPublicData } from 'pro-sejongpeia';

const publicData = new SejongPublicData();
const departments = await publicData.departments({ categoryCode: '20', year: '2026', semesterCode: '20' });
const lectures = await publicData.lectures({ categoryCode: '20', year: '2026', semesterCode: '20', departmentCode: departments[0].code });
```

- **학교 값을 그대로** 돌려주고, 쓰지 않은 학교 필드는 `extra`에 담깁니다. 코드표를 박아 두지 않아 학과가 바뀌어도 코드를 고칠 필요가 없습니다.
- **한 곳이 실패해도 나머지는 그대로**: 실패하면 마지막 캐시 값이나 빈 목록을 돌려주고 예외를 던지지 않습니다 (잘못된 입력값만 예외). 결과는 메모리에 24시간 캐시합니다.
- 계약과 필드 대응: [spec/PUBLIC_DATA.md](https://github.com/Cassiiopeia/pro-sejongpeia/blob/main/spec/PUBLIC_DATA.md)

로그인 결과에는 고전독서 학기 3개(`completedSemesters`, `certifiedSemesters`, `certificationStatus`), 사용자 구분(`userCategory`), 학과의 상위 소속 이름(`parentUnitName`)도 포함됩니다.

### 대신 해결해 주는 것

직접 구현하려면 모두 풀어야 하는 부분입니다.

- 최신 환경의 기본 설정이 막는, 포털이 유일하게 허용하는 암호(`TLS_RSA_WITH_AES_256_CBC_SHA`)를 Node `https.Agent` 설정으로 자동 허용
- 방화벽이 막는 기본 User-Agent 대신 식별 가능한 User-Agent 전송
- 여러 세종대 도메인에 걸친 SSO 쿠키와 리다이렉트 처리
- 200 응답 안의 판정으로 비밀번호 오류, 계정 잠금, 이용 불가를 즉시 구분 (틀린 비밀번호는 재시도하지 않음)
- DHC의 HTML 표와 SIS의 JSON을 타입이 있는 결과로 변환

설정: `new SejongAuthClient({ timeoutSeconds: 10, maxRetry: 3, sslVerification: true })`. 서버 전용(Node 18 이상)이며 브라우저에서는 쓸 수 없습니다.

에러 코드(`error.code`): `AUTHENTICATION_FAILED`, `ACCOUNT_LOCKED`, `ACCOUNT_UNAVAILABLE`, `INVALID_INPUT`, `CONNECTION_FAILED`, `CONNECTION_TIMEOUT`, `SESSION_ERROR`, `DATA_FETCH_FAILED`, `PARSE_ERROR`, `SSL_CONFIGURATION_ERROR`. 메시지가 아니라 코드로 분기하세요.

> **계정 잠금.** 세종대 포털은 비밀번호를 여러 번 틀리면(약 5회) 계정을 잠급니다. <https://portal.sejong.ac.kr/jsp/inquiry/pwinquiry.jsp> 에서 비밀번호를 재설정해야 풀립니다. 라이브러리는 거부된 로그인을 재시도하지 않습니다. 누구나 학번을 입력할 수 있는 서비스라면 학번별 실패 횟수를 제한하세요.

세종대학교 공식 제품이 아닙니다.

문서, 라이브 데모, Java·Python 버전: https://cassiiopeia.github.io/pro-sejongpeia/

## English

Verify a Sejong University student with a student ID and portal password, and get their major, year, classic-reading certification and contact info. The API above is the same in English; error messages are in English. Full English docs: [README.en.md](https://github.com/Cassiiopeia/pro-sejongpeia/blob/main/README.en.md).

Also included: public data (no login): course lists, academic calendar, dormitory and cafeteria menus, campus places and extracurricular events via `SejongPublicData`, with caching and failure isolation. See the Korean section above and [README.en.md](https://github.com/Cassiiopeia/pro-sejongpeia/blob/main/README.en.md).
