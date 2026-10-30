# 기여하기

한국어 | [English](CONTRIBUTING.en.md)

버그 제보, 포털 변경 대응, 문서 개선 모두 환영합니다. 이슈와 PR은 한국어, 영어 모두 괜찮습니다.

보안 취약점은 공개 이슈로 올리지 말고 [SECURITY.md](SECURITY.md)의 비공개 신고를 이용해 주세요.

질문과 제안은 이슈 대신 Discussions에, 어디에 무엇을 올릴지는 [SUPPORT.md](SUPPORT.md)를 봐 주세요. 참여자는 [행동 강령](CODE_OF_CONDUCT.md)을 따릅니다. 처음이라면 `good first issue`, 도움이 필요한 이슈는 `help wanted`, 새 학교 정보 출처 제안은 `data-source` 라벨을 찾아보세요.

코딩 에이전트(Claude Code, Cursor, Codex 등)로 작업한다면 에이전트가 [AGENTS.md](AGENTS.md)를 먼저 읽게 하세요. 구조, 의존 규칙, 테스트 명령, 하지 말아야 할 것이 짧게 모여 있습니다.

## 이슈 먼저

- 버그: 에러 코드(`SejongAuthException.getErrorCode()`)와 사용한 메서드를 적어 주세요. 학번·비밀번호·개인정보는 절대 올리지 마세요.
- 포털이 바뀌어 인증이 깨졌다면 [데모](https://cassiiopeia.github.io/pro-sejongpeia/#demo)로 먼저 재현 여부를 확인해 주세요.
- 큰 변경은 PR 전에 이슈로 방향을 맞춥니다.

## 레포 구성

| 폴더 | 내용 |
|---|---|
| `java/` | Java / Spring Boot 라이브러리 (`kr.suhsaechan:sejongpeia`) |
| `python/` | Python 패키지 (`pro-sejongpeia`, import `sejongpeia`) |
| `node/` | Node.js 패키지 (`pro-sejongpeia`, TypeScript) |
| `spec/` | 공통 계약: 포털 흐름, 결과 스키마, 모든 언어 테스트가 쓰는 개인정보 제거 응답 샘플 |

세 언어는 버전 번호 하나를 같이 씁니다 (`version.yml`, 릴리스 워크플로우가 갱신).

## 개발 환경

```bash
./gradlew build                                   # Java (JDK 17+, Gradle Wrapper 포함)
cd python && python -m pip install pytest && python -m pytest   # Python 3.9+
cd node && npm ci && npm test                     # Node 18+
```

## 실제 포털 연동 테스트

실제 세종대 포털에 로그인하므로 계정을 넣었을 때만 실행됩니다.

- Python·Node: `SEJONG_TEST_STUDENT_ID`, `SEJONG_TEST_PASSWORD` 환경변수를 설정하고 위 명령으로 실행
- Java: `java/src/test/resources/application-dev.yml` 생성 (`.gitignore` 대상)

  ```yaml
  sejong:
    auth:
      test:
        student-id: "본인 학번"
        password: "본인 비밀번호"
  ```

  `SuhSejongAuthEngineTest`의 `실제인증_*` 테스트는 `@Disabled`입니다. 로컬에서만 빼고 실행한 뒤 커밋 전에 되돌립니다.
- 세 언어 한 번에, 결과를 필드별로 비교 (개인정보 출력 없음):

  ```bash
  SEJONG_TEST_STUDENT_ID=... SEJONG_TEST_PASSWORD=... scripts/verify-live.sh
  ```

- 맞는 비밀번호로만 점검 (틀린 비밀번호 시도 없음): `scripts/live-check.sh`. `PROJECT-SEJONG-LIVE-CHECK` 워크플로우가 매일 실행하고, 실패하면 이슈를 엽니다.

> 비밀번호를 여러 번 틀리면 포털 계정이 잠길 수 있습니다. 실패하면 반복 실행하지 말고 원인을 먼저 확인하세요.

## 포털이 바뀌었을 때

1. `spec/PORTAL_FLOW.md`와 `spec/contract.json` 수정 (응답 모양이 바뀌었으면 `spec/fixtures/`도, 개인정보는 제거). 언어별 contract 테스트는 상수가 `spec/contract.json`과 같아질 때까지 실패합니다
2. Java, Python, Node를 **한 PR에서** 같이 수정
3. 언어별 테스트와 `scripts/verify-live.sh` 실행

## 문서 언어

- 기본 문서는 한국어 `X.md`, 번역은 `X.<언어코드>.md`입니다 (예: `README.en.md`). 새 언어는 같은 규칙으로 파일을 추가하면 됩니다.
- 문서 맨 위 언어 전환 줄에 같은 문서의 모든 언어를 링크합니다 (예: `한국어 | [English](README.en.md) | [日本語](README.ja.md)`).
- 한국어 문서를 고치면 번역도 같은 PR에서 고칩니다. 번역을 못 하면 PR 설명에 적어 주세요.
- `README.md`의 `## 최신 버전 :` 줄은 릴리스 워크플로우가 갱신하므로 직접 고치지 않습니다.
- 확인: `python3 scripts/check-docs.py` (깨진 상대 링크, 언어 전환 링크 누락). PR에서도 `PROJECT-SEJONG-DOCS-CHECK`가 실행됩니다.

## 새 정보 출처 추가하기

학교가 주는 새 정보(개설강좌, 식단 같은 공개 데이터)를 추가하는 방법입니다. 소스는 서로 독립적이고, 공통 계층(`common`: 에러, HTTP 전송, 버전)만 함께 씁니다. 인증(`auth`)도 공개 데이터와 같은 하나의 소스입니다. 그래서 내 소스만 만들면 다른 소스를 건드릴 필요가 없습니다.

1. **이슈로 제안**합니다 (`새 정보 출처 제안` 이슈 템플릿을 쓰세요). 출처 위치, 로그인 필요 여부, 요청 형태(JSON 또는 HTML), 필드, 개인정보 포함 여부를 적어 주세요. JSON이 아니고 HTML만 있는 정보는 기본 원본으로 삼지 않습니다.
2. **계약을 먼저** 씁니다: 주소, 요청 키, 고정 값은 `spec/contract.json`의 `publicData`에 넣고 (언어별 상수는 각자 두며 계약 테스트가 일치를 검사합니다), `spec/PUBLIC_DATA.md`에 메서드, 요청, 필드 대응을 한 줄씩 추가합니다. 확인한 날짜와 확인한 계정 종류도 적습니다. 에이전트용 메서드 목록 `spec/api-index.json`에도 같은 메서드를 추가합니다 (인자, 반환 모델, Java 시그니처). 세 언어 구현과 어긋나면 `python3 scripts/check-api-index.py`가 실패합니다.
3. **합성 fixture와 기대 결과**를 만듭니다: `scripts/generate-public-data-fixtures.py`의 `FIXTURES`와 `map_all()`에 추가하고 실행합니다. 실제 응답을 복사하지 않고, 교직원 이름 같은 개인 식별 값은 넣지 않습니다.
4. **Java, Python, Node를 한 PR에서** 구현합니다. 요청, 캐시, 실패 격리는 `SejongPublicData`의 공통 헬퍼를 그대로 씁니다. 값 규칙(없으면 빈 문자열, `extra`)은 계약을 따릅니다.
5. **테스트는 필요한 만큼만**: fixture 결과가 기대 결과와 같은지 하나면 충분합니다. 학교 서버를 부르는 테스트는 넣지 않습니다.
6. **형태 점검**에 항목을 추가합니다 (`scripts/probe-public-data.py`의 `SOURCES` 표에 한 줄, 계정 없이 도는 점검).
7. 아래 `새 기능을 추가했다면` 노출 체크를 확인합니다.

의존 규칙은 테스트가 지킵니다. 세 계층으로 나뉩니다: 공통(`common`)은 어떤 소스도 임포트하지 않고, 소스(`auth`, `public_data`)는 공통만 임포트하며 서로 임포트하지 않습니다(`python/tests/test_dependency_rules.py`, `node/test/dependencyRules.test.mjs`, Java `DependencyRulesTest`). 규칙에 걸리면 공유가 필요한 부분을 `common`으로 옮기세요. 최상위 진입점(`__init__.py`, `index.ts`, Spring `config`)은 조립만 하므로 모든 계층을 가져올 수 있습니다.

**폴더 구조** (세 언어 같은 모양)

| 계층 | Python `python/src/sejongpeia/` | Node `node/src/` | Java `java/src/main/java/kr/suhsaechan/sejongpeia/` |
|---|---|---|---|
| 공통 | `common/` (`errors`, `transport`, `version`) | `common/` (`errors`, `transport`, `version`) | `common/` (`exception`, `util`, `http`, `config`) |
| 인증 | `auth/` (`client`, `parsers`, `portal_main_parser`, `models`) | `auth/` (`client`, `parsers`, `portalMainParser`, `models`) | `auth/` (`client`, `parser`, `model`, `service`) |
| 공개 데이터 | `public_data/` (`client`, `models`) | `publicData/` (`client`, `models`) | `publicdata/` (`model`) |
| 조립 | `__init__.py` | `index.ts` | `config/` (Spring 자동 설정, TLS 초기화) |

**코드를 놓을 위치** (공개 데이터 소스 기준, 메서드 하나에 파일 세 곳이 대응합니다)

| 언어 | 메서드와 요청 | 결과 모델 | 테스트 |
|---|---|---|---|
| Python | `python/src/sejongpeia/public_data/client.py` | `public_data/models.py` | `python/tests/test_public_data.py` |
| Node | `node/src/publicData/client.ts` | `publicData/models.ts` | `node/test/publicData.test.mjs` |
| Java | `publicdata/SejongPublicData.java`, 변환은 `PublicDataMapper.java` | `publicdata/model/` | `SejongPublicDataTest.java` |

- 메서드 이름은 Python `snake_case`, Node와 Java `camelCase`입니다. 쓰지 않은 학교 필드는 `extra`에 담습니다.
- 로그인이 필요한 소스는 이 절차의 대상이 아닙니다. 소스끼리는 서로 임포트할 수 없어서 로그인 세션을 어떻게 나눌지 설계가 필요하니, 이슈에서 먼저 방향을 맞춰 주세요.
- 공개 메서드나 결과 필드를 더하거나 바꾸면 [API 안정성 정책](docs/API_STABILITY.md)의 「공개 API」 목록과 [통합 예시](docs/INTEGRATION_EXAMPLES.md)가 여전히 맞는지 확인하세요.

## 새 기능을 추가했다면

기능은 만들었다고 끝이 아닙니다. 사용하는 사람이 보는 모든 곳에 드러나야 합니다. PR을 올리기 전에 확인하세요 (PR 템플릿에도 같은 체크리스트가 있습니다).

- README 한국어·영어: 기능 소개와 사용 예시, 필드가 늘었다면 `결과 필드` 표
- `python/README.md`, `node/README.md`: PyPI, npm 패키지 페이지
- `spec/`: 계약 문서, `spec/contract.json`, 합성 fixture
- 소개 사이트(`website/`, 한국어·영어 문서): 기능 소개, 레퍼런스, 가이드. `main`에 머지되면 자동으로 배포되므로 같은 PR에서 함께 고칩니다
- 패키지 설명과 키워드: `python/pyproject.toml`, `node/package.json`, `java/build.gradle`
- 릴리즈 노트에 들어갈 한 줄 요약 (커밋 메시지)
- 홍보 문구는 실제로 지원하는 기능만 씁니다

## AI·에이전트 기여 정책

- 코딩 에이전트가 쓴 PR도 환영합니다. PR을 올리는 사람이 [AGENTS.md](AGENTS.md)의 명령을 직접 실행해 통과를 확인하고, PR 설명에 사용한 에이전트(예: Claude Code, Codex)를 적어 주세요.
- 개인정보와 학교의 실제 응답을 코드, fixture, 이슈, PR에 넣지 않습니다. 합성 값만 씁니다.
- 검토 기준은 사람이 쓴 PR과 같습니다. 올린 사람이 변경 내용을 설명할 수 있어야 합니다.

## PR

- 브랜치는 `main` 기준으로 만듭니다.
- 동작이 바뀌면 테스트를 함께 추가합니다. 파서 변경은 `spec/fixtures/`에 샘플을 두고 모든 언어가 같은 기대값을 쓰게 합니다.
- 로그·예외 메시지에 비밀번호나 개인정보가 남지 않는지 확인합니다.

## 라이선스

기여한 코드는 [MIT 라이선스](LICENSE)로 배포됩니다.
