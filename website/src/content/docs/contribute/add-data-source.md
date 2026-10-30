---
title: 새 정보 출처 추가하기
description: 학교가 공개로 주는 새 정보를 세 언어에 추가하는 7단계 절차와 코드 위치, 의존 규칙, 안전 규칙을 설명합니다.
---

학교가 공개로 주는 새 정보(개설강좌, 식단 같은 공개 데이터)를 추가하는 방법입니다. 소스는 서로 독립적이고, 공통 계층(`common`: 에러, HTTP 전송, 버전)만 함께 씁니다. 인증(`auth`)도 공개 데이터와 같은 하나의 소스입니다. 그래서 내 소스만 만들면 다른 소스를 건드릴 필요가 없습니다.

## 절차

1. **이슈로 제안**합니다. 이슈 템플릿 `새 정보 출처 제안`을 쓰세요. 출처 위치, 로그인 필요 여부, 요청 형태(JSON 또는 HTML), 필드, 개인정보 포함 여부를 적어 주세요. JSON이 아니고 HTML만 있는 정보는 기본 원본으로 삼지 않습니다.
2. **계약을 먼저** 씁니다. 주소, 요청 키, 고정 값은 `spec/contract.json`의 `publicData`에 넣고 (언어별 상수는 각자 두며 계약 테스트가 일치를 검사합니다), `spec/PUBLIC_DATA.md`에 메서드, 요청, 필드 대응을 한 줄씩 추가합니다. 확인한 날짜와 확인한 계정 종류도 적습니다. 에이전트용 메서드 목록 `spec/api-index.json`에도 같은 메서드를 추가합니다 (인자, 반환 모델, Java 시그니처). 세 언어 구현과 어긋나면 `python3 scripts/check-api-index.py`가 실패합니다.
3. **합성 fixture와 기대 결과**를 만듭니다. `scripts/generate-public-data-fixtures.py`의 `FIXTURES`와 출처별 `map_*()` 함수에 추가하고(`map_all()`이 합칩니다) 실행합니다. 실제 응답을 복사하지 않고, 교직원 이름 같은 개인 식별 값은 넣지 않습니다.
4. **Java, Python, Node를 한 PR에서** 구현합니다. 출처마다 파일 하나(`<출처>_source.py`, `<출처>Source.ts`, `<출처>Source.java`)에 요청 조립과 변환을 두고 `SejongPublicData`에는 공개 메서드 한 줄만 더합니다. 캐시와 실패 격리는 공유 파일(`fetcher`)을 그대로 씁니다. 값 규칙(없으면 빈 문자열, `extra`)은 계약을 따릅니다.
5. **테스트는 필요한 만큼만** 씁니다. fixture 결과가 기대 결과와 같은지 하나면 충분합니다. 학교 서버를 부르는 테스트는 넣지 않습니다.
6. **형태 점검**에 항목을 추가합니다. `scripts/probe-public-data.py`의 `SOURCES` 표에 한 줄을 더합니다. 계정 없이 도는 점검입니다.
7. [기능 노출 체크리스트](../structure/)의 항목을 확인합니다.

## 코드를 놓을 위치

공개 데이터 소스 기준입니다. 메서드 하나에 파일 세 곳이 대응합니다.

| 언어 | 메서드와 요청 | 결과 모델 | 테스트 |
|---|---|---|---|
| Python | `python/src/sejongpeia/public_data/<출처>_source.py` (요청, 변환), 공개 메서드 한 줄은 `client.py` | `public_data/models.py` | `python/tests/test_public_data.py` |
| Node | `node/src/publicData/<출처>Source.ts` (요청, 변환), 공개 메서드 한 줄은 `client.ts`, 인자 객체는 `queries.ts` | `publicData/models.ts` | `node/test/publicData.test.mjs` |
| Java | `publicdata/<출처>Source.java` (요청, 변환), 공개 메서드는 `SejongPublicData.java` | `publicdata/model/` | `SejongPublicDataTest.java` |

- 메서드 이름은 Python `snake_case`, Node와 Java `camelCase`입니다. 쓰지 않은 학교 필드는 `extra`에 담습니다.
- 로그인이 필요한 소스는 이 절차의 대상이 아닙니다. 소스끼리는 서로 임포트할 수 없어서 로그인 세션을 어떻게 나눌지 설계가 필요하니, 이슈에서 먼저 방향을 맞춰 주세요.
- 공개 메서드나 결과 필드를 더하거나 바꾸면 [API 안정성](../../api-stability/) 문서의 「공개 API」 목록과 [통합 예시](../../guides/student-verification/)가 여전히 맞는지 확인하세요.

