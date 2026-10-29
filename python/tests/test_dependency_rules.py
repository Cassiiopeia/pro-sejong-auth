"""의존 방향 규칙 3계층: 공통(common)은 소스를 모르고, 소스(auth, public_data)는 공통만 쓰며 서로 임포트하지 않는다"""

import ast
from pathlib import Path

PACKAGE_NAME = "sejongpeia"
SOURCE_DIR = Path(__file__).resolve().parent.parent / "src" / PACKAGE_NAME
SOURCE_PACKAGES = ("auth", "public_data")
COMMON_PACKAGE = "common"
# 각 계층이 임포트해도 되는 계층 (자기 자신 제외)
ALLOWED_LAYERS_BY_LAYER = {
    COMMON_PACKAGE: set(),
    "auth": {COMMON_PACKAGE},
    "public_data": {COMMON_PACKAGE},
}


def _imported_layers(file: Path, own_layer: str):
    """파일이 임포트하는 최상위 계층 이름 집합 (상대 임포트 `..x` 와 절대 임포트 `sejongpeia.x` 모두)"""
    layers = set()
    for node in ast.walk(ast.parse(file.read_text(encoding="utf-8"))):
        if isinstance(node, ast.ImportFrom):
            if node.level >= 2:
                # 상위 패키지로 올라간 임포트: `from ..common.x import y` > common, `from .. import x` > x
                target = (node.module or "").split(".")[0] or node.names[0].name
                layers.add(target)
            elif node.level == 0 and node.module and node.module.split(".")[0] == PACKAGE_NAME:
                parts = node.module.split(".")
                layers.add(parts[1] if len(parts) > 1 else node.names[0].name)
        elif isinstance(node, ast.Import):
            for alias in node.names:
                parts = alias.name.split(".")
                if parts[0] == PACKAGE_NAME and len(parts) > 1:
                    layers.add(parts[1])
    layers.discard(own_layer)
    return layers


def _python_files(layer: str):
    return sorted((SOURCE_DIR / layer).rglob("*.py"))


def test_every_layer_has_files():
    for layer in ALLOWED_LAYERS_BY_LAYER:
        assert _python_files(layer), f"{layer} has no python files: rule test would pass without checking anything"


def test_layers_import_only_allowed_layers():
    for layer, allowed_layers in ALLOWED_LAYERS_BY_LAYER.items():
        for file in _python_files(layer):
            forbidden = _imported_layers(file, layer) - allowed_layers
            assert not forbidden, (
                f"{layer}/{file.name} imports {sorted(forbidden)}: sources share only common, common knows no source"
            )
