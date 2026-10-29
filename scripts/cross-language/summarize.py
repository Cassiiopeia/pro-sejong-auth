"""Python 통합 인증 결과 > 필드별 해시 JSON (개인정보 출력 없이 세 언어 비교용)

사용: SEJONG_TEST_STUDENT_ID, SEJONG_TEST_PASSWORD 설정 후 python3 summarize.py [--wrong-password]
"""

import hashlib
import json
import os
import sys
from dataclasses import asdict
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "python" / "src"))

from sejong_auth import SejongAuthError, authenticate  # noqa: E402

# 비교 필드 (Java·Node 이름 기준) > Python 필드
FIELD_BY_COMPARED_NAME = {
    "studentId": "student_id", "name": "name", "major": "major", "grade": "grade", "status": "status",
    "email": "email", "phoneNumber": "phone_number", "englishName": "english_name",
    "userCategory": "user_category", "parentUnitName": "parent_unit_name", "classicReading": "classic_reading",
}


def to_camel_case(value):
    if isinstance(value, dict):
        return {"".join(part.capitalize() if index else part for index, part in enumerate(key.split("_"))): to_camel_case(item)
                for key, item in value.items()}
    if isinstance(value, list):
        return [to_camel_case(item) for item in value]
    return value


def field_hash(value) -> str:
    canonical = json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False)
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()[:16]


def main() -> None:
    student_id = os.environ["SEJONG_TEST_STUDENT_ID"]
    password = os.environ["SEJONG_TEST_PASSWORD"] + ("-wrong" if "--wrong-password" in sys.argv else "")
    try:
        result = authenticate(student_id, password)
    except SejongAuthError as error:
        print(json.dumps({"error": error.code.value}))
        return
    values = {name: getattr(result, field) for name, field in FIELD_BY_COMPARED_NAME.items()}
    values["classicReading"] = to_camel_case(asdict(values["classicReading"]))
    print(json.dumps({"fields": {name: field_hash(value) for name, value in values.items()}}, sort_keys=True))


if __name__ == "__main__":
    main()