## 의존 규칙

세 계층으로 나뉩니다. 공통(`common`)은 어떤 소스도 임포트하지 않고, 소스(`auth`, `public_data`)는 공통만 임포트하며 서로 임포트하지 않습니다. 최상위 진입점(`__init__.py`, `index.ts`, Spring `config`)은 조립만 하므로 모든 계층을 임포트할 수 있습니다. 테스트가 이 규칙을 지킵니다.

| 언어 | 규칙 테스트 |
|---|---|
| Python | `python/tests/test_dependency_rules.py` |
| Node | `node/test/dependencyRules.test.mjs` |
| Java | `DependencyRulesTest` |

공개 데이터 안에서도 출처 파일(`*_source`)끼리와 진입점(`client`)은 서로 임포트하지 않고 공유 파일(`fetcher`, `row_reader`, `models`)만 씁니다. 규칙에 걸리면 공유가 필요한 부분을 `common`으로 옮기세요.

**폴더 구조** (세 언어 같은 모양)

| 계층 | Python `python/src/sejongpeia/` | Node `node/src/` | Java `java/src/main/java/kr/suhsaechan/sejongpeia/` |
|---|---|---|---|
| 공통 | `common/` (`errors`, `transport`, `version`) | `common/` (`errors`, `transport`, `version`) | `common/` (`exception`, `util`, `http`, `config`) |
| 인증 | `auth/` (`client`, `parsers`, `portal_main_parser`, `models`) | `auth/` (`client`, `parsers`, `portalMainParser`, `models`) | `auth/` (`client`, `parser`, `model`, `service`) |
| 공개 데이터 | `public_data/` (`client`, `fetcher`, `row_reader`, `models`, `*_source`) | `publicData/` (`client`, `fetcher`, `rowReader`, `queries`, `models`, `*Source`) | `publicdata/` (`SejongPublicData`, `*Source`, `PublicDataFetcher`, `model`) |
| 조립 | `__init__.py` | `index.ts` | `config/` (Spring 자동 설정, TLS 초기화) |

## spec/에 무엇을 두는가

학교의 서비스와 세 구현 사이의 계약은 `spec/`에 있습니다. 학교가 무언가를 바꾸면 `spec/`을 먼저 고치고, 같은 PR에서 세 구현을 고칩니다.

| 종류 | 위치 | 규칙 |
|---|---|---|
| 요청·응답 계약, 필드 의미, 한계 | `spec/<출처>.md` | 출처마다 문서 하나. 확인한 것만 쓰고, 확인하지 못한 것은 못 했다고 씁니다 |
| 구현이 함께 쓰는 값 (주소, 오류 문구) | `spec/contract.json` | 단일 원본. 값이 어긋나면 언어별 contract 테스트가 실패합니다 |
| 학교가 무엇을 어디서 주는지, 막다른 길, 확인하지 못한 곳 | `spec/REFERENCE_DATA_SURVEY.md` | 다시 찾기 전에 먼저 읽고, 새로 살펴본 곳이 있으면 고칩니다 |
| 테스트용 응답 샘플 | `spec/fixtures/` | 합성 데이터만. 실제 응답이나 개인정보, 세션 값, 교직원 이름은 두지 않습니다 |
| 응답 모양 변화 감지 | `scripts/probe-*.py` | 계정 없이 돌고, 출처마다 한 항목 |

## 안전을 지키는 규칙

- 학교 문자열 그대로 돌려줍니다. 라이브러리에 값 목록이나 코드표를 두지 않고, 이름으로 코드를 짐작하지 않습니다.
- 한 출처의 실패가 다른 출처를 막지 않습니다. 특히 인증은 막지 않습니다. 빈 값이나 마지막 캐시 값을 돌려줍니다.
- 내부 서버 정보(스택 트레이스, 세션 식별자, IP 주소, 비밀번호 상태 플래그)를 로그나 결과에 남기지 않습니다.
- 공개 페이지와 엔드포인트에는 부담을 주지 않습니다. 캐시하고, 간격을 두고, 한 번 갱신에 보내는 요청 수를 제한합니다.

더 자세한 내용은 [실패 격리와 캐시](../../understand/failure-isolation-cache/)와 [공통 규칙](../../data/overview/)을 보세요.
