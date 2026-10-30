# pro-sejongpeia (Python)

![pro-sejongpeia: 세종대 개발에 필요한 모든 것을 한 번에. 학번 인증부터 학과·강의·학사일정·학식까지](https://raw.githubusercontent.com/Cassiiopeia/pro-sejongpeia/main/docs/assets/banner/banner-a.ko.png)

**세종대 개발에 필요한 모든 것을 한 번에.** 학번 인증부터 학과·강의·학사일정·학식까지, Python 호출 한 번으로 받습니다. 요청은 아끼고, 비밀번호는 저장하지 않으며, 매일 실제 포털로 점검합니다.

[사이트](https://cassiiopeia.github.io/pro-sejongpeia/) · [브라우저에서 체험](https://cassiiopeia.github.io/pro-sejongpeia/#demo) · [전체 기능표](https://github.com/Cassiiopeia/pro-sejongpeia#기능) · [GitHub](https://github.com/Cassiiopeia/pro-sejongpeia) · 한국어 | [English](https://github.com/Cassiiopeia/pro-sejongpeia/blob/main/README.en.md)

[![PyPI](https://img.shields.io/badge/PyPI-pro--sejongpeia-324D60)](https://pypi.org/project/pro-sejongpeia/) [![License: MIT](https://img.shields.io/badge/License-MIT-324D60)](https://github.com/Cassiiopeia/pro-sejongpeia/blob/main/LICENSE) [![Live portal check](https://github.com/Cassiiopeia/pro-sejongpeia/actions/workflows/PROJECT-SEJONG-LIVE-CHECK.yml/badge.svg)](https://github.com/Cassiiopeia/pro-sejongpeia/actions/workflows/PROJECT-SEJONG-LIVE-CHECK.yml)

```bash
pip install pro-sejongpeia
```

## 무엇을 해 주나요

학번 인증 하나만 있는 패키지가 아닙니다. 학교가 로그인 없이 내려주는 정보도 같은 모양으로 받습니다.

- 🔐 **학번 인증**: 이름·학과·학년·학적, 고전독서 인증 현황, 이메일·전화번호, 학과 코드 같은 학교 코드. 통합 · DHC · SIS 중 선택
- 🏫 **학과·학기 코드**: 학부·대학원 분류, 학기, 학과 전체
- 📚 **개설강좌**: 학기 전체 또는 학과·과목명·교수명 검색
- 🎓 **교육과정·교과목 개요**, 📅 **학사일정** (학부·대학원, 로그인으로 보는 업무별 허용 기간)
- 🛏️ **기숙사** 7일 식단·공지, 🍚 **학식** 식당 목록과 메뉴·가격, 📍 **캠퍼스 장소**, 🎉 **비교과 일정**

## 왜 이 라이브러리인가

- ⚡ **빠르고 가볍게**: 표준 라이브러리 외 의존성 0개. 학과 목록은 0.1초 안팎, 학기 전체 강좌도 1초 안팎, 캐시된 답은 마이크로초 (Python, 서울, 5회 중앙값). 로그인은 호출 한 번에 포털 로그인 한 번 (약 0.8초)
- 🧠 **요청은 아끼고**: 24시간 메모리 캐시, 같은 인자의 동시 호출은 요청 1번, 실패는 60초 기억, 캐시 최대 500개 ([근거](https://github.com/Cassiiopeia/pro-sejongpeia/blob/main/spec/PUBLIC_DATA.md))
- 🔒 **안전하게**: 비밀번호를 저장하지 않고, 틀리면 첫 실패에서 멈춰 계정 잠금을 피하며, SIS 부가 필드에서 민감 키를 항상 제외
- ✅ **믿을 수 있게**: 매일 실제 포털에 세 언어로 로그인 점검, 세 언어 결과를 [spec/contract.json](https://github.com/Cassiiopeia/pro-sejongpeia/blob/main/spec/contract.json) 한 곳으로 맞추고 어긋나면 테스트 실패

## 바로 해 보기

학번도 비밀번호도 필요 없습니다. 학과 목록과 소프트웨어학과(3220) 개설강좌입니다.

```python
from sejongpeia import SejongPublicData

public_data = SejongPublicData()
departments = public_data.departments("20", "2026", "20")
lectures = public_data.lectures("20", "2026", "20", department_code="3220")
print(len(departments), lectures[0].name)
```

예시 출력입니다 (Python, 학교 서버 3번 호출에 0.3초).

![SejongPublicData 예시 출력: 학과 목록, 소프트웨어학과 개설강좌, 학사일정](https://raw.githubusercontent.com/Cassiiopeia/pro-sejongpeia/main/docs/assets/demo/public-data-run.png)

## 학번 인증

학번과 포털 비밀번호로 세종대 재학생 여부를 확인하고 학과, 학년, 고전독서 인증, 연락처를 가져옵니다. 표준 라이브러리 외 의존성이 없고, 호출 한 번에 포털 로그인 한 번(약 0.8초)입니다. 비밀번호가 틀리면 바로 멈추고, 매일 실제 포털로 점검합니다.

```python
from sejongpeia import authenticate, SejongAuthError, ErrorCode

try:
    result = authenticate("21011234", "비밀번호")
    print(result.name, result.major, result.grade)
except SejongAuthError as error:
    if error.code == ErrorCode.AUTHENTICATION_FAILED:
        print("학번 또는 비밀번호가 틀렸습니다")
    elif error.code == ErrorCode.ACCOUNT_LOCKED:
        print("포털 계정이 잠겼습니다. 포털에서 비밀번호를 재설정하세요")
```

| 함수 | 반환 |
|---|---|
| `authenticate(id, pw, include_raw=False, include_extra=False)` | 학생 정보 + 고전독서 + 연락처 (연락처 조회가 실패하면 연락처만 `None`, `include_raw=True` 면 `raw_html`·`raw_json` 원문 포함: 세션값·접속 IP가 있어 외부에 그대로 전달 금지, `include_extra=True` 면 나머지 SIS 필드 `extra`. 학과 코드 `department_code` 등 SIS 코드는 항상 포함) |
| `authenticate_dhc(id, pw, include_raw=False)` | 학생 정보 + 고전독서 |
| `authenticate_sis(id, pw, include_raw=False, include_extra=False)` | 학생 정보 + 이메일, 전화번호, 영문 이름 + 학교 제공 코드 (`department_code` 등) |
| `fetch_sis_business_periods(id, pw, year, month="")` | SIS 학사 업무 기간 목록 (로그인 필요, 학교 공개 학사일정과 다른 자료, month 비우면 연간 전체) |

로그인 결과에는 고전독서 학기 3개(`completed_semesters`, `certified_semesters`, `certification_status`), 사용자 구분(`user_category`), 학과의 상위 소속 이름(`parent_unit_name`)도 포함됩니다.

## 전부 지원합니다

Java·Python·Node가 **같은 기능, 같은 결과**를 줍니다. 로그인 없이 쓰는 학교 공개 데이터까지 모두 이 패키지 하나로 됩니다. 한눈에 보는 [전체 기능표](https://github.com/Cassiiopeia/pro-sejongpeia#기능)는 루트 README에 있습니다.

| 정보 (로그인 없음) | 메서드 |
|---|---|
| 단과대·학과 코드 (소속 분류, 학기별) | `organization_categories`, `terms`, `departments` |
| 개설강좌 (학과·과목명·교수명으로 검색, 모두 비우면 그 학기 전체) | `lectures`, `course_types`, `selective_domains` |
| 학과 교육과정의 과목 (권장 학년·학기, 학점, 교과목 개요) | `curriculum_courses` |
| 학사일정 | `calendar` |
| 대학원 학사일정 (사이트별) | `graduate_calendar` |
| 기숙사 식단·공지 | `dormitory_menu`, `dormitory_notices` |
| 학식 식당 목록·메뉴, 캠퍼스 장소 | `cafeteria_places`, `cafeteria_menu`, `campus_places` |
| 비교과 프로그램 일정 | `extracurricular_events` |

```python
from sejongpeia import SejongPublicData

public_data = SejongPublicData()
departments = public_data.departments("20", "2026", "20")          # 학부, 학기 값은 학교 값
lectures = public_data.lectures("20", "2026", "20", department_code=departments[0].code)
# 강의의 과목 번호로 그 학과 교육과정의 학년·학기·교과목 개요 (없으면 빈 목록)
courses = public_data.curriculum_courses(lectures[0].department_code, lectures[0].course_number)
```

- **학교 값을 그대로** 돌려주고, 쓰지 않은 학교 필드는 `extra`에 담깁니다. 코드표를 박아 두지 않아 학과가 바뀌어도 코드를 고칠 필요가 없습니다.
- **한 곳이 실패해도 나머지는 그대로**: 실패하면 마지막 캐시 값이나 빈 목록을 돌려주고 예외를 던지지 않습니다 (잘못된 입력값만 예외). 결과는 메모리에 24시간 캐시합니다.
- 계약과 필드 대응: [spec/PUBLIC_DATA.md](https://github.com/Cassiiopeia/pro-sejongpeia/blob/main/spec/PUBLIC_DATA.md)

## 대신 해결해 주는 것

직접 구현하려면 모두 풀어야 하는 부분입니다.

- 최신 환경의 기본 설정이 막는, 포털이 유일하게 허용하는 암호(`TLS_RSA_WITH_AES_256_CBC_SHA`)를 Python `ssl` 설정으로 자동 허용
- 방화벽이 막는 기본 User-Agent 대신 식별 가능한 User-Agent 전송
- 여러 세종대 도메인에 걸친 SSO 쿠키와 리다이렉트 처리
- 200 응답 안의 판정으로 비밀번호 오류, 계정 잠금, 이용 불가를 즉시 구분 (틀린 비밀번호는 재시도하지 않음)
- DHC의 HTML 표와 SIS의 JSON을 타입이 있는 결과로 변환

설정: `SejongAuthClient(timeout_seconds=10, max_retry=3, ssl_verification=True)`.

에러 코드(`error.code`): `AUTHENTICATION_FAILED`, `ACCOUNT_LOCKED`, `ACCOUNT_UNAVAILABLE`, `INVALID_INPUT`, `CONNECTION_FAILED`, `CONNECTION_TIMEOUT`, `SESSION_ERROR`, `DATA_FETCH_FAILED`, `PARSE_ERROR`, `SSL_CONFIGURATION_ERROR`. 메시지가 아니라 코드로 분기하세요.

> **계정 잠금.** 세종대 포털은 비밀번호를 여러 번 틀리면(약 5회) 계정을 잠급니다. <https://portal.sejong.ac.kr/jsp/inquiry/pwinquiry.jsp> 에서 비밀번호를 재설정해야 풀립니다. 라이브러리는 거부된 로그인을 재시도하지 않습니다. 누구나 학번을 입력할 수 있는 서비스라면 학번별 실패 횟수를 제한하세요.

Python 3.9 이상. 세종대학교 공식 제품이 아닙니다.

문서, 라이브 데모, Java·Node 버전: https://cassiiopeia.github.io/pro-sejongpeia/

Java 버전은 Maven Central에서 받습니다 (`kr.suhsaechan:sejongpeia`, 최신 버전은 [아티팩트 페이지](https://central.sonatype.com/artifact/kr.suhsaechan/sejongpeia)에서 확인).

## English

Verify a Sejong University student with a student ID and portal password, and get their major, year, classic-reading certification and contact info. The API above is the same in English; error messages are in English. Full English docs: [README.en.md](https://github.com/Cassiiopeia/pro-sejongpeia/blob/main/README.en.md).

Also included: public data (no login): course lists, academic calendar, dormitory and cafeteria menus, campus places and extracurricular events via `SejongPublicData`, with caching and failure isolation. See the Korean section above and [README.en.md](https://github.com/Cassiiopeia/pro-sejongpeia/blob/main/README.en.md).
