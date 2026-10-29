# Survey: where does the school serve department / organization data?

Record of how [REFERENCE_DATA.md](REFERENCE_DATA.md) was found, what else was checked, what was a dead end, and what was **not** checked. Read this before searching again: it saves repeating the same pages.

Period: 2026-10-29 to 2026-10-30. Pages were read in a browser, read-only. No application, save or admin action was performed, and no personal data is kept in the repository.

## Question

The library returns the user's major (`major`) and, with #34, `user_category` and `parent_unit_name`. Callers also want to classify a user's college / department / category **without hard-coded name or code tables**, because colleges and departments are renamed, merged and recoded often. Wanted: a school-served source, preferably usable without login, with HTML parsing only as a last resort.

Working rule: use only what the school's own web screens already call, and never guess hidden parameters.

## Result in one line

The SIS lecture-timetable screen (`sjpt.sejong.ac.kr/app/ext/SueOpenTimeQ.jsp`) serves **organization categories, terms and department codes as JSON without login**, with the same codes the SIS returns for a user. See [REFERENCE_DATA.md](REFERENCE_DATA.md).

## Site by site

| Site | What was seen | Useful? |
|---|---|---|
| `sjpt` (SIS) timetable `/app/ext/SueOpenTimeQ.jsp` | Three JSON endpoints fill the category, term and department dropdowns. Works with no cookie. Reading only the top frame's text looked blank on the first attempts: the screen is inside an iframe (a screenshot shows it). Not a login requirement | **Yes: primary source** |
| `sjpt` (SIS) timetable lecture list | `POST /sch/sch/ext/SueOpenTimeQ/doList.do` returns `dl_main` with 33 fields per lecture (course number, section, name, course type, credits, time, room, instructor, ...). Works with no cookie. The screen requires at least one of department, course name, instructor, but the server does not: with all three empty it returns the whole term (2026-10-30: undergraduate 2026/2 2,786 rows, graduate 1,132 rows) | **Yes**, see issue #43 |
| `sjpt` (SIS) after login | Student menu tree has only personal screens (certificates, status change, grades, scholarship, awards) and a service tab (language institute, dormitory, student labor). No list of departments. The academic calendar (`SudBaseScheSelfServQ/doList.do`) works only inside a login session and needs the screen's `addParam` run parameters (a malformed request makes the server end the session). `SchStudentBaseInfo/doStudent.do` returns about 100 student-record fields, some of them sensitive. `initUserInfo.do` carries the user's own department code (`dm_UserInfoSch.DEPT_CD`, `dm_UserInfoGam.DEPT_NO`, equal and present in the timetable list) | Own code only |
| `sjpt` (SIS) `doGatheringOnload.do` | Category code table with only the logged-in user's own row | No |
| `www.sejong.ac.kr` | Main page calls only a chatbot widget. "All departments" (`/kor/college/academics.do`) and the undergraduate / graduate intro pages are fully server-rendered HTML: names and links, no codes, no data requests | Names only (HTML, last resort) |
| `portal.sejong.ac.kr` | Login, main page (`parent_unit_name` source, #34) | Already used |
| `classic.sejong.ac.kr` (DHC) | Reading-status HTML tables | Already used |
| `do.sejong.ac.kr` (두드림, non-curricular programs) | Program search "operating organization" dropdown: 17 top-level options are server-rendered; choosing one calls `POST /ko/process/eco/getSearchInstitution` (`iidx=<code>`, JSON `{"success":true,"lists":[{"value","display"}]}`). 16 college / office codes gave 99 child entries. 13 of 16 top-level codes equal `DEPT_L_CD`, 67 of 99 child codes equal `DEPT_CD`; the rest are offices, centers and project teams | Cross-check only: a program-operating-organization list, not the academic structure |
| `happydorm.sejong.ac.kr` (dormitory) | Public JSON: weekly menu (`POST /food/indexFoodList.do`, `/food/getWeeklyMenu.do`) and notice list (`POST /bbs/getBbsList.do`). Responses are JSON with a `text/html` content type. Works with no cookie (checked) | **Yes**, see issue #59 |
| `ipsi.sejong.ac.kr` (admissions) | No data JSON found. Notice boards are server-rendered HTML, admission guides and results are PDF or XLS attachments | HTML and PDF only (not used) |
| `library.sejong.ac.kr` | "Learning info by department" (`/webcontent/info/8`): HTML table of college and department. Its "University departments" menu links to `academics.do`. Book search and holdings are HTML; a few JSON calls exist (shelf position `/search/ajax/bookPosition`, adjacent call numbers, cover image). Seat status and study-room reservation need a login | HTML, partial JSON; shelf position needs an item number from HTML/XML, see Follow-up findings (#60) |
| `udream` (career) | ASP.NET page inside a frame. Support-center list and job filters; a "college" column in a table. No organization list | No |
| `safety`, `abeek` | First screens only: counters, popups and tokens (safety); menu items without department data requests (abeek) | No |
| `ecampus` (Moodle) | Nothing relevant | No |
| `graduate`, `gsi`, `mba`, `pub`, `tourgrad`, `edu`, `arts` (graduate schools, one CMS) | Public JSON on all seven: academic calendar (`GET /_custom/sejong/_app/calendar-graduate/proc/getCalendarData.jsp?year&month&siteId`, `items` is a JSON string; two sites checked) and faculty lists (`GET /professor/getProfessorList.do?menuCd&siteId&univerType`, with personal fields; `menuCd` comes from the page HTML). Course lists and department codes for the graduate schools come from the same SIS timetable API (category codes `30`, `31`, `33`, `34`, `3J`, `3K`: general graduate school returned 1,132 lecture rows for 2026/2). `gsi` has a curriculum HTML table, other schools publish course lists as PDF | Calendar: issue #61. Faculty: not adopted for now, see Follow-up findings (#62). Lectures and codes: #36, #43 |
| `sos` (one-stop service for international admissions) | Opened 2026-10-30, see Follow-up findings (#86). "Organization chart" is one static HTML table of the office's own staff (positions, names, duties). "Department intro" is a link out to server-rendered English pages on `en.sejong.ac.kr`. Only data request on the pages is a popup list | No (HTML, and staff names) |

## Follow-up findings (2026-10-30)

Read-only checks with the library transport (about 1 request per second) and one browser session. Items marked "from the issue" were recorded in the issue earlier and were not re-run here.

### Syllabus / course overview (#44): overview adopted, instructor syllabus not

Second pass 2026-10-30 (read-only, login-free; the screen was driven in a browser and the report calls were replayed once each with the library transport).

- The `교과목개요` and `수업계획서` link columns do not call a data endpoint. They open a report viewer:
  1. `POST /sch/sch/ext/SueOpenPlanR/doListPdfMake.do` (`수업계획서` only) with `psOrgnClsfCd`, `psYear`, `psSmtCd`, `psCuriNo`, `psDeptCd`, `psClass`, `psOpenDiv`. It returns one `dl_mainList` row with the PDF-build status (`OZ_PDF_MAKE_YN`, `OZ_PDF_MAKE_DT`, plus an employee number field). No syllabus text.
  2. `GET /cmn/sys/ozReport.do` (a shell page), then the OZ report server `oz.sejong.ac.kr/oz80`: `POST ozhviewer/ozhSvgViewer.jsp`, `POST server?newFrame` (JSON body with the report name and arguments) > `server?checkStatus` > `server?getPage`. The answer is an **SVG drawing** of the page.
  3. `교과목개요` is the report `/sch/sue/SueOpenPlanSummaryR.ozr` with arguments `psOrgnClsfCd`, `psYear`, `psSmtCd`, `psCuriNo`, `psDeptCd`, `psClass` (all from the lecture list row). It works without login or cookies, but it is only a drawing: text split into lines at the box width.
- `수업계획서` is a multi-page form that includes the instructor's name and office / contact lines. Personal data, so it must not be redistributed by default. Not implemented.
- **The overview text is also served as JSON.** The department curriculum popup of the main site (`GET www.sejong.ac.kr/curriculum/getCurriculumOneKor.do?curiNo=<CURI_NO>&deptNo=<DEPT_CD>`, found in that page's script `/_custom/sejong/_app/curriculum-kor/script.js`) returns a row whose `remark` is the course overview. For one course, `remark` was identical to the text of the OZ `교과목개요` report apart from the report's line breaks. It needs the header `Accept: application/json` (404 without it), no login, no `menuCd`.
- Result: adopted as `curriculum_courses(department_code, course_number)` (see [PUBLIC_DATA.md](PUBLIC_DATA.md)), `remark` > `overview`. Limits found: only courses in a degree department's curriculum come back (courses of the liberal-arts college department and the graduate department that were tried returned an empty `items`), and the overview is the curriculum's text for the current curriculum year, not a per-section, per-instructor text.
- The viewer script (`OZJSSVGViewer.js`) only knows the page formats `svg`, `pdf` and `mht`; no JSON or CSV path. Other report names and arguments, and the viewer's export button, were not tried.

### Library shelf position and cover (#60): not adopted (input only from HTML and XML)

- `GET /search/ajax/bookPosition?accession_no=<number>&code_div=<x>` works without login and returns JSON (`CALL_NO`, `LOCATION_NAME`, `POSITION_NAME`, `SUB_LOCATION`, `BOOKSHELF`, `TITLE`, `AUTHOR`, `PUBLISHER`, `IMG_PATH`). Three different `code_div` values (`CAT`, `1`, `0002`) gave identical output, so it does not change the result for the one item checked. `IMG_PATH` (the floor-plan image) was `null` for that item.
- `accession_no` is not obtainable from JSON. Search results are HTML. Per-item holdings come from `POST /search/prevLoc/<ctrl>` (body `loc=<code>`), which is **XML**, and `<ctrl>` and `loc` come from the HTML result page.
- `POST /openapi/thumbnail` (`isbn`, `sysdiv`, `ctrl`) returns JSON `smallUrl`, `largeUrl`, but the addresses point to a third-party shopping image host, not to a school resource.
- Conclusion: a clean API would need HTML search parsing plus XML parsing. Not adopted. `callNoBrowsing` and `recommender` were not re-run.

### Department curriculum (#67): one-course lookup adopted, list not

Second pass 2026-10-30.

- The curriculum **list** (`GET www.sejong.ac.kr/curriculum/getCurriculumListKor.do?menuCd&siteId&years&deptNo`) decides the department from `menuCd` and ignores `deptNo` (checked: the same `menuCd` with two different `deptNo` returned the same department; a blank `menuCd` is answered with `success: false`). `menuCd` exists only in page HTML (`"menuCd":"<number>"`, menu links `id="menu-<number>"`; about 1,000 menu entries on the site, no JSON menu tree found). Walking `menuCd` numbers was not done. Still no code-to-`menuCd` mapping, so the list is not adopted.
- The curriculum **single course** lookup `getCurriculumOneKor.do?curiNo&deptNo` needs no `menuCd`. Its `deptNo` is the SIS `DEPT_CD` (`3513` and `3517` returned the department names 컴퓨터공학과 and AI로봇학과, both codes are in the timetable department list), so `deptCd` of the curriculum JSON and `DEPT_CD` are the same code system for these departments. It returns the course's suggested year and semester, course type, credits and hours, and the course overview.
- Result: adopted as `curriculum_courses(department_code, course_number)`. A whole curriculum can be assembled by looking up each course of the department's lecture list (one request per course), but no list method is provided.
- Wider coverage of `deptCd` versus `DEPT_CD` was not measured (two departments).

### `sos` organization chart and department intro (#86): not adopted

Checked 2026-10-30 in a browser, read-only, with XHR capture.

- `sos.sejong.ac.kr` is the international admissions office site. Its "organization chart" (`/kor/comm/menu/<id>/content/index.do`) is a static HTML content page: one table of that office's staff (position, name, duties). No data request on the page except a popup list. It is not the university's organization structure, and every row is a named person.
- Its "department intro" links to `en.sejong.ac.kr/eng/academics/Undergraduate_Academic_Departments.do` (and a graduate counterpart): server-rendered English pages listing colleges and departments by name. Zero XHR / fetch calls, no codes.
- The university-wide chart on the main site (`www.sejong.ac.kr/kor/intro/organization.do`) is also server-rendered HTML (unit names, no codes), and its only XHR calls are a search widget and a chatbot.
- Conclusion: nothing usable as JSON. The first is personal data, the others are HTML without codes. The existing timetable department list (#36) stays the code-bearing source.

### Government standard data vs school department codes (#87): not adopted

Checked 2026-10-30. Measured against the 2026/2 undergraduate department list (103 rows, 96 distinct names).

- Dataset: "전국대학별학과정보표준데이터" on the public data portal (`data.go.kr/data/15107737`), provider 한국대학교육협의회, ministry 교육부, update cycle "as needed", last change shown 2026-03-19. Fields per row include school name, degree course, department name, college name (`단과대학명`), status (existing / changed / new / closed), a 12-character department code (`G` plus digits), a standard classification code, admission quota, graduate count, main subjects, related jobs.
- Form: file downloads (XLS, XML, JSON, RDF, CSV), an OpenAPI that needs a portal service key, and a search grid that answers with a server-rendered HTML table (a form POST, 499 rows on its first page for the school's name). The portal says the grid is capped at 50,000 rows and a key-based API is meant for full data. Whether the files need a portal login was not checked. No login-free JSON was found.
- Correspondence with the school's `DEPT_CD`: **none by code**. The standard data carries its own `G...` code and has no school code. By name, after removing spaces and middle dots, 55 of 96 school department names (57 percent) are found among the dataset's 99 current rows for the main school, and 55 of 70 distinct current dataset names are found in the school list. The college column has 11 distinct values among the 99 current rows, and 10 rows say "no college division".
- The dataset also lists 207 closed departments, graduate schools as separate "schools" (11 names), and reflects the 2025 disclosure year. It is a different slicing of the same facts, not a key into the school's systems.
- 대학알리미 (`academyinfo.go.kr`): the mobile site shows a few JSON calls on its main page (board and indicator lists). Department-level disclosure items were not traced; they are the same disclosure family as the standard data and use the association's own codes. Not checked in depth.
- Conclusion: no code mapping, name-only matching at 57 percent, a service key is needed for the official API. Not implemented; the issue's own rule ("weak correspondence: do not implement") applies.

### Graduate school faculty lists (#62): not adopted for now

- From the issue (not re-run): `GET /professor/getProfessorList.do?menuCd&siteId&univerType` returns about 29 fields per person (name, contact, career, research field) and needs a per-department `menuCd` that exists only in page HTML.
- Two independent blockers: (1) personal data on every row, so the library would redistribute staff names and contacts as its default output; (2) `menuCd` only in HTML.
- Safe-subset option (no names, contacts: only department, research field, degree) does not work, because rows without the name cannot be told apart and the department mapping is HTML anyway.
- Recommendation: do not implement. Decision rests with the maintainer.

### Personal academic records (#46): feasibility conclusion

- Feasible technically: the SIS screens for grades, scholarship history, awards and discipline, and student base info are JSON calls with a login session plus the screen's `addParam` run parameters (a malformed request ends the session).
- Risk is the problem, not feasibility: grades, GPA, scholarship amounts and account fields, high school, entrance exam scores, address, phone. A wrong parameter can end the user's session; repeated errors may lock the account.
- If the maintainer decides to go: opt-in per call only (never in the default login result), own record only, read-only screens only, sensitive fields dropped by an allow-list (not a deny-list), nothing logged, and the calling app owns the privacy duty. Start with the least sensitive screen and verify session safety first.
- Nothing was implemented or called for this issue.

### Coverage summary (#47)

| Group | Covered by the library | Not covered, reason |
|---|---|---|
| Organization, terms, departments, lectures | SIS timetable JSON (#36, #43) | the instructor syllabus holds staff contacts and is a rendered report (#44) |
| Calendar, campus map, cafeteria, extracurricular events | JSON sources (#45, #65, #66, #69, #102) | cafeteria: no list endpoint, the list is built from menu answers for numbers 1 to 10; one card (`빅베어레스토랑`) has no menu number |
| Dormitory menu and notices | JSON (#59) | none |
| Graduate school calendar | JSON (#61) | none |
| Library | none | search is HTML, holdings XML (#60) |
| Faculty | none | personal data and HTML-only menu number (#62) |
| Department curriculum | single course by department code (#67), includes the course overview (#44) | whole-curriculum list needs a menu number found only in HTML |
| Notices (university, admissions, rnd, safety, volunteer, career), program lists, classic books | none | HTML only |
| Seats, study rooms, personal records | none | login required (#46 for personal records) |

Still unchecked after this pass: non-student accounts (see below).

## Not checked

State plainly so nobody assumes coverage that does not exist:

- Individual department homepages (domain pattern not identified).
- `sjgw`, `mail` (login only). `en`, `cn`, `iacf`, `rnd`, `volunteer`, `pr`, `convention`, `gallery` were skimmed: no data JSON worth using (HTML lists, or JSON with personal fields).
- A Blackboard site that was mentioned: the only LMS found was `ecampus` (Moodle); no Blackboard address was identified.
- 대학알리미 department-level disclosure data: only its main page was looked at (see #87).
- Non-student accounts (graduate students, staff, other enrollment statuses) in the SIS: only one undergraduate account was used.

## How complete is the timetable list?

Measured on 2026-10-30 (undergraduate category, 2026 second semester: 103 rows).

| Comparison | Result |
|---|---|
| Public `academics.do` names (49 extracted) vs timetable list | 42 present; 7 not (different spelling, or listed elsewhere) |
| 두드림 child codes (99) vs `DEPT_CD` | 67 equal; unmatched are mostly non-academic offices and centers, plus a few sub-majors |
| 두드림 top-level codes (16) vs `DEPT_L_CD` | 13 equal; unmatched: Innovation Education Office, Major Design Support Center, Academic Information Center |
| Term dependence | Rows by term: 71 (2000/1), 73 (2010/1), 79 (2020/1), 101 (2025/2), 103 (2026/2) |
| Categories with departments for 2026/2 | 8 of 22 (`20`: 103, `30`: 59, `31`: 7, `33`: 8, `3J`: 5, `39`: 4, `3K`: 4, `34`: 1); the other 14 return 0 (closed or merged schools, or not offered this term) |

Conclusion: the timetable list is the most authoritative source found for **teaching departments with codes and a category dimension**. It is not a complete list of every administrative unit, and name spelling differs from the public pages. No single source covers everything; do not describe the result as "all organizations".

## Dead ends worth remembering

- Public pages with a department list (`academics.do`, library table) are HTML without codes.
- 두드림 has codes, but it lists program-operating organizations (offices, centers, project teams mixed in).
- `doGatheringOnload.do` returns only the caller's own row.
- The first timetable load looked empty: read the frame, wait for it to load, or check the three endpoints directly.

## Portal login notes for contributors

- The login page has a "keyboard security" option that swallows automated typing and submits empty fields. Turn it off before typing (checkbox `chkNos`), then type into `#id` and `#password`.
- Wrong passwords count toward the lock: never retry blindly.
- The SIS home shows a resolution notice popup that covers the page until confirmed.

## Re-verify

```
python3 scripts/probe-reference-data.py            # live: three requests, no login
python3 scripts/probe-reference-data.py --offline  # fixture self-check
```

By hand: open the timetable URL above, open the browser's Network tab, reload, and look for `doOnload.do`, `doYearsmtlist.do`, `doLesndeptgroup.do`.
