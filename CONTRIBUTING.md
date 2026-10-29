# 기여하기

한국어 | [English](CONTRIBUTING.en.md)

버그 제보, 포털 변경 대응, 문서 개선 모두 환영합니다. 이슈와 PR은 한국어, 영어 모두 괜찮습니다.

보안 취약점은 공개 이슈로 올리지 말고 [SECURITY.md](SECURITY.md)의 비공개 신고를 이용해 주세요.

## 이슈 먼저

- 버그: 에러 코드(`SejongAuthException.getErrorCode()`)와 사용한 메서드를 적어 주세요. 학번·비밀번호·개인정보는 절대 올리지 마세요.
- 포털이 바뀌어 인증이 깨졌다면 [데모](https://cassiiopeia.github.io/pro-sejong-auth/#demo)로 먼저 재현 여부를 확인해 주세요.
- 큰 변경은 PR 전에 이슈로 방향을 맞춥니다.

## 레포 구성

| 폴더 | 내용 |
|---|---|
| `java/` | Java / Spring Boot 라이브러리 (`kr.suhsaechan:sejong-univ-auth`) |
| `python/` | Python 패키지 (`pro-sejong-auth`, import `sejong_auth`) |
| `node/` | Node.js 패키지 (`pro-sejong-auth`, TypeScript) |
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

## PR

- 브랜치는 `main` 기준으로 만듭니다.
- 동작이 바뀌면 테스트를 함께 추가합니다. 파서 변경은 `spec/fixtures/`에 샘플을 두고 모든 언어가 같은 기대값을 쓰게 합니다.
- 로그·예외 메시지에 비밀번호나 개인정보가 남지 않는지 확인합니다.

## 라이선스

기여한 코드는 [MIT 라이선스](LICENSE)로 배포됩니다.
