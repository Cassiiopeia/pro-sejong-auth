# Portal flow (single source for all languages)

Every language implementation (Java, Python, Node) follows exactly this flow. When the Sejong portal changes, update this file first, then all three implementations in the same PR.

Reference implementation: `java/src/main/java/kr/suhsaechan/sejongpeia/client/`.

See also (no login needed): [REFERENCE_DATA.md](REFERENCE_DATA.md) for the public organization / department code tables, and [REFERENCE_DATA_SURVEY.md](REFERENCE_DATA_SURVEY.md) for where the school serves what.

## Transport requirements

| Requirement | Value | Why |
|---|---|---|
| TLS | TLSv1.2, cipher `AES256-SHA` (`TLS_RSA_WITH_AES_256_CBC_SHA`) must be allowed | The portal offers only this cipher. Recent JDK / OpenSSL defaults disable it |
| Python | `ssl` context ciphers `DEFAULT:AES256-SHA:@SECLEVEL=1` | Verified with Python 3.14 / OpenSSL 3.6 |
| Node | `https.Agent({ ciphers: 'DEFAULT:AES256-SHA:@SECLEVEL=1' })` | Verified with Node 26 / OpenSSL 3.6 (keep SECLEVEL 1: level 0 would also accept weak certificate keys) |
| Java | remove `TLS_RSA_*` from `jdk.tls.disabledAlgorithms` (JVM-wide, see `SejongTlsSupport`) | JSSE reads the property once, at the JVM's first TLS use: later changes have no effect. Spring Boot: `SejongTlsEnvironmentPostProcessor` at startup. Plain Java: call `SejongTlsSupport.ensureLegacyCipherEnabled()` first in `main()`. Too late: `CONNECTION_FAILED` with that hint |
| User-Agent | Always send a non-empty, non-default User-Agent: `pro-sejongpeia-<language>/<version>` | Without it classic.sejong.ac.kr returns 404 after SSO, and sjpt.sejong.ac.kr's firewall blocks default library agents (for example `Python-urllib`) with an "Alert!!!" HTML page |
| Cookies | One cookie jar per authentication call, honoring `Domain=.sejong.ac.kr` | `ssotoken` is set for `.sejong.ac.kr` and must reach classic / sjpt |
| Redirects | Follow 301/302/303/307 automatically | SSO answers 303 |
| Timeout | default 10 seconds (connect and read) | |
| Retry | default 3 attempts, only for the portal login request: retry on non-2xx (close the response first) and on timeout. Timeout on the last attempt: `CONNECTION_TIMEOUT`. Exhausted: `CONNECTION_FAILED` | |
| Certificate verification | on by default, optional switch to turn it off | |

## Step 1: Portal login (shared by DHC and SIS)

```
POST https://portal.sejong.ac.kr/jsp/login/login_action.jsp
Content-Type: application/x-www-form-urlencoded
Host: portal.sejong.ac.kr
Referer: https://portal.sejong.ac.kr
Cookie: chknos=false

mainLogin=N&rtUrl=<return url>&id=<student id>&password=<password>
```

- DHC `rtUrl`: `classic.sejong.ac.kr`
- SIS `rtUrl`: `sjpt.sejong.ac.kr/main/view/Login/doSsoLogin.do?p=`
- Integrated call: log in once with the DHC `rtUrl`, then run Step 2a and Step 2b with the same cookie jar (SIS SSO works after a DHC login, verified 2026-10-24)
- URLs, verdict values and error messages shared by all languages: `contract.json`
- The 200 response is a small HTML page whose script holds the verdict: `var result = '<value>'` (samples `fixtures/portal_login_ok.html`, `fixtures/portal_login_erridpwd.html`). Handle it exactly like the portal page does, and never retry a rejected login (each wrong password counts toward the lock):

