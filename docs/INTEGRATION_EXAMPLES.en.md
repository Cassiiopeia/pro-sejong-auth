# Integration examples

[한국어](INTEGRATION_EXAMPLES.md) | English

Copy-paste examples for four common use cases in Java, Python and Node.js. Read the [API stability policy](API_STABILITY.en.md) first (breaking changes can happen, so pin the version).

| Use case | Login |
|---|---|
| [1. Student verification (club sign-up)](#1-student-verification-club-sign-up) | required |
| [2. Department and lecture search page](#2-department-and-lecture-search-page) | not needed |
| [3. Academic calendar widget](#3-academic-calendar-widget) | not needed |
| [4. Dormitory and cafeteria info bot](#4-dormitory-and-cafeteria-info-bot) | not needed |

School data changes, so the values in the response examples only show the shape. The example code is for reference; the Java annotations (`@Service`, `@RestController`, ...) are meant for a project that has the Spring dependency.

## Setup

| Java / Spring Boot | Python | Node.js |
|---|---|---|
| `implementation "kr.suhsaechan:sejongpeia:<version>"` | `pip install pro-sejongpeia==<version>` | `npm install pro-sejongpeia@<version>` |

- `<version>` is the latest release number (see [Maven Central](https://central.sonatype.com/artifact/kr.suhsaechan/sejongpeia), [PyPI](https://pypi.org/project/pro-sejongpeia/), [npm](https://www.npmjs.com/package/pro-sejongpeia)).
- Spring Boot registers `SuhSejongAuthEngine` and `SejongPublicData` as beans. For plain Java see "Java: without Spring" in the [configuration docs](https://cassiiopeia.github.io/pro-sejongpeia/en/auth/configuration/).
- Create **one** `SejongPublicData` per process. The in-memory cache (24 hours by default) and request coalescing live inside the instance.
- When a school server fails, public data returns the last cached value or an **empty list** without throwing. An empty list can mean "no data" or "a problem on the school side", so show a hint such as "could not load" next to empty results.
- The Python examples use syntax that works on 3.9+, the Node examples on 18+ (ESM, top-level `await`).

## 1. Student verification (club sign-up)

Take the student ID and password from a sign-up form, check enrollment, and fetch only student ID, name and major. The password is neither stored nor logged.

### Request parameters

Names follow Python `snake_case`; Node.js and Java use `camelCase`. A portal login is required.

| Method | Parameter | Type | Required | Default | Meaning |
|---|---|---|---|---|---|
| `authenticate` | `student_id` | string | Yes | | Student ID |
| | `password` | string | Yes | | Portal password. Never stored or logged |
| | `include_raw` | boolean | No | `false` | Also return the portal's raw text (`rawHtml`, `rawJson`) |
| | `include_extra` | boolean | No | `false` | Return the school's remaining SIS fields as `extra` |
| `authenticate_dhc` | `student_id`, `password`, `include_raw` | | | | When only student ID, name, major, year and status are needed (lighter, no contact lookup) |

### Response data structure

The result `authenticate` returns (top) and the value the example function hands back to the sign-up form (bottom). Field names are Python `snake_case` (`student_id`); Node.js and Java use `camelCase` (`studentId`, `getStudentId()`). Values are made-up and only show the shape.

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

| Field | Meaning |
|---|---|
| `student_id` / `name` / `major` | Student ID / name / major. What the example function returns |
| `status` | Enrollment status (enrolled, on leave, graduated, ...). The portal's own text, so the wording can change |
| `grade` | Year of study (string) |
| `email` / `phone_number` / `english_name` / `user_category` | Contact (SIS) lookup. `null` when the SIS lookup fails. The other SIS school-code fields are omitted here |
| `verified` | Built by the example function by comparing `status` with the enrolled value. Not a library field |

All fields: [Result fields](https://cassiiopeia.github.io/pro-sejongpeia/en/auth/result-fields/).

**On failure**: a wrong password, a locked account and so on are thrown as `SejongAuthError` (with an error code). [Error codes](https://cassiiopeia.github.io/pro-sejongpeia/en/auth/error-codes/)

### Good to know

- **Never resend a wrong password repeatedly.** The portal locks the account. The library stops at the first failure, but if anyone can type a student ID into your form, **limit failures per student ID** (see the [account lock note in the README](../README.en.md#error-codes)).
- `status` and `major` are the portal's own strings. The `재학` comparison above is an example; keep it as a constant in one place since the wording can change.
- One `authenticate` call includes the contact (SIS) lookup. If you only need student ID, name and major, the DHC-only call `authenticate_dhc` / `authenticateDhc` / `authenticateWithDHC` is lighter (result fields in the [feature table](../README.en.md#features)).
- Call it from a server only. Calling from a browser or app exposes the password to third-party code.

### Example code

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

## 2. Department and lecture search page

A small server that returns departments and lectures as JSON. A front end only needs these two URLs.

- `GET /departments`: department code, college, department name
- `GET /lectures?department=<department code>&name=<course name>`: one of the two is required (a rule of this example server; the library returns the whole term when all conditions are empty)

### Request parameters

The example server's HTTP request, and the library methods it calls. Names follow Python `snake_case`; Node.js takes a `camelCase` options object and Java takes positional arguments. No login is needed.

**Example server**

| URL | Parameter | Required | Meaning |
|---|---|---|---|
| `GET /departments` | | | No parameters |
| `GET /lectures` | `department` | No | Department code |
| | `name` | No | Course name (substring). Requiring one of the two is a rule of this example server |

**Library methods**

| Method | Parameter | Type | Required | Default | Meaning |
|---|---|---|---|---|---|
| `departments` | `category_code` | string | Yes | | Organization category (`20` undergraduate) |
| | `year` | string | Yes | | Year |
| | `semester_code` | string | Yes | | Semester code (`10` first, `20` second, `11` and `21` seasonal) |
| `lectures` | `category_code`, `year`, `semester_code` | string | Yes | | Same as above |
| | `department_code` | string | No | empty string | Department code |
| | `course_name` | string | No | empty string | Course name |
| | `instructor_name` | string | No | empty string | Instructor name. With all three conditions empty you get the whole term |

All code values are the school's own; list them with `organization_categories()` and `terms(...)` ([Department and term codes](https://cassiiopeia.github.io/pro-sejongpeia/en/data/codes/)).

### Response data structure

The JSON the example server returns. The top block is the response of `GET /departments`, the bottom one of `GET /lectures?...`; values only show the shape. Key names follow the Python example; in the Node.js example `course_number` is `courseNumber`. The Java controller returns the library models (`Department`, `Lecture`) as they are, so it has more fields, named in `camelCase`.

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

| Field | Meaning |
|---|---|
| `code` | Department code. Pass it as-is to `/lectures?department=` |
| `college` / `name` | College / department (or major) name |
| `course` / `department` | Course name / offering department name |
| `schedule` | Weekday and time (school text, for example `화13:00-15:00`) |
| `credits` | Credits (string) |
| `course_number` | Course number. Usable to continue to the curriculum lookup |

For every field the library returns, see [Course listings](https://cassiiopeia.github.io/pro-sejongpeia/en/data/lectures/#response-data-structure) and [Department and term codes](https://cassiiopeia.github.io/pro-sejongpeia/en/data/codes/#response-data-structure).

**On failure**: when the school server fails you get the last cached value or an empty list, without an exception. The example server returns it as an empty array (`[]`).

### Good to know

- Department code, course name and instructor name of `lectures` are all optional. **With all of them empty you get the whole term**: a response of several MB (see [spec/PUBLIC_DATA.md](../spec/PUBLIC_DATA.md)), so the example server requires a condition. If you need everything, query department by department.
- Lecture results include the instructor name (`instructor`). The examples return only selected fields; decide which fields to expose when you publish this.
- Course-name search matches substrings. Some names return 0 results, so show an empty-result hint.
- A lecture's `course_number` and `department_code` lead on to the department curriculum entry (grade, semester, course overview) with `curriculum_courses`.

### Example code

**Python** (standard library server)

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

## 3. Academic calendar widget

Shows the undergraduate academic events within the next 30 days, ordered by start date.

### Request parameters

Names follow Python `snake_case`; Node.js takes a `camelCase` options object (`{ start, end }`) and Java takes positional arguments. No login is needed.

| Method | Parameter | Type | Required | Default | Meaning |
|---|---|---|---|---|---|
| `calendar` | `start` | string | Yes | | Start date (`yyyy-MM-dd`) |
| | `end` | string | Yes | | End date (`yyyy-MM-dd`) |
| | `category_code` | string | No | `0001` | Category code. `0001` undergraduate, `0002` to `0007` graduate. New categories only need a different code |
| `graduate_calendar` | `site_id` | string | Yes | | Graduate site name (for example `graduate`). The caller passes it, so a new graduate school needs no release |
| | `year` / `month` | integer | Yes | | Year / month to fetch |

### Response data structure

One entry of the list `calendar(start, end)` returns. Field names are Python `snake_case`; Node.js uses `camelCase` (`start_date` -> `startDate`) and Java uses getters (`getStartDate()`). Values only show the shape.

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

| Field | Meaning |
|---|---|
| `title` / `title_en` | Title / English title (school text) |
| `start_date` / `end_date` | Start / end date (`yyyy-MM-dd` for undergraduate; same value for a one-day event) |
| `year` / `month` | Year / month the entry belongs to |
| `category_code` / `category_name` | Category code / name (`0001` is undergraduate) |
| `extra` | Other school fields (empty for undergraduate events) |

All fields: [Academic calendar](https://cassiiopeia.github.io/pro-sejongpeia/en/data/calendar/#response-data-structure).

**On failure**: when the school server fails you get the last cached value or an empty list, without an exception. It looks the same as a period with no events.

### Good to know

- `calendar(start, end, category_code="0001")`: `0001` is undergraduate; graduate schedules are `0002` to `0007`, or per site with `graduate_calendar(site_id, year, month)`. See [spec/PUBLIC_DATA.md](../spec/PUBLIC_DATA.md) for the date format differences.
- Opening the widget does not hit the school each time. The same arguments are cached for 24 hours.
- Titles are the school's own text (Korean). The English title is `title_en` / `titleEn`.

### Example code

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

## 4. Dormitory and cafeteria info bot

Builds today's dormitory menu, cafeteria menus and dormitory notices as message strings. Attach it to whatever sends them: Slack, Discord, Kakao and so on.

### Request parameters

Names follow Python `snake_case`; Node.js takes a `camelCase` options object (`{ rows: 3 }`) and Java takes positional arguments. No login is needed.

| Method | Parameter | Type | Required | Default | Meaning |
|---|---|---|---|---|---|
| `dormitory_menu` | `location` | string | No | `SJ` | Dormitory location code |
| `cafeteria_menu` | `place_id` | string | Yes | | The school's cafeteria number. The caller passes it, so new cafeterias need no library change |
| `cafeteria_places` | `max_place_id` | integer | No | library default | Scans cafeteria numbers from 1 up to this value to build the list. Raise it to widen the range |
| `dormitory_notices` | `rows` | integer | No | `10` | Notices per request |
| | `page` | integer | No | `1` | Page number |
| | `keyword` | string | No | empty string | Title search text |
| | `location` | string | No | `SJ` | Dormitory location code |

### Response data structure

The raw data the example turns into messages. Field names are Python `snake_case`; Node.js uses `camelCase` (`meal_name` -> `mealName`) and Java uses getters (`getMealName()`). Values only show the shape.

**`dormitory_menu()`**: one entry per day, up to 7 days

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

| Field | Meaning |
|---|---|
| `date` | Date (`yyyy-MM-dd`) |
| `breakfast` / `lunch` / `dinner` | Meals. The school's own text, so several dishes sit in one string |
| `alternative` | Alternative meal (empty string if none) |

**`cafeteria_menu(place_id)`**: one menu line per entry. Depending on the cafeteria, a dated weekly menu or an undated standing menu

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

| Field | Meaning |
|---|---|
| `place_id` / `place_name` | Cafeteria number / name |
| `building_name` / `location` | Building / floor |
| `date` / `day` | Date / weekday. Empty string for standing-menu cafeterias |
| `meal_name` | Meal (lunch, dinner, ...). Empty string for standing menus |
| `menu_name` | Menu (several dishes joined with ` · `) |
| `price` | Price (KRW, as a string) |

**`dormitory_notices(rows=3)`**: notices, split into pinned (`pinned`) and regular (`items`)

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

| Field | Meaning |
|---|---|
| `total` | Total number of notices (integer) |
| `pinned` / `items` | Pinned / regular notice lists. Entries have the same shape |
| `id` / `title` / `date` | Notice number / title / posted date |
| `views` / `file_name` | View count (integer) / attachment file name (empty string if none) |

All fields: [Dormitory](https://cassiiopeia.github.io/pro-sejongpeia/en/data/dormitory/#response-data-structure) and [Cafeterias](https://cassiiopeia.github.io/pro-sejongpeia/en/data/cafeteria/#response-data-structure).

**On failure**: when the school server fails you get the last cached value or an empty list (an empty result with `total` `0` for `dormitory_notices`), without an exception. An empty list looks the same as "no data", so show a hint.

### Good to know

- Cafeteria numbers are the school's values. Numbers can change, and `place_name` in the result tells which cafeteria it is. An empty list appears both for a wrong number and for a day without a menu.
- Depending on the cafeteria, you get a week of dated menus or an undated standing menu. The examples handle both.
- The dormitory menu covers 7 days: `dormitory_menu(location="SJ")`.
- To go easy on the school servers, refreshing a few times a day is enough (the library cache is already 24 hours).

### Example code

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

## Changing the code in this document

The three languages are written to behave the same. When a library method changes, update the examples here and run them again (the procedure for adding a source is in [CONTRIBUTING](../CONTRIBUTING.en.md#adding-a-new-information-source)).
