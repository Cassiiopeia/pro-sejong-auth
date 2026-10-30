#!/usr/bin/env python3
"""
사용자용 문서의 고정값 점검: 관리되지 않는 날짜·프로젝트 버전이 본문에 박혀 있으면 실패

  규칙 2개 (코드 블록 안은 호출 인자·응답 예시라서 제외)
    1. ISO 날짜 (YYYY-MM-DD): "2026-10-30 확인" 같은 확인 날짜는 곧 낡은 정보가 됨
    2. 이 프로젝트의 버전 (1.2.N, 그리고 version.yml 의 현재 버전): 최신 버전은 사이트가 version.yml 에서 읽어 보여 줌
  점검 대상: README 계열, 문서 사이트 본문·컴포넌트·문구 (spec/ 과 조사 기록은 검증 기록이라 대상 아님)
  예외: 아래 ALLOWED_VALUES 에만 이유와 함께 둠
"""

import fnmatch
import re
import subprocess
import sys
from pathlib import Path

REPOSITORY_ROOT = Path(__file__).resolve().parent.parent
VERSION_FILE_PATH = REPOSITORY_ROOT / "version.yml"

# 점검 대상 (git 추적 파일 기준 경로 패턴)
TARGET_PATTERNS = (
    "README.md", "README.en.md", "python/README.md", "node/README.md",
    "website/src/content/docs/*", "website/src/components/*", "website/src/i18n/*",
)
CHECKED_EXTENSIONS = (".md", ".mdx", ".astro", ".ts")

# 코드 블록 시작·끝 줄 (``` 또는 ~~~, 들여쓰기 허용)
CODE_FENCE_PATTERN = re.compile(r"^\s*(```|~~~)")
ISO_DATE_PATTERN = re.compile(r"\b20\d{2}-\d{2}-\d{2}\b")
# 1.2.N 은 이 프로젝트가 1.2 계열인 동안의 고정 패턴, 현재 버전은 version.yml 에서 읽음
PROJECT_VERSION_PATTERN_TEXT = r"(?<![\w.])1\.2\.\d+(?![\w.])"
CURRENT_VERSION_PATTERN = re.compile(r'^version:\s*"?([0-9]+(?:\.[0-9]+)+)"?', re.MULTILINE)

# 예외 (경로 패턴, 줄에 포함된 글자, 이유): 새로 추가할 때는 이유를 반드시 적는다
ALLOWED_VALUES = (
    ("README.md", "최신 버전 :", "README 버전 워크플로우(PROJECT-COMMON-README-VERSION-UPDATE)가 이 줄을 배포마다 갱신함"),
    ("README.en.md", "Latest version", "README 버전 워크플로우가 이 줄을 배포마다 갱신함"),
)


def read_current_version():
    matched = CURRENT_VERSION_PATTERN.search(VERSION_FILE_PATH.read_text(encoding="utf-8"))
    return matched.group(1) if matched else ""


def build_version_pattern(current_version):
    escaped = re.escape(current_version) if current_version else None
    texts = [PROJECT_VERSION_PATTERN_TEXT] + ([rf"(?<![\w.]){escaped}(?![\w.])"] if escaped else [])
    return re.compile("|".join(texts))


def list_target_files():
    # -z: 한글 경로를 따옴표·8진수로 감싸지 않게
    output = subprocess.run(["git", "ls-files", "-z"], cwd=REPOSITORY_ROOT, capture_output=True, text=True, check=True).stdout
    return [path for path in output.split("\0")
            if path.endswith(CHECKED_EXTENSIONS) and any(fnmatch.fnmatch(path, pattern) for pattern in TARGET_PATTERNS)]


def is_allowed(path, line):
    return any(fnmatch.fnmatch(path, path_pattern) and allowed_text in line for path_pattern, allowed_text, _ in ALLOWED_VALUES)


def find_fixed_values(path, version_pattern):
    """코드 블록 밖의 줄에서 날짜·프로젝트 버전 찾기 > (줄 번호, 종류, 값, 줄 내용)"""
    violations = []
    is_inside_code_block = False
    is_markdown_like = path.endswith((".md", ".mdx"))
    for line_number, line in enumerate((REPOSITORY_ROOT / path).read_text(encoding="utf-8").splitlines(), start=1):
        if is_markdown_like and CODE_FENCE_PATTERN.match(line):
            is_inside_code_block = not is_inside_code_block
            continue
        if is_inside_code_block or is_allowed(path, line):
            continue
        for kind, pattern in (("date", ISO_DATE_PATTERN), ("version", version_pattern)):
            for matched in pattern.finditer(line):
                violations.append((line_number, kind, matched.group(0), line.strip()))
    return violations


def main():
    version_pattern = build_version_pattern(read_current_version())
    problem_count = 0
    for path in list_target_files():
        for line_number, kind, value, line in find_fixed_values(path, version_pattern):
            print(f"{path}:{line_number}: fixed {kind} `{value}` : {line[:120]}")
            problem_count += 1
    if problem_count:
        print(f"\n{problem_count} fixed value(s) found. Remove dates and project versions from user-facing docs "
              "(the site reads the latest version from version.yml), or add a reasoned entry to ALLOWED_VALUES.")
        return 1
    print("No fixed dates or project versions in user-facing docs.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
