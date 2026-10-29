"""라이브러리 버전: 공통 계층의 단일 읽기 지점 (User-Agent 등에서 사용)"""

from importlib.metadata import PackageNotFoundError, version

try:
    # 버전 단일 원본: pyproject.toml (projectops 가 version.yml 과 함께 갱신)
    __version__ = version("pro-sejongpeia")
except PackageNotFoundError:  # 설치 없이 소스로 실행 (테스트)
    __version__ = "0.0.0-dev"