| `result` | Portal message | Library |
|---|---|---|
| `OK` | (redirects to rtUrl) | continue |
| `erridpwd`, `Error` | 아이디나 비밀번호가 일치하지 않습니다 | `AUTHENTICATION_FAILED`, no further requests |
| `pwdNeedChg` | Account locked after repeated wrong passwords, reset the password | `ACCOUNT_LOCKED` (reset page `https://portal.sejong.ac.kr/jsp/inquiry/pwinquiry.jsp`) |
| `invalidDt`, `invalid` | Use period ended / blocked by the administrator | `ACCOUNT_UNAVAILABLE` |
| any other value | 로그인 처리 중 오류 | `SESSION_ERROR` |
| no `var result` | (page format changed) | continue, the later 401 decides |

Expected mapping for tests: `fixtures/expected_portal_login.json`. The portal source comments say the lock happens after 5 failures; the exact count and whether a success resets it are not verified.

## Step 2a: DHC (classic.sejong.ac.kr)

1. `GET https://classic.sejong.ac.kr/_custom/sejong/sso/sso-return.jsp?returnUrl=https://classic.sejong.ac.kr/classic/index.do` (non-2xx after redirects: `SESSION_ERROR`)
2. `GET https://classic.sejong.ac.kr/classic/reading/status.do`
   - `401`: `AUTHENTICATION_FAILED`
   - other non-200 or empty body: `DATA_FETCH_FAILED`
   - `200`: HTML parsed by the rules below

## Step 2b: SIS (sjpt.sejong.ac.kr)

1. `GET https://sjpt.sejong.ac.kr/main/view/Login/doSsoLogin.do?p=` with `Referer: https://portal.sejong.ac.kr` (non-2xx: `SESSION_ERROR`)
2. `POST https://sjpt.sejong.ac.kr/main/sys/UserInfo/initUserInfo.do?addParam=<addParam>` body `{}`
   - headers: `Accept: application/json`, `Content-Type: application/json; charset=UTF-8`, `Referer: https://sjpt.sejong.ac.kr/main/view/Login/doSsoLogin.do?p=`, `Origin: https://sjpt.sejong.ac.kr`
   - `addParam` = Base64( URL-form-encode( `{"_runIntgUsrNo":"","_runPgLoginDt":"","_runningSejong":""}` ) ), appended to the URL as is
   - `401`: `AUTHENTICATION_FAILED`; other non-200 or empty body: `DATA_FETCH_FAILED`
   - `200` with `{"_SUBMIT_ERROR_": {"ERRTYPE": "LOGOUT", ...}}`: `AUTHENTICATION_FAILED` (a wrong password leaves no session, the API answers 200 with this error; sample `fixtures/sis_submit_error_logout.json`). Any other `_SUBMIT_ERROR_`: `DATA_FETCH_FAILED`

## Step 2d: SIS business periods (login required, separate operation)

`fetchSisBusinessPeriods(id, pw, year, month)` (Java `fetchSisBusinessPeriods`, Python `fetch_sis_business_periods`, Node `fetchSisBusinessPeriods(id, pw, { year, month })`): the school's business-period calendar of the student's own SIS ("학사일정조회": which business is allowed in which period). It is a different data set from the public academic calendar (`calendar`). Verified on 2026-10-30 with an undergraduate account against the browser's own request flow (2026 October 53 rows, whole year 160 rows). Other student types are not verified.

Input check first: `year` is 4 digits, `month` is 1-12 or empty (empty = the whole year), otherwise `INVALID_INPUT` without any request.

Every request of this operation is a `POST` with the headers of step 2b (`Content-Type: application/json; charset="UTF-8"`). **A malformed request makes the school end the session, so the order and the values below are exact and a failed step is never retried.** Same session, one login: portal login with `rtUrl` of step 2b, SSO, `initUserInfo` of step 2b, then

