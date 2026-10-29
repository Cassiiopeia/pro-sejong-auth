# API 안정성 정책

한국어 | [English](API_STABILITY.en.md)

이 라이브러리를 가져다 쓰는 개발자를 위해, 무엇이 공개 API이고, 버전 번호가 무엇을 뜻하며, 무엇이 바뀔 수 있는지 솔직하게 적은 문서입니다.

## 먼저 결론

| 질문 | 답 |
|---|---|
| 하위 호환을 보장하나요? | **아니요.** 이 프로젝트는 하위 호환을 목표로 하지 않습니다 (유지보수자의 결정). 같은 `1.x` 안에서도 깨지는 변경이 생길 수 있습니다. |
| 그러면 버전 번호는 뭘 뜻하나요? | 릴리스 순서입니다. 깨지는 변경이 있는지는 번호가 아니라 **릴리스 노트**로 알려 드립니다. |
| 어떻게 쓰면 안전한가요? | 버전을 **정확히 고정**하고, 올리기 전에 릴리스 노트를 읽고, 이 문서의 「안전하게 쓰는 방법」을 따르세요. |
| 학교가 바뀌면요? | 매일 실제 학교 서버로 점검하고, 깨지면 이슈가 열려 빠르게 고칩니다. 수정 시점에 대한 약속(SLA)은 없습니다. |
| 세 언어는 같은가요? | 기능, 에러 코드, 결과 필드, 값 규칙은 같게 유지하고 테스트가 지킵니다. 이름 표기와 일부 언어 전용 기능은 다릅니다. |

## 공개 API와 내부 구현

「공개 API」는 바꿀 때 릴리스 노트에 적고 문서(README, `spec/`)를 함께 고치는 대상입니다. 「내부」는 예고 없이 바뀝니다.

| 언어 | 공개 API | 내부 (직접 쓰지 마세요) |
|---|---|---|
| Python | `sejongpeia` 패키지의 `__all__`에 있는 이름: `authenticate`, `authenticate_dhc`, `authenticate_sis`, `fetch_sis_business_periods`, `SejongAuthClient`, `SejongPublicData`, 결과·모델 클래스, `SejongAuthError`, `ErrorCode` | `sejongpeia.common.*`, `sejongpeia.auth.*`, `sejongpeia.public_data.*` 안의 모듈과 도우미 (`from sejongpeia import ...`로 가져오는 이름만 공개), 밑줄로 시작하는 이름과 인자 (`_session_factory`, `_clock`) |
| Node | 패키지 진입점(`pro-sejongpeia`)이 내보내는 함수, 클래스, 타입. `package.json`의 `exports`가 이 진입점 하나만 열어 둡니다 | 진입점에서 내보내지 않는 모든 것, 밑줄로 시작하는 옵션 (`_sessionFactory`, `_nowMilliseconds`) |
| Java | `auth.service.SuhSejongAuthEngine`의 공개 메서드, `auth.model.*`, `common.exception.*`, `publicdata.SejongPublicData`와 `publicdata.model.*`, `common.util.SejongTlsSupport`, `common.config.SejongAuthProperties`, 설정 접두사 `sejongpeia.*` | `common.util.CommonUtil`, `publicdata`의 호출기·매퍼·페처 클래스 |

Java의 `auth.client.*`, `auth.parser.*`는 Spring 없이 쓸 때 [README](../README.md)의 예시처럼 직접 만들어 엮어야 해서 공개되어 있습니다. 그래도 **생성자 모양은 바뀔 수 있는 반공개**입니다. Spring Boot 자동 설정을 쓰면 영향이 없습니다.

공개 API라도 아래는 **보장 범위 밖**입니다.

- 원본 옵션(`include_raw`, `includeRaw`, `authenticateWithRaw`)이 돌려주는 `rawHtml`, `rawJson`: 학교 원문이라 키와 구조가 학교를 따라 바뀝니다.
- `extra`의 키와 값: 학교가 필드를 더하거나 빼면 달라집니다. 이름 붙지 않은 필드에 접근하는 통로일 뿐입니다.
- 에러 메시지 문구: 분기는 항상 에러 **코드**로 하세요.
- 로그 문구, 요청 순서, 내부 캐시 방식 (캐시 기본값은 설정으로 바꿀 수 있고 문서에 적혀 있지만 구현은 바뀔 수 있음).

