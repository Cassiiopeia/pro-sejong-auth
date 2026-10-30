#!/usr/bin/env python3
"""README 데모 화면 원본 생성: 로그인 없는 공개 데이터를 실제로 호출해 터미널 모양 HTML 1장으로 저장

  사용: python3 scripts/generate-readme-demo.py <출력 html 경로>
  흐름: 실제 학교 서버 호출 > 결과 문자열 정리 > HTML 저장 (PNG 변환은 브라우저 도구로 별도 수행)
  개인정보 없음: 교수명 등 사람 정보는 출력하지 않음
"""

import html
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "python" / "src"))
from sejongpeia import SejongPublicData  # noqa: E402

# 데모 조회 조건: 학부(20), 2026학년도 2학기(20), 소프트웨어학과(3220)
CATEGORY_CODE, YEAR, SEMESTER_CODE, DEPARTMENT_CODE = "20", "2026", "20", "3220"
CALENDAR_START, CALENDAR_END = "2026-11-01", "2026-12-31"
LECTURE_ROW_COUNT, CALENDAR_ROW_COUNT = 3, 3


def collect_demo_lines():
    """코드 줄(>>>)과 실제 출력 줄을 순서대로 [(종류, 문자열)]로 반환"""
    public_data = SejongPublicData()
    started_at = time.time()
    departments = public_data.departments(CATEGORY_CODE, YEAR, SEMESTER_CODE)
    lectures = public_data.lectures(CATEGORY_CODE, YEAR, SEMESTER_CODE, department_code=DEPARTMENT_CODE)
    events = public_data.calendar(CALENDAR_START, CALENDAR_END)
    elapsed_seconds = time.time() - started_at

    lines = [
        ("code", "from sejongpeia import SejongPublicData"),
        ("code", "public_data = SejongPublicData()   # 학번·비밀번호 없이"),
        ("blank", ""),
        ("code", f'departments = public_data.departments("{CATEGORY_CODE}", "{YEAR}", "{SEMESTER_CODE}")'),
        ("code", "len(departments)"),
        ("out", str(len(departments))),
        ("blank", ""),
        ("code", f'lectures = public_data.lectures("{CATEGORY_CODE}", "{YEAR}", "{SEMESTER_CODE}", department_code="{DEPARTMENT_CODE}")'),
        ("code", f"for lecture in lectures[:{LECTURE_ROW_COUNT}]:"),
        ("code2", "print(lecture.course_number, lecture.name, lecture.schedule, lecture.room)"),
    ]
    lines += [("out", f"{row.course_number} {row.name} {row.schedule} {row.room}") for row in lectures[:LECTURE_ROW_COUNT]]
    lines += [
        ("blank", ""),
        ("code", f'events = public_data.calendar("{CALENDAR_START}", "{CALENDAR_END}")'),
        ("code", f"for event in events[:{CALENDAR_ROW_COUNT}]:"),
        ("code2", "print(event.start_date, event.end_date, event.title)"),
    ]
    lines += [("out", f"{row.start_date} {row.end_date} {row.title}") for row in events[:CALENDAR_ROW_COUNT]]
    return lines, elapsed_seconds, len(lectures), len(events)


def render_html(lines, elapsed_seconds, lecture_count, event_count):
    body = []
    for kind, text in lines:
        escaped = html.escape(text)
        if kind == "code":
            body.append(f'<div><span class="prompt">&gt;&gt;&gt;</span> {escaped}</div>')
        elif kind == "code2":
            body.append(f'<div><span class="prompt">...</span>     {escaped}</div>')
        elif kind == "out":
            body.append(f'<div class="out">{escaped}</div>')
        else:
            body.append("<div>&nbsp;</div>")
    # 날짜·개수는 시간이 지나면 어긋나므로 적지 않음
    footer = "로그인 없이 호출한 예시 출력"
    return f"""<!doctype html><html lang="ko"><head><meta charset="utf-8"><style>
body {{ margin:0; padding:28px; background:#EFE9E1; font-family:'Apple SD Gothic Neo','Noto Sans KR',sans-serif; }}
.window {{ width:880px; border-radius:12px; overflow:hidden; background:#1B2733; box-shadow:0 10px 30px rgba(0,0,0,.25); }}
.bar {{ display:flex; align-items:center; gap:8px; padding:12px 16px; background:#0F1923; color:#9FB1BF; font-size:13px; }}
.dot {{ width:12px; height:12px; border-radius:50%; }}
pre {{ margin:0; padding:20px 24px 18px; font:15px/1.65 ui-monospace,'SF Mono',Menlo,'Apple SD Gothic Neo',monospace; color:#E8EEF2; }}
.prompt {{ color:#E8556D; font-weight:700; }}
.out {{ color:#8FD3A8; }}
.foot {{ padding:10px 24px 14px; color:#9FB1BF; font-size:12.5px; border-top:1px solid #2A3947; }}
</style></head><body><div class="window">
<div class="bar"><span class="dot" style="background:#FF5F57"></span><span class="dot" style="background:#FEBC2E"></span><span class="dot" style="background:#28C840"></span>
<span style="margin-left:10px">python  ·  pip install pro-sejongpeia</span></div>
<pre>{''.join(body)}</pre>
<div class="foot">{html.escape(footer)}</div></div></body></html>"""


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    lines, elapsed_seconds, lecture_count, event_count = collect_demo_lines()
    Path(sys.argv[1]).write_text(render_html(lines, elapsed_seconds, lecture_count, event_count), encoding="utf-8")


if __name__ == "__main__":
    main()
