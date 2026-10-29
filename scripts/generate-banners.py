#!/usr/bin/env python3
"""README·소개 사이트 배너 시안 생성: 6종 x 한국어/영어 = docs/assets/banner/banner-<시안>.<언어>.svg

  사용: python3 scripts/generate-banners.py
  문구는 COPY 한 곳에서만 고친다 (홍보 문구는 현재 지원하는 기능만, 이슈 #50)
"""

from pathlib import Path

OUT_DIR = Path(__file__).resolve().parent.parent / "docs" / "assets" / "banner"
WIDTH, HEIGHT = 1280, 320

# 브랜드 색 (README 배지와 동일 계열)
CRIMSON, CRIMSON_DEEP, SLATE, INK, PAPER = "#C20F2F", "#7A0A1F", "#324D60", "#1B2733", "#F7F4EF"
SANS = "'Apple SD Gothic Neo','Noto Sans KR','Malgun Gothic','Segoe UI',system-ui,sans-serif"
MONO = "ui-monospace,'SF Mono',Menlo,Consolas,monospace"

TITLE = "pro-sejongpeia"
COPY = {
    "ko": {
        "tagline": "세종대학교 통합 라이브러리",
        "tagline_lines": ["세종대학교 통합 라이브러리", "로그인과 학교 데이터를 한 번에"],
        "sub": "포털 로그인과 학교 공개 데이터를 Java, Python, Node.js로",
        "chips": ["Java", "Python", "Node.js"],
        "checks": ["레거시 TLS 암호 자동 처리", "SSO 쿠키와 리다이렉트 처리", "방화벽 User-Agent 대응",
                   "계정 잠금, 비밀번호 오류 즉시 판정", "DHC 표와 SIS JSON 파싱", "세 언어 동일 동작, 매일 실제 점검"],
        "flow": [("포털 로그인", "로그인 판정"), ("SSO", "도메인 간 쿠키"), ("DHC 와 SIS", "표와 JSON"), ("결과", "타입 있는 응답")],
        "sample": {"name": "홍길동", "major": "컴퓨터공학과", "status": "재학"},
        "hard_title": "직접 구현하면 어려운 것들",
    },
    "en": {
        "tagline": "The Sejong University integrated library",
        "tagline_lines": ["The Sejong University", "integrated library"],
        "sub": "Portal login and public school data for Java, Python, Node.js",
        "chips": ["Java", "Python", "Node.js"],
        "checks": ["Legacy TLS cipher handled", "SSO cookies and redirects", "Firewall-safe User-Agent",
                   "Instant lock and wrong-password detection", "DHC tables and SIS JSON parsed", "Same in 3 languages, checked daily"],
        "flow": [("Portal login", "verdict check"), ("SSO", "cross-domain cookies"), ("DHC and SIS", "tables and JSON"), ("Result", "typed response")],
        "sample": {"name": "Gildong Hong", "major": "Computer Science", "status": "Enrolled"},
        "hard_title": "What you would otherwise build",
    },
}


def svg_document(inner: str, defs: str = "") -> str:
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{WIDTH}" height="{HEIGHT}" viewBox="0 0 {WIDTH} {HEIGHT}" role="img" '
            f'aria-label="{TITLE}">\n<defs>{defs}</defs>\n{inner}\n</svg>\n')


def chips(labels, x, y, fill, stroke, text_color) -> str:
    parts, cursor = [], x
    for label in labels:
        chip_width = 28 + len(label) * 13
        parts.append(f'<rect x="{cursor}" y="{y}" width="{chip_width}" height="38" rx="19" fill="{fill}" stroke="{stroke}" stroke-width="1.5"/>'
                     f'<text x="{cursor + chip_width / 2}" y="{y + 25}" font-family="{MONO}" font-size="17" font-weight="700" '
                     f'fill="{text_color}" text-anchor="middle">{label}</text>')
        cursor += chip_width + 12
    return "".join(parts)


def banner_a(copy) -> str:
    """A 크림슨 클래식: 학교 색 그라데이션과 큰 제목"""
    defs = (f'<linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="{CRIMSON}"/>'
            f'<stop offset="1" stop-color="{CRIMSON_DEEP}"/></linearGradient>')
    rings = "".join(f'<circle cx="1090" cy="160" r="{r}" fill="none" stroke="#fff" stroke-opacity="{o}" stroke-width="2"/>'
                    for r, o in ((70, .35), (110, .25), (150, .16), (190, .1), (230, .06)))
    inner = (f'<rect width="{WIDTH}" height="{HEIGHT}" rx="20" fill="url(#g)"/>{rings}'
             f'<text x="64" y="128" font-family="{MONO}" font-size="68" font-weight="800" fill="#fff">{TITLE}</text>'
             f'<text x="66" y="178" font-family="{SANS}" font-size="30" font-weight="700" fill="#fff">{copy["tagline"]}</text>'
             f'<text x="66" y="214" font-family="{SANS}" font-size="20" fill="#fff" fill-opacity=".85">{copy["sub"]}</text>'
             + chips(copy["chips"], 64, 244, "none", "#fff", "#fff"))
    return svg_document(inner, defs)


