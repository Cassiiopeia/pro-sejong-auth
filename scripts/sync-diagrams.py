#!/usr/bin/env python3
"""
다이어그램 단일 원본 동기화: docs/assets/diagrams/*.mmd > README 의 mermaid 블록
원본: 다이어그램별 `<이름>.<언어>.mmd` 와 공통 스타일 `_classes.mmd` (문서 사이트는 빌드 때 같은 파일을 읽음)
README 쪽 표시: `<!-- diagram:<이름>:start -->` 와 `<!-- diagram:<이름>:end -->` 사이를 이 스크립트가 채움
사용: python3 scripts/sync-diagrams.py (갱신), --check (어긋나면 실패, check-docs.py 가 호출)
"""
import os
import re
import sys

DIAGRAM_DIRECTORY = "docs/assets/diagrams"
STYLE_FILE_NAME = "_classes.mmd"
# README 파일 > 언어 (README.md 는 한국어, README.<코드>.md 는 그 언어)
README_LANGUAGES = {"README.md": "ko", "README.en.md": "en"}
MARKER_PATTERN = re.compile(r"(<!-- diagram:(?P<name>[a-z-]+):start -->\n).*?(<!-- diagram:(?P=name):end -->)", re.DOTALL)
# GitHub 은 mermaid click 을 쓰지 못하고, `%%` 줄(alt 설명, narrow 방향)은 사이트 전용이라 뺀다
README_EXCLUDED_LINE_PATTERN = re.compile(r"^\s*(click\s|%%)")


def build_readme_mermaid_block(repository_root, diagram_name, language):
    source_path = os.path.join(repository_root, DIAGRAM_DIRECTORY, f"{diagram_name}.{language}.mmd")
    style_path = os.path.join(repository_root, DIAGRAM_DIRECTORY, STYLE_FILE_NAME)
    source_lines = open(source_path, encoding="utf-8").read().splitlines()
    body_lines = [line for line in source_lines if not README_EXCLUDED_LINE_PATTERN.match(line)]
    style_lines = open(style_path, encoding="utf-8").read().splitlines()
    # 스타일은 맨 아래에 붙인다 (노드 정의 뒤)
    mermaid_text = "\n".join(body_lines + [""] + ["  " + line for line in style_lines if line.strip()])
    return f"```mermaid\n{mermaid_text}\n```\n"


def sync_readme_text(repository_root, text, language):
    def replace_block(match):
        block = build_readme_mermaid_block(repository_root, match.group("name"), language)
        return f"{match.group(1)}{block}{match.group(3)}"
    return MARKER_PATTERN.sub(replace_block, text)


def main():
    repository_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    is_check_mode = "--check" in sys.argv
    outdated_names = []
    for readme_name, language in README_LANGUAGES.items():
        readme_path = os.path.join(repository_root, readme_name)
        text = open(readme_path, encoding="utf-8").read()
        synced_text = sync_readme_text(repository_root, text, language)
        if synced_text == text:
            continue
        outdated_names.append(readme_name)
        if not is_check_mode:
            open(readme_path, "w", encoding="utf-8").write(synced_text)
    if is_check_mode and outdated_names:
        print(f"[diagram sync] outdated : {', '.join(outdated_names)} (run python3 scripts/sync-diagrams.py)")
        return 1
    print(f"[diagram sync] {'ok' if is_check_mode else 'updated ' + str(len(outdated_names)) + ' file(s)'}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
