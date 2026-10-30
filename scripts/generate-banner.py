#!/usr/bin/env python3
"""README 배너·공유(og) 이미지 생성: scripts/banner/banner.html > 헤드리스 Chrome 렌더링 > PNG (2x, 용량 줄임)

  사용: python3 scripts/generate-banner.py            # 시안 3종 x 한국어/영어 + og.png
        python3 scripts/generate-banner.py --only a   # 시안 하나만
  필요: Chrome 계열 브라우저 (CHROME_PATH 로 지정 가능), 인터넷 (웹 폰트), Pillow (없으면 용량을 줄이지 못함)
  출력: docs/assets/banner/banner-<시안>.<언어>.png, docs/assets/og.png (공유 이미지 단일 원본, 사이트로는 빌드 때 복사)
  문구·수치·디자인은 banner.html 한 곳에서만 고친다
"""

import argparse
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

REPOSITORY_ROOT = Path(__file__).resolve().parent.parent
BANNER_HTML_PATH = REPOSITORY_ROOT / "scripts" / "banner" / "banner.html"
OUTPUT_DIRECTORY = REPOSITORY_ROOT / "docs" / "assets" / "banner"
OG_IMAGE_PATH = REPOSITORY_ROOT / "docs" / "assets" / "og.png"

VARIANTS = ("a", "b", "c")
LANGUAGES = ("ko", "en")
# README 폭 880px 기준 배너 880x340, 공유 이미지는 880x462 를 1200x630 으로 확대
BANNER_SIZE = (880, 340)
BANNER_SCALE = 2
OG_SIZE = (880, 462)
OG_SCALE = 1200 / 880
# README 에 올리는 시안과 og 에 쓰는 시안 (이슈 #126: A 어두운 코드 창 시안)
OG_VARIANT, OG_LANGUAGE = "a", "ko"
# 한 장 최대 용량 (바이트)
MAX_IMAGE_BYTES = 250_000
# 색 수를 줄여 용량을 낮추는 팔레트 크기 (큰 값부터 시도)
PALETTE_SIZES = (256, 192, 128, 96, 64)
# 색을 그대로 지킬 평평한 면 색 (기준별 개수, 최소 픽셀 수)
PINNED_COLOR_COUNT, PINNED_COLOR_MIN_PIXELS = 16, 200
# 폰트 로딩 대기 (가상 시간, 밀리초)
FONT_WAIT_MILLISECONDS = 8000


def find_chrome_executable():
    """CHROME_PATH > macOS 기본 위치 > PATH 의 chrome 계열 순으로 찾기"""
    candidates = [os.environ.get("CHROME_PATH", ""), "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
                  "/Applications/Chromium.app/Contents/MacOS/Chromium"]
    candidates += [shutil.which(name) or "" for name in ("google-chrome", "google-chrome-stable", "chromium", "chromium-browser", "chrome")]
    for candidate in candidates:
        if candidate and os.path.exists(candidate):
            return candidate
    sys.exit("Chrome 계열 브라우저를 찾지 못했습니다. CHROME_PATH 로 실행 파일 경로를 지정하세요")


def render_png(chrome_path, variant, language, layout, size, scale, output_path):
    """banner.html 을 주소 인자로 열어 화면 그대로 PNG 로 저장 (렌더링 로그는 버림)"""
    url = f"{BANNER_HTML_PATH.as_uri()}?variant={variant}&lang={language}&layout={layout}"
    command = [chrome_path, "--headless=new", "--disable-gpu", "--hide-scrollbars", f"--force-device-scale-factor={scale}",
               f"--window-size={size[0]},{size[1]}", f"--virtual-time-budget={FONT_WAIT_MILLISECONDS}",
               f"--screenshot={output_path}", url]
    subprocess.run(command, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=120)


def pin_flat_colors(original, palette_image):
    """평평한 면(글자, 로고 칸, 칩)의 색을 팔레트에 원래 값으로 심음 (디더링 전에 해야 면이 얼룩지지 않음)"""
    palette = palette_image.getpalette()
    palette_size = len(palette) // 3
    pinned_indexes = set()
    color_counts = original.getcolors(maxcolors=original.width * original.height) or []
    flat_colors = [(count, color) for count, color in color_counts if count >= PINNED_COLOR_MIN_PIXELS]
    # 면적이 큰 색 + 채도가 높은 색 (어두운 배경에서 적은 면적의 브랜드 색이 묻히는 것 방지)
    by_area = sorted(flat_colors, reverse=True)[:PINNED_COLOR_COUNT]
    by_chroma = sorted(flat_colors, key=lambda entry: max(entry[1]) - min(entry[1]), reverse=True)[:PINNED_COLOR_COUNT]
    for count, color in dict.fromkeys(by_area + by_chroma):
        # 아직 심지 않은 칸 중 가장 가까운 색의 칸을 덮어씀
        nearest_index = min((index for index in range(palette_size) if index not in pinned_indexes),
                            key=lambda index: sum((palette[index * 3 + channel] - color[channel]) ** 2 for channel in range(3)))
        palette[nearest_index * 3:nearest_index * 3 + 3] = list(color)
        pinned_indexes.add(nearest_index)
    palette_image.putpalette(palette)


def shrink_png(path):
    """원본이 제한을 넘으면 팔레트 색 수를 줄여 제한 안으로 (디더링으로 그라데이션 띠 완화). 반환: 최종 바이트 수"""
    try:
        from PIL import Image
    except ImportError:
        print("  Pillow 가 없어 용량을 줄이지 못했습니다 (pip install pillow)")
        return path.stat().st_size
    if path.stat().st_size <= MAX_IMAGE_BYTES:
        return path.stat().st_size
    image = Image.open(path).convert("RGB")
    for palette_size in PALETTE_SIZES:
        palette_image = image.quantize(colors=palette_size, method=Image.Quantize.MEDIANCUT)
        pin_flat_colors(image, palette_image)
        dithered = image.quantize(palette=palette_image, dither=Image.Dither.FLOYDSTEINBERG)
        dithered.save(path, optimize=True)
        if path.stat().st_size <= MAX_IMAGE_BYTES:
            break
    return path.stat().st_size


def build(variants, is_og_included):
    chrome_path = find_chrome_executable()
    OUTPUT_DIRECTORY.mkdir(parents=True, exist_ok=True)
    for variant in variants:
        for language in LANGUAGES:
            output_path = OUTPUT_DIRECTORY / f"banner-{variant}.{language}.png"
            render_png(chrome_path, variant, language, "banner", BANNER_SIZE, BANNER_SCALE, output_path)
            print(f"{output_path.relative_to(REPOSITORY_ROOT)} {shrink_png(output_path):,} bytes")
    if is_og_included:
        with tempfile.TemporaryDirectory() as temporary_directory:
            temporary_path = Path(temporary_directory) / "og.png"
            render_png(chrome_path, OG_VARIANT, OG_LANGUAGE, "og", OG_SIZE, OG_SCALE, temporary_path)
            size_bytes = shrink_png(temporary_path)
            shutil.copyfile(temporary_path, OG_IMAGE_PATH)
        print(f"{OG_IMAGE_PATH.relative_to(REPOSITORY_ROOT)} {size_bytes:,} bytes")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--only", choices=VARIANTS, help="시안 하나만 생성 (og.png 은 건드리지 않음)")
    arguments = parser.parse_args()
    build((arguments.only,) if arguments.only else VARIANTS, is_og_included=arguments.only is None)