| # | Request | `addParam` keys (in this order) | Body |
|---|---|---|---|
| 1 | `main/view/Menu/doListUserMenuListLeft.do` (puts the menu permission into the server session; without it step 2 answers a system error) | `_runIntgUsrNo`, `_runPgLoginDt`, `_runningSejong` | `{"dm_ReqLeftMenu":{"MENU_SYS_ID":<menuSystemId>,"SYSTEM_DIV":"SCH","MENU_SYS_NM":<menuSystemName>}}` |
| 2 | `main/sys/UserRole/initUserRole.do` (opens the screen) | `pbForceLog` (`"false"`), `_runPgmKey`, `_runSysKey`, `_runIntgUsrNo`, `_runPgLoginDt`, `_runningSejong` | the same keys and values as the `addParam` |
| 3 | `sch/sch/sud/SudBaseScheSelfServQ/doList.do` | `_runPgmKey`, `_runSysKey`, `_runIntgUsrNo`, `_runPgLoginDt`, `_runningSejong` | `{"dm_search":{"ORGN_CLSF_CD":<organizationClassificationCode>,"YEAR":"<year>","SMT_CD":"","SMT_NM":"","USER_CLSFM_CD":<userClassificationMiddleCode>,"MONTH":"<month without leading zero or empty>"}}` |

`addParam` = Base64( URL-form-encode( compact JSON of the keys ) ). Values:

| Key | Value |
|---|---|
| `_runPgmKey` | `SELF_STUDSELF_SUB_20SELF_MENU_10SudBaseScheSelfServQ` (the menu code of "학사일정조회", fixed) |
| `_runSysKey` | `SCH` |
| `_runIntgUsrNo` | `dm_UserInfo.INTG_USR_NO` of `initUserInfo` |
| `_runningSejong` | `dm_UserInfo.RUNNING_SEJONG` of `initUserInfo` |
| `_runPgLoginDt` | steps 1 and 2: `dm_UserInfoGam.LOGIN_DT` (fallback `dm_UserInfoSch.LOGIN_DT`) of `initUserInfo`. Step 3: `dm_UserRole.PG_LOGIN_DT` of the step 2 answer |

The session values (`INTG_USR_NO`, `RUNNING_SEJONG`, `LOGIN_DT`) are never part of a result, a log line or an error message. `ORGN_CLSF_CD`, `USER_CLSFM_CD`, `MENU_SYS_ID` and `MENU_SYS_NM` are the user's own school codes (`organizationClassificationCode`, `userClassificationMiddleCode`, `menuSystemId`, `menuSystemName`); one of them empty: `DATA_FETCH_FAILED` without further requests.

