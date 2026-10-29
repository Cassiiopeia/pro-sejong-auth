# pro-sejongpeia (Python)

**세종대 로그인 API (Python)** · Sejong University login API

한국어 | [English](https://github.com/Cassiiopeia/pro-sejongpeia/blob/main/README.en.md)

학번과 포털 비밀번호로 세종대 재학생 여부를 확인하고 학과, 학년, 고전독서 인증, 연락처를 가져옵니다. 표준 라이브러리 외 의존성이 없습니다.

**[▶ 브라우저에서 바로 체험하기](https://cassiiopeia.github.io/pro-sejongpeia/#demo)** · 호출 한 번에 포털 로그인 한 번 (약 0.8초) · Java·Node 패키지와 같은 결과 · 비밀번호가 틀리면 바로 멈춤 · 매일 실제 포털로 점검

```bash
pip install pro-sejongpeia
```

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
| `authenticate(id, pw)` | 학생 정보 + 고전독서 + 연락처 (연락처 조회가 실패하면 연락처만 `None`) |
| `authenticate_dhc(id, pw, include_raw=False)` | 학생 정보 + 고전독서 |
| `authenticate_sis(id, pw, include_raw=False)` | 학생 정보 + 이메일, 전화번호, 영문 이름 |

설정: `SejongAuthClient(timeout_seconds=10, max_retry=3, ssl_verification=True)`.

에러 코드(`error.code`): `AUTHENTICATION_FAILED`, `ACCOUNT_LOCKED`, `ACCOUNT_UNAVAILABLE`, `INVALID_INPUT`, `CONNECTION_FAILED`, `CONNECTION_TIMEOUT`, `SESSION_ERROR`, `DATA_FETCH_FAILED`, `PARSE_ERROR`, `SSL_CONFIGURATION_ERROR`. 메시지가 아니라 코드로 분기하세요.

> **계정 잠금.** 세종대 포털은 비밀번호를 여러 번 틀리면(약 5회) 계정을 잠급니다. <https://portal.sejong.ac.kr/jsp/inquiry/pwinquiry.jsp> 에서 비밀번호를 재설정해야 풀립니다. 라이브러리는 거부된 로그인을 재시도하지 않습니다. 누구나 학번을 입력할 수 있는 서비스라면 학번별 실패 횟수를 제한하세요.

Python 3.9 이상. 세종대학교 공식 제품이 아닙니다.

문서, 라이브 데모, Java·Node 버전: https://cassiiopeia.github.io/pro-sejongpeia/

## English

Verify a Sejong University student with a student ID and portal password, and get their major, year, classic-reading certification and contact info. The API above is the same in English; error messages are in English. Full English docs: [README.en.md](https://github.com/Cassiiopeia/pro-sejongpeia/blob/main/README.en.md).
