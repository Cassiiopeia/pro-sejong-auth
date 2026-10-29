# 기여하기

한국어 | [English](CONTRIBUTING.en.md)

버그 제보, 포털 변경 대응, 문서 개선 모두 환영합니다. 이슈와 PR은 한국어, 영어 모두 괜찮습니다.

보안 취약점은 공개 이슈로 올리지 말고 [SECURITY.md](SECURITY.md)의 비공개 신고를 이용해 주세요.

질문과 제안은 이슈 대신 Discussions에, 어디에 무엇을 올릴지는 [SUPPORT.md](SUPPORT.md)를 봐 주세요. 참여자는 [행동 강령](CODE_OF_CONDUCT.md)을 따릅니다. 처음이라면 `good first issue`, 도움이 필요한 이슈는 `help wanted`, 새 학교 정보 출처 제안은 `data-source` 라벨을 찾아보세요.

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

학교가 주는 새 정보(개설강좌, 식단 같은 공개 데이터)를 추가하는 방법입니다. 소스는 서로 독립적이고, 공통 계층(`transport`, `errors`)만 함께 씁니다. 그래서 내 소스만 만들면 다른 소스를 건드릴 필요가 없습니다.

1. **이슈로 제안**합니다 (`새 정보 출처 제안` 이슈 템플릿을 쓰세요). 출처 위치, 로그인 필요 여부, 요청 형태(JSON 또는 HTML), 필드, 개인정보 포함 여부를 적어 주세요. JSON이 아니고 HTML만 있는 정보는 기본 원본으로 삼지 않습니다.
2. **계약을 먼저** 씁니다: `spec/PUBLIC_DATA.md`에 메서드, 요청, 필드 대응을 한 줄씩 추가합니다. 확인한 날짜와 확인한 계정 종류도 적습니다.
3. **합성 fixture와 기대 결과**를 만듭니다: `scripts/generate-public-data-fixtures.py`의 `FIXTURES`와 `map_all()`에 추가하고 실행합니다. 실제 응답을 복사하지 않고, 교직원 이름 같은 개인 식별 값은 넣지 않습니다.
4. **Java, Python, Node를 한 PR에서** 구현합니다. 요청, 캐시, 실패 격리는 `SejongPublicData`의 공통 헬퍼를 그대로 씁니다. 값 규칙(없으면 빈 문자열, `extra`)은 계약을 따릅니다.
5. **테스트는 필요한 만큼만**: fixture 결과가 기대 결과와 같은지 하나면 충분합니다. 학교 서버를 부르는 테스트는 넣지 않습니다.
6. **형태 점검**에 항목을 추가합니다 (`scripts/probe-public-data.py`의 `SOURCES` 표에 한 줄, 계정 없이 도는 점검).
7. 아래 `새 기능을 추가했다면` 노출 체크를 확인합니다.

의존 규칙은 테스트가 지킵니다: 인증 코드와 공개 데이터는 서로 임포트하지 않습니다(`python/tests/test_dependency_rules.py`, `node/test/dependencyRules.test.mjs`, Java `DependencyRulesTest`). 규칙에 걸리면 공유가 필요한 부분을 `transport`나 `errors`로 옮기세요.

## 새 기능을 추가했다면

기능은 만들었다고 끝이 아닙니다. 사용하는 사람이 보는 모든 곳에 드러나야 합니다. PR을 올리기 전에 확인하세요 (PR 템플릿에도 같은 체크리스트가 있습니다).

- README 한국어·영어: 기능 소개와 사용 예시, 필드가 늘었다면 `결과 필드` 표
- `python/README.md`, `node/README.md`: PyPI, npm 패키지 페이지
- `spec/`: 계약 문서, `spec/contract.json`, 합성 fixture
- 소개 사이트(`gh-pages` 브랜치의 `index.html`, `sitemap.xml`): 기능 소개, 필드 표, 데모. 릴리즈 뒤에 반영하므로 해야 할 일을 이슈에 남겨 둡니다
- 패키지 설명과 키워드: `python/pyproject.toml`, `node/package.json`, `java/build.gradle`
- 릴리즈 노트에 들어갈 한 줄 요약 (커밋 메시지)
- 홍보 문구는 실제로 지원하는 기능만 씁니다

## PR

- 브랜치는 `main` 기준으로 만듭니다.
- 동작이 바뀌면 테스트를 함께 추가합니다. 파서 변경은 `spec/fixtures/`에 샘플을 두고 모든 언어가 같은 기대값을 쓰게 합니다.
- 로그·예외 메시지에 비밀번호나 개인정보가 남지 않는지 확인합니다.

## 라이선스

기여한 코드는 [MIT 라이선스](LICENSE)로 배포됩니다.
