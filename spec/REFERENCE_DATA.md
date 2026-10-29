# Reference data: organization categories, departments, terms (no login)

Public code tables that the Sejong SIS serves to its own lecture-timetable screen. They let a caller classify a user's department **without hard-coding** college or department names and codes, which change every few years.

Status: **unofficial, undocumented endpoints** that the school's own web screen calls (see [REFERENCE_DATA_SURVEY.md](REFERENCE_DATA_SURVEY.md) for what else was checked). Treat these like any other part of the portal flow: when they change, update this file first, then all three implementations in the same PR.

Verified: 2026-10-30, Python 3.14 with the library's own transport, and a cookieless browser context.

## Source screen

`https://sjpt.sejong.ac.kr/app/ext/SueOpenTimeQ.jsp` (lecture timetable / syllabus lookup). The page wraps a WebSquare screen (`/sch/ext/SueOpenTimeQ.xml`) that loads its dropdowns from the three endpoints below.

## Transport

| Item | Value | Note |
|---|---|---|
| Login | **not required** | Same data with no cookie at all (verified with plain HTTP, no prior GET) |
| Host | `sjpt.sejong.ac.kr` | Same host and TLS rules as [PORTAL_FLOW.md](PORTAL_FLOW.md): legacy cipher, non-default User-Agent |
| Method / body | `POST`, `Content-Type: application/json; charset="UTF-8"`, JSON body | `Accept: application/json` |
| Base path | `/sch/sch/ext/SueOpenTimeQ/` | |
| Optional | `?addParam=<base64 of URL-encoded {"extLocale":"KOR"}>`, header `submissionid`, header `Referer` | The page sends them; the server answered the same without each of them (verified one by one) |

## Endpoints

All request bodies wrap the filter in `dm_search`. Unused filters can be omitted.

| Endpoint | Minimal body | Response dataset | Rows (2026-10-30) |
|---|---|---|---|
| `doOnload.do` | `{"dm_search":{}}` | `dl_code_orgn`, `dl_code_sltDomain`, `dl_code_curiType` | 22 / 31 / 81 |
| `doYearsmtlist.do` | `{"dm_search":{"ORGN_CLSF_CD":"20"}}` | `dl_code_yearSmt` | 115 for `20`, 57 for `30` |
| `doLesndeptgroup.do` | `{"dm_search":{"ORGN_CLSF_CD":"20","YEAR":"2026","SMT_CD":"20","YEAR_SMT":"2026/20"}}` | `dl_code_dept` | 103 for `20` at 2026/2 |

Failure shape: HTTP 200 with a `_SUBMIT_ERROR_` object (`ERRMSG`, `ERRTYPE`, `ERRCODE`) instead of the dataset, for example `doLesndeptgroup.do` with an empty `dm_search` ([fixtures/reference_submit_error.json](fixtures/reference_submit_error.json)). Always check for the expected dataset key; never trust the status code alone. The server may also include a `STACK_TRACE` with internal class and SQL details: never log it or pass it to callers, keep only `ERRTYPE` and `ERRCODE`.

### `dl_code_orgn`: organization category (조직분류)

| Field | Meaning |
|---|---|
| `COM_CD` | Category code. Use as `ORGN_CLSF_CD` elsewhere (`20` = undergraduate, `30` = general graduate school, `31`, `33`, `34`, `39`, `3J`, `3K` = professional / special graduate schools) |
| `CD_NM`, `CD_NM_ENG` | Category name (Korean / English) |
| `PRT_ORD` | Display order |
| `CLOSE_YN` | Closed flag (was empty for all rows) |

Do not read, store or log `CD_ALIAS1`..`CD_ALIAS3` (and their `_ENG` variants): `CD_ALIAS3` holds a staff member's personal name.

Several categories return 0 departments for the current term (closed or merged graduate schools): an empty list is a valid answer, not an error.

### `dl_code_yearSmt`: terms

`YEAR`, `SMT_CD`, `YEAR_SMT_CD` (`2026/20`), `YEAR_SMT_NM` (`2026/2학기`).

| `SMT_CD` | Term |
|---|---|
| `10` | 1st semester |
| `11` | summer session |
| `20` | 2nd semester |
| `21` | winter session |

### `dl_code_dept`: department / major (학과전공)

