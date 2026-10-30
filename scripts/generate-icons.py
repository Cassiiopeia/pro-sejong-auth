"""기능 아이콘 SVG 생성: Lucide(ISC) 선 아이콘을 받아 세종 크림슨으로 칠해 docs/assets/icons 에 저장
README 와 사이트가 같은 파일을 쓰므로 이모지 대신 이 아이콘을 쓴다. 아이콘을 바꿀 때만 실행 (python3 scripts/generate-icons.py)"""

import sys
import time
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUTPUT_DIR = ROOT / "docs" / "assets" / "icons"
LUCIDE_BASE_URL = "https://raw.githubusercontent.com/lucide-icons/lucide/main/"
CRIMSON = "#C20F2F"  # 사이트 토큰 --sejong-crimson 과 같은 값
# 파일 이름(기능) > Lucide 아이콘 이름
LUCIDE_ICON_BY_FILE_NAME = {
    "verify": "shield-check",
    "codes": "school",
    "lectures": "book-open",
    "curriculum": "graduation-cap",
    "calendar": "calendar-days",
    "dormitory": "bed-double",
    "cafeteria": "utensils",
    "campus": "map-pin",
    "events": "ticket",
    "fast": "zap",
    "efficient": "layers",
    "safe": "lock",
    "reliable": "badge-check",
}


def fetch(url: str) -> str:
    request = urllib.request.Request(url, headers={"User-Agent": "pro-sejongpeia-icon-generator"})
    for attempt in range(3):
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                return response.read().decode("utf-8")
        except OSError:
            time.sleep(2 * (attempt + 1))
    raise SystemExit(f"[generate-icons] failed to fetch {url}")


def main() -> int:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    for file_name, lucide_name in LUCIDE_ICON_BY_FILE_NAME.items():
        svg = fetch(f"{LUCIDE_BASE_URL}icons/{lucide_name}.svg")
        if not svg.lstrip().startswith("<svg"):
            print(f"[generate-icons] unexpected response for {lucide_name}", file=sys.stderr)
            return 1
        (OUTPUT_DIR / f"{file_name}.svg").write_text(svg.replace("currentColor", CRIMSON), encoding="utf-8")
        print(f"[generate-icons] wrote {file_name}.svg ({lucide_name})")
    (OUTPUT_DIR / "LICENSE-lucide.txt").write_text(fetch(f"{LUCIDE_BASE_URL}LICENSE"), encoding="utf-8")
    return 0


if __name__ == "__main__":
    sys.exit(main())