## 버전 규칙

- 현재 `1.x`입니다. 세 언어(Java, Python, Node)가 **버전 번호 하나**를 같이 씁니다.
- 릴리스 워크플로우가 **patch(세 번째 자리)를 자동으로 올립니다.** minor와 major는 유지보수자가 직접 올립니다.
- 이 번호는 [유의적 버전(SemVer)](https://semver.org/lang/ko/)의 약속(patch는 호환, major만 파괴)을 따르지 **않습니다.** 자동으로 올라가는 patch 릴리스에도 동작이 바뀌는 변경이 들어갈 수 있습니다.
- 지금 번호가 `1`이라서 안정 선언을 했다는 뜻도 아닙니다.
- 깨지는 변경은 이렇게 알립니다. 모든 릴리스는 [GitHub Releases](https://github.com/Cassiiopeia/pro-sejongpeia/releases)와 [CHANGELOG.md](../CHANGELOG.md)에 한 줄 요약이 남고, 이름이나 시그니처가 바뀌면 그 줄에 적습니다.

이름·시그니처 변경(메서드나 필드 이름, 패키지 경로, 설정 키)은 호환용 별칭 없이 한꺼번에 바뀔 수 있습니다. 그런 변경이 앞으로도 생길 수 있다는 뜻입니다.

### 그래도 하는 일

호환을 약속하지는 않지만, 가능한 한 사용자가 덜 놀라게 합니다.

- 새 기능은 기존 필드를 바꾸지 않고 **더하는 쪽**을 먼저 고릅니다 (결과 필드, 메서드 추가).
- 이름이나 모양을 바꿔야 하면 세 언어와 문서를 **한 PR에서** 함께 바꿉니다.
- 학교가 바꾼 것을 따라가는 변경은 사용자 코드가 깨지지 않는 방향을 우선하되, 학교 데이터 자체가 달라지는 것은 막을 수 없습니다.

## 세 언어가 같게 유지하는 것

아래는 언어가 달라도 같아야 하는 것이고, 어긋나면 테스트가 실패하도록 해 두었습니다.

| 같게 유지하는 것 | 지키는 방법 |
|---|---|
| 요청 주소, 포털 판정 → 에러 코드 대응 등 공유 값 | [spec/contract.json](../spec/contract.json)이 단일 원본이고, 언어별 contract 테스트가 자기 상수와 비교 |
| 에러 코드 10개 (`AUTHENTICATION_FAILED`, `ACCOUNT_LOCKED` 등)와 의미 | [spec/RESULT_SCHEMA.md](../spec/RESULT_SCHEMA.md), contract 테스트 |
| 인증 결과 필드와 의미 (표기만 다름) | RESULT_SCHEMA, 개인정보를 제거한 응답 샘플(`spec/fixtures/`)에 대한 기대값 테스트 |
| 공개 데이터 메서드 15종과 결과 필드, 값 규칙 (없는 값은 빈 문자열, 실패 격리, 캐시) | [spec/PUBLIC_DATA.md](../spec/PUBLIC_DATA.md), 같은 기대 결과 파일(`expected_public_data.json`)과 세 언어 비교 |
| 인증과 공개 데이터가 서로 의존하지 않음 | 의존 규칙 테스트 (Python, Node, Java 각각) |
| 실제 포털에서의 결과 | 매일 세 언어로 실제 로그인하는 점검, 필드별 해시 비교(`scripts/verify-live.sh`) |

**언어마다 다른 것**은 의도된 차이입니다.

- 이름 표기: Python `snake_case`, Node와 Java `camelCase` (메서드 `authenticate_dhc` / `authenticateDhc` / `authenticateWithDHC`처럼 Java는 일부 이름이 다름). 대응표는 README의 기능 표에 있습니다.
- 호출 방식: Python과 Java는 동기, Node는 `Promise`.
- 언어 전용: Spring Boot 자동 설정과 `sejongpeia.*` 설정 키(Java), `SejongTlsSupport`(Java), 옛 코드 `INVALID_CREDENTIALS`(Java에만 남아 있고 던지지 않음).
- 날짜·시간 타입: 인증 시각은 각 언어의 날짜·시간 타입.

## 폐기(deprecation)

- 없애려는 이름은 가능하면 한동안 남겨 두고, 문서와 릴리스 노트에 적고, Java에서는 `@Deprecated`를 답니다. 예: `SejongAuthErrorCode.INVALID_CREDENTIALS`는 더 이상 던지지 않지만 기존 코드가 컴파일되도록 남아 있습니다.
- **기간은 약속하지 않습니다.** 폐기 표시 없이 바로 사라지거나 바뀔 수 있습니다. 위 「버전 규칙」의 알림(릴리스 노트)이 유일한 공식 신호입니다.

## 학교 데이터는 학교 원문입니다

공개 데이터와 인증 결과의 학교 필드는 **학교가 준 문자열 그대로**입니다. 라이브러리는 코드표나 이름 매핑을 들고 있지 않습니다.

- 학과 코드, 소속 이름, 식당 번호, 일정 제목, 학적 상태 표기 같은 값은 **학교가 바꾸면 달라집니다.** 값 목록으로 분기하는 코드를 만들지 마세요 (예: `parentUnitName`은 학년도마다 바뀌는 표기라 인증 판정에 쓰지 않습니다).
- 날짜 형식도 학교 소스마다 다릅니다 (학사일정은 `yyyy-MM-dd`, 대학원 학사일정은 `MM.dd`). 각 형식은 [spec/PUBLIC_DATA.md](../spec/PUBLIC_DATA.md)에 적혀 있습니다.
- 값이 없으면 빈 문자열입니다 (공개 데이터는 `null`이 없음, 인증의 SIS 필드는 조회 실패 시 `null`).
- 쓰지 않은 학교 필드는 `extra`에 담기고, 학교가 필드를 더하면 새 릴리스 없이도 `extra`에 나타날 수 있습니다.

### 학교 쪽이 바뀌면

1. 매일 09:00(KST)에 점검이 돌아갑니다. 맞는 비밀번호로 세 언어가 실제 로그인하고(틀린 비밀번호 시도는 하지 않음), 계정이 필요 없는 공개 데이터 소스의 응답 모양도 확인합니다.
2. 이상을 감지하면 이슈가 자동으로 열립니다.
3. 계약 문서(`spec/`)와 세 언어 구현을 한 PR에서 고치고 릴리스합니다.

공개 데이터는 한 소스가 실패해도 예외를 던지지 않고 마지막 캐시 값이나 빈 목록을 돌려줍니다. 그래서 **빈 목록은 「데이터 없음」과 「학교 쪽 문제」를 구분하지 못합니다.** 필요하면 호출하는 쪽에서 의미를 따로 판단하세요.

## 안전하게 쓰는 방법

- 의존성 버전을 **정확히 고정**하세요 (예: `pro-sejongpeia==1.2.49`, `"pro-sejongpeia": "1.2.49"`, `kr.suhsaechan:sejongpeia:1.2.49`, 1.2.49는 작성 시점의 최신 번호). 범위 지정(`^`, `~=`, `+`)은 깨지는 변경을 자동으로 받습니다.
- 올리기 전에 [릴리스 노트](https://github.com/Cassiiopeia/pro-sejongpeia/releases)를 읽고 배포 전 테스트를 돌리세요.
- 라이브러리를 앱 안의 얇은 어댑터 한 곳으로 감싸면 바뀔 때 고칠 곳이 한 곳입니다.
- 에러는 메시지가 아니라 **코드**로 분기하세요.
- `extra`, 원본 응답, 학교 코드 값에 앱의 핵심 로직을 걸지 마세요.
- 사용 예시는 [INTEGRATION_EXAMPLES.md](INTEGRATION_EXAMPLES.md)에 있습니다.

## 이 정책이 바뀌려면

하위 호환을 약속하는 쪽으로 방침을 바꾸게 되면 이 문서를 먼저 고치고 릴리스 노트에 알립니다. 의견은 [Discussions](https://github.com/Cassiiopeia/pro-sejongpeia/discussions)나 이슈로 남겨 주세요.
