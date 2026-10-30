# 통합 예시

한국어 | [English](INTEGRATION_EXAMPLES.en.md)

자주 쓰는 네 가지 용도를 Java, Python, Node.js로 복사해서 바로 쓸 수 있게 모았습니다. 시작 전에 [API 안정성 정책](API_STABILITY.md)(깨지는 변경이 생길 수 있어 버전을 고정하세요)을 먼저 읽어 보세요.

| 용도 | 로그인 | 어디까지 확인했나 |
|---|---|---|
| [1. 학생 인증 (동아리 가입)](#1-학생-인증-동아리-가입) | 필요 | **실행하지 않음** (계정 없음). 시그니처는 소스로 확인, 빈 입력 분기만 Python, Node에서 실행 |
| [2. 학과·강의 검색 페이지](#2-학과강의-검색-페이지) | 불필요 | Python, Node는 서버를 띄워 요청, Java는 서버 없이 라이브러리 호출만 컴파일·실행 |
| [3. 학사일정 위젯](#3-학사일정-위젯) | 불필요 | 세 언어 모두 실행 |
| [4. 기숙사·학식 안내 봇](#4-기숙사학식-안내-봇) | 불필요 | 세 언어 모두 실행 |

확인 환경: 2026-10-30, 실제 학교 서버, 1.2.49 소스(Python 3.14, Node 26, JDK 17). 학교 데이터는 바뀌므로 아래 출력은 그날의 예입니다. Java의 `@Service`, `@RestController`, `@GetMapping`, `@RequestParam` 어노테이션은 Spring 의존성 없이 같은 모양의 임시 어노테이션으로 컴파일했으므로, 라이브러리 호출 부분만 검증한 것이고 Spring 연결은 확인하지 않았습니다.

## 준비

| Java / Spring Boot | Python | Node.js |
|---|---|---|
| `implementation "kr.suhsaechan:sejongpeia:<version>"` | `pip install pro-sejongpeia==<version>` | `npm install pro-sejongpeia@<version>` |

- `<version>`은 최신 릴리스 번호입니다 (README의 배지, 또는 [Maven Central](https://central.sonatype.com/artifact/kr.suhsaechan/sejongpeia), [PyPI](https://pypi.org/project/pro-sejongpeia/), [npm](https://www.npmjs.com/package/pro-sejongpeia)). 이 문서의 예시는 1.2.49 소스로 확인했습니다.
- Spring Boot는 `SuhSejongAuthEngine`과 `SejongPublicData`를 빈으로 등록합니다. 일반 Java는 [설정 문서](https://cassiiopeia.github.io/pro-sejongpeia/auth/configuration/)의 「Java: Spring 없이 쓰기」를 보세요.
- `SejongPublicData`(공개 데이터)는 **프로세스당 하나만** 만들어 쓰세요. 메모리 캐시(기본 24시간)와 같은 요청 합치기가 인스턴스 안에 있습니다.
- 공개 데이터는 학교 서버 실패 시 예외 없이 마지막 캐시 또는 **빈 목록**을 돌려줍니다. 빈 목록은 「데이터 없음」일 수도 「학교 쪽 문제」일 수도 있어, 화면에는 「불러오지 못했습니다」와 같은 안내를 함께 두는 것이 좋습니다.
- Python 예시는 3.9 이상, Node 예시는 18 이상(ESM, 최상위 `await`)에서 동작하는 문법만 썼습니다 (직접 실행해 본 것은 위 확인 환경).

## 1. 학생 인증 (동아리 가입)

가입 폼에서 받은 학번과 비밀번호로 재학 여부를 확인하고, 학번·이름·학과만 가져옵니다. 비밀번호는 저장하거나 로그에 남기지 않습니다.

> **실행하지 않은 예시입니다.** 실제 포털에 로그인하는 코드라 계정이 필요한데 이 문서를 쓸 때 쓸 수 있는 계정이 없었습니다. 메서드 이름, 인자, 결과 필드, 에러 코드는 소스(`__init__.py`, `index.ts`, `SuhSejongAuthEngine.java`)와 [spec/RESULT_SCHEMA.md](../spec/RESULT_SCHEMA.md)를 읽고 맞췄습니다. Python과 Node는 빈 학번·비밀번호로 호출해 `INVALID_INPUT` 분기가 동작하는 것만 실행해 확인했습니다 (네트워크 호출 전에 거절됨). Java는 컴파일만 확인했습니다. 실제 계정이 있다면 [CONTRIBUTING](../CONTRIBUTING.md)의 `scripts/verify-live.sh`로 세 언어 결과를 비교할 수 있습니다.

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

주의할 점:

- **틀린 비밀번호를 반복해서 보내지 마세요.** 포털이 계정을 잠급니다. 라이브러리는 첫 실패에서 멈추지만, 누구나 학번을 입력할 수 있는 폼이라면 **학번별 실패 횟수를 제한**하세요 ([README의 계정 잠금 안내](../README.md#에러-코드)).
- `status`, `major`는 포털의 원문 문자열입니다. 위 `재학` 비교는 예시이며, 표기가 바뀔 수 있으니 한 곳에 상수로 두세요.
- 한 번의 `authenticate`는 연락처(SIS) 조회까지 포함합니다. 학번·이름·학과만 필요하면 DHC만 부르는 `authenticate_dhc` / `authenticateDhc` / `authenticateWithDHC`가 더 가볍습니다 (결과 필드는 각각 [기능 표](../README.md#기능) 참고).
- 서버에서만 호출하세요. 브라우저나 앱에서 직접 부르면 비밀번호가 제3자 코드에 노출됩니다.

## 2. 학과·강의 검색 페이지

학과 목록과 개설강좌를 JSON으로 내려주는 작은 서버입니다. 프런트엔드는 이 두 주소만 부르면 됩니다.

- `GET /departments` : 학과 코드, 단과대, 학과 이름
- `GET /lectures?department=<학과 코드>&name=<과목명>` : 둘 중 하나는 필수 (이 예시 서버가 정한 규칙이며, 라이브러리는 조건을 모두 비우면 그 학기 전체를 줍니다)

학부·2026년 2학기 기준이며 `"20"`(학부)과 학기 값은 학교 값입니다. 목록은 `organization_categories()`, `terms(...)`로 확인하세요.

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
            if not department_code and not course_name:  # 조건이 없으면 그 학기 전체(학부 약 2.4MB)라서 이 서버는 조건을 필수로 함
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
    if (!departmentCode && !courseName) { // 조건이 없으면 그 학기 전체(학부 약 2.4MB)라서 이 서버는 조건을 필수로 함
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
    if (department.isBlank() && name.isBlank()) { // 조건이 없으면 그 학기 전체(학부 약 2.4MB)라서 이 서버는 조건을 필수로 함
      return List.of();
    }
    return publicData.lectures(UNDERGRADUATE_CATEGORY, YEAR, SEMESTER, department, name, "");
  }
}
```

확인한 결과 (각 서버를 띄우고 `curl`로 호출):

- `/departments`: 학과 103건 (세 언어 모두 같은 건수)
- `/lectures?name=알고리즘`: 18건. 한 건의 모양: `{"course":"알고리즘및실습","department":"컴퓨터공학과","schedule":"화13:00-15:00","credits":"3.0","courseNumber":"009954"}`
- 조건 없이 `/lectures`: 400 (Python, Node). Java 컨트롤러는 같은 경우 빈 목록을 돌려주게 썼지만 Spring 서버로는 실행하지 않았습니다 (라이브러리 호출만 실행)

참고:

- `lectures`의 학과 코드, 과목명, 교수명은 모두 선택입니다. **모두 비우면 그 학기 전체**를 받는데 학부는 약 2,800행, 2.4MB라서(2026-10-30 기준, [spec/PUBLIC_DATA.md](../spec/PUBLIC_DATA.md)), 예시 서버는 조건을 필수로 했습니다. 전체가 필요하면 학과 단위로 나눠 조회하세요.
- 강의 결과에는 교수 이름(`instructor`)도 들어 있습니다. 예시는 필요한 필드만 골라서 내려주고, 외부에 공개할 때는 어떤 필드를 낼지 직접 정하세요.
- 과목명 검색은 부분 일치였습니다 (`알고리즘` 18건, `프로그래밍` 73건). 결과가 0건인 이름도 있으므로(예: 이 날 `자료구조`는 0건) 빈 결과 안내가 필요합니다.
- 강의의 `course_number`와 `department_code`로 학과 교육과정의 학년·학기·교과목 개요(`curriculum_courses`)를 이어서 조회할 수 있습니다.

## 3. 학사일정 위젯

오늘부터 30일 안의 학부 학사일정을 시작일 순으로 보여 줍니다.

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

실행 결과 (2026-10-30, 세 언어 모두 같은 4건):

```text
2026-10-27 ~ 2026-11-02  2학기 중간고사 성적 입력
2026-11-02 ~ 2026-11-26  복수·부전공, 연계융합전공 신청
2026-11-03 ~ 2026-11-07  2학기 중간고사 성적 열람 및 정정
2026-11-09 ~ 2026-12-18  세종인재자기설계전공 신청
```

참고:

- `calendar(start, end, category_code="0001")`: `0001`이 학부이고, 대학원 일정은 `0002`~`0007` 또는 사이트별 `graduate_calendar(site_id, year, month)`입니다. 날짜 형식 차이는 [spec/PUBLIC_DATA.md](../spec/PUBLIC_DATA.md)를 보세요.
- 위젯이 열릴 때마다 학교에 요청하지 않습니다. 같은 인자는 24시간 캐시됩니다.
- 제목은 학교 원문입니다. 영문 제목은 `title_en` / `titleEn`입니다.

## 4. 기숙사·학식 안내 봇

오늘의 기숙사 식단, 학식, 기숙사 공지를 메시지 문자열로 만듭니다. 슬랙, 디스코드, 카카오 등 보내는 쪽에 붙이면 됩니다.

**Python**

```python
from datetime import date

from sejongpeia import SejongPublicData

public_data = SejongPublicData()
CAFETERIA_PLACE_IDS = ["1", "3", "4", "5", "6"]  # 학교의 식당 번호 (2026-10-30 확인), 바뀔 수 있음


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
const CAFETERIA_PLACE_IDS = ['1', '3', '4', '5', '6']; // 학교의 식당 번호 (2026-10-30 확인), 바뀔 수 있음
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

실행 결과의 앞부분 (2026-10-30, 중간 줄임):

```text
[기숙사 2026-10-30]
조식: 돈육장조림버터비빔밥 음료
중식: 쌀밥 육개장 부추전 우엉떡잡채 아삭이고추무침 배추김치
석식: 쌀밥 북어계란국 순살안동찜닭 꼬시래기무침 참나물흑임자무침 배추김치
[계절밥상 군자관 6층]
중식 얼갈이된장국 · 오징어제육볶음 · 감자튀김 · ... 7000원
[진관키친 진관홀 B1층]
라면 3300원
...
2026-10-19 [기숙사] 2026년 10월 정기소독 실시 안내
```

참고:

- 식당 번호는 학교 값입니다 (2026-10-30 확인: 1, 3, 4, 5, 6번에 메뉴, 2번은 빈 목록). 번호는 바뀔 수 있고, 어느 식당인지는 결과의 `place_name`으로 알 수 있습니다. 빈 목록은 번호가 틀렸을 때와 그날 메뉴가 없을 때 모두 나옵니다.
- 1번 식당은 일주일치가 날짜와 함께 오고, 3~6번은 날짜 없는 상시 메뉴였습니다. 예시는 둘 다 처리합니다.
- 기숙사 식단은 7일치이며 `dormitory_menu(location="SJ")`입니다.
- 학교 서버에 부담을 주지 않도록 봇은 하루 몇 번만 갱신해도 충분합니다 (라이브러리 캐시가 이미 24시간).

## 이 문서의 코드를 고치려면

세 언어 예시는 같은 동작을 하도록 맞췄습니다. 라이브러리 메서드가 바뀌면 이 문서의 예시도 함께 고치고 다시 실행해 보세요 (새 정보 출처 추가 절차는 [CONTRIBUTING](../CONTRIBUTING.md#새-정보-출처-추가하기)).
