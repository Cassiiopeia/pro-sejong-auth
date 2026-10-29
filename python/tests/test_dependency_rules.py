"""의존 방향 규칙: 인증 코드와 공개 데이터 소스는 서로 임포트하지 않고, 공통 계층(transport, errors)만 함께 쓴다"""

import re
from pathlib import Path

SOURCE_DIR = Path(__file__).resolve().parent.parent / "src" / "sejongpeia"
# 인증 흐름 모듈 (공개 데이터 소스가 가져가면 안 되는 쪽)
AUTH_MODULES = ("client", "parsers", "portal_main_parser", "models")
PUBLIC_DATA_MODULES = ("public_data", "public_data_models")


def _imported_modules(file_stem: str):
    text = (SOURCE_DIR / f"{file_stem}.py").read_text(encoding="utf-8")
    return set(re.findall(r"^from \.(\w+) import|^from \. import (\w+)", text, re.MULTILINE) and
               [a or b for a, b in re.findall(r"^from \.(\w+) import|^from \. import (\w+)", text, re.MULTILINE)])


def test_public_data_never_imports_authentication_modules():
    for module in PUBLIC_DATA_MODULES:
        forbidden = _imported_modules(module) & set(AUTH_MODULES)
        assert not forbidden, f"{module} imports authentication modules {sorted(forbidden)}: share through transport or errors"


def test_authentication_never_imports_public_data():
    for module in AUTH_MODULES:
        forbidden = _imported_modules(module) & set(PUBLIC_DATA_MODULES)
        assert not forbidden, f"{module} imports public data modules {sorted(forbidden)}: keep sources independent"
