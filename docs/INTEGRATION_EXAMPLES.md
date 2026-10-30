# 통합 예시

한국어 | [English](INTEGRATION_EXAMPLES.en.md)

자주 쓰는 네 가지 용도를 Java, Python, Node.js로 복사해서 바로 쓸 수 있게 모았습니다. 시작 전에 [API 안정성 정책](API_STABILITY.md)(깨지는 변경이 생길 수 있어 버전을 고정하세요)을 먼저 읽어 보세요.

| 용도 | 로그인 |
|---|---|
| [1. 학생 인증 (동아리 가입)](#1-학생-인증-동아리-가입) | 필요 |
| [2. 학과·강의 검색 페이지](#2-학과강의-검색-페이지) | 불필요 |
| [3. 학사일정 위젯](#3-학사일정-위젯) | 불필요 |
| [4. 기숙사·학식 안내 봇](#4-기숙사학식-안내-봇) | 불필요 |

학교 데이터는 바뀌므로 응답 예시의 값은 형태를 보여 주기 위한 것입니다. 예시 코드는 참고용이며, Java의 Spring 어노테이션(`@Service`, `@RestController` 등)은 Spring 의존성이 있는 프로젝트에서 그대로 쓰는 형태입니다.

## 준비

| Java / Spring Boot | Python | Node.js |
|---|---|---|
| `implementation "kr.suhsaechan:sejongpeia:<version>"` | `pip install pro-sejongpeia==<version>` | `npm install pro-sejongpeia@<version>` |

- `<version>`은 최신 릴리스 번호입니다 ([Maven Central](https://central.sonatype.com/artifact/kr.suhsaechan/sejongpeia), [PyPI](https://pypi.org/project/pro-sejongpeia/), [npm](https://www.npmjs.com/package/pro-sejongpeia)).
- Spring Boot는 `SuhSejongAuthEngine`과 `SejongPublicData`를 빈으로 등록합니다. 일반 Java는 [설정 문서](https://cassiiopeia.github.io/pro-sejongpeia/auth/configuration/)의 「Java: Spring 없이 쓰기」를 보세요.
- `SejongPublicData`(공개 데이터)는 **프로세스당 하나만** 만들어 쓰세요. 메모리 캐시(기본 24시간)와 같은 요청 합치기가 인스턴스 안에 있습니다.
- 공개 데이터는 학교 서버 실패 시 예외 없이 마지막 캐시 또는 **빈 목록**을 돌려줍니다. 빈 목록은 「데이터 없음」일 수도 「학교 쪽 문제」일 수도 있어, 화면에는 「불러오지 못했습니다」와 같은 안내를 함께 두는 것이 좋습니다.
- Python 예시는 3.9 이상, Node 예시는 18 이상(ESM, 최상위 `await`)에서 동작하는 문법만 썼습니다.

## 1. 학생 인증 (동아리 가입)

가입 폼에서 받은 학번과 비밀번호로 재학 여부를 확인하고, 학번·이름·학과만 가져옵니다. 비밀번호는 저장하거나 로그에 남기지 않습니다.

### 요청 파라미터

필드 이름은 Python `snake_case` 기준이고, Node.js와 Java는 `camelCase`입니다. 포털 로그인이 필요합니다.

| 메서드 | 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|---|
| `authenticate` | `student_id` | 문자열 | 예 | | 학번 |
| | `password` | 문자열 | 예 | | 포털 비밀번호. 저장하거나 로그에 남기지 않음 |
| | `include_raw` | boolean | 아니오 | `false` | 포털 원문(`rawHtml`, `rawJson`)도 받음 |
| | `include_extra` | boolean | 아니오 | `false` | 학교가 주는 나머지 SIS 필드를 `extra`로 받음 |
| `authenticate_dhc` | `student_id`, `password`, `include_raw` | | | | 학번·이름·학과·학년·학적 상태만 필요할 때 (연락처 조회 없이 더 가벼움) |

### 응답 데이터 구조

`authenticate`가 돌려주는 결과(위쪽)와 예시 함수가 가입 폼에 돌려주는 값(아래쪽)입니다. 필드 이름은 Python `snake_case`이고(`student_id`), Node.js와 Java는 `camelCase`(`studentId`, `getStudentId()`)입니다. 값은 형태를 보여 주기 위한 가상의 예시입니다.

```json
{
  "success": true,
  "student_id": "20000000",
  "name": "홍길동",
  "major": "컴퓨터공학과",
  "grade": "3",
  "status": "재학",
  "email": "user@example.com",
  "phone_number": "010-0000-0000",
  "english_name": "Gildong Hong",
  "user_category": "학부생"
}
```

```json
{ "verified": true, "student_id": "20000000", "name": "홍길동", "major": "컴퓨터공학과" }
```

| 필드 | 의미 |
|---|---|
| `student_id` / `name` / `major` | 학번 / 이름 / 학과(전공). 예시 함수가 돌려주는 값 |
| `status` | 학적 상태 (재학, 휴학, 졸업 등). 포털 원문이라 표기가 바뀔 수 있음 |
| `grade` | 학년 (문자열) |
| `email` / `phone_number` / `english_name` / `user_category` | 연락처(SIS) 조회 결과. SIS 조회가 실패하면 `null`. 그 밖의 SIS 학교 코드 필드는 생략함 |
| `verified` | 예시 함수가 `status`를 `재학`과 비교해 만든 값. 라이브러리 필드가 아님 |

전체 필드는 [결과 필드](https://cassiiopeia.github.io/pro-sejongpeia/auth/result-fields/)를 보세요.

**실패 시**: 비밀번호 오류, 계정 잠김 등은 `SejongAuthError`(에러 코드 포함)로 던져집니다. [에러 코드](https://cassiiopeia.github.io/pro-sejongpeia/auth/error-codes/)

### 알아둘 점

- **틀린 비밀번호를 반복해서 보내지 마세요.** 포털이 계정을 잠급니다. 라이브러리는 첫 실패에서 멈추지만, 누구나 학번을 입력할 수 있는 폼이라면 **학번별 실패 횟수를 제한**하세요 ([README의 계정 잠금 안내](../README.md#에러-코드)).
- `status`, `major`는 포털의 원문 문자열입니다. 위 `재학` 비교는 예시이며, 표기가 바뀔 수 있으니 한 곳에 상수로 두세요.
- 한 번의 `authenticate`는 연락처(SIS) 조회까지 포함합니다. 학번·이름·학과만 필요하면 DHC만 부르는 `authenticate_dhc` / `authenticateDhc` / `authenticateWithDHC`가 더 가볍습니다 (결과 필드는 각각 [기능 표](../README.md#기능) 참고).
- 서버에서만 호출하세요. 브라우저나 앱에서 직접 부르면 비밀번호가 제3자 코드에 노출됩니다.

### 예시 코드

**Python**

```python
from sejongpeia import authenticate, SejongAuthError, ErrorCode

ACTIVE_STATUS = "재학"  # 학교 원문 문자열 그대로 비교 (포털 표기가 바뀌면 여기만 수정)


def verify_sejong_student(student_id: str, password: str) -> dict:
    """동아리 가입 시 학생 인증: 학번, 이름, 학과만 돌려주고 비밀번호는 보관하지 않음"""
    try:
        result = authenticate(student_id, password)
    except SejongAuthError as error:
        if error.code == ErrorCode.AUTHENTICATION_FAILED:
            return {"verified": False, "reason": "학번 또는 비밀번호가 틀렸습니다"}
        if error.code == ErrorCode.ACCOUNT_LOCKED:
            return {"verified": False, "reason": "포털 계정이 잠겼습니다. 포털에서 비밀번호를 재설정하세요"}
        if error.code == ErrorCode.INVALID_INPUT:
            return {"verified": False, "reason": "학번과 비밀번호를 입력하세요"}
        return {"verified": False, "reason": "지금은 확인할 수 없습니다. 잠시 뒤 다시 시도하세요"}  # 연결, 파싱 등

    return {
        "verified": result.status == ACTIVE_STATUS,
        "student_id": result.student_id,
        "name": result.name,
        "major": result.major,
    }
```

**Node.js**

```javascript
import { authenticate, SejongAuthError, ErrorCode } from 'pro-sejongpeia';

const ACTIVE_STATUS = '재학'; // 학교 원문 문자열 그대로 비교 (포털 표기가 바뀌면 여기만 수정)

/** 동아리 가입 시 학생 인증: 학번, 이름, 학과만 돌려주고 비밀번호는 보관하지 않음 */
export async function verifySejongStudent(studentId, password) {
  try {
    const result = await authenticate(studentId, password);
    return { verified: result.status === ACTIVE_STATUS, studentId: result.studentId, name: result.name, major: result.major };
  } catch (error) {
    if (!(error instanceof SejongAuthError)) throw error;
    if (error.code === ErrorCode.AUTHENTICATION_FAILED) return { verified: false, reason: '학번 또는 비밀번호가 틀렸습니다' };
    if (error.code === ErrorCode.ACCOUNT_LOCKED) return { verified: false, reason: '포털 계정이 잠겼습니다. 포털에서 비밀번호를 재설정하세요' };
    if (error.code === ErrorCode.INVALID_INPUT) return { verified: false, reason: '학번과 비밀번호를 입력하세요' };
    return { verified: false, reason: '지금은 확인할 수 없습니다. 잠시 뒤 다시 시도하세요' }; // 연결, 파싱 등
  }
}
```

**Java / Spring Boot**

```java
import kr.suhsaechan.sejongpeia.auth.model.SejongAuthResult;
import kr.suhsaechan.sejongpeia.auth.service.SuhSejongAuthEngine;
import kr.suhsaechan.sejongpeia.common.exception.SejongAuthErrorCode;
import kr.suhsaechan.sejongpeia.common.exception.SejongAuthException;
import org.springframework.stereotype.Service;

@Service
class StudentVerificationService {

  private static final String ACTIVE_STATUS = "재학"; // 학교 원문 그대로 비교

  private final SuhSejongAuthEngine sejongAuthEngine;

  StudentVerificationService(SuhSejongAuthEngine sejongAuthEngine) {
    this.sejongAuthEngine = sejongAuthEngine;
  }

  /** 학번, 이름, 학과만 돌려주고 비밀번호는 보관하지 않음 */
  String verifySejongStudent(String studentId, String password) {
    try {
      SejongAuthResult result = sejongAuthEngine.authenticate(studentId, password);
      boolean isActiveStudent = ACTIVE_STATUS.equals(result.getStatus());
      return isActiveStudent ? result.getName() + " (" + result.getMajor() + ")" : "재학생이 아닙니다";
    } catch (SejongAuthException exception) {
      SejongAuthErrorCode errorCode = exception.getErrorCode();
      if (errorCode == SejongAuthErrorCode.AUTHENTICATION_FAILED) {
        return "학번 또는 비밀번호가 틀렸습니다";
      }
      if (errorCode == SejongAuthErrorCode.ACCOUNT_LOCKED) {
        return "포털 계정이 잠겼습니다. 포털에서 비밀번호를 재설정하세요";
      }
      return "지금은 확인할 수 없습니다. 잠시 뒤 다시 시도하세요";
    }
  }
}
```

## 2. 학과·강의 검색 페이지

학과 목록과 개설강좌를 JSON으로 내려주는 작은 서버입니다. 프런트엔드는 이 두 주소만 부르면 됩니다.

- `GET /departments` : 학과 코드, 단과대, 학과 이름
- `GET /lectures?department=<학과 코드>&name=<과목명>` : 둘 중 하나는 필수 (이 예시 서버가 정한 규칙이며, 라이브러리는 조건을 모두 비우면 그 학기 전체를 줍니다)

### 요청 파라미터

예시 서버의 HTTP 요청과, 그 안에서 부르는 라이브러리 메서드입니다. 필드 이름은 Python `snake_case` 기준이고, Node.js는 `camelCase` 객체 인자, Java는 위치 인자입니다. 로그인은 필요 없습니다.

**예시 서버**

| 주소 | 파라미터 | 필수 | 설명 |
|---|---|---|---|
| `GET /departments` | | | 파라미터 없음 |
| `GET /lectures` | `department` | 아니오 | 학과 코드 |
| | `name` | 아니오 | 과목명 (부분 일치). 둘 중 하나는 필수인 것은 이 예시 서버가 정한 규칙 |

**라이브러리 메서드**

| 메서드 | 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|---|
| `departments` | `category_code` | 문자열 | 예 | | 소속 분류 (`20` 학부) |
| | `year` | 문자열 | 예 | | 연도 |
| | `semester_code` | 문자열 | 예 | | 학기 코드 (`10` 1학기, `20` 2학기, `11`·`21` 계절학기) |
| `lectures` | `category_code`, `year`, `semester_code` | 문자열 | 예 | | 위와 같음 |
| | `department_code` | 문자열 | 아니오 | 빈 문자열 | 학과 코드 |
| | `course_name` | 문자열 | 아니오 | 빈 문자열 | 과목명 |
| | `instructor_name` | 문자열 | 아니오 | 빈 문자열 | 교수명. 세 조건을 모두 비우면 그 학기 전체 |

코드 값은 모두 학교 값이며 목록은 `organization_categories()`, `terms(...)`로 받습니다 ([학과·학기 코드](https://cassiiopeia.github.io/pro-sejongpeia/data/codes/)).

### 응답 데이터 구조

예시 서버가 내려주는 JSON입니다. 위쪽이 `GET /departments`, 아래쪽이 `GET /lectures?...`의 응답이며, 값은 형태를 보여 주기 위한 예시입니다. 키 이름은 Python 예시 기준이고 Node.js 예시는 `course_number`가 `courseNumber`입니다. Java 컨트롤러는 라이브러리 모델(`Department`, `Lecture`)을 그대로 돌려주므로 필드가 더 많고 이름은 `camelCase`입니다.

```json
[
  { "code": "3517", "college": "인공지능융합대학", "name": "AI로봇학과" },
  { "code": "3331", "college": "연계전공", "name": "스마트투어리즘매니지먼트소프트웨어" }
]
```

```json
[
  {
    "course": "알고리즘및실습",
    "department": "컴퓨터공학과",
    "schedule": "화13:00-15:00",
    "credits": "3.0",
    "course_number": "009954"
  }
]
```

| 필드 | 의미 |
|---|---|
| `code` | 학과 코드. `/lectures?department=`에 그대로 넘김 |
| `college` / `name` | 단과대 / 학과(또는 전공) 이름 |
| `course` / `department` | 과목명 / 개설 학과 이름 |
| `schedule` | 요일과 시간 (학교 원문, 예: `화13:00-15:00`) |
| `credits` | 학점 (문자열) |
| `course_number` | 교과목 번호. 교육과정 조회에 이어서 쓸 수 있음 |

라이브러리가 돌려주는 전체 필드(`extra` 포함)는 [개설강좌](https://cassiiopeia.github.io/pro-sejongpeia/data/lectures/#응답-데이터-구조)와 [학과·학기 코드](https://cassiiopeia.github.io/pro-sejongpeia/data/codes/#응답-데이터-구조)를 보세요.

**실패 시**: 학교 서버가 실패하면 예외 없이 마지막 캐시 또는 빈 목록입니다. 예시 서버는 이를 빈 배열(`[]`)로 내려줍니다.

### 알아둘 점

- `lectures`의 학과 코드, 과목명, 교수명은 모두 선택입니다. **모두 비우면 그 학기 전체**를 받는데 응답이 수 MB로 크므로([spec/PUBLIC_DATA.md](../spec/PUBLIC_DATA.md)), 예시 서버는 조건을 필수로 했습니다. 전체가 필요하면 학과 단위로 나눠 조회하세요.
- 강의 결과에는 교수 이름(`instructor`)도 들어 있습니다. 예시는 필요한 필드만 골라서 내려주고, 외부에 공개할 때는 어떤 필드를 낼지 직접 정하세요.
- 과목명 검색은 부분 일치입니다. 결과가 0건인 이름도 있으므로 빈 결과 안내가 필요합니다.
- 강의의 `course_number`와 `department_code`로 학과 교육과정의 학년·학기·교과목 개요(`curriculum_courses`)를 이어서 조회할 수 있습니다.

### 예시 코드

**Python** (표준 라이브러리 서버)

```python
import json
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import parse_qs, urlparse

from sejongpeia import SejongPublicData

public_data = SejongPublicData()  # 프로세스당 하나만 만들어 캐시를 공유
UNDERGRADUATE_CATEGORY = "20"     # 학교의 소속 분류 코드 (organization_categories()로 확인)
YEAR, SEMESTER = "2026", "20"     # 학기 목록은 terms(UNDERGRADUATE_CATEGORY)


def search_lectures(department_code: str, course_name: str) -> list:
    lectures = public_data.lectures(UNDERGRADUATE_CATEGORY, YEAR, SEMESTER,
                                    department_code=department_code, course_name=course_name)
    return [
        {"course": lecture.name, "department": lecture.department_name, "schedule": lecture.schedule,
         "credits": lecture.credits, "course_number": lecture.course_number}
        for lecture in lectures
    ]


class LectureSearchHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        url = urlparse(self.path)
        if url.path == "/departments":
            departments = public_data.departments(UNDERGRADUATE_CATEGORY, YEAR, SEMESTER)
            body = [{"code": department.code, "college": department.college_name,
                     "name": department.major_name or department.unit_name}
                    for department in departments]
        elif url.path == "/lectures":
            query = parse_qs(url.query)
            department_code = query.get("department", [""])[0].strip()
            course_name = query.get("name", [""])[0].strip()
            if not department_code and not course_name:  # 조건이 없으면 그 학기 전체(수 MB)라서 이 서버는 조건을 필수로 함
                self.send_error(400, "department or name is required")
                return
            body = search_lectures(department_code, course_name)
        else:
            self.send_error(404)
            return
        payload = json.dumps(body, ensure_ascii=False).encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.end_headers()
        self.wfile.write(payload)


if __name__ == "__main__":
    HTTPServer(("127.0.0.1", 8000), LectureSearchHandler).serve_forever()
```

**Node.js**

```javascript
import { createServer } from 'node:http';
import { SejongPublicData } from 'pro-sejongpeia';

const publicData = new SejongPublicData(); // 프로세스당 하나만 만들어 캐시를 공유
const UNDERGRADUATE_CATEGORY = '20';       // 학교의 소속 분류 코드 (organizationCategories()로 확인)
const YEAR = '2026';
const SEMESTER = '20';                     // 학기 목록은 terms(UNDERGRADUATE_CATEGORY)

async function searchLectures(departmentCode, courseName) {
  const lectures = await publicData.lectures({
    categoryCode: UNDERGRADUATE_CATEGORY, year: YEAR, semesterCode: SEMESTER, departmentCode, courseName,
  });
  return lectures.map((lecture) => ({
    course: lecture.name, department: lecture.departmentName, schedule: lecture.schedule,
    credits: lecture.credits, courseNumber: lecture.courseNumber,
  }));
}

createServer(async (request, response) => {
  const url = new URL(request.url, 'http://localhost');
  let body;
  if (url.pathname === '/departments') {
    const departments = await publicData.departments({ categoryCode: UNDERGRADUATE_CATEGORY, year: YEAR, semesterCode: SEMESTER });
    body = departments.map((department) => ({ code: department.code, college: department.collegeName, name: department.majorName || department.unitName }));
  } else if (url.pathname === '/lectures') {
    const departmentCode = (url.searchParams.get('department') ?? '').trim();
    const courseName = (url.searchParams.get('name') ?? '').trim();
    if (!departmentCode && !courseName) { // 조건이 없으면 그 학기 전체(수 MB)라서 이 서버는 조건을 필수로 함
      response.writeHead(400).end('department or name is required');
      return;
    }
    body = await searchLectures(departmentCode, courseName);
  } else {
    response.writeHead(404).end();
    return;
  }
  response.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' }).end(JSON.stringify(body));
}).listen(8000, '127.0.0.1');
```

**Java / Spring Boot**

```java
import java.util.List;
import kr.suhsaechan.sejongpeia.publicdata.SejongPublicData;
import kr.suhsaechan.sejongpeia.publicdata.model.Department;
import kr.suhsaechan.sejongpeia.publicdata.model.Lecture;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class LectureSearchController {

  private static final String UNDERGRADUATE_CATEGORY = "20";
  private static final String YEAR = "2026";
  private static final String SEMESTER = "20";

  private final SejongPublicData publicData;

  LectureSearchController(SejongPublicData publicData) {
    this.publicData = publicData;
  }

  @GetMapping("/departments")
  List<Department> departments() {
    return publicData.departments(UNDERGRADUATE_CATEGORY, YEAR, SEMESTER);
  }

  @GetMapping("/lectures")
  List<Lecture> lectures(@RequestParam(defaultValue = "") String department,
                         @RequestParam(defaultValue = "") String name) {
    if (department.isBlank() && name.isBlank()) { // 조건이 없으면 그 학기 전체(수 MB)라서 이 서버는 조건을 필수로 함
      return List.of();
    }
    return publicData.lectures(UNDERGRADUATE_CATEGORY, YEAR, SEMESTER, department, name, "");
  }
}
```

## 3. 학사일정 위젯

오늘부터 30일 안의 학부 학사일정을 시작일 순으로 보여 줍니다.

### 요청 파라미터

필드 이름은 Python `snake_case` 기준이고, Node.js는 `camelCase` 객체 인자(`{ start, end }`), Java는 위치 인자입니다. 로그인은 필요 없습니다.

| 메서드 | 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|---|
| `calendar` | `start` | 문자열 | 예 | | 시작일 (`yyyy-MM-dd`) |
| | `end` | 문자열 | 예 | | 종료일 (`yyyy-MM-dd`) |
| | `category_code` | 문자열 | 아니오 | `0001` | 분류 코드. `0001` 학부, `0002`~`0007` 대학원. 분류가 늘어도 코드만 바꿔 넘기면 됨 |
| `graduate_calendar` | `site_id` | 문자열 | 예 | | 대학원 사이트 이름 (예: `graduate`). 호출하는 쪽이 넘기므로 새 대학원이 생겨도 릴리스가 필요 없음 |
| | `year` / `month` | 정수 | 예 | | 조회할 연도 / 월 |

### 응답 데이터 구조

`calendar(start, end)`가 돌려주는 목록의 한 건입니다. 필드 이름은 Python `snake_case`이고, Node.js는 `camelCase`(`start_date` → `startDate`), Java는 getter(`getStartDate()`)입니다. 값은 형태를 보여 주기 위한 예시입니다.

```json
[
  {
    "year": "2026",
    "month": "3",
    "category_code": "0001",
    "category_name": "학부",
    "title": "2학기 중간고사 성적 입력",
    "title_en": "Grades input for Fall semester Midterm Examination",
    "start_date": "2026-03-02",
    "end_date": "2026-03-08",
    "extra": {}
  }
]
```

| 필드 | 의미 |
|---|---|
| `title` / `title_en` | 제목 / 영문 제목 (학교 원문) |
| `start_date` / `end_date` | 시작일 / 종료일 (학부는 `yyyy-MM-dd`, 하루짜리는 같은 값) |
| `year` / `month` | 일정이 속한 연도 / 월 |
| `category_code` / `category_name` | 분류 코드 / 이름 (`0001` 학부) |
| `extra` | 그 밖의 학교 필드 (학부 일정은 비어 있음) |

전체 필드는 [학사일정](https://cassiiopeia.github.io/pro-sejongpeia/data/calendar/#응답-데이터-구조)을 보세요.

**실패 시**: 학교 서버가 실패하면 예외 없이 마지막 캐시 또는 빈 목록입니다. 일정이 없는 기간과 구분되지 않습니다.

### 알아둘 점

- `calendar(start, end, category_code="0001")`: `0001`이 학부이고, 대학원 일정은 `0002`~`0007` 또는 사이트별 `graduate_calendar(site_id, year, month)`입니다. 날짜 형식 차이는 [spec/PUBLIC_DATA.md](../spec/PUBLIC_DATA.md)를 보세요.
- 위젯이 열릴 때마다 학교에 요청하지 않습니다. 같은 인자는 24시간 캐시됩니다.
- 제목은 학교 원문입니다. 영문 제목은 `title_en` / `titleEn`입니다.

### 예시 코드

**Python**

```python
from datetime import date, timedelta

from sejongpeia import SejongPublicData

public_data = SejongPublicData()


def upcoming_academic_events(days_ahead: int = 30) -> list:
    """오늘부터 days_ahead일 안에 걸쳐 있는 학부 학사일정 (시작일 순)"""
    today = date.today()
    events = public_data.calendar(today.isoformat(), (today + timedelta(days=days_ahead)).isoformat())
    # 날짜는 학교가 주는 yyyy-MM-dd 문자열이라 문자열 정렬이 날짜 순서와 같음
    return sorted(events, key=lambda event: event.start_date)


if __name__ == "__main__":
    for event in upcoming_academic_events():
        period = event.start_date if event.start_date == event.end_date else f"{event.start_date} ~ {event.end_date}"
        print(f"{period}  {event.title}")
```

**Node.js**

```javascript
import { SejongPublicData } from 'pro-sejongpeia';

const publicData = new SejongPublicData();

/** 오늘부터 daysAhead일 안에 걸쳐 있는 학부 학사일정 (시작일 순) */
export async function upcomingAcademicEvents(daysAhead = 30) {
  const toDateText = (date) => date.toLocaleDateString('sv-SE'); // yyyy-MM-dd (로컬 날짜)
  const today = new Date();
  const end = new Date(today.getTime() + daysAhead * 24 * 60 * 60 * 1000);
  const events = await publicData.calendar({ start: toDateText(today), end: toDateText(end) });
  // 날짜는 학교가 주는 yyyy-MM-dd 문자열이라 문자열 정렬이 날짜 순서와 같음
  return events.sort((first, second) => first.startDate.localeCompare(second.startDate));
}

for (const event of await upcomingAcademicEvents()) {
  const period = event.startDate === event.endDate ? event.startDate : `${event.startDate} ~ ${event.endDate}`;
  console.log(`${period}  ${event.title}`);
}
```

**Java**

```java
import java.time.LocalDate;
import java.util.List;
import kr.suhsaechan.sejongpeia.publicdata.SejongPublicData;
import kr.suhsaechan.sejongpeia.publicdata.model.CalendarEvent;

class AcademicCalendarWidget {

  static List<CalendarEvent> upcomingAcademicEvents(SejongPublicData publicData, int daysAhead) {
    LocalDate today = LocalDate.now();
    return publicData.calendar(today.toString(), today.plusDays(daysAhead).toString()).stream()
        .sorted(java.util.Comparator.comparing(CalendarEvent::getStartDate))
        .toList();
  }
}
```

## 4. 기숙사·학식 안내 봇

오늘의 기숙사 식단, 학식, 기숙사 공지를 메시지 문자열로 만듭니다. 슬랙, 디스코드, 카카오 등 보내는 쪽에 붙이면 됩니다.

### 요청 파라미터

필드 이름은 Python `snake_case` 기준이고, Node.js는 `camelCase` 객체 인자(`{ rows: 3 }`), Java는 위치 인자입니다. 로그인은 필요 없습니다.

| 메서드 | 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|---|
| `dormitory_menu` | `location` | 문자열 | 아니오 | `SJ` | 기숙사 위치 코드 |
| `cafeteria_menu` | `place_id` | 문자열 | 예 | | 학교의 식당 번호. 번호는 호출하는 쪽이 넘기므로 식당이 늘어도 라이브러리 수정이 필요 없음 |
| `cafeteria_places` | `max_place_id` | 정수 | 아니오 | 라이브러리 기본값 | 식당 번호를 1부터 이 값까지 훑어 식당 목록을 만듦. 번호 범위를 넓힐 때 올림 |
| `dormitory_notices` | `rows` | 정수 | 아니오 | `10` | 한 번에 받을 공지 수 |
| | `page` | 정수 | 아니오 | `1` | 공지 쪽 번호 |
| | `keyword` | 문자열 | 아니오 | 빈 문자열 | 제목 검색어 |
| | `location` | 문자열 | 아니오 | `SJ` | 기숙사 위치 코드 |

### 응답 데이터 구조

예시 코드가 메시지로 만드는 원본 데이터입니다. 필드 이름은 Python `snake_case`이고, Node.js는 `camelCase`(`meal_name` → `mealName`), Java는 getter(`getMealName()`)입니다. 값은 형태를 보여 주기 위한 예시입니다.

**`dormitory_menu()`**: 하루 한 건, 최대 7일치 목록

```json
[
  {
    "date": "2026-03-02",
    "breakfast": "쌀밥 된장국 계란말이",
    "lunch": "쌀밥 육개장 부추전 배추김치",
    "dinner": "쌀밥 북어국 안동찜닭 배추김치",
    "alternative": "식권 1장으로 간편식 교환"
  }
]
```

| 필드 | 의미 |
|---|---|
| `date` | 날짜 (`yyyy-MM-dd`) |
| `breakfast` / `lunch` / `dinner` | 조식 / 중식 / 석식. 학교 원문이라 반찬이 한 문자열에 이어져 있음 |
| `alternative` | 대체 메뉴 (없으면 빈 문자열) |

**`cafeteria_menu(place_id)`**: 메뉴 한 줄이 한 건. 식당에 따라 날짜가 있는 주간 메뉴이거나 날짜가 빈 상시 메뉴

```json
[
  {
    "place_id": "1",
    "place_name": "계절밥상",
    "building_name": "군자관",
    "location": "6층",
    "date": "2026-03-02",
    "day": "월",
    "meal_type_id": "1",
    "meal_name": "중식",
    "menu_name": "된장국 · 제육볶음 · 배추김치",
    "price": "7000"
  },
  {
    "place_id": "3",
    "place_name": "진관키친",
    "building_name": "진관홀",
    "location": "B1층",
    "date": "",
    "day": "",
    "meal_type_id": "",
    "meal_name": "",
    "menu_name": "라면",
    "price": "3300"
  }
]
```

| 필드 | 의미 |
|---|---|
| `place_id` / `place_name` | 식당 번호 / 이름 |
| `building_name` / `location` | 건물 / 층 |
| `date` / `day` | 날짜 / 요일. 상시 메뉴 식당은 빈 문자열 |
| `meal_name` | 식사 구분 (중식, 석식 등). 상시 메뉴는 빈 문자열 |
| `menu_name` | 메뉴 (여러 개는 ` · `로 이어짐) |
| `price` | 가격 (원, 문자열) |

**`dormitory_notices(rows=3)`**: 공지 목록. 고정 공지(`pinned`)와 일반 공지(`items`)가 나뉘어 있음

```json
{
  "total": 120,
  "pinned": [],
  "items": [
    {
      "id": "37570",
      "title": "[기숙사] 정기소독 실시 안내",
      "date": "2026-03-02",
      "views": 426,
      "file_name": "안내.png"
    }
  ]
}
```

| 필드 | 의미 |
|---|---|
| `total` | 전체 공지 수 (정수) |
| `pinned` / `items` | 고정 공지 / 일반 공지 목록. 항목 모양은 같음 |
| `id` / `title` / `date` | 공지 번호 / 제목 / 등록일 |
| `views` / `file_name` | 조회수 (정수) / 첨부 파일 이름 (없으면 빈 문자열) |

전체 필드는 [기숙사](https://cassiiopeia.github.io/pro-sejongpeia/data/dormitory/#응답-데이터-구조)와 [학식](https://cassiiopeia.github.io/pro-sejongpeia/data/cafeteria/#응답-데이터-구조)을 보세요.

**실패 시**: 학교 서버가 실패하면 예외 없이 마지막 캐시 또는 빈 목록(`dormitory_notices`는 `total`이 `0`인 빈 결과)을 돌려줍니다. 빈 목록은 「데이터 없음」과 구분되지 않으니 화면에 안내를 두세요.

### 알아둘 점

- 식당 번호는 학교 값입니다. 번호는 바뀔 수 있고, 어느 식당인지는 결과의 `place_name`으로 알 수 있습니다. 빈 목록은 번호가 틀렸을 때와 그날 메뉴가 없을 때 모두 나옵니다.
- 식당에 따라 일주일치가 날짜와 함께 오거나, 날짜 없는 상시 메뉴가 옵니다. 예시는 둘 다 처리합니다.
- 기숙사 식단은 7일치이며 `dormitory_menu(location="SJ")`입니다.
- 학교 서버에 부담을 주지 않도록 봇은 하루 몇 번만 갱신해도 충분합니다 (라이브러리 캐시가 이미 24시간).

### 예시 코드

**Python**

```python
from datetime import date

from sejongpeia import SejongPublicData

public_data = SejongPublicData()
CAFETERIA_PLACE_IDS = ["1", "3", "4", "5", "6"]  # 학교의 식당 번호, 바뀔 수 있음


def dormitory_menu_message() -> str:
    today = date.today().isoformat()
    menu_days = public_data.dormitory_menu()
    # 오늘 날짜가 없으면(주말 갱신 전 등) 학교가 준 첫날을 보여 줌
    menu_day = next((day for day in menu_days if day.date == today), menu_days[0] if menu_days else None)
    if menu_day is None:
        return "기숙사 식단을 불러오지 못했습니다"
    return f"[기숙사 {menu_day.date}]\n조식: {menu_day.breakfast}\n중식: {menu_day.lunch}\n석식: {menu_day.dinner}"


def cafeteria_message(place_id: str) -> str:
    items = public_data.cafeteria_menu(place_id)
    if not items:  # 식당 번호가 없거나 그날 메뉴가 없으면 빈 목록 (둘을 구분하지 못함)
        return f"{place_id}번 식당: 메뉴 없음"
    today = date.today().isoformat()
    # 날짜가 있는 식당은 일주일치가 한 번에 오므로 오늘 것만, 날짜가 없는 식당(상시 메뉴)은 그대로
    todays_items = [item for item in items if item.date in ("", today)] or items
    first = todays_items[0]
    lines = [f"{item.meal_name} {item.menu_name} {item.price}원".strip() for item in todays_items[:5]]
    return f"[{first.place_name} {first.building_name} {first.location}]\n" + "\n".join(lines)


def latest_dormitory_notice_message() -> str:
    notices = public_data.dormitory_notices(rows=3)
    return "\n".join(f"{notice.date} {notice.title}" for notice in notices.items) or "공지를 불러오지 못했습니다"


if __name__ == "__main__":
    print(dormitory_menu_message())
    for place_id in CAFETERIA_PLACE_IDS:
        print(cafeteria_message(place_id))
    print(latest_dormitory_notice_message())
```

**Node.js**

```javascript
import { SejongPublicData } from 'pro-sejongpeia';

const publicData = new SejongPublicData();
const CAFETERIA_PLACE_IDS = ['1', '3', '4', '5', '6']; // 학교의 식당 번호, 바뀔 수 있음
const todayText = () => new Date().toLocaleDateString('sv-SE'); // yyyy-MM-dd (로컬 날짜)

export async function dormitoryMenuMessage() {
  const menuDays = await publicData.dormitoryMenu();
  // 오늘 날짜가 없으면(주말 갱신 전 등) 학교가 준 첫날을 보여 줌
  const menuDay = menuDays.find((day) => day.date === todayText()) ?? menuDays[0];
  if (!menuDay) return '기숙사 식단을 불러오지 못했습니다';
  return `[기숙사 ${menuDay.date}]\n조식: ${menuDay.breakfast}\n중식: ${menuDay.lunch}\n석식: ${menuDay.dinner}`;
}

export async function cafeteriaMessage(placeId) {
  const items = await publicData.cafeteriaMenu(placeId);
  if (items.length === 0) return `${placeId}번 식당: 메뉴 없음`; // 식당 번호가 없거나 그날 메뉴가 없으면 빈 목록 (둘을 구분하지 못함)
  // 날짜가 있는 식당은 일주일치가 한 번에 오므로 오늘 것만, 날짜가 없는 식당(상시 메뉴)은 그대로
  const todaysItems = items.filter((item) => item.date === '' || item.date === todayText());
  const shownItems = todaysItems.length > 0 ? todaysItems : items;
  const lines = shownItems.slice(0, 5).map((item) => `${item.mealName} ${item.menuName} ${item.price}원`.trim());
  const first = shownItems[0];
  return `[${first.placeName} ${first.buildingName} ${first.location}]\n${lines.join('\n')}`;
}

export async function latestDormitoryNoticeMessage() {
  const notices = await publicData.dormitoryNotices({ rows: 3 });
  return notices.items.map((notice) => `${notice.date} ${notice.title}`).join('\n') || '공지를 불러오지 못했습니다';
}

console.log(await dormitoryMenuMessage());
for (const placeId of CAFETERIA_PLACE_IDS) console.log(await cafeteriaMessage(placeId));
console.log(await latestDormitoryNoticeMessage());
```

**Java**

```java
import java.time.LocalDate;
import java.util.List;
import kr.suhsaechan.sejongpeia.publicdata.SejongPublicData;
import kr.suhsaechan.sejongpeia.publicdata.model.CafeteriaMenuItem;
import kr.suhsaechan.sejongpeia.publicdata.model.DormitoryMenuDay;

class CampusInfoBot {

  static String dormitoryMenuMessage(SejongPublicData publicData) {
    String today = LocalDate.now().toString();
    List<DormitoryMenuDay> menuDays = publicData.dormitoryMenu();
    DormitoryMenuDay menuDay = menuDays.stream().filter(day -> today.equals(day.getDate())).findFirst()
        .orElse(menuDays.isEmpty() ? null : menuDays.get(0));
    if (menuDay == null) {
      return "기숙사 식단을 불러오지 못했습니다";
    }
    return "[기숙사 " + menuDay.getDate() + "]\n조식: " + menuDay.getBreakfast()
        + "\n중식: " + menuDay.getLunch() + "\n석식: " + menuDay.getDinner();
  }

  static String cafeteriaMessage(SejongPublicData publicData, String placeId) {
    String today = LocalDate.now().toString();
    List<CafeteriaMenuItem> items = publicData.cafeteriaMenu(placeId);
    if (items.isEmpty()) {
      return placeId + "번 식당: 메뉴 없음";
    }
    List<CafeteriaMenuItem> todaysItems = items.stream()
        .filter(item -> item.getDate().isEmpty() || item.getDate().equals(today)).toList();
    List<CafeteriaMenuItem> shownItems = todaysItems.isEmpty() ? items : todaysItems;
    StringBuilder message = new StringBuilder("[" + shownItems.get(0).getPlaceName() + "]");
    shownItems.stream().limit(5).forEach(item ->
        message.append("\n").append(item.getMealName()).append(" ").append(item.getMenuName()).append(" ").append(item.getPrice()).append("원"));
    return message.toString();
  }
}
```

## 이 문서의 코드를 고치려면

세 언어 예시는 같은 동작을 하도록 맞췄습니다. 라이브러리 메서드가 바뀌면 이 문서의 예시도 함께 고치고 다시 실행해 보세요 (새 정보 출처 추가 절차는 [CONTRIBUTING](../CONTRIBUTING.md#새-정보-출처-추가하기)).