Errors (no retry): non-200 or empty body `DATA_FETCH_FAILED`; `401` `AUTHENTICATION_FAILED`; `_SUBMIT_ERROR_` with `ERRTYPE` `LOGOUT` `SESSION_ERROR`, any other `_SUBMIT_ERROR_` `DATA_FETCH_FAILED` (only the step name in the message, the school's text never); missing `dm_UserRole.PG_LOGIN_DT` or `dl_mainList` that is not an array `PARSE_ERROR`.

Result: a list, one item per `dl_mainList` row. Samples: `fixtures/sis_business_periods.json`, `fixtures/sis_user_role.json`, expected `fixtures/expected_sis_business_periods.json`.

| Field (Python) | `dl_mainList` key | Note |
|---|---|---|
| period | `PERIOD` | text, for example `2026-03-01 ∼ 2026-03-15` |
| startDate (start_date), endDate (end_date) | `START_DT`, `TO_DT` | `yyyyMMdd` as-is (open ended periods end `29991231`) |
| scheduleName | `SCHEDULE` | |
| courseGroupName | `CORS_SCHE_GRP_CD_NM` | |
| target | `TARGET_AUTH` | |
| businessName | `SYS_NM` | |
| yearSemester | `YEAR_SMT` | |
| timeText | `TMS` | |
| categoryCode | `ORGN_CLSF_CD` | |
| sortOrder | `PRT_ORD` | string |
| extra | other scalar keys | same exclusion pattern as the SIS `extra` |

Missing or null values are `""`; one odd row never fails the list. An empty list is a valid answer.

## Step 2c: portal main (portal.sejong.ac.kr), integrated call only

1. `GET https://portal.sejong.ac.kr/user/index.do` with `Referer: https://portal.sejong.ac.kr`, same session, one attempt, the normal timeout, no retry
2. The result is `null` (call failure) for: non-2xx, timeout, network error, empty body, a login page answer (session expired: body contains `login_action.jsp`). Never an exception, the integrated result is returned with the other fields intact (Java logs one line `[portal main] skipped : <reason>`, no body)
3. Otherwise parse (below): no usable value gives `""` (parse stage), never `null`

## Portal main parsing (parentUnitName)

Sample: `fixtures/portal_main.html`, expected: `fixtures/expected_portal_main.json` (synthetic data).

- Text of the first `span.txt4` inside `div.top_myinfo`, whitespace collapsed to one space and trimmed. Shape `<parent unit> <major>`, for example `공과대학 컴퓨터공학과`
- Rule 1: the major known from DHC equals the text: `""`. The text ends with a space plus the major: the text before it (trimmed)
- Rule 2: otherwise, only when the text has exactly 2 space-separated tokens: the first token. One token, or 3 or more tokens: `""` (no guessing, a wrong value is worse than an empty one)
- No `div.top_myinfo`, no `span.txt4` or a broken page (a staff-type screen): `""`, never an exception
- The value is the portal text as-is: no list of units, no code table, no validation against allowed values

## DHC HTML parsing

Sample: `fixtures/dhc_status.html`, expected result: `fixtures/expected_dhc.json`.

- Each section is `div.b-con-box` with `h4.b-h4-tit01` title text; rows are `table.b-board-table tbody tr`
- Student info (title `사용자 정보`): rows of `th` label, `td` value. Labels: `학과명` major, `학번` student id, `이름` name, `학년` grade, `사용자 상태` status. Missing label: empty string. No rows at all: `PARSE_ERROR`. The same table also gives the classic-reading fields `이수 학기` completedSemesters, `인증완료 학기` certifiedSemesters, `인증여부` certificationStatus (as-is text, missing label: empty string, never `PARSE_ERROR`)
- Certifications (`영역별 인증현황`): row `th` = area, first `td` = requiredCount, second `td` = certifiedCount. Rows without a `th` or with fewer than 2 `td` are skipped
- Exam records (`인증 시험 현황`): 6 `td`: semester, area, bookTitle, examDate, score, passStatus
- Subject substitutions (`과목 대체 인증 현황`) and curriculum records (`교과연계 인증 현황`): 5 `td`: semester, subjectName, area, bookTitle, completion
- Contest records (`대회 인증 현황`): 4 `td`: semester, contestName, area, bookTitle
- Rows with fewer cells (for example "검색된 결과가 없습니다." in one `colspan` cell) are skipped
- Cell text: decode HTML entities (`&middot;` → `·`), collapse inner whitespace to one space, trim

## SIS JSON parsing

Sample: `fixtures/sis_user_info.json`, expected: `fixtures/expected_sis.json`.

| Field | Source | Fallback |
|---|---|---|
| studentId | `dm_UserInfo.INTG_USR_NO` (missing `dm_UserInfo`: `PARSE_ERROR`) | |
| name | `dm_UserInfo.INTG_USR_NM` | |
| major | `dm_UserInfoGam.DEPT_NM` | `dm_UserInfoSch.DEPT_NM` |
| email | `dm_UserInfoGam.USER_EMAIL` | |
| phoneNumber | `USER_PHONE_NO1`-`NO2`-`NO3` of `dm_UserInfoGam`, non-empty parts joined with `-` | |
| englishName | `dm_UserInfo.INTG_ENG_NM` | `dm_UserInfoSch.NM_ENG` |
| userCategory | `dm_UserInfoSch.USER_CLSF_CD_NM` | |

Missing values are empty strings. Invalid JSON: `PARSE_ERROR`.
