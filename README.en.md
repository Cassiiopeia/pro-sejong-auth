<div align="center">

<img src="docs/assets/sejong-seal.png" alt="Sejong University seal" width="120">

<img src="docs/assets/banner/banner-a.en.png" alt="pro-sejongpeia: Sejong University in one library. Student login verification plus departments, lectures, academic calendar, cafeteria and campus data for Java, Python and Node.js" width="880">

**Student login verification and 15 kinds of public Sejong University data, in one call from Java, Python or Node.js.**
Fast and light · gentle on requests · never stores a password · checked against the real portal every day.

<sub>세종대 로그인 API · 세종대학교 포털 인증 · 학번 인증 라이브러리 (Java · Python · Node)</sub>

[Site](https://cassiiopeia.github.io/pro-sejongpeia/) · [Try it in the browser](https://cassiiopeia.github.io/pro-sejongpeia/#demo) · [What you get](#what-you-get) · [Try it now](#try-it-now) · [Docs](#more-integration-examples-and-api-stability) · [한국어](README.md) | English

[![Maven Central](https://img.shields.io/maven-central/v/kr.suhsaechan/sejongpeia?label=Maven%20Central&color=C20F2F)](https://central.sonatype.com/artifact/kr.suhsaechan/sejongpeia)
[![PyPI](https://img.shields.io/pypi/v/pro-sejongpeia?label=PyPI&color=324D60)](https://pypi.org/project/pro-sejongpeia/)
[![npm](https://img.shields.io/npm/v/pro-sejongpeia?label=npm&color=324D60)](https://www.npmjs.com/package/pro-sejongpeia)
[![License: MIT](https://img.shields.io/badge/License-MIT-324D60)](LICENSE)
[![Live portal check](https://github.com/Cassiiopeia/pro-sejongpeia/actions/workflows/PROJECT-SEJONG-LIVE-CHECK.yml/badge.svg)](https://github.com/Cassiiopeia/pro-sejongpeia/actions/workflows/PROJECT-SEJONG-LIVE-CHECK.yml)

</div>

---

## What you get

Not just student verification. The public data the school serves without a login comes in the same shape, 15 kinds in all. Numbers below were checked by calling the real school servers on 2026-10-30 (undergraduate, 2026 second semester).

| | Feature | What you get |
|:-:|---|---|
| 🔐 | **Student verification** | Confirm a student with the student ID and portal password. Name, major, year, enrollment status, classic-reading certification, email and phone, and 11 school codes such as the department code. Pick integrated, DHC or SIS |
| 🏫 | **Department and term codes** | 22 organization categories (undergraduate and graduate), the term list and the department list (**103** undergraduate departments in 2026-2) |
| 📚 | **Lectures** | The whole term (**2,786 rows** for undergraduate 2026-2) or a search by department, course name or instructor, with schedule and room |
| 🎓 | **Curriculum and course overview** | Suggested year and semester, credits and the course overview of a department's curriculum |
| 📅 | **Academic calendar** | Undergraduate and graduate schedules, plus the per-business allowed periods (course registration, certificate printing) you see after login |
| 🛏️ | **Dormitory** | Seven days of menus and the notices |
| 🍚 | **Cafeterias** | The list of **5** cafeterias and daily menus with prices |
| 📍 | **Campus places** | **60** buildings and welfare facilities with name, coordinates and info |
| 🎉 | **Extracurricular events** | Event schedule with category and location |

## Why this library

Four promises, each with proof you can check in the code or the tests.

### ⚡ Fast and light

- **Zero dependencies** (Python, Node): standard library only (`dependencies = []`, `"dependencies": {}`).
- **Measured**: department list about 0.12 s, one department's lectures 0.06 s, the whole term's 2,786 lectures (2.4 MB) 0.9 s. Cached answers take microseconds (Python, 2026-10-30, Seoul, median of 5 fresh clients; most of the time is the school server's own response).
- **One login**: the integrated call signs in to the portal once and reads DHC and SIS with the same session. Java 0.57 s, Python 0.76 s, Node 0.69 s (2026-10-24, median of 5).

### 🧠 Gentle on requests

- Results are cached in memory for **24 hours**, and **simultaneous calls with the same arguments share one request**. The cache holds at most 500 entries (default) and evicts the oldest first.
- A call sends one request. The only exception is the cafeteria list, which sends one per number in a range (10 by default), **one after the other**, per refresh.
- A failed call is **remembered for 60 seconds** so the school's servers are not hammered. Proof: the cache section of [spec/PUBLIC_DATA.md](spec/PUBLIC_DATA.md).

### 🔒 Safe

- **The password is never stored.** It is only used for the portal login request and never put into error messages (test `test_error_message_never_contains_password`).
- **Stops at the first failure.** A wrong password ends with `AUTHENTICATION_FAILED` and no retry, so the account is not locked by the library, and a locked account is reported as `ACCOUNT_LOCKED`.
- The SIS extra fields (`extra`) **always exclude** session values, client IP, account state, resident number, bank account, address and staff names, and server-internal error details are never returned.
- A failing school server does not shake your app: public data returns the last cached value or an empty list instead of throwing.

### ✅ Trustworthy

- **Checked against the real portal every day**: at 09:00 KST all three languages sign in to the live portal, and a failure opens an issue automatically (see the badge above).
- **Same results in three languages**: shared values live in [spec/contract.json](spec/contract.json), and each language's tests fail when they drift.
- **Method list drift check**: CI verifies that the 19 public methods match across [spec/api-index.json](spec/api-index.json), the three sources and the docs, and the shape of the school's responses is checked daily without an account.

The hard parts of doing it yourself (the one cipher the portal accepts, firewall User-Agents, SSO cookies across three domains, verdicts hidden in 200 responses) are already solved. See the [project site](https://cassiiopeia.github.io/pro-sejongpeia/) for details.

## Features

**Everything is supported in all three languages**, with the same features and the same result shape. Only the naming follows each language's convention (snake_case in Python).

| Feature | Java | Python | Node |
|---|:-:|:-:|:-:|
| **Student verification**, 4: integrated · DHC · SIS · SIS business periods | ✅ | ✅ | ✅ |
| **Codes and lectures**, 7: categories · terms · departments · lectures · course types · selective domains · curriculum | ✅ | ✅ | ✅ |
| **Schedule and campus life**, 8: academic calendar (undergrad, graduate) · dormitory menu and notices · cafeteria list and menu · campus places · extracurricular events | ✅ | ✅ | ✅ |
| Raw response, SIS codes and `extra` options | ✅ | ✅ | ✅ |

<details>
<summary>All method names (per language)</summary>

| Feature | Java | Python | Node |
|---|---|---|---|
| Integrated | `authenticate` | `authenticate` | `authenticate` |
| DHC | `authenticateWithDHC` | `authenticate_dhc` | `authenticateDhc` |
| SIS | `authenticateWithSIS` | `authenticate_sis` | `authenticateSis` |
| SIS codes, `extra` | `authenticate(id, pw, false, true)` | `include_extra=True` | `{ includeExtra: true }` |
| Raw response | `authenticateWithRaw` and more | `include_raw=True` | `{ includeRaw: true }` |
| SIS business periods | `fetchSisBusinessPeriods` | `fetch_sis_business_periods` | `fetchSisBusinessPeriods` |
| Department and term codes | `organizationCategories`, `terms`, `departments` | `organization_categories`, `terms`, `departments` | `organizationCategories`, `terms`, `departments` |
| Lectures | `lectures`, `courseTypes`, `selectiveDomains` | `lectures`, `course_types`, `selective_domains` | `lectures`, `courseTypes`, `selectiveDomains` |
| Curriculum and overview | `curriculumCourses` | `curriculum_courses` | `curriculumCourses` |
| Academic calendar (undergrad, graduate) | `calendar`, `graduateCalendar` | `calendar`, `graduate_calendar` | `calendar`, `graduateCalendar` |
| Dormitory menu and notices | `dormitoryMenu`, `dormitoryNotices` | `dormitory_menu`, `dormitory_notices` | `dormitoryMenu`, `dormitoryNotices` |
| Cafeteria list and menu | `cafeteriaPlaces`, `cafeteriaMenu` | `cafeteria_places`, `cafeteria_menu` | `cafeteriaPlaces`, `cafeteriaMenu` |
| Campus places | `campusPlaces` | `campus_places` | `campusPlaces` |
| Extracurricular events | `extracurricularEvents` | `extracurricular_events` | `extracurricularEvents` |

- **Integrated** returns student info + classic reading + contact, **DHC** student info + classic-reading certification status, **SIS** student info + email, phone and English name. **SIS codes** are the 11 school-provided codes such as the department code, and `extra` holds the other SIS fields. In Java the raw-response variants are `authenticateWithRaw`, `authenticateWithDHCRaw` and `authenticateWithSISRaw`.
- SIS business periods are not the public academic calendar (`calendar`): they are the per-business allowed periods of the student's own SIS "학사일정조회" screen (`period`, `scheduleName`, `businessName`, `target`, `timeText` and more, school text as-is), queried by year and month (empty month = the whole year). The school ends the session on a malformed request, so the library sends the requests in the browser's exact order and stops without retry on a failure. Verified with one undergraduate account; other student types are not verified. The flow is step 2d of [spec/PORTAL_FLOW.md](spec/PORTAL_FLOW.md).
- The public-data contract is [spec/PUBLIC_DATA.md](spec/PUBLIC_DATA.md).

</details>

## Try it now

No student ID, no password. This fetches the 2026 second-semester department list and the lectures of the Software department (3220). Same call and same result in all three languages.

**Python** · `pip install pro-sejongpeia`

```python
from sejongpeia import SejongPublicData

public_data = SejongPublicData()
departments = public_data.departments("20", "2026", "20")
lectures = public_data.lectures("20", "2026", "20", department_code="3220")
print(len(departments), lectures[0].name)
```

**Node.js** · `npm install pro-sejongpeia`

```js
import { SejongPublicData } from 'pro-sejongpeia';

const publicData = new SejongPublicData();
const term = { categoryCode: '20', year: '2026', semesterCode: '20' };
const departments = await publicData.departments(term);
const lectures = await publicData.lectures({ ...term, departmentCode: '3220' });
console.log(departments.length, lectures[0].name);
```

**Java / Spring Boot** · `implementation "kr.suhsaechan:sejongpeia:<version>"`

```java
SejongPublicData publicData = new SejongPublicData();   // a bean in Spring
List<Department> departments = publicData.departments("20", "2026", "20");
List<Lecture> lectures = publicData.lectures("20", "2026", "20", "3220", "", "");
System.out.println(departments.size() + " " + lectures.get(0).getName());
```

The real output (Python, 2026-10-30, 3 calls to the school servers in 0.3 s). The school data itself is Korean.

<p align="center">
  <img src="docs/assets/demo/public-data-run.png" alt="Real output of SejongPublicData: 103 departments, Software department lectures and the academic calendar" width="760">
</p>

### Student verification in one line

```python
result = authenticate("21011234", "password")             # Python
print(result.name, result.major, result.grade)
```

```js
const result = await authenticate('21011234', 'password');   // Node.js
console.log(result.name, result.major, result.grade);
```

```java
SejongAuthResult result = sejongAuthEngine.authenticate(studentId, password);   // Java, a Spring Boot bean
System.out.println(result.getName() + " " + result.getMajor() + " " + result.getGrade());
```

Error handling and Spring setup are in the [quick start](#quick-start-java--spring-boot) below. Student verification can also be tried in the browser first.

<p align="center">
  <a href="https://cassiiopeia.github.io/pro-sejongpeia/#demo">
    <img src="docs/assets/demo-en-dark.png" alt="Live authentication demo on the project page" width="760">
  </a>
  <br>
  <sub>Live demo on the project page (click to try). A real authentication result with personal fields replaced by sample values.</sub>
</p>

## Use cases

**Verifying Sejong students** when they join a club or student council, checking major or year for campus events, classic-reading progress helpers, department and lecture search, academic calendar widgets, dormitory and cafeteria bots ([integration examples](docs/INTEGRATION_EXAMPLES.en.md)).

## Quick start: Java / Spring Boot

Published to **Maven Central** (no extra repository needed). Java 17+, Spring Boot 3.x. Spring Boot auto-configuration registers the `SuhSejongAuthEngine` and `SejongPublicData` beans.

```groovy
dependencies {
    implementation "kr.suhsaechan:sejongpeia:<version>"   // see the Maven Central badge
}
```

```java
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final SuhSejongAuthEngine sejongAuthEngine;   // kr.suhsaechan.sejongpeia.auth.service

    @PostMapping("/auth/sejong")
    public SejongAuthResult authenticate(@RequestParam String studentId,
                                         @RequestParam String password) {
        return sejongAuthEngine.authenticate(studentId, password);
    }
}
```

<details>
<summary>Maven, without Spring (plain Java), checking classic reading</summary>

```xml
<dependency>
    <groupId>kr.suhsaechan</groupId>
    <artifactId>sejongpeia</artifactId>
    <version><!-- see the Maven Central badge --></version>
</dependency>
```

Spring Boot is an optional dependency: plain Java projects do not download it.

```java
import kr.suhsaechan.sejongpeia.auth.client.SejongPortalClient;
import kr.suhsaechan.sejongpeia.auth.client.SejongSisClient;
import kr.suhsaechan.sejongpeia.auth.model.SejongAuthResult;
import kr.suhsaechan.sejongpeia.auth.parser.SejongClassicReadingParser;
import kr.suhsaechan.sejongpeia.auth.parser.SejongSisParser;
import kr.suhsaechan.sejongpeia.auth.parser.SejongStudentInfoParser;
import kr.suhsaechan.sejongpeia.auth.service.SuhSejongAuthEngine;
import kr.suhsaechan.sejongpeia.common.config.SejongAuthProperties;
import kr.suhsaechan.sejongpeia.common.util.SejongTlsSupport;

SejongTlsSupport.ensureLegacyCipherEnabled();   // first thing in main(), before any HTTPS call (see the TLS note)
SejongAuthProperties properties = new SejongAuthProperties();
SuhSejongAuthEngine engine = new SuhSejongAuthEngine(new SejongPortalClient(properties), new SejongStudentInfoParser(),
    new SejongClassicReadingParser(), new SejongSisClient(properties), new SejongSisParser());
SejongAuthResult result = engine.authenticate(studentId, password);
```

Check classic reading (Java)

```java
SejongDhcAuthResult result = sejongAuthEngine.authenticateWithDHC(studentId, password);

for (ClassicCertification certification : result.getClassicReading().getCertifications()) {
    System.out.println(certification.getArea() + " "
        + certification.getCertifiedCount() + "/" + certification.getRequiredCount());
}
```

</details>

## Quick start: Python · Node.js

Python 3.9+ or Node 18+ (zero dependencies, TypeScript types, ESM and CommonJS, server-side only). Branch on the error `code`, not on the message.

```python
from sejongpeia import authenticate, SejongAuthError, ErrorCode

try:
    result = authenticate("21011234", "password")
except SejongAuthError as error:
    if error.code == ErrorCode.AUTHENTICATION_FAILED:
        print("Wrong student ID or password")
    elif error.code == ErrorCode.ACCOUNT_LOCKED:
        print("Portal account locked: reset the password on the portal")
```

```js
import { authenticate, SejongAuthError, ErrorCode } from 'pro-sejongpeia';

try {
  const result = await authenticate('21011234', 'password');
} catch (error) {
  if (error instanceof SejongAuthError && error.code === ErrorCode.AUTHENTICATION_FAILED) console.log('Wrong student ID or password');
  else if (error instanceof SejongAuthError && error.code === ErrorCode.ACCOUNT_LOCKED) console.log('Portal account locked');
}
```

## School data without login

For the method list see [Features](#features) above, and for the first call see [Try it now](#try-it-now). Here is an example that continues from a lecture to its curriculum (Node and Java use the same method names in camelCase).

```python
from sejongpeia import SejongPublicData

public_data = SejongPublicData()
departments = public_data.departments("20", "2026", "20")          # undergraduate, 2026 second semester
lectures = public_data.lectures("20", "2026", "20", department_code=departments[0].code)
# the course overview and suggested year/semester in that department's curriculum (empty list if not part of it)
courses = public_data.curriculum_courses(lectures[0].department_code, lectures[0].course_number)
```

- **Raw school values.** No code tables or name mappings are baked in, so renamed departments need no code change, and school fields that are not mapped land in `extra`.
- On failure you get the last cached value or an empty list, not an exception (only invalid input raises). Please be gentle with the school's servers.

<details>
<summary>Notes on whole-term lectures and the cafeteria list</summary>

- The lecture search handles the awkward parts for you, such as the school requiring every filter key in the request. With department, course name and instructor all empty it returns the whole term in one call (2,786 undergraduate rows and 2.4 MB for 2026/2, so prefer one department at a time when you can).
- `cafeteria_places` lists the cafeterias (number, name, building, location). The school has no list endpoint, so the list is built from the menu answers for numbers 1 to 10 (the default; `max_place_id` raises it up to 50): a cafeteria with no menu that day can be missing. `meal_type_id` 1 is lunch and 2 is dinner; fixed-menu cafeterias leave it empty. Details in [spec/PUBLIC_DATA.md](spec/PUBLIC_DATA.md).

</details>

## Result fields

<details>
<summary>Show all result fields</summary>

Names below are Java / Node. Python uses snake_case (`student_id`, `phone_number`, ...). Full schema: [spec/RESULT_SCHEMA.md](spec/RESULT_SCHEMA.md). Text values come from the portal as-is (Korean).

- **Common**: `success`, `studentId`, `name`, `major`, `grade`, `status` (재학 enrolled, 휴학 on leave, 졸업 graduated), `authenticatedAt`
- **Classic reading** `classicReading`: certification, exam, contest and substitution records, plus `completedSemesters`, `certifiedSemesters` and `certificationStatus` (portal text as-is)
- **SIS contact** (null if SIS fails): `email`, `phoneNumber` (`010-1234-5678` format), `englishName`, `userCategory` (for example `학부생`)
- `parentUnitName`: the unit above the major (a college or a school), from the portal main page as-is (null if that call fails, `""` if unavailable). It changes by academic year: do not branch on a value list or use it to decide authentication
- **11 SIS school codes** (null if SIS fails, `""` if missing): `departmentCode`, `organizationClassificationCode`, `enrollmentStatusCode`, `userClassificationCode`, `userClassificationLargeCode`, `userClassificationMiddleCode`, `roleCode`, `roleName`, `menuSystemId`, `menuSystemName`, `locale`. There is no code-to-name mapping in the library
- `extra` (only with the extra option): the other SIS fields as `{"dataset.FIELD": "value"}`. Sensitive keys (session value, client IP, password and account flags, resident number, bank account, address, staff names) are always excluded, but by name pattern, so do not pass it to an external client as-is
- `rawHtml` (DHC page) and `rawJson` (SIS response): only with the raw option. It adds no request, but the SIS raw text contains values that look like a session identifier, the client IP and login time, so never pass it to an external client as-is. Its structure can change when the school changes it and is outside the compatibility guarantee
- A DHC result has no contact fields; a SIS result has no classic-reading field and `grade` and `status` are empty strings

</details>

## Error codes

Every language raises one error type with a `code`.

<details>
<summary>Show the 10 error codes</summary>

`AUTHENTICATION_FAILED` student ID or password is wrong · `ACCOUNT_LOCKED` the portal locked the account after repeated wrong passwords · `ACCOUNT_UNAVAILABLE` the portal account cannot sign in (use period ended or blocked by the administrator) · `INVALID_INPUT` input is empty (checked before any network call) · `CONNECTION_FAILED` cannot reach the Sejong portal · `CONNECTION_TIMEOUT` portal timed out · `DATA_FETCH_FAILED` failed to fetch student info · `PARSE_ERROR` could not parse the portal response (structure changed?) · `SESSION_ERROR` portal session error · `SSL_CONFIGURATION_ERROR` SSL configuration error

</details>

> **Account lock.** The Sejong portal locks an account after repeated wrong passwords (its page says about 5). Unlocking needs a password reset at <https://portal.sejong.ac.kr/jsp/inquiry/pwinquiry.jsp>; no library can do it for the student. The library never retries a rejected login, and stops at the first wrong password without further requests. If your service lets anyone type a student ID, limit failed attempts per student ID so a stranger cannot lock someone else's account.

## Configuration

All optional. Defaults: 10 second timeout, 3 login retries, certificate verification on.

| Java (`application.yml`) | Python | Node |
|---|---|---|
| `sejongpeia.timeout-seconds: 10` | `SejongAuthClient(timeout_seconds=10)` | `new SejongAuthClient({ timeoutSeconds: 10 })` |
| `sejongpeia.max-retry: 3` | `max_retry=3` | `maxRetry: 3` |
| `sejongpeia.ssl-verification: true` | `ssl_verification=True` | `sslVerification: true` |

<details>
<summary>Java TLS note and portal flow docs</summary>

The Sejong portal only offers the legacy `TLS_RSA_WITH_AES_256_CBC_SHA` cipher, which recent JDKs disable. The Java library removes `TLS_RSA_*` from `jdk.tls.disabledAlgorithms`, which affects **the whole JVM** and only works before the JVM's first TLS connection. Spring Boot does this automatically at startup. Without Spring, call `SejongTlsSupport.ensureLegacyCipherEnabled()` first thing in `main()`; if it runs too late, authentication fails with `CONNECTION_FAILED` and a message saying so. If your service cannot accept the JVM-wide change, run the authentication in a separate process. Python and Node only change the TLS settings of their own connections.

How the portal flow works: [spec/PORTAL_FLOW.md](spec/PORTAL_FLOW.md), [DHC login flow](docs/DHC_LOGIN_FLOW.en.md), [SIS login flow](docs/SIS_LOGIN_FLOW.en.md).

</details>

## More: integration examples and API stability

- [Integration examples](docs/INTEGRATION_EXAMPLES.en.md): student verification (club sign-up), department and lecture search, an academic calendar widget and a dormitory/cafeteria bot in Java, Python and Node. The public-data examples were run against the real school servers.
- [API stability policy](docs/API_STABILITY.en.md): what is public API and what is internal, and the version rule. **Backward compatibility is not guaranteed, so pin the version.**
- [Using with coding agents](https://cassiiopeia.github.io/pro-sejongpeia/en/guides/agents/): [llms.txt](https://cassiiopeia.github.io/pro-sejongpeia/llms.txt), [api.json](https://cassiiopeia.github.io/pro-sejongpeia/api.json) and prompt examples for agents such as Claude Code, Cursor and Codex. An agent contributing to this repository reads [AGENTS.md](AGENTS.md).

## FAQ

<details>
<summary><b>Is this an official Sejong University library?</b></summary>

No. It is a personal open-source project by a Sejong student and is not affiliated with the university.
</details>

<details>
<summary><b>Authentication suddenly fails / my portal account got locked</b></summary>

Try the [demo](https://cassiiopeia.github.io/pro-sejongpeia/#demo) first. If the demo also fails, the portal has probably changed: please open an [issue](https://github.com/Cassiiopeia/pro-sejongpeia/issues) with the error code.
`ACCOUNT_LOCKED` means the portal locked the account: reset the password at <https://portal.sejong.ac.kr/jsp/inquiry/pwinquiry.jsp> (identity verification required). Waiting does not unlock it.
</details>

The password is not stored. The demo server behind the project page does not store input either; in your own service, never log or persist passwords.

## Contributing

Bug reports, portal-change fixes and docs improvements are all welcome. A portal change is fixed in all three languages in one PR. See [CONTRIBUTING.en.md](CONTRIBUTING.en.md).
Report security issues privately as described in [SECURITY.md](SECURITY.md).

If this library saved you time, a ⭐ helps other Sejong developers find it.

## License

[MIT](LICENSE) © 2025 Suh Saechan

---

<sub>This project is not an official product of Sejong University and is not affiliated with it. The Sejong University seal belongs to Sejong University.</sub>
