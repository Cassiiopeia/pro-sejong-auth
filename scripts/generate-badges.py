"""레지스트리 배지 SVG 생성: 외부 배지 서비스를 GitHub 이미지 프록시가 4초 안에 못 받아 깨지므로 저장소에 직접 둔다
라벨이 바뀔 때만 실행 (python3 scripts/generate-badges.py), 결과는 docs/assets/badges/*.svg"""

import sys
import urllib.request
from pathlib import Path

OUTPUT_DIR = Path(__file__).resolve().parent.parent / "docs" / "assets" / "badges"
# 파일 이름 > 배지 주소 (정적 배지라 값이 바뀌지 않음, 버전은 README 상단의 자동 갱신 줄이 알려 줌)
BADGE_URL_BY_FILE_NAME = {
    "maven-central.svg": "https://img.shields.io/badge/Maven%20Central-kr.suhsaechan%3Asejongpeia-C20F2F?logo=apachemaven&logoColor=white",
    "pypi.svg": "https://img.shields.io/badge/PyPI-pro--sejongpeia-324D60?logo=pypi&logoColor=white",
    "npm.svg": "https://img.shields.io/badge/npm-pro--sejongpeia-324D60?logo=npm&logoColor=white",
}


def main() -> int:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    for file_name, url in BADGE_URL_BY_FILE_NAME.items():
        # 기본 파이썬 User-Agent 는 차단되므로 식별 가능한 값을 보냄
        request = urllib.request.Request(url, headers={"User-Agent": "pro-sejongpeia-badge-generator"})
        with urllib.request.urlopen(request, timeout=30) as response:
            svg = response.read().decode("utf-8")
        if not svg.lstrip().startswith("<svg"):
            print(f"[generate-badges] unexpected response for {file_name}", file=sys.stderr)
            return 1
        (OUTPUT_DIR / file_name).write_text(svg, encoding="utf-8")
        print(f"[generate-badges] wrote {file_name} ({len(svg)} bytes)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
