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
| `sjpt` (SIS) after login | Student menu tree has only personal screens (certificates, status change, grades, scholarship, awards) and a service tab (language institute, dormitory, student labor). No list of departments. `initUserInfo.do` carries the user's own department code (`dm_UserInfoSch.DEPT_CD`, `dm_UserInfoGam.DEPT_NO`, equal and present in the timetable list) | Own code only |
| `sjpt` (SIS) `doGatheringOnload.do` | Category code table with only the logged-in user's own row | No |
| `www.sejong.ac.kr` | Main page calls only a chatbot widget. "All departments" (`/kor/college/academics.do`) and the undergraduate / graduate intro pages are fully server-rendered HTML: names and links, no codes, no data requests | Names only (HTML, last resort) |
| `portal.sejong.ac.kr` | Login, main page (`parent_unit_name` source, #34) | Already used |
| `classic.sejong.ac.kr` (DHC) | Reading-status HTML tables | Already used |
| `do.sejong.ac.kr` (두드림, non-curricular programs) | Program search "operating organization" dropdown: 17 top-level options are server-rendered; choosing one calls `POST /ko/process/eco/getSearchInstitution` (`iidx=<code>`, JSON `{"success":true,"lists":[{"value","display"}]}`). 16 college / office codes gave 99 child entries. 13 of 16 top-level codes equal `DEPT_L_CD`, 67 of 99 child codes equal `DEPT_CD`; the rest are offices, centers and project teams | Cross-check only: a program-operating-organization list, not the academic structure |
| `library.sejong.ac.kr` | "Learning info by department" (`/webcontent/info/8`): HTML table of college and department. Its "University departments" menu links to `academics.do` | HTML only |
| `udream` (career) | ASP.NET page inside a frame. Support-center list and job filters; a "college" column in a table. No organization list | No |
| `safety`, `abeek` | First screens only: counters, popups and tokens (safety); menu items without department data requests (abeek) | No |
| `ecampus` (Moodle) | Nothing relevant | No |
| `graduate` (general graduate school) | Viewed early on; no department code source was found (details were not recorded, so a second look is cheap insurance) | No |
| `sos` (one-stop service) | Menus "organization chart" and "department intro" exist; **not opened** | Unknown |

## Not checked

State plainly so nobody assumes coverage that does not exist:

- Graduate-school sites `gsi`, `mba`, `pub`, `tourgrad`, `edu`, `arts`.
- `sos` organization chart and department intro pages.
- Individual department homepages (domain pattern not identified).
- `en`, `cn`, `iacf`, `rnd`, `sjgw`, `mail`, `volunteer`, `pr`, `ipsi`, `convention`, `gallery`, `happydorm`.
- A Blackboard site that was mentioned: the only LMS found was `ecampus` (Moodle); no Blackboard address was identified.
- Government open data ("standard data of national universities", University Alimi): not checked, mapping to school codes unknown.
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