def banner_b(copy) -> str:
    """B 터미널: 호출 한 줄과 결과 미리보기"""
    sample = copy["sample"]
    lines = [("$ authenticate(student_id, password)", "#E6EDF5"), ("{", "#8AA0B8"),
             (f'  "name": "{sample["name"]}",', "#9BD1A8"), (f'  "major": "{sample["major"]}",', "#9BD1A8"),
             (f'  "status": "{sample["status"]}"', "#9BD1A8"), ("}", "#8AA0B8")]
    code = "".join(f'<text x="722" y="{112 + i * 30}" font-family="{MONO}" font-size="20" fill="{color}" xml:space="preserve">{text}</text>'
                   for i, (text, color) in enumerate(lines))
    dots = "".join(f'<circle cx="{738 + i * 22}" cy="68" r="6" fill="{c}"/>' for i, c in enumerate(("#FF5F57", "#FEBC2E", "#28C840")))
    inner = (f'<rect width="{WIDTH}" height="{HEIGHT}" rx="20" fill="#0E1621"/>'
             f'<rect x="700" y="40" width="540" height="240" rx="16" fill="#131F30" stroke="#24344B"/>{dots}{code}'
             f'<text x="64" y="128" font-family="{MONO}" font-size="60" font-weight="800" fill="#fff">{TITLE}</text>'
             f'<text x="66" y="178" font-family="{SANS}" font-size="26" font-weight="700" fill="#E6EDF5">{copy["tagline_lines"][0]}</text>'
             f'<text x="66" y="212" font-family="{SANS}" font-size="26" font-weight="700" fill="#E6EDF5">{copy["tagline_lines"][1]}</text>'
             + chips(copy["chips"], 64, 244, "none", "#3B516B", "#9DB2C9")
             + f'<rect x="64" y="148" width="64" height="4" rx="2" fill="{CRIMSON}"/>')
    return svg_document(inner)


def banner_c(copy) -> str:
    """C 흐름도: 포털 로그인 > SSO > DHC·SIS > 결과"""
    nodes, arrows = [], []
    box_width, gap, start_x, y = 232, 62, 64, 150
    for i, (name, note) in enumerate(copy["flow"]):
        x = start_x + i * (box_width + gap)
        is_last = i == len(copy["flow"]) - 1
        fill, text_color, note_color = (CRIMSON, "#fff", "#FBD5DB") if is_last else ("#fff", INK, "#5C6F7F")
        nodes.append(f'<rect x="{x}" y="{y}" width="{box_width}" height="110" rx="16" fill="{fill}" stroke="{CRIMSON if not is_last else CRIMSON_DEEP}" stroke-width="2"/>'
                     f'<text x="{x + box_width / 2}" y="{y + 52}" font-family="{SANS}" font-size="26" font-weight="800" fill="{text_color}" text-anchor="middle">{name}</text>'
                     f'<text x="{x + box_width / 2}" y="{y + 84}" font-family="{SANS}" font-size="17" fill="{note_color}" text-anchor="middle">{note}</text>')
        if not is_last:
            ax = x + box_width + 8
            arrows.append(f'<path d="M{ax} {y + 55} H{ax + gap - 22}" stroke="{CRIMSON}" stroke-width="3" stroke-linecap="round"/>'
                          f'<path d="M{ax + gap - 30} {y + 45} L{ax + gap - 18} {y + 55} L{ax + gap - 30} {y + 65}" fill="none" stroke="{CRIMSON}" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>')
    inner = (f'<rect width="{WIDTH}" height="{HEIGHT}" rx="20" fill="{PAPER}"/>'
             f'<text x="64" y="78" font-family="{MONO}" font-size="44" font-weight="800" fill="{INK}">{TITLE}</text>'
             f'<text x="66" y="112" font-family="{SANS}" font-size="22" fill="{SLATE}">{copy["tagline"]}</text>'
             + "".join(arrows) + "".join(nodes))
    return svg_document(inner)