One row per unit, `DEPT_CD` unique within a response. The list **depends on the term**: undergraduate rows were 71 (2000/1), 73 (2010/1), 79 (2020/1), 101 (2025/2), 103 (2026/2). Departments are added and renamed by term, so always pass the term you mean.

| Field | Meaning |
|---|---|
| `DEPT_CD` | Unit code. **The same code the SIS returns for a user**: `dm_UserInfoSch.DEPT_CD` and `dm_UserInfoGam.DEPT_NO` of `initUserInfo.do` (equal for the account checked, and present in this list) |
| `DEPT_LEVEL_CD` | `2`: department or division (`DEPT_CD` == `DEPT_M_CD`). `3`: major inside a division (`DEPT_CD` == `DEPT_S_CD`, parent is `DEPT_M_CD`) |
| `DEPT_L_CD`, `DEPT_L_PRINT` | College code and clean name |
| `DEPT_M_CD`, `DEPT_M_PRINT` | Department / division code and clean name |
| `DEPT_S_CD`, `DEPT_S_PRINT` | Major code and clean name (only for level 3) |
| `DEPT_NM_ENG`, `DEPT_M_NM_ENG`, `DEPT_L_NM_ENG` | English names (some rows empty) |
| `ORGN_CLSF_CD` | Category the row belongs to |
| `CLOSE_DT` | Closing date (empty for every row seen) |
| `PRT_L_ORD`, `PRT_COMBO_ORD` | Display order |

Notes for parsers:

- The `*_NM` and `*_ALIAS` fields carry a decoration such as `【3517 학부】` on every row. The `*_PRINT` fields are clean (checked on all 103 rows), but `DEPT_L_PRINT` and `DEPT_M_PRINT` were empty on 2 of 103 rows. Use `*_PRINT` for names; when one is empty, return an empty string. Do not strip the decoration from `*_NM` to fill the gap.
- `DEPT_L_CD` is not always a real college. Seen: `3300` (interdisciplinary majors), `2021` (a legacy grouping), `2050`. The code is the join key; do not assume the name is a college.
- About 55 columns per row, many empty. Read only the fields above; unknown fields may be added or removed.

## Relation to other school data (code systems agree)

- The 두드림 (`do.sejong.ac.kr`) program-search operator list uses the same codes: 13 of its 16 top-level codes equal `DEPT_L_CD` here, and 67 of 99 child codes equal `DEPT_CD`. The unmatched ones are non-academic offices and centers (for example Innovation Education Office, Academic Information Center). The library does not use it: it lists program-operating organizations, not the academic structure.
- The public `www.sejong.ac.kr` "All departments" page (`academics.do`, server-rendered HTML) matched this list for 42 of 49 names; the rest differ in spelling or are listed under other names. It carries no codes.

Net: this timetable lookup is the only source found that has both **codes and a category dimension**, comes from the SIS that owns the user's department code, and needs no login.

## Design rules for implementations

These follow the existing result rules in [RESULT_SCHEMA.md](RESULT_SCHEMA.md).

1. Separate from `authenticate`: a failure here never affects an authentication result, and existing fields do not change.
2. Return the school's raw strings. No value lists, no code-to-name tables in the library, no guessing a category from a name.
3. Failure isolation: connection failure, a missing dataset key, `_SUBMIT_ERROR_` or an empty parse returns the last cached value or an empty list. Do not throw from the lookup for upstream shape problems.
4. Be gentle: cache in memory (24 hours by default, configurable), merge concurrent refreshes into one request, at most three requests per refresh, identifying User-Agent.
5. Fixtures are synthetic ([fixtures/reference_orgn.json](fixtures/reference_orgn.json), [fixtures/reference_dept.json](fixtures/reference_dept.json), [fixtures/reference_yearsmt.json](fixtures/reference_yearsmt.json)). Do not copy real responses into the repository.
6. Drift check: `scripts/probe-reference-data.py` calls the three endpoints and verifies dataset keys and required fields. It exits non-zero when the shape changes. It needs no credentials.

## Known limits

- Unofficial and unversioned: the school can change or remove these endpoints without notice.
- The `DEPT_CD` equality with the user's SIS department was checked for one undergraduate account only. Graduate students, students with a double major, and other enrollment statuses are not verified.
- The list describes departments **offering lectures** in a term (it feeds the timetable filter), not necessarily every administrative unit. Non-academic organizations (centers, offices) are not in it.
