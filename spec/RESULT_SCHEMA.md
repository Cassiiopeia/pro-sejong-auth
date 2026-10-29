# Result schema and error codes (single source for all languages)

Java is the reference. Python uses snake_case, Node uses camelCase (same as Java).

## Operations

| Operation | Java | Python | Node |
|---|---|---|---|
| Integrated (DHC + SIS contacts) | `authenticate(id, pw)` / `authenticateWithRaw(id, pw)` / `authenticate(id, pw, raw, extra)` | `authenticate(id, pw, include_raw=False, include_extra=False)` | `authenticate(id, pw, { includeRaw, includeExtra })` |
| DHC | `authenticateWithDHC` / `authenticateWithDHCRaw` | `authenticate_dhc(id, pw, include_raw=False)` | `authenticateDhc(id, pw, { includeRaw })` |
| SIS | `authenticateWithSIS` / `authenticateWithSISRaw` / `authenticateWithSIS(id, pw, raw, extra)` | `authenticate_sis(id, pw, include_raw=False, include_extra=False)` | `authenticateSis(id, pw, { includeRaw, includeExtra })` |

Input check first: blank student id or password raises `INVALID_INPUT` before any network call.

## Integrated result

| Java / Node | Python | Type | Rule |
|---|---|---|---|
| success | success | boolean | always true when returned |
| studentId | student_id | string | from DHC |
| name | name | string | from DHC |
| major | major | string | from DHC |
| grade | grade | string | from DHC |
| status | status | string | from DHC |
| classicReading | classic_reading | object | from DHC |
| email | email | string or null | from SIS; null if the SIS lookup fails for any reason |
| phoneNumber | phone_number | string or null | same |
| englishName | english_name | string or null | same |
| userCategory | user_category | string or null | same (SIS `dm_UserInfoSch.USER_CLSF_CD_NM`, for example `학부생`) |
| parentUnitName | parent_unit_name | string or null | portal main page `span.txt4`; null if the portal main call fails, `""` if the page has no usable value |
| authenticatedAt | authenticated_at | datetime (local time) | |
| (SIS school codes) | (same names in snake_case) | string or null | the 11 fields of the table below; null if the SIS lookup fails |
| extra | extra | map or null | only with the extra option, see below; null otherwise |
| rawHtml | raw_html | string or null | DHC page as received; only with the raw option, otherwise null |
| rawJson | raw_json | string or null | SIS `initUserInfo` response as received; only with the raw option, null if the SIS lookup failed |

`parentUnitName` is the name of the unit above the major (a college or a school), extracted from the portal screen text as extra information: it can be `""` depending on the student type, never use it to decide authentication. It is the portal text as-is that changes with the academic year, never branch on a list of values.

The integrated call logs in to the portal once and uses that session for DHC, SIS and the portal main page. The raw option adds no request: it only keeps the responses already received. It is off by default and changes no other field.

Safety: the SIS raw text contains values that look like a session identifier, the client IP and login time. Never pass it to an external client as-is. Its keys and structure can change whenever the school changes them, so it is outside the compatibility guarantee (use the named fields for stable access).

A failing DHC step fails the whole call. A failing SIS step or portal main step never fails the integrated call.

## SIS school codes and extra

All values are the school's strings as-is (no code tables, no name mapping, `""` when missing). Datasets are searched in the order `dm_UserInfoSch`, `dm_UserInfoGam`, `dm_UserInfo`; the first non-blank value wins. They are returned by default in the SIS result and the integrated result (null there if the SIS lookup fails).