def banner_d(copy) -> str:
    """D 해결 목록: 직접 구현하면 어려운 부분을 체크 표시로"""
    rows = []
    for i, text in enumerate(copy["checks"]):
        y = 56 + i * 41
        rows.append(f'<circle cx="716" cy="{y}" r="13" fill="{CRIMSON}"/>'
                    f'<path d="M709.5 {y} L714.5 {y + 5} L723 {y - 5}" fill="none" stroke="#fff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>'
                    f'<text x="744" y="{y + 7}" font-family="{SANS}" font-size="22" font-weight="600" fill="{INK}">{text}</text>')
    inner = (f'<rect width="{WIDTH}" height="{HEIGHT}" rx="20" fill="#fff" stroke="#E3DED5" stroke-width="2"/>'
             f'<rect x="0" y="0" width="14" height="{HEIGHT}" rx="7" fill="{CRIMSON}"/>'
             f'<text x="64" y="110" font-family="{MONO}" font-size="52" font-weight="800" fill="{INK}">{TITLE}</text>'
             f'<text x="66" y="156" font-family="{SANS}" font-size="26" font-weight="700" fill="{CRIMSON}">{copy["tagline_lines"][0]}</text>'
             f'<text x="66" y="190" font-family="{SANS}" font-size="26" font-weight="700" fill="{CRIMSON}">{copy["tagline_lines"][1]}</text>'
             + chips(copy["chips"], 64, 226, "none", SLATE, SLATE)
             + f'<text x="716" y="30" font-family="{SANS}" font-size="15" font-weight="700" fill="#7C8B97" letter-spacing="1">{copy["hard_title"]}</text>'
             + "".join(rows))
    return svg_document(inner)


def banner_e(copy) -> str:
    """E 세 언어: Java, Python, Node.js 코드 조각 나란히"""
    cards = (("Java", "Spring Boot", "engine.authenticate(id, password)"),
             ("Python", "pip", "authenticate(id, password)"),
             ("Node.js", "npm", "await authenticate(id, password)"))
    parts = []
    for i, (language, installer, code) in enumerate(cards):
        x = 64 + i * 392
        parts.append(f'<rect x="{x}" y="150" width="368" height="118" rx="16" fill="#131F30" stroke="#24344B"/>'
                     f'<text x="{x + 24}" y="190" font-family="{SANS}" font-size="24" font-weight="800" fill="#fff">{language}</text>'
                     f'<text x="{x + 344}" y="190" font-family="{MONO}" font-size="15" fill="#7E93AB" text-anchor="end">{installer}</text>'
                     f'<rect x="{x + 24}" y="206" width="320" height="2" fill="{CRIMSON}"/>'
                     f'<text x="{x + 24}" y="242" font-family="{MONO}" font-size="16" fill="#9BD1A8">{code}</text>')
    inner = (f'<rect width="{WIDTH}" height="{HEIGHT}" rx="20" fill="#0E1621"/>'
             f'<text x="64" y="84" font-family="{MONO}" font-size="48" font-weight="800" fill="#fff">{TITLE}</text>'
             f'<text x="66" y="124" font-family="{SANS}" font-size="24" font-weight="600" fill="#C9D6E4">{copy["tagline"]}</text>'
             + "".join(parts))
    return svg_document(inner)


def banner_f(copy) -> str:
    """F 미니멀 라이트: 제목과 한 줄 설명만"""
    defs = '<pattern id="dots" width="28" height="28" patternUnits="userSpaceOnUse"><circle cx="2" cy="2" r="1.4" fill="#D9D3C7"/></pattern>'
    inner = (f'<rect width="{WIDTH}" height="{HEIGHT}" rx="20" fill="#FBFAF8"/><rect width="{WIDTH}" height="{HEIGHT}" rx="20" fill="url(#dots)"/>'
             f'<rect x="64" y="84" width="8" height="150" rx="4" fill="{CRIMSON}"/>'
             f'<text x="96" y="140" font-family="{MONO}" font-size="76" font-weight="800" fill="{INK}">{TITLE}</text>'
             f'<text x="98" y="190" font-family="{SANS}" font-size="28" font-weight="700" fill="{SLATE}">{copy["tagline"]}</text>'
             + chips(copy["chips"], 96, 226, "#fff", "#CFC8BA", SLATE))
    return svg_document(inner, defs)


BANNERS = {"a-crimson": banner_a, "b-terminal": banner_b, "c-flow": banner_c, "d-checklist": banner_d, "e-languages": banner_e, "f-minimal": banner_f}


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for name, render in BANNERS.items():
        for language, copy in COPY.items():
            path = OUT_DIR / f"banner-{name}.{language}.svg"
            path.write_text(render(copy), encoding="utf-8")
            print(path.relative_to(OUT_DIR.parent.parent.parent))


if __name__ == "__main__":
    main()
