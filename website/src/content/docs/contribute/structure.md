---
title: 레포 구조
description: 레포 폴더 구성, 개발 환경 명령, 실제 포털 테스트 방법, 포털 변경 대응 규칙, 문서 언어 규칙, 기능 노출 체크리스트를 정리합니다.
---

## 폴더 구성

| 폴더 | 내용 |
|---|---|
| `java/` | Java / Spring Boot 라이브러리 (`kr.suhsaechan:sejongpeia`) |
| `python/` | Python 패키지 (`pro-sejongpeia`, import `sejongpeia`) |
| `node/` | Node.js 패키지 (`pro-sejongpeia`, TypeScript) |
| `spec/` | 공통 계약: 포털 흐름, 결과 스키마, 모든 언어 테스트가 쓰는 개인정보 제거 응답 샘플 |
| `website/` | 이 문서 사이트 (Astro Starlight). 한국어가 기본, 영어는 `/en/` |
| `scripts/` | 실제 포털 점검, 형태 점검, fixture 생성, 문서 점검 스크립트 |
| `docs/` | 통합 예시, API 안정성, 로그인 흐름 문서와 이미지 |

세 언어는 버전 번호 하나를 같이 씁니다. `version.yml`을 두고 릴리스 워크플로우가 갱신합니다. 각 언어 소스 안의 폴더 구조(`common`, `auth`, `public_data`)는 [새 정보 출처 추가하기](../add-data-source/)의 표에 있습니다.

## 개발 환경

```bash
./gradlew build                                   # Java (JDK 17+, Gradle Wrapper 포함)
cd python && python -m pip install pytest && python -m pytest   # Python 3.9+
cd node && npm ci && npm test                     # Node 18+
```

## 실제 포털 연동 테스트

실제 세종대 포털에 로그인하므로 계정을 넣었을 때만 실행됩니다.

- Python, Node: `SEJONG_TEST_STUDENT_ID`, `SEJONG_TEST_PASSWORD` 환경변수를 설정하고 위 명령으로 실행합니다.
- Java: `java/src/test/resources/application-dev.yml`을 만듭니다 (`.gitignore` 대상).

  ```yaml
  sejong:
    auth:
      test:
        student-id: "본인 학번"
        password: "본인 비밀번호"
  ```

  `SuhSejongAuthEngineTest`의 `실제인증_*` 테스트는 `@Disabled`입니다. 로컬에서만 빼고 실행한 뒤 커밋 전에 되돌립니다.
- 세 언어를 한 번에 돌리고 결과를 필드별로 비교합니다 (개인정보는 출력하지 않음).

  ```bash
  SEJONG_TEST_STUDENT_ID=... SEJONG_TEST_PASSWORD=... scripts/verify-live.sh
  ```

- 맞는 비밀번호로만 점검합니다 (틀린 비밀번호 시도 없음): `scripts/live-check.sh`. `PROJECT-SEJONG-LIVE-CHECK` 워크플로우가 매일 실행하고, 실패하면 이슈를 엽니다. 자세한 내용은 [매일 도는 실제 포털 점검](../../understand/daily-live-check/)에 있습니다.

:::caution
비밀번호를 여러 번 틀리면 포털 계정이 잠길 수 있습니다. 실패하면 반복 실행하지 말고 원인을 먼저 확인하세요.
:::

## 포털이 바뀌었을 때

1. `spec/PORTAL_FLOW.md`와 `spec/contract.json`을 고칩니다. 응답 모양이 바뀌었으면 `spec/fixtures/`도 고치되 개인정보는 제거합니다. 언어별 contract 테스트는 상수가 `spec/contract.json`과 같아질 때까지 실패합니다.
2. Java, Python, Node를 **한 PR에서** 같이 고칩니다.
3. 언어별 테스트와 `scripts/verify-live.sh`를 실행합니다.

## 문서 언어

- 기본 문서는 한국어 `X.md`, 번역은 `X.<언어코드>.md`입니다 (예: `README.en.md`). 새 언어는 같은 규칙으로 파일을 추가하면 됩니다.
- 문서 맨 위 언어 전환 줄에 같은 문서의 모든 언어를 링크합니다.
- 한국어 문서를 고치면 번역도 같은 PR에서 고칩니다. 번역을 못 하면 PR 설명에 적어 주세요.
- `README.md`의 `## 최신 버전 :` 줄은 릴리스 워크플로우가 갱신하므로 직접 고치지 않습니다.
- 확인: `python3 scripts/check-docs.py` (깨진 상대 링크, 언어 전환 링크 누락). PR에서는 `PROJECT-SEJONG-DOCS-CHECK`도 실행됩니다.
- 이 사이트(`website/`)의 모든 페이지는 한국어와 영어 짝이 있어야 합니다. 한쪽을 고치면 다른 쪽도 같은 PR에서 고칩니다.

## 기능 노출 체크리스트

기능은 만들었다고 끝이 아닙니다. 사용하는 사람이 보는 모든 곳에 드러나야 합니다. PR을 올리기 전에 확인하세요. PR 템플릿에도 같은 체크리스트가 있습니다.

- README 한국어·영어: 기능 소개와 사용 예시, 필드가 늘었다면 `결과 필드` 표
- `python/README.md`, `node/README.md`: PyPI, npm 패키지 페이지
- `spec/`: 계약 문서, `spec/contract.json`, 합성 fixture
- 문서 사이트 `website/`: 기능 소개, 필드 표, 해당 레퍼런스 페이지 (한국어·영어)
- 패키지 설명과 키워드: `python/pyproject.toml`, `node/package.json`, `java/build.gradle`
- 릴리스 노트에 들어갈 한 줄 요약 (커밋 메시지)
- 홍보 문구는 실제로 지원하는 기능만 씁니다

다음으로 [이슈와 PR](../issues-and-prs/) 규칙을 보세요.
