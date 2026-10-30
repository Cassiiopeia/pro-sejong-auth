<div align="center">

<div align="right">

**한국어** · [English](README.en.md)

</div>

<img src="docs/assets/sejong-seal.png" alt="Sejong University seal" width="96">

<img src="docs/assets/banner/banner-a.ko.png" alt="PRO SEJONGPEIA: 세종대 개발에 필요한 모든 것을 한 번에. 학번 인증부터 학과·강의·학사일정·학식까지, Java · Python · Node.js" width="880">

<br>

**학번 인증부터 학과·강의·학사일정·학식까지,<br>Java · Python · Node.js에서 호출 한 번으로.**

가볍고 빠르며, 요청은 아끼고, 비밀번호는 저장하지 않습니다.

**[문서 사이트](https://cassiiopeia.github.io/pro-sejongpeia/)** &nbsp;·&nbsp; [브라우저에서 체험](https://cassiiopeia.github.io/pro-sejongpeia/#demo) &nbsp;·&nbsp; [빠른 시작](https://cassiiopeia.github.io/pro-sejongpeia/start/quickstart/) &nbsp;·&nbsp; [학교 데이터](https://cassiiopeia.github.io/pro-sejongpeia/data/overview/)

[![Maven Central](docs/assets/badges/maven-central.svg)](https://central.sonatype.com/artifact/kr.suhsaechan/sejongpeia) [![PyPI](docs/assets/badges/pypi.svg)](https://pypi.org/project/pro-sejongpeia/) [![npm](docs/assets/badges/npm.svg)](https://www.npmjs.com/package/pro-sejongpeia) [![License](docs/assets/badges/license-mit.svg)](LICENSE)<br>
[![매일 실제 포털 점검](https://github.com/Cassiiopeia/pro-sejongpeia/actions/workflows/PROJECT-SEJONG-LIVE-CHECK.yml/badge.svg)](https://github.com/Cassiiopeia/pro-sejongpeia/actions/workflows/PROJECT-SEJONG-LIVE-CHECK.yml)

</div>

## 무엇을 해 주나요

학번 인증 하나만 있는 라이브러리가 아닙니다. 학교가 로그인 없이 내려주는 정보도 같은 모양으로 받습니다. 전체 목록은 아래 [기능](#기능) 표에 있습니다.

| | 기능 | 이런 걸 받아요 |
|:-:|---|---|
| <img src="docs/assets/icons/verify.svg" width="22" alt=""> | **[학번 인증](https://cassiiopeia.github.io/pro-sejongpeia/auth/methods/)** | 학번과 포털 비밀번호로 재학생 확인. 이름·학과·학년·학적, 고전독서 인증 현황, 이메일·전화번호, 학과 코드 같은 학교 코드. 통합 · DHC · SIS 중 골라 씁니다 |
| <img src="docs/assets/icons/codes.svg" width="22" alt=""> | **[학과·학기 코드](https://cassiiopeia.github.io/pro-sejongpeia/data/codes/)** | 학부와 대학원 분류, 학기 목록, 학과 전체 목록 |
| <img src="docs/assets/icons/lectures.svg" width="22" alt=""> | **[개설강좌](https://cassiiopeia.github.io/pro-sejongpeia/data/lectures/)** | 학기 전체 또는 학과·과목명·교수명 검색. 시간표·강의실 포함 |
| <img src="docs/assets/icons/curriculum.svg" width="22" alt=""> | **[교육과정·교과목 개요](https://cassiiopeia.github.io/pro-sejongpeia/data/curriculum/)** | 학과 교육과정의 권장 학년·학기, 학점, 교과목 개요 |
| <img src="docs/assets/icons/calendar.svg" width="22" alt=""> | **[학사일정](https://cassiiopeia.github.io/pro-sejongpeia/data/calendar/)** | 학부와 대학원 일정, 그리고 로그인으로 보는 수강신청·증명서 출력 같은 업무별 허용 기간 |
| <img src="docs/assets/icons/dormitory.svg" width="22" alt=""> | **[기숙사](https://cassiiopeia.github.io/pro-sejongpeia/data/dormitory/)** | 7일치 식단과 공지 |
| <img src="docs/assets/icons/cafeteria.svg" width="22" alt=""> | **[학식](https://cassiiopeia.github.io/pro-sejongpeia/data/cafeteria/)** | 식당 목록과 날짜별 메뉴·가격 |
| <img src="docs/assets/icons/campus.svg" width="22" alt=""> | **[캠퍼스 장소](https://cassiiopeia.github.io/pro-sejongpeia/data/campus-places/)** | 건물·복지시설의 이름, 위치 좌표, 안내 |
| <img src="docs/assets/icons/events.svg" width="22" alt=""> | **[비교과 일정](https://cassiiopeia.github.io/pro-sejongpeia/data/extracurricular/)** | 비교과 프로그램 일정, 분류, 장소 |

## 왜 이 라이브러리인가

네 가지를 약속하고, 약속마다 코드나 테스트로 확인할 수 있는 근거를 붙였습니다.

### <img src="docs/assets/icons/fast.svg" width="22" alt="" align="top"> 빠르고 가볍게

- **의존성 0개** (Python, Node): 표준 라이브러리만 씁니다 (`dependencies = []`, `"dependencies": {}`).
- **실측**: 학과 목록과 한 학과 강좌는 0.1초 안팎, 학기 전체 강좌도 1초 안팎이고 캐시에 있으면 마이크로초입니다 (Python, 서울, 새 클라이언트 5회 중앙값. 시간 대부분은 학교 서버 응답입니다).
- **로그인 한 번**: 통합 인증은 포털에 한 번만 로그인해 DHC와 SIS를 함께 읽습니다. 세 언어 모두 0.6~0.8초 (5회 중앙값).

→ [이 라이브러리가 하는 일](https://cassiiopeia.github.io/pro-sejongpeia/start/introduction/)

### <img src="docs/assets/icons/efficient.svg" width="22" alt="" align="top"> 요청은 아끼고

- 결과를 메모리에 **24시간 캐시**하고, 같은 인자의 **동시 호출은 요청 1번으로 합칩니다**. 캐시는 최대 500개이고 가득 차면 오래된 것부터 지웁니다.
- 호출 하나가 보내는 요청은 1번입니다. 예외는 학식 식당 목록 하나로, 한 번 갱신에 번호 범위(기본 10)만큼 **순서대로** 보냅니다.
- 실패한 호출은 **60초간 기억**해 학교 서버를 반복해서 두드리지 않습니다. 근거: [spec/PUBLIC_DATA.md](spec/PUBLIC_DATA.md)의 캐시 절.

→ [캐시와 실패 격리](https://cassiiopeia.github.io/pro-sejongpeia/understand/failure-isolation-cache/)

### <img src="docs/assets/icons/safe.svg" width="22" alt="" align="top"> 안전하게

- **비밀번호를 저장하지 않습니다.** 포털 로그인 요청에만 쓰고, 오류 메시지에 담기지 않습니다 (테스트 `test_error_message_never_contains_password`).
- **첫 실패에서 멈춥니다.** 비밀번호가 틀리면 재시도 없이 `AUTHENTICATION_FAILED`로 끝내 계정 잠금을 피하고, 잠긴 계정은 `ACCOUNT_LOCKED`로 알려 줍니다.
- SIS 부가 필드(`extra`)에서 세션값·접속 IP·계정 상태·주민번호·계좌·주소·직원 이름을 **항상 제외**하고, 학교 서버의 내부 오류 정보는 돌려주지 않습니다.
- 학교 서버가 실패해도 앱이 흔들리지 않습니다. 공개 데이터는 예외 대신 마지막 캐시 값이나 빈 목록을 줍니다.

→ [로그인 판정과 계정 잠금 방지](https://cassiiopeia.github.io/pro-sejongpeia/understand/login-verdict/)

### <img src="docs/assets/icons/reliable.svg" width="22" alt="" align="top"> 믿을 수 있게

- **매일 실제 포털 점검**: 매일 09:00(KST)에 세 언어가 실제 포털에 로그인하고, 실패하면 이슈가 자동으로 열립니다 (위 배지).
- **세 언어가 같은 결과**: 공통 값은 [spec/contract.json](spec/contract.json) 한 곳에 두고, 어긋나면 언어별 테스트가 실패합니다.
- **목록 어긋남 점검**: 공개 메서드 목록이 [spec/api-index.json](spec/api-index.json)과 세 언어 소스, 문서에서 같은지 CI가 검사하고, 학교 응답 형태는 계정 없이 매일 확인합니다.

→ [매일 도는 실제 포털 점검](https://cassiiopeia.github.io/pro-sejongpeia/understand/daily-live-check/)

직접 구현할 때 만나는 어려운 부분(포털이 허용하는 암호 하나, 방화벽 User-Agent, 도메인 3곳의 SSO 쿠키, 200 응답 속 판정)도 이미 풀려 있습니다. 자세히는 [문서 사이트](https://cassiiopeia.github.io/pro-sejongpeia/)를 보세요.

## 기능

**전부 세 언어에서 똑같이 지원합니다.** 같은 기능, 같은 결과 모양이고 이름 표기만 언어 관례(Python은 snake_case)를 따릅니다.

| 기능 | Java | Python | Node |
|---|:-:|:-:|:-:|
| **[학번 인증](https://cassiiopeia.github.io/pro-sejongpeia/auth/methods/)**: 통합 · DHC · SIS · SIS 학사 업무 기간 | ✓ | ✓ | ✓ |
| **[코드·강좌](https://cassiiopeia.github.io/pro-sejongpeia/data/codes/)**: 분류 · 학기 · 학과 · 개설강좌 · 강좌 유형 · 선택 영역 · 교육과정 | ✓ | ✓ | ✓ |
| **[일정·생활](https://cassiiopeia.github.io/pro-sejongpeia/data/overview/)**: 학사일정(학부·대학원) · 기숙사 식단·공지 · 학식 식당·메뉴 · 캠퍼스 장소 · 비교과 일정 | ✓ | ✓ | ✓ |
| [원본 응답, SIS 코드와 `extra` 옵션](https://cassiiopeia.github.io/pro-sejongpeia/auth/raw-and-extra/) | ✓ | ✓ | ✓ |

메서드 이름 전체(언어별 표기, Java 원본 응답 변형 포함)는 [인증 메서드](https://cassiiopeia.github.io/pro-sejongpeia/auth/methods/)와 [학교 데이터 공통 규칙](https://cassiiopeia.github.io/pro-sejongpeia/data/overview/)에 있습니다.

- **통합 인증**은 학생 정보 + 고전독서 + 연락처, **DHC**는 학생 정보 + 고전독서, **SIS**는 학생 정보 + 연락처입니다.
- SIS 학사 업무 기간은 공개 학사일정(`calendar`)과 다른, 본인 SIS의 업무별 허용 기간입니다. [학사 업무 기간](https://cassiiopeia.github.io/pro-sejongpeia/auth/business-periods/)
- 공개 데이터 계약은 [spec/PUBLIC_DATA.md](spec/PUBLIC_DATA.md)입니다.

## 바로 해 보기

학번도 비밀번호도 필요 없습니다. 학과 목록과 소프트웨어학과(3220) 개설강좌를 받아 옵니다. 세 언어가 같은 호출, 같은 결과입니다.

**Python** · `pip install pro-sejongpeia`

```python
from sejongpeia import SejongPublicData

public_data = SejongPublicData()
departments = public_data.departments("20", "2026", "20")
lectures = public_data.lectures("20", "2026", "20", department_code="3220")
print(len(departments), lectures[0].name)
```

**Node.js** · `npm install pro-sejongpeia`

```js
import { SejongPublicData } from 'pro-sejongpeia';

const publicData = new SejongPublicData();
const term = { categoryCode: '20', year: '2026', semesterCode: '20' };
const departments = await publicData.departments(term);
const lectures = await publicData.lectures({ ...term, departmentCode: '3220' });
console.log(departments.length, lectures[0].name);
```

**Java / Spring Boot** · `implementation "kr.suhsaechan:sejongpeia:<버전>"` ([최신 버전 확인](https://central.sonatype.com/artifact/kr.suhsaechan/sejongpeia))

```java
SejongPublicData publicData = new SejongPublicData();   // Spring에서는 빈으로 주입
List<Department> departments = publicData.departments("20", "2026", "20");
List<Lecture> lectures = publicData.lectures("20", "2026", "20", "3220", "", "");
System.out.println(departments.size() + " " + lectures.get(0).getName());
```

예시 출력입니다 (Python, 학교 서버 3번 호출에 0.3초).

<p align="center">
  <img src="docs/assets/demo/public-data-run.png" alt="SejongPublicData 예시 출력: 학과 목록, 소프트웨어학과 개설강좌, 학사일정" width="760">
</p>

### 학번 인증도 한 줄

```python
result = authenticate("21011234", "비밀번호")             # Python
print(result.name, result.major, result.grade)
```

```js
const result = await authenticate('21011234', '비밀번호');   // Node.js
console.log(result.name, result.major, result.grade);
```

```java
SejongAuthResult result = sejongAuthEngine.authenticate(studentId, password);   // Java, Spring Boot 빈
System.out.println(result.getName() + " " + result.getMajor() + " " + result.getGrade());
```

에러 처리와 Spring 설정은 아래 [빠른 시작](#빠른-시작-java--spring-boot)에 있습니다. 학번 인증은 브라우저에서 먼저 해 볼 수도 있습니다.

<p align="center">
  <a href="https://cassiiopeia.github.io/pro-sejongpeia/#demo">
    <img src="docs/assets/demo-ko.png" alt="소개 페이지의 실시간 인증 데모" width="760">
  </a>
  <br>
  <sub>소개 페이지의 실시간 데모 (눌러서 체험). 실제 인증 결과이며 개인정보 칸만 예시값으로 바꿨습니다.</sub>
</p>

## 이런 곳에 씁니다

- [**세종대 학생 인증**](https://cassiiopeia.github.io/pro-sejongpeia/guides/student-verification/): 동아리·학생회 가입, 학과·학년 조건이 있는 이벤트 확인, 고전독서 현황 도우미
- [**학과·강의 검색**](https://cassiiopeia.github.io/pro-sejongpeia/guides/department-lecture-search/): 학과별 개설강좌 검색 페이지
- [**학사일정 위젯**](https://cassiiopeia.github.io/pro-sejongpeia/guides/academic-calendar/): 이번 달 학사일정 보여 주기
- [**기숙사·학식 봇**](https://cassiiopeia.github.io/pro-sejongpeia/guides/dormitory-cafeteria-bot/): 오늘의 식단 알림

모든 예시는 세 언어로 있습니다 ([통합 예시](docs/INTEGRATION_EXAMPLES.md)).

## 빠른 시작: Java / Spring Boot

설치부터 첫 호출까지 [빠른 시작 페이지](https://cassiiopeia.github.io/pro-sejongpeia/start/quickstart/)에도 정리돼 있습니다.

[![Maven Central](docs/assets/badges/maven-central.svg)](https://central.sonatype.com/artifact/kr.suhsaechan/sejongpeia)

**Maven Central**에서 배포합니다 (저장소 추가 불필요). Java 17+, Spring Boot 3.x. Spring Boot 자동 설정이 `SuhSejongAuthEngine`과 `SejongPublicData` 빈을 등록합니다.

```groovy
dependencies {
    implementation "kr.suhsaechan:sejongpeia:<버전>"
}
```

```xml
<dependency>
    <groupId>kr.suhsaechan</groupId>
    <artifactId>sejongpeia</artifactId>
    <version><!-- 버전 --></version>
</dependency>
```

`<버전>`(`<version>`)에는 최신 버전을 넣으세요. [Maven Central 아티팩트 페이지](https://central.sonatype.com/artifact/kr.suhsaechan/sejongpeia)에서 확인하고, 전체 버전 목록은 [저장소 목록](https://repo1.maven.org/maven2/kr/suhsaechan/sejongpeia/)에 있습니다.

```java
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final SuhSejongAuthEngine sejongAuthEngine;   // kr.suhsaechan.sejongpeia.auth.service

    @PostMapping("/auth/sejong")
    public SejongAuthResult authenticate(@RequestParam String studentId,
                                         @RequestParam String password) {
        return sejongAuthEngine.authenticate(studentId, password);
    }
}
```

Spring 없이 쓰는 일반 Java는 `main()` 맨 앞에서 `SejongTlsSupport.ensureLegacyCipherEnabled()`를 먼저 부르세요. 객체를 엮는 코드는 [설정](https://cassiiopeia.github.io/pro-sejongpeia/auth/configuration/), 고전독서 인증 현황을 읽는 코드는 [결과 필드](https://cassiiopeia.github.io/pro-sejongpeia/auth/result-fields/)에 있습니다.

## 빠른 시작: Python · Node.js

[빠른 시작 페이지](https://cassiiopeia.github.io/pro-sejongpeia/start/quickstart/)에서 세 언어를 탭으로 나란히 볼 수 있습니다.

Python 3.9+ 또는 Node 18+ (의존성 0개, TypeScript 타입 포함, ESM·CommonJS, 서버 전용). 에러는 메시지가 아니라 `code`로 분기하세요.

```python
from sejongpeia import authenticate, SejongAuthError, ErrorCode

try:
    result = authenticate("21011234", "비밀번호")
except SejongAuthError as error:
    if error.code == ErrorCode.AUTHENTICATION_FAILED:
        print("학번 또는 비밀번호가 틀렸습니다")
    elif error.code == ErrorCode.ACCOUNT_LOCKED:
        print("포털 계정이 잠겼습니다. 포털에서 비밀번호를 재설정하세요")
```

```js
import { authenticate, SejongAuthError, ErrorCode } from 'pro-sejongpeia';

try {
  const result = await authenticate('21011234', '비밀번호');
} catch (error) {
  if (error instanceof SejongAuthError && error.code === ErrorCode.AUTHENTICATION_FAILED) console.log('학번 또는 비밀번호가 틀렸습니다');
  else if (error instanceof SejongAuthError && error.code === ErrorCode.ACCOUNT_LOCKED) console.log('포털 계정이 잠겼습니다');
}
```

## 로그인 없이 쓰는 학교 데이터

메서드 목록은 위 [기능](#기능), 첫 호출은 [바로 해 보기](#바로-해-보기)를 보세요. 요청 파라미터와 응답 구조는 기능별로 [학교 데이터 레퍼런스](https://cassiiopeia.github.io/pro-sejongpeia/data/overview/)에 있습니다. 강의에서 교육과정으로 이어 가는 예시입니다 (Node·Java는 같은 이름의 camelCase 메서드).

```python
from sejongpeia import SejongPublicData

public_data = SejongPublicData()
departments = public_data.departments("20", "2026", "20")          # 학부, 학기 값은 학교 값
lectures = public_data.lectures("20", "2026", "20", department_code=departments[0].code)
# 강의의 과목 번호로 그 학과 교육과정의 학년·학기·교과목 개요 (없으면 빈 목록)
courses = public_data.curriculum_courses(lectures[0].department_code, lectures[0].course_number)
```

- **학교 값을 그대로** 돌려줍니다. 코드표나 이름 매핑을 박아 두지 않아서 학과가 바뀌어도 코드를 고칠 필요가 없고, 쓰지 않은 학교 필드는 `extra`에 담깁니다.
- 실패하면 마지막 캐시 값이나 빈 목록을 돌려주고 예외를 던지지 않습니다 (잘못된 입력값만 예외). 학교 서버에 부담을 주지 않도록 쓰세요.

- 학과·과목명·교수명을 모두 비우면 그 학기 전체를 한 번에 받습니다 (응답이 크니 가능하면 학과 단위로 조회하세요). [개설강좌](https://cassiiopeia.github.io/pro-sejongpeia/data/lectures/)
- 학식 `cafeteria_places`는 학교에 식당 목록 API가 없어 메뉴 응답에서 모은 목록이라, 그날 메뉴가 빈 식당은 빠질 수 있습니다. [학식](https://cassiiopeia.github.io/pro-sejongpeia/data/cafeteria/)

## 결과 필드

이름은 Java·Node 기준이고 Python은 snake_case입니다. 공통 필드(`studentId`, `name`, `major`, `grade`, `status`), 고전독서, SIS 연락처와 학교 코드, 옵션 `extra`·원본을 [결과 필드 표](https://cassiiopeia.github.io/pro-sejongpeia/auth/result-fields/)와 [spec/RESULT_SCHEMA.md](spec/RESULT_SCHEMA.md)에 정리했습니다. `extra`와 원본에는 세션값·접속 IP가 있을 수 있으니 외부 클라이언트에 그대로 내려주지 마세요.

## 에러 코드

모든 언어가 `code`를 가진 에러 하나를 던집니다. 에러 코드(`AUTHENTICATION_FAILED`, `ACCOUNT_LOCKED`, `ACCOUNT_UNAVAILABLE`, `INVALID_INPUT`, `CONNECTION_FAILED` 등)의 뜻은 [에러 코드](https://cassiiopeia.github.io/pro-sejongpeia/auth/error-codes/)에 있습니다.

> **계정 잠금.** 포털은 비밀번호를 여러 번 틀리면 계정을 잠그고, 라이브러리는 거부된 로그인을 재시도하지 않습니다. 잠금은 <https://portal.sejong.ac.kr/jsp/inquiry/pwinquiry.jsp> 에서 비밀번호를 재설정해야 풀립니다. 누구나 학번을 입력할 수 있는 서비스라면 학번별 실패 횟수를 제한하세요.

## 설정

모두 선택 항목입니다. 기본값: 제한 시간 10초, 로그인 재시도 3회, 인증서 검증 켬.

| Java (`application.yml`) | Python | Node |
|---|---|---|
| `sejongpeia.timeout-seconds: 10` | `SejongAuthClient(timeout_seconds=10)` | `new SejongAuthClient({ timeoutSeconds: 10 })` |
| `sejongpeia.max-retry: 3` | `max_retry=3` | `maxRetry: 3` |
| `sejongpeia.ssl-verification: true` | `ssl_verification=True` | `sslVerification: true` |

Java는 포털의 레거시 cipher 때문에 JVM 전체 TLS 설정을 바꿉니다. Spring Boot는 자동 처리하고, 그 밖에는 `main()` 맨 앞에서 `SejongTlsSupport.ensureLegacyCipherEnabled()`를 부르세요. 자세한 내용은 [레거시 TLS](https://cassiiopeia.github.io/pro-sejongpeia/understand/legacy-tls/), 포털 흐름은 [spec/PORTAL_FLOW.md](spec/PORTAL_FLOW.md), [DHC 로그인 플로우](docs/DHC_LOGIN_FLOW.md), [SIS 로그인 플로우](docs/SIS_LOGIN_FLOW.md)에 있습니다.

## 더 보기: 통합 예시와 API 안정성

- [통합 예시](docs/INTEGRATION_EXAMPLES.md): 학생 인증(동아리 가입), 학과·강의 검색, 학사일정 위젯, 기숙사·학식 봇을 Java, Python, Node로.
- [API 안정성 정책](docs/API_STABILITY.md): 공개 API와 내부 구현의 경계, 버전 규칙. **하위 호환은 보장하지 않으니 버전을 고정해서 쓰세요.**
- [코딩 에이전트와 함께 쓰기](https://cassiiopeia.github.io/pro-sejongpeia/guides/agents/): Claude Code, Cursor, Codex 같은 에이전트에게 줄 [llms.txt](https://cassiiopeia.github.io/pro-sejongpeia/llms.txt), [api.json](https://cassiiopeia.github.io/pro-sejongpeia/api.json)과 프롬프트 예시. 이 저장소에 기여하는 에이전트는 [AGENTS.md](AGENTS.md)를 읽습니다.

## FAQ

<details>
<summary><b>세종대학교 공식 라이브러리인가요?</b></summary>

아닙니다. 세종대 학생이 만든 개인 오픈소스이며 학교와 제휴 관계가 없습니다.
</details>

<details>
<summary><b>인증이 갑자기 실패해요 / 포털 계정이 잠겼어요</b></summary>

[데모](https://cassiiopeia.github.io/pro-sejongpeia/#demo)로 먼저 확인해 보세요. 데모도 실패하면 포털 쪽이 바뀌었을 가능성이 높으니 [이슈](https://github.com/Cassiiopeia/pro-sejongpeia/issues)에 에러 코드와 함께 남겨 주세요.
`ACCOUNT_LOCKED`는 포털이 계정을 잠근 것으로, <https://portal.sejong.ac.kr/jsp/inquiry/pwinquiry.jsp> 에서 본인 확인 후 비밀번호를 재설정해야 합니다. 기다린다고 풀리지 않습니다.
</details>

더 많은 질문은 [사이트 FAQ](https://cassiiopeia.github.io/pro-sejongpeia/faq/)에 있습니다.

## 기여하기

버그 제보, 포털 변경 대응, 문서 개선 모두 환영합니다. 포털이 바뀌면 세 언어를 한 PR에서 같이 고칩니다. [CONTRIBUTING.md](CONTRIBUTING.md)를 보세요. 새 학교 정보 출처는 [새 출처 추가하기](https://cassiiopeia.github.io/pro-sejongpeia/contribute/add-data-source/), 구조는 [레포 구조](https://cassiiopeia.github.io/pro-sejongpeia/contribute/structure/), 이슈·PR은 [이슈와 PR](https://cassiiopeia.github.io/pro-sejongpeia/contribute/issues-and-prs/)를 참고하세요.
보안 문제는 [SECURITY.md](SECURITY.md)의 비공개 신고를 이용해 주세요.

이 라이브러리로 시간을 아꼈다면 ⭐ 하나가 다른 세종대 개발자들이 찾는 데 도움이 됩니다.

<!-- AUTO-VERSION-SECTION: DO NOT EDIT MANUALLY -->
## 최신 버전 : v1.2.72

[전체 변경 이력](CHANGELOG.md)

## 라이선스

[MIT](LICENSE) © 2025 Suh Saechan

---

<sub>이 프로젝트는 세종대학교의 공식 제품이 아니며 세종대학교와 제휴 관계가 없습니다. 세종대학교 교표의 권리는 세종대학교에 있습니다.</sub>
