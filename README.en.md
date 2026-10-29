<div align="center">

<img src="docs/assets/sejong-seal.png" alt="Sejong University seal" width="120">

<img src="docs/assets/banner/banner-a-crimson.en.svg" alt="pro-sejongpeia: the Sejong University integrated library, portal login and public school data" width="880">

# pro-sejongpeia

**Pro Sejongpeia: a little utopia for Sejong developers.**
From student verification to departments, lectures, the academic calendar and cafeteria menus: one call in Java, Python or Node.js.

<sub>세종대 로그인 API · 세종대학교 포털 인증 · 학번 인증 라이브러리 (Java · Python · Node)</sub>

[Site](https://cassiiopeia.github.io/pro-sejongpeia/) · [Try it in the browser](https://cassiiopeia.github.io/pro-sejongpeia/#demo) · [Quick start](#quick-start-java--spring-boot) · [Features](#features) · [Docs](#more-integration-examples-and-api-stability) · [한국어](README.md) | English

[![Maven Central](https://img.shields.io/maven-central/v/kr.suhsaechan/sejongpeia?label=Maven%20Central&color=C20F2F)](https://central.sonatype.com/artifact/kr.suhsaechan/sejongpeia)
[![PyPI](https://img.shields.io/pypi/v/pro-sejongpeia?label=PyPI&color=324D60)](https://pypi.org/project/pro-sejongpeia/)
[![npm](https://img.shields.io/npm/v/pro-sejongpeia?label=npm&color=324D60)](https://www.npmjs.com/package/pro-sejongpeia)
[![License: MIT](https://img.shields.io/badge/License-MIT-324D60)](LICENSE)
[![Live portal check](https://github.com/Cassiiopeia/pro-sejongpeia/actions/workflows/PROJECT-SEJONG-LIVE-CHECK.yml/badge.svg)](https://github.com/Cassiiopeia/pro-sejongpeia/actions/workflows/PROJECT-SEJONG-LIVE-CHECK.yml)

</div>

---

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

Student verification can be tried in the browser first. The code is in the [quick start](#quick-start-java--spring-boot).

<p align="center">
  <a href="https://cassiiopeia.github.io/pro-sejongpeia/#demo">
    <img src="docs/assets/demo-en-dark.png" alt="Live authentication demo on the project page" width="760">
  </a>
  <br>
  <sub>Live demo on the project page (click to try). A real authentication result with personal fields replaced by sample values.</sub>
</p>

## Features

**Everything is supported in all three languages**, with the same features and the same result shape. Only the naming follows each language's convention (snake_case in Python).

| Feature | Java | Python | Node |
|---|---|---|---|
| **Login (student ID + portal password)** | | | |
| Integrated | ✅ `authenticate` | ✅ `authenticate` | ✅ `authenticate` |
| DHC | ✅ `authenticateWithDHC` | ✅ `authenticate_dhc` | ✅ `authenticateDhc` |
| SIS | ✅ `authenticateWithSIS` | ✅ `authenticate_sis` | ✅ `authenticateSis` |
| SIS codes, `extra` | ✅ `authenticate(id, pw, false, true)` | ✅ `include_extra=True` | ✅ `{ includeExtra: true }` |
| Raw response | ✅ `authenticateWithRaw` and more | ✅ `include_raw=True` | ✅ `{ includeRaw: true }` |
| SIS business periods | ✅ `fetchSisBusinessPeriods` | ✅ `fetch_sis_business_periods` | ✅ `fetchSisBusinessPeriods` |
| **No login (public school data)** | | | |
| Department and term codes | ✅ `departments` and more | ✅ `departments` and more | ✅ `departments` and more |
| Lectures | ✅ `lectures` | ✅ `lectures` | ✅ `lectures` |
| Curriculum and overview | ✅ `curriculumCourses` | ✅ `curriculum_courses` | ✅ `curriculumCourses` |
| Academic calendar (undergrad, graduate) | ✅ `calendar`, `graduateCalendar` | ✅ `calendar`, `graduate_calendar` | ✅ `calendar`, `graduateCalendar` |
| Dormitory menu and notices | ✅ `dormitoryMenu`, `dormitoryNotices` | ✅ `dormitory_menu`, `dormitory_notices` | ✅ `dormitoryMenu`, `dormitoryNotices` |
| Cafeteria list and menu | ✅ `cafeteriaPlaces`, `cafeteriaMenu` | ✅ `cafeteria_places`, `cafeteria_menu` | ✅ `cafeteriaPlaces`, `cafeteriaMenu` |
| Campus places | ✅ `campusPlaces` | ✅ `campus_places` | ✅ `campusPlaces` |
| Extracurricular events | ✅ `extracurricularEvents` | ✅ `extracurricular_events` | ✅ `extracurricularEvents` |

- **Integrated** returns student info + classic reading + contact, **DHC** student info + classic-reading certification status, **SIS** student info + email, phone and English name. **SIS codes** are the 11 school-provided codes such as the department code, and `extra` holds the other SIS fields. **SIS business periods** are the per-business allowed periods such as course registration or certificate printing.
- **Lectures** are searched by department, course name or instructor, and an empty search returns the whole term. **Curriculum and overview** give the suggested year and semester, credits and the course overview.
- The category, term and department methods are `organizationCategories`, `terms` and `departments`; the lecture helper codes are `courseTypes` and `selectiveDomains` (Python: `organization_categories`, `course_types`, `selective_domains`). The public-data contract is [spec/PUBLIC_DATA.md](spec/PUBLIC_DATA.md).
- In Java the raw-response variants are `authenticateWithRaw`, `authenticateWithDHCRaw` and `authenticateWithSISRaw`.
- **Same results across languages** is enforced by keeping shared values in one place, [spec/contract.json](spec/contract.json), and failing each language's tests when they drift.
- The integrated call logs in to the portal **once** and reads DHC and SIS with the same session. It still returns student info when the SIS (contact) lookup fails; contact fields are then null. Measured call time (2026-10-24, from Seoul, median of 5 calls): Java 0.57 s, Python 0.76 s, Node 0.69 s, mostly the portal's own response time.
- SIS business periods are not the public academic calendar (`calendar`): they are the per-business allowed periods of the student's own SIS "학사일정조회" screen (`period`, `scheduleName`, `businessName`, `target`, `timeText` and more, school text as-is), queried by year and month (empty month = the whole year). The school ends the session on a malformed request, so the library sends the requests in the browser's exact order and stops without retry on a failure. Verified with one undergraduate account; other student types are not verified. The flow is step 2d of [spec/PORTAL_FLOW.md](spec/PORTAL_FLOW.md).

| Login data source | Domain | Data |
|---|---|---|
| **DHC** (Daeyang Humanity College) | classic.sejong.ac.kr | Student info + classic-reading certification |
| **SIS** (Student Information System) | sjpt.sejong.ac.kr | Student info + email, phone, English name |

## Why this library

The hard parts you would otherwise run into are already solved.

| Doing it yourself | With this library |
|---|---|
| Re-enable the only cipher the portal accepts (`TLS_RSA_WITH_AES_256_CBC_SHA`), which current JDK and OpenSSL defaults block, differently in each language | Handled in Java (JVM setting), Python and Node, and applied automatically at startup in Spring Boot |
| Work around a firewall that blocks default library User-Agents (and sites that answer 404 without one) | Sends an identifying User-Agent automatically |
| Portal login form, SSO redirects, session cookies across three Sejong domains | Handled per call, nothing to store (see [spec/PORTAL_FLOW.md](spec/PORTAL_FLOW.md)) |
| Read the portal's verdict from a 200 response (wrong password, locked account); a retried wrong password locks the account | Stops at the first failure with `AUTHENTICATION_FAILED` / `ACCOUNT_LOCKED` |
| Parse HTML tables (DHC) and a JSON API (SIS); log in once for student info, again for contacts | Typed results, one portal login per call |
| One school server failing shakes your whole app | Public data falls back to the last cached value or an empty list, with a 24-hour in-memory cache |
| Find out the portal changed when your users complain | A daily workflow signs in to the live portal in all three languages and opens an issue when it fails |
| Keep three implementations in sync | Shared values live in [spec/contract.json](spec/contract.json); tests fail if they drift |

Python and Node have **zero runtime dependencies**.

## Use cases

- **Verifying Sejong students** when they join a club, student council or campus community
- Checking major or year for campus events, study helpers that show classic-reading certification progress
- Department and lecture search, academic calendar widgets, dormitory and cafeteria bots ([integration examples](docs/INTEGRATION_EXAMPLES.en.md))

## Quick start: Java / Spring Boot

Published to **Maven Central** (no extra repository needed). Java 17+, Spring Boot 3.x.

```groovy
dependencies {
    implementation "kr.suhsaechan:sejongpeia:<version>"   // see the Maven Central badge
}
```

<details>
<summary>Maven</summary>

```xml
<dependency>
    <groupId>kr.suhsaechan</groupId>
    <artifactId>sejongpeia</artifactId>
    <version><!-- see the Maven Central badge --></version>
</dependency>
```

</details>

Spring Boot auto-configuration registers a `SuhSejongAuthEngine` bean.

```java
import kr.suhsaechan.sejongpeia.auth.model.SejongAuthResult;
import kr.suhsaechan.sejongpeia.auth.service.SuhSejongAuthEngine;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final SuhSejongAuthEngine sejongAuthEngine;

    @PostMapping("/auth/sejong")
    public SejongAuthResult authenticate(@RequestParam String studentId,
                                         @RequestParam String password) {
        return sejongAuthEngine.authenticate(studentId, password);
    }
}
```

<details>
<summary>Without Spring (plain Java)</summary>

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

</details>

**Check classic reading (Java)**

```java
SejongDhcAuthResult result = sejongAuthEngine.authenticateWithDHC(studentId, password);

for (ClassicCertification certification : result.getClassicReading().getCertifications()) {
    System.out.println(certification.getArea() + " "
        + certification.getCertifiedCount() + "/" + certification.getRequiredCount());
}
```

## Quick start: Python

Python 3.9+, no dependencies beyond the standard library.

```bash
pip install pro-sejongpeia
```

```python
from sejongpeia import authenticate, SejongAuthError, ErrorCode

try:
    result = authenticate("21011234", "password")
    print(result.name, result.major, result.grade)
except SejongAuthError as error:
    if error.code == ErrorCode.AUTHENTICATION_FAILED:
        print("Wrong student ID or password")
    elif error.code == ErrorCode.ACCOUNT_LOCKED:
        print("Portal account locked: reset the password on the portal")
```

## Quick start: Node.js

Node 18+, zero dependencies, TypeScript types, ESM and CommonJS. Server-side only.

```bash
npm install pro-sejongpeia
```

```js
import { authenticate, SejongAuthError, ErrorCode } from 'pro-sejongpeia';

try {
  const result = await authenticate('21011234', 'password');
  console.log(result.name, result.major, result.grade);
} catch (error) {
  if (error instanceof SejongAuthError && error.code === ErrorCode.AUTHENTICATION_FAILED) {
    console.log('Wrong student ID or password');
  } else if (error instanceof SejongAuthError && error.code === ErrorCode.ACCOUNT_LOCKED) {
    console.log('Portal account locked: reset the password on the portal');
  }
}
```

## School data without login

Public school data in the same shape, no student ID or password needed. For the method list see the [Features](#features) table above, and for the first call see [Try it now](#try-it-now). Here is an example that continues from a lecture to its curriculum (Node and Java use the same method names in camelCase).

```python
from sejongpeia import SejongPublicData

public_data = SejongPublicData()
departments = public_data.departments("20", "2026", "20")          # undergraduate, 2026 second semester
lectures = public_data.lectures("20", "2026", "20", department_code=departments[0].code)
# the course overview and suggested year/semester in that department's curriculum (empty list if not part of it)
courses = public_data.curriculum_courses(lectures[0].department_code, lectures[0].course_number)
```

- **Raw school values.** No code tables or name mappings are baked in, so renamed departments need no code change. School fields that are not mapped land in `extra`.
- **One source failing never breaks the rest.** On failure you get the last cached value or an empty list, not an exception (only invalid input raises).
- Results are cached in memory for 24 hours and identical requests are sent once. Please be gentle with the school's servers.
- `cafeteria_places` lists the cafeterias (number, name, building, location). The school has no list endpoint, so the list is built from the menu answers for numbers 1 to 10: a cafeteria with no menu that day can be missing, and one refresh makes up to 10 requests, one after the other (cached for 24 hours). `meal_type_id` 1 is lunch and 2 is dinner; fixed-menu cafeterias leave it empty. Details in [spec/PUBLIC_DATA.md](spec/PUBLIC_DATA.md).
- The lecture search handles the awkward parts for you, such as the school requiring every filter key in the request. With department, course name and instructor all empty it returns the whole term in one call (about 2,800 undergraduate rows and 2.4 MB for 2026/2, so prefer one department at a time when you can).

## Result fields

<details>
<summary>Show all result fields</summary>

Names below are Java / Node. Python uses snake_case (`student_id`, `phone_number`, ...). Full schema: [spec/RESULT_SCHEMA.md](spec/RESULT_SCHEMA.md).

| Field | Type | Description |
|---|---|---|
| `success` | boolean | Whether authentication succeeded |
| `studentId` | string | Student ID |
| `name` | string | Name |
| `major` | string | Major |
| `grade` | string | Year |
| `status` | string | Enrollment status (재학 enrolled, 휴학 on leave, 졸업 graduated) |
| `classicReading` | object | Classic-reading certification, exam, contest and substitution records, plus `completedSemesters`, `certifiedSemesters` and `certificationStatus` (portal text as-is) |
| `email` | string or null | Email (null if SIS fails) |
| `phoneNumber` | string or null | Phone, `010-1234-5678` format (null if SIS fails) |
| `englishName` | string or null | English name (null if SIS fails) |
| `userCategory` | string or null | User category, for example `학부생` (null if SIS fails) |
| `parentUnitName` | string or null | Unit above the major, a college or a school, from the portal main page (null if that call fails, `""` if unavailable). Portal text as-is that changes by academic year: do not branch on a value list or use it to decide authentication |
| `authenticatedAt` | datetime | Authentication time |
| `departmentCode` and 10 more codes | string or null | School codes from SIS as-is: `departmentCode`, `organizationClassificationCode`, `enrollmentStatusCode`, `userClassificationCode`, `userClassificationLargeCode`, `userClassificationMiddleCode`, `roleCode`, `roleName`, `menuSystemId`, `menuSystemName`, `locale` (null if SIS fails, `""` if missing). No code-to-name mapping in the library |
| `extra` | map or null | Only with the extra option: the other SIS fields as `{"dataset.FIELD": "value"}`. Sensitive keys (session value, client IP, password and account flags, resident number, bank account, address, staff names) are always excluded, but by name pattern, so do not pass it to an external client as-is |
| `rawHtml` | string or null | DHC page as received (only with the raw option) |
| `rawJson` | string or null | SIS response as received (only with the raw option, null if SIS fails) |

- The raw option adds no request: it only keeps responses already received. The SIS raw text contains values that look like a session identifier, the client IP and login time, so never pass it to an external client as-is. Its keys and structure can change when the school changes them and are outside the compatibility guarantee.
- DHC result: no contact fields; `rawHtml` with the raw option
- SIS result: includes `userCategory`, no classic-reading field, `grade` and `status` are empty strings; `rawJson` with the raw option

Text values come from the portal as-is (Korean).

</details>

## Error codes

Every language raises one error type with a `code`:

| Code | Meaning |
|---|---|
| `AUTHENTICATION_FAILED` | Student ID or password is wrong |
| `ACCOUNT_LOCKED` | The portal locked the account after repeated wrong passwords |
| `ACCOUNT_UNAVAILABLE` | The portal account cannot sign in (use period ended or blocked by the administrator) |
| `INVALID_INPUT` | Input is empty (checked before any network call) |
| `CONNECTION_FAILED` | Cannot reach the Sejong portal |
| `CONNECTION_TIMEOUT` | Portal timed out |
| `DATA_FETCH_FAILED` | Failed to fetch student info |
| `PARSE_ERROR` | Could not parse the portal response (structure changed?) |
| `SESSION_ERROR` | Portal session error |
| `SSL_CONFIGURATION_ERROR` | SSL configuration error |

Branch on the code, not on the message.

> **Account lock.** The Sejong portal locks an account after repeated wrong passwords (its page says about 5). Unlocking needs a password reset at <https://portal.sejong.ac.kr/jsp/inquiry/pwinquiry.jsp>; no library can do it for the student. The library never retries a rejected login, and stops at the first wrong password without further requests. If your service lets anyone type a student ID, limit failed attempts per student ID so a stranger cannot lock someone else's account.

## Configuration

All optional. Defaults: 10 second timeout, 3 login retries, certificate verification on.

| Java (`application.yml`) | Python | Node |
|---|---|---|
| `sejongpeia.timeout-seconds: 10` | `SejongAuthClient(timeout_seconds=10)` | `new SejongAuthClient({ timeoutSeconds: 10 })` |
| `sejongpeia.max-retry: 3` | `max_retry=3` | `maxRetry: 3` |
| `sejongpeia.ssl-verification: true` | `ssl_verification=True` | `sslVerification: true` |

> **Java TLS note**: the Sejong portal only offers the legacy `TLS_RSA_WITH_AES_256_CBC_SHA` cipher, which recent JDKs disable. The Java library removes `TLS_RSA_*` from `jdk.tls.disabledAlgorithms`, which affects **the whole JVM** and only works before the JVM's first TLS connection. Spring Boot does this automatically at startup. Without Spring, call `SejongTlsSupport.ensureLegacyCipherEnabled()` first thing in `main()`; if it runs too late, authentication fails with `CONNECTION_FAILED` and a message saying so. If your service cannot accept the JVM-wide change, run the authentication in a separate process. Python and Node only change the TLS settings of their own connections.

How the portal flow works: [spec/PORTAL_FLOW.md](spec/PORTAL_FLOW.md), [DHC login flow](docs/DHC_LOGIN_FLOW.en.md), [SIS login flow](docs/SIS_LOGIN_FLOW.en.md).

## More: integration examples and API stability

- [Integration examples](docs/INTEGRATION_EXAMPLES.en.md): student verification (club sign-up), department and lecture search, an academic calendar widget and a dormitory/cafeteria bot in Java, Python and Node. The public-data examples were run against the real school servers.
- [API stability policy](docs/API_STABILITY.en.md): what is public API and what is internal, and the version rule. **Backward compatibility is not guaranteed, so pin the version.**

## FAQ

<details>
<summary><b>Is the password stored?</b></summary>

No. The libraries only use it for the portal login request. The demo server behind the project page also returns the result without storing input.
In your own service, never log or persist passwords.
</details>

<details>
<summary><b>Is this an official Sejong University library?</b></summary>

No. It is a personal open-source project by a Sejong student and is not affiliated with the university.
</details>

<details>
<summary><b>Authentication suddenly fails</b></summary>

Try the [demo](https://cassiiopeia.github.io/pro-sejongpeia/#demo) first. If the demo also fails, the portal has probably changed.
Please open an [issue](https://github.com/Cassiiopeia/pro-sejongpeia/issues) with the error code.
</details>

<details>
<summary><b>My portal account got locked</b></summary>

The portal locks an account after repeated wrong passwords, and the library reports `ACCOUNT_LOCKED`. Reset the password at <https://portal.sejong.ac.kr/jsp/inquiry/pwinquiry.jsp> (identity verification required). Waiting does not unlock it.
</details>

## Contributing

Bug reports, portal-change fixes and docs improvements are all welcome. A portal change is fixed in all three languages in one PR. See [CONTRIBUTING.en.md](CONTRIBUTING.en.md).
Report security issues privately as described in [SECURITY.md](SECURITY.md).

If this library saved you time, a ⭐ helps other Sejong developers find it.

## License

[MIT](LICENSE) © 2025 Suh Saechan

---

<sub>This project is not an official product of Sejong University and is not affiliated with it. The Sejong University seal belongs to Sejong University.</sub>
