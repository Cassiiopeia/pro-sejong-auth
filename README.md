<div align="center">

<img src="docs/assets/sejong-seal.png" alt="Sejong University seal" width="120">

# pro-sejong-auth

**세종대 로그인 API: 세종대학교 포털 인증을 Java·Python·Node에서 한 번에**

한국어 | [English](README.en.md)

<!-- AUTO-VERSION-SECTION: DO NOT EDIT MANUALLY -->
## 최신 버전 : v1.2.21 (2026-10-24)

[![Maven Central](https://img.shields.io/maven-central/v/kr.suhsaechan/sejong-univ-auth?label=Maven%20Central&color=C20F2F)](https://central.sonatype.com/artifact/kr.suhsaechan/sejong-univ-auth)
[![PyPI](https://img.shields.io/pypi/v/pro-sejong-auth?label=PyPI&color=324D60)](https://pypi.org/project/pro-sejong-auth/)
[![npm](https://img.shields.io/npm/v/pro-sejong-auth?label=npm&color=324D60)](https://www.npmjs.com/package/pro-sejong-auth)
[![License: MIT](https://img.shields.io/badge/License-MIT-324D60)](LICENSE)
[![Live portal check](https://github.com/Cassiiopeia/pro-sejong-auth/actions/workflows/PROJECT-SEJONG-LIVE-CHECK.yml/badge.svg)](https://github.com/Cassiiopeia/pro-sejong-auth/actions/workflows/PROJECT-SEJONG-LIVE-CHECK.yml)

### [▶ 브라우저에서 바로 체험하기](https://cassiiopeia.github.io/pro-sejong-auth/#demo)

</div>

---

학번과 비밀번호만으로 세종대 재학생 여부, 학과, 학년, 고전독서 인증, 연락처를 가져옵니다.
포털 로그인, SSO 리다이렉트, 세션 쿠키, 오래된 TLS 방식, HTML·JSON 파싱은 라이브러리가 처리합니다. 어떤 언어로 써도 기능과 결과가 같습니다.

| Java / Spring Boot | Python | Node.js |
|---|---|---|
| `implementation "kr.suhsaechan:sejong-univ-auth:<버전>"` | `pip install pro-sejong-auth` | `npm install pro-sejong-auth` |

**호출 한 번, 포털 로그인 한 번** (약 0.6~0.8초) · **세 언어 모두 같은 결과 객체** · Python·Node는 **외부 의존성 0개** · **비밀번호가 틀리면 바로 멈춰** 계정 잠금을 막음 · **매일 실제 포털로 점검**

<p align="center">
  <a href="https://cassiiopeia.github.io/pro-sejong-auth/#demo">
    <img src="docs/assets/demo-ko.png" alt="소개 페이지의 실시간 인증 데모" width="820">
  </a>
  <br>
  <sub>소개 페이지의 실시간 데모. 실제 인증 결과이며 개인정보 칸만 예시값으로 바꿨습니다.</sub>
</p>

## 왜 이 라이브러리인가

| 직접 구현하면 | 이 라이브러리 |
|---|---|
| 포털 로그인 폼과 SSO 리다이렉트를 직접 분석 | `authenticate()`가 로그인과 SSO 과정을 처리 |
| 세종대 도메인 3곳의 세션 쿠키를 직접 관리 | 호출마다 쿠키 관리, 따로 저장할 것 없음 |
| 포털의 오래된 암호화 방식과 방화벽을 직접 통과 | 언어별로 처리됨 ([spec/PORTAL_FLOW.md](spec/PORTAL_FLOW.md)) |
| HTML 표(DHC)와 JSON API(SIS)를 직접 파싱 | 정리된 결과 객체 |
| 200 응답 속 포털 판정(비밀번호 오류, 계정 잠금)을 직접 판별 | `AUTHENTICATION_FAILED` / `ACCOUNT_LOCKED`로 즉시 실패 |
| 학생 정보 따로, 연락처 따로 두 번 로그인 | 호출 한 번에 포털 로그인 한 번 |
| 포털이 바뀐 걸 사용자 불만으로 알게 됨 | 매일 워크플로우가 세 언어로 실제 포털에 로그인하고, 실패하면 이슈를 엶 |
| 세 언어 구현을 손으로 맞춤 | 공통 값은 [spec/contract.json](spec/contract.json) 한 곳, 어긋나면 언어별 테스트가 실패 |

## 목차

- [이런 곳에 씁니다](#이런-곳에-씁니다)
- [빠른 시작: Java / Spring Boot](#빠른-시작-java--spring-boot)
- [빠른 시작: Python](#빠른-시작-python)
- [빠른 시작: Node.js](#빠른-시작-nodejs)
- [기능](#기능)
- [결과 필드](#결과-필드)
- [에러 코드](#에러-코드)
- [설정](#설정)
- [FAQ](#faq)
- [기여하기](#기여하기)

## 이런 곳에 씁니다

- 동아리·학생회·학내 커뮤니티 가입 시 **세종대 학생 인증**
- 학과·학년 조건이 있는 학내 이벤트 참여 확인
- 고전독서 인증 현황을 보여 주는 학습 도우미

| 데이터 소스 | 도메인 | 제공 정보 |
|---|---|---|
| **DHC** (대양휴머니티칼리지) | classic.sejong.ac.kr | 학생 정보 + 고전독서 인증 현황 |
| **SIS** (학사정보시스템) | sjpt.sejong.ac.kr | 학생 정보 + 이메일, 전화번호, 영문 이름 |

## 빠른 시작: Java / Spring Boot

**Maven Central**에서 배포합니다 (저장소 추가 불필요). Java 17+, Spring Boot 3.x.

```groovy
dependencies {
    implementation "kr.suhsaechan:sejong-univ-auth:<버전>"   // Maven Central 배지 확인
}
```

<details>
<summary>Maven</summary>

```xml
<dependency>
    <groupId>kr.suhsaechan</groupId>
    <artifactId>sejong-univ-auth</artifactId>
    <version><!-- Maven Central 배지 확인 --></version>
</dependency>
```

</details>

Spring Boot 자동 설정이 `SuhSejongAuthEngine` 빈을 등록합니다.

```java
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final SuhSejongAuthEngine sejongAuthEngine;

    @PostMapping("/auth/sejong")
    public SejongAuthResult authenticate(@RequestParam String studentId,
                                         @RequestParam String password) {
        return sejongAuthEngine.authenticate(studentId, password);
    }
}
```

<details>
<summary>Spring 없이 쓰기 (일반 Java)</summary>

Spring Boot는 선택 의존성이라 일반 Java 프로젝트는 받지 않습니다.

```java
SejongTlsSupport.ensureLegacyCipherEnabled();   // main() 맨 앞, 다른 HTTPS 호출보다 먼저 (TLS 안내 참고)
SejongAuthProperties properties = new SejongAuthProperties();
SuhSejongAuthEngine engine = new SuhSejongAuthEngine(new SejongPortalClient(properties), new SejongStudentInfoParser(),
    new SejongClassicReadingParser(), new SejongSisClient(properties), new SejongSisParser());
SejongAuthResult result = engine.authenticate(studentId, password);
```

</details>

## 빠른 시작: Python

Python 3.9+, 표준 라이브러리 외 의존성 없음.

```bash
pip install pro-sejong-auth
```

```python
from sejong_auth import authenticate, SejongAuthError, ErrorCode

try:
    result = authenticate("21011234", "비밀번호")
    print(result.name, result.major, result.grade)
except SejongAuthError as error:
    if error.code == ErrorCode.AUTHENTICATION_FAILED:
        print("학번 또는 비밀번호가 틀렸습니다")
    elif error.code == ErrorCode.ACCOUNT_LOCKED:
        print("포털 계정이 잠겼습니다. 포털에서 비밀번호를 재설정하세요")
```

## 빠른 시작: Node.js

Node 18+, 의존성 0개, TypeScript 타입 포함, ESM·CommonJS 모두 지원. 서버 전용입니다.

```bash
npm install pro-sejong-auth
```

```js
import { authenticate, SejongAuthError, ErrorCode } from 'pro-sejong-auth';

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

## 기능

| 기능 | Java | Python | Node |
|---|---|---|---|
| 통합: 학생 정보 + 고전독서 + 연락처 | `authenticate(id, pw)` | `authenticate(id, pw)` | `authenticate(id, pw)` |
| DHC: 학생 정보 + 고전독서 | `authenticateWithDHC` | `authenticate_dhc` | `authenticateDhc` |
| SIS: 학생 정보 + 연락처 | `authenticateWithSIS` | `authenticate_sis` | `authenticateSis` |
| 원본 응답 포함 (디버깅) | `authenticateWithDHCRaw`, `authenticateWithSISRaw` | `include_raw=True` | `{ includeRaw: true }` |

통합 인증은 포털에 **한 번만** 로그인하고 같은 세션으로 DHC와 SIS를 조회합니다. SIS(연락처) 조회가 실패해도 학생 정보는 돌려주고, 연락처 칸만 null이 됩니다.

통합 인증 실측 시간 (2026-10-24, 서울, 5회 중앙값): Java 0.57초, Python 0.76초, Node 0.69초. 대부분 포털 자체 응답 시간입니다.

**고전독서 인증 확인 (Java)**

```java
SejongDhcAuthResult result = sejongAuthEngine.authenticateWithDHC(studentId, password);

for (ClassicCertification certification : result.getClassicReading().getCertifications()) {
    System.out.println(certification.getArea() + " "
        + certification.getCertifiedCount() + "/" + certification.getRequiredCount());
}
```

## 결과 필드

아래 이름은 Java·Node 기준입니다. Python은 snake_case(`student_id`, `phone_number` 등)를 씁니다. 전체 스키마: [spec/RESULT_SCHEMA.md](spec/RESULT_SCHEMA.md)

| 필드 | 타입 | 설명 |
|---|---|---|
| `success` | boolean | 인증 성공 여부 |
| `studentId` | string | 학번 |
| `name` | string | 이름 |
| `major` | string | 학과(전공) |
| `grade` | string | 학년 |
| `status` | string | 학적 상태 (재학, 휴학, 졸업) |
| `classicReading` | object | 고전독서 인증 현황, 시험·대회·대체 이수 기록 |
| `email` | string 또는 null | 이메일 (SIS 실패 시 null) |
| `phoneNumber` | string 또는 null | 전화번호, `010-1234-5678` 형식 (SIS 실패 시 null) |
| `englishName` | string 또는 null | 영문 이름 (SIS 실패 시 null) |
| `authenticatedAt` | 날짜·시간 | 인증 시각 |

- DHC 결과: 연락처 필드 없음, 원본 옵션 사용 시 `rawHtml` 포함
- SIS 결과: 고전독서 필드 없음, `grade`·`status`는 빈 문자열, 원본 옵션 사용 시 `rawJson` 포함

## 에러 코드

모든 언어가 `code`를 가진 에러 하나를 던집니다.

| 코드 | 의미 |
|---|---|
| `AUTHENTICATION_FAILED` | 학번 또는 비밀번호가 일치하지 않음 |
| `ACCOUNT_LOCKED` | 비밀번호를 여러 번 틀려 포털이 계정을 잠금 |
| `ACCOUNT_UNAVAILABLE` | 포털 계정으로 로그인할 수 없음 (사용 기간 종료 또는 관리자 차단) |
| `INVALID_INPUT` | 입력값이 비었음 (네트워크 호출 전에 확인) |
| `CONNECTION_FAILED` | 세종대 포털에 연결할 수 없음 |
| `CONNECTION_TIMEOUT` | 포털 응답 시간 초과 |
| `DATA_FETCH_FAILED` | 학생 정보 조회 실패 |
| `PARSE_ERROR` | 포털 응답 구조 변경 등으로 파싱 실패 |
| `SESSION_ERROR` | 포털 세션 처리 오류 |
| `SSL_CONFIGURATION_ERROR` | SSL 설정 오류 |

분기는 메시지가 아니라 코드로 하세요.

> **계정 잠금.** 세종대 포털은 비밀번호를 여러 번 틀리면(포털 안내상 약 5회) 계정을 잠급니다. 잠금은 <https://portal.sejong.ac.kr/jsp/inquiry/pwinquiry.jsp> 에서 비밀번호를 재설정해야 풀리며, 라이브러리가 대신 풀 수 없습니다. 라이브러리는 거부된 로그인을 재시도하지 않고, 첫 실패에서 추가 요청 없이 멈춥니다. 누구나 학번을 입력할 수 있는 서비스라면 학번별 실패 횟수를 제한해 남이 다른 학생의 계정을 잠그지 못하게 하세요.

## 설정

모두 선택 항목입니다. 기본값: 제한 시간 10초, 로그인 재시도 3회, 인증서 검증 켬.

| Java (`application.yml`) | Python | Node |
|---|---|---|
| `sejong.auth.timeout-seconds: 10` | `SejongAuthClient(timeout_seconds=10)` | `new SejongAuthClient({ timeoutSeconds: 10 })` |
| `sejong.auth.max-retry: 3` | `max_retry=3` | `maxRetry: 3` |
| `sejong.auth.ssl-verification: true` | `ssl_verification=True` | `sslVerification: true` |

> **Java TLS 안내**: 세종 포털은 최신 JDK가 막아 둔 레거시 cipher(`TLS_RSA_WITH_AES_256_CBC_SHA`)만 제공합니다. Java 라이브러리는 `jdk.tls.disabledAlgorithms`에서 `TLS_RSA_*`를 빼는데, 이 설정은 **JVM 전체**에 적용되고 JVM의 첫 TLS 연결 전에만 효과가 있습니다. Spring Boot는 시작할 때 자동으로 처리합니다. Spring 없이 쓰면 `main()` 맨 앞에서 `SejongTlsSupport.ensureLegacyCipherEnabled()`를 부르세요. 늦게 부르면 인증이 `CONNECTION_FAILED`와 그 원인을 알리는 메시지로 실패합니다. JVM 전체 변경을 허용할 수 없다면 인증을 별도 프로세스로 분리하세요. Python·Node는 자기 연결의 TLS 설정만 바꿉니다.

포털 흐름 설명: [spec/PORTAL_FLOW.md](spec/PORTAL_FLOW.md), [DHC 로그인 플로우](docs/DHC_LOGIN_FLOW.md), [SIS 로그인 플로우](docs/SIS_LOGIN_FLOW.md)

## FAQ

<details>
<summary><b>비밀번호를 저장하나요?</b></summary>

라이브러리는 비밀번호를 저장하지 않고 포털 로그인 요청에만 씁니다. 소개 페이지의 데모 서버도 결과만 돌려주고 입력값을 저장하지 않습니다.
여러분의 서비스에서도 비밀번호를 로그나 DB에 남기지 않도록 주의하세요.
</details>

<details>
<summary><b>세종대학교 공식 라이브러리인가요?</b></summary>

아닙니다. 세종대 학생이 만든 개인 오픈소스이며 학교와 제휴 관계가 없습니다.
</details>

<details>
<summary><b>인증이 갑자기 실패해요</b></summary>

[데모](https://cassiiopeia.github.io/pro-sejong-auth/#demo)로 먼저 확인해 보세요. 데모도 실패하면 포털 쪽이 바뀌었을 가능성이 높습니다.
[이슈](https://github.com/Cassiiopeia/pro-sejong-auth/issues)에 에러 코드와 함께 남겨 주세요.
</details>

<details>
<summary><b>포털 계정이 잠겼어요</b></summary>

비밀번호를 여러 번 틀리면 포털이 계정을 잠그고, 라이브러리는 `ACCOUNT_LOCKED`를 돌려줍니다. <https://portal.sejong.ac.kr/jsp/inquiry/pwinquiry.jsp> 에서 본인 확인 후 비밀번호를 재설정하세요. 기다린다고 풀리지 않습니다.
</details>

<details>
<summary><b>예전에 SUH Nexus 저장소를 썼어요</b></summary>

예전 Java 버전은 `https://nexus.suhsaechan.kr/repository/maven-releases/`에 배포했습니다. 새 버전은 Maven Central에 올라가므로(당분간 Nexus에도 함께 배포) 저장소 설정을 지워도 됩니다.
</details>

## 기여하기

버그 제보, 포털 변경 대응, 문서 개선 모두 환영합니다. 포털이 바뀌면 세 언어를 한 PR에서 같이 고칩니다. [CONTRIBUTING.md](CONTRIBUTING.md)를 보세요.
보안 문제는 [SECURITY.md](SECURITY.md)의 비공개 신고를 이용해 주세요.

이 라이브러리로 시간을 아꼈다면 ⭐ 하나가 다른 세종대 개발자들이 찾는 데 도움이 됩니다.

## 라이선스

[MIT](LICENSE) © 2025 Suh Saechan

---

<sub>이 프로젝트는 세종대학교의 공식 제품이 아니며 세종대학교와 제휴 관계가 없습니다. 세종대학교 교표의 권리는 세종대학교에 있습니다.</sub>