| Field (Python) | SIS key | Note |
|---|---|---|
| departmentCode (department_code) | `DEPT_CD`, else `DEPT_NO` | `DEPT_CD` first; the two are expected to be equal and are not verified for every student state |
| organizationClassificationCode | `ORGN_CLSF_CD` | |
| enrollmentStatusCode | `STATUS_DIV_CD` | raw code only, meaning not verified for every state |
| userClassificationCode / userClassificationLargeCode / userClassificationMiddleCode | `USER_CLSF_CD` / `USER_CLSF_L_CD` / `USER_CLSF_M_CD` | |
| roleCode / roleName | `RPSN_ROLE_CD` / `RPSN_ROLE_CD_NM` | |
| menuSystemId / menuSystemName | `MENU_SYS_ID` / `MENU_SYS_NM` | |
| locale | `INTG_LOCALE` | |

`extra` (off by default, option `include_extra` / `includeExtra` / `shouldIncludeExtra`): the other scalar fields of the three datasets as `{"dataset.FIELD": "value"}`, so a field the school adds is reachable without a release. Keys matching `sis.extraExcludedKeyPattern` in `contract.json` (case-insensitive) are never included: session value, client IP, password and account state flags, resident number, bank account, address, staff-name `ALIAS` fields, phone, mail, birth date. The filter is a best-effort name pattern, so do not forward `extra` to an external client as-is.

## DHC result

`success, studentId, name, major, grade, status, classicReading, authenticatedAt, rawHtml` (rawHtml only with the raw option, otherwise null)

## SIS result

`success, studentId, name, major, grade (always ""), status (always ""), email, phoneNumber, englishName, userCategory, the school codes above, extra (only with the extra option), authenticatedAt, rawJson` (rawJson only with the raw option)

## classicReading

| Field | Type | Rule |
|---|---|---|
| completedSemesters | string | DHC user info table `이수 학기` as-is (for example `8 학기`), empty string if missing |
| certifiedSemesters | string | `인증완료 학기` as-is, empty string if missing |
| certificationStatus | string | `인증여부` as-is (free text, can carry a substitution reason), empty string if missing |

| Field | Item fields |
|---|---|
| certifications | area, requiredCount, certifiedCount |
| examRecords | semester, area, bookTitle, examDate, score, passStatus |
| subjectSubstitutions | semester, subjectName, area, bookTitle, completion |
| contestRecords | semester, contestName, area, bookTitle |
| curriculumRecords | semester, subjectName, area, bookTitle, completion |

Python field names are snake_case (`completed_semesters`, `certified_semesters`, `certification_status`, `required_count`, `book_title`, ...).

## Error codes

All languages raise one error type carrying `code` (string, Java enum name) and a message in English.

| Code | Meaning |
|---|---|
| `INVALID_INPUT` | Student ID or password is blank |
| `AUTHENTICATION_FAILED` | Wrong student ID or password (portal login `erridpwd`/`Error`; fallback DHC 401, SIS 200 with `_SUBMIT_ERROR_` LOGOUT) |
| `CONNECTION_FAILED` | Network error, or login retries exhausted |
| `CONNECTION_TIMEOUT` | Timed out on the last login attempt |
| `SESSION_ERROR` | SSO step returned non-2xx |
| `DATA_FETCH_FAILED` | Data page returned non-200 or empty |
| `PARSE_ERROR` | Response structure not as expected |
| `SSL_CONFIGURATION_ERROR` | TLS setup failed |
| `ACCOUNT_LOCKED` | Portal locked the account after repeated wrong passwords (`pwdNeedChg`); reset at https://portal.sejong.ac.kr/jsp/inquiry/pwinquiry.jsp |
| `ACCOUNT_UNAVAILABLE` | Portal account cannot sign in: use period ended (`invalidDt`) or blocked by the administrator (`invalid`) |

Java also keeps the deprecated `INVALID_CREDENTIALS`, which is never thrown and does not exist in Python or Node.

## Defaults

| Setting | Java | Python | Node | Default |
|---|---|---|---|---|
| timeout | `sejongpeia.timeout-seconds` | `timeout_seconds` | `timeoutSeconds` | 10 |
| retries | `sejongpeia.max-retry` | `max_retry` | `maxRetry` | 3 |
| certificate check | `sejongpeia.ssl-verification` | `ssl_verification` | `sslVerification` | true |
