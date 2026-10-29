#!/usr/bin/env python3
"""
문서 점검: 상대 링크 존재, 언어별 문서 쌍의 전환 링크
규칙: 기본(한국어) `X.md` + 번역 `X.<언어코드>.md` (예: README.en.md, README.ja.md)
"""
import os
import re
import subprocess
import sys

# 점검 제외: 작업 기록·도구 설정·자동 생성 문서
EXCLUDED_PREFIXES = ("docs/projectops/", "docs/superpowers/", ".claude/", ".cursor/", ".github/", ".agents/")
EXCLUDED_NAMES = {"CHANGELOG.md"}

# 번역 파일 이름: {기본 이름}.{소문자 2글자 언어코드}.md
TRANSLATION_PATTERN = re.compile(r"^(?P<base>.+)\.(?P<language>[a-z]{2})\.md$")
# 마크다운 링크 `](대상)` 과 HTML `src="대상"`
LINK_PATTERN = re.compile(r"\]\(([^)\s]+)\)|src=\"([^\"]+)\"")
EXTERNAL_LINK_PATTERN = re.compile(r"^(https?:|mailto:|#)")
# 코드 블록·인라인 코드 (링크 예시가 들어 있어 점검에서 제외)
CODE_PATTERN = re.compile(r"```.*?```|`[^`\n]*`", re.DOTALL)


def list_tracked_markdown_files(repository_root):
    # -z: 한글 경로를 따옴표·8진수로 감싸지 않게
    output = subprocess.run(["git", "ls-files", "-z", "*.md"], cwd=repository_root,
                            capture_output=True, text=True, check=True).stdout
    return [path for path in output.split("\0")
            if path and not path.startswith(EXCLUDED_PREFIXES) and os.path.basename(path) not in EXCLUDED_NAMES]


def find_broken_links(repository_root, markdown_path, text):
    broken_links = []
    for match in LINK_PATTERN.finditer(text):
        target = (match.group(1) or match.group(2)).split("#")[0]
        if not target or EXTERNAL_LINK_PATTERN.match(target):
            continue
        resolved = os.path.normpath(os.path.join(os.path.dirname(markdown_path), target))
        if not os.path.exists(os.path.join(repository_root, resolved)):
            broken_links.append(target)
    return broken_links


def group_language_variants(markdown_paths):
    """기본 문서 경로 > 같은 문서의 번역 경로 목록 (번역이 있는 문서만)"""
    existing_paths = set(markdown_paths)
    translations_by_base = {}
    for path in markdown_paths:
        directory, name = os.path.split(path)
        match = TRANSLATION_PATTERN.match(name)
        if not match:
            continue
        base_path = os.path.join(directory, match.group("base") + ".md")
        if base_path in existing_paths:
            translations_by_base.setdefault(base_path, []).append(path)
    return translations_by_base


def find_missing_language_links(text, own_path, sibling_paths):
    """한 문서에서 같은 묶음의 다른 언어 문서로 가는 링크가 빠진 것"""
    linked_names = {os.path.basename(match.group(1).split("#")[0])
                    for match in LINK_PATTERN.finditer(text) if match.group(1)}
    return [os.path.basename(path) for path in sibling_paths
            if path != own_path and os.path.basename(path) not in linked_names]


def main():
    repository_root = subprocess.run(["git", "rev-parse", "--show-toplevel"],
                                     capture_output=True, text=True, check=True).stdout.strip()
    markdown_paths = list_tracked_markdown_files(repository_root)
    text_by_path = {path: CODE_PATTERN.sub("", open(os.path.join(repository_root, path), encoding="utf-8").read())
                    for path in markdown_paths}
    problems = []

    # 상대 링크 존재
    for path, text in text_by_path.items():
        for target in find_broken_links(repository_root, path, text):
            problems.append(f"{path}: broken link -> {target}")

    # 언어 묶음 안에서 서로 전환 링크
    for base_path, translation_paths in group_language_variants(markdown_paths).items():
        group = [base_path] + translation_paths
        for path in group:
            for missing_name in find_missing_language_links(text_by_path[path], path, group):
                problems.append(f"{path}: missing language link -> {missing_name}")

    for problem in problems:
        print(f"[docs check] {problem}")
    print(f"[docs check] {len(markdown_paths)} files, {len(problems)} problem(s)")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
