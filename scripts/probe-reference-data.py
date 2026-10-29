#!/usr/bin/env python3
"""학교 참조 데이터 API 형태 점검 (조직분류, 학기, 학과전공) - 로그인·계정 불필요

  실제 호출:   python3 scripts/probe-reference-data.py          (요청 3건, 1초 간격)
  오프라인:    python3 scripts/probe-reference-data.py --offline (spec/fixtures 로 검사 로직만 확인)
  종료 코드: 0 정상, 1 형태 변경 감지 (spec/REFERENCE_DATA.md 와 구현을 함께 고칠 것)
출력은 건수와 누락 필드 이름만 (학과명·교직원명 등 값은 출력하지 않음)
"""

import json
import sys
import time
from pathlib import Path
from typing import Dict, List, Optional

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "python" / "src"))

from sejongpeia.client import USER_AGENT  # noqa: E402
from sejongpeia.transport import UrllibSession, create_ssl_context  # noqa: E402

BASE_URL = "https://sjpt.sejong.ac.kr/sch/sch/ext/SueOpenTimeQ/"
REQUEST_INTERVAL_SECONDS = 1
TIMEOUT_SECONDS = 20
UNDERGRADUATE_ORGN_CD = "20"

# 데이터셋별 필수 필드 (spec/REFERENCE_DATA.md 표와 동일하게 유지)
REQUIRED_ORGN_FIELDS = ("COM_CD", "CD_NM")
REQUIRED_TERM_FIELDS = ("YEAR", "SMT_CD", "YEAR_SMT_CD", "YEAR_SMT_NM")
REQUIRED_DEPT_FIELDS = ("DEPT_CD", "DEPT_LEVEL_CD", "DEPT_L_CD", "DEPT_M_CD", "ORGN_CLSF_CD")


def find_dataset_problems(body: dict, dataset_key: str, required_fields: tuple, is_empty_allowed: bool) -> List[str]:
    """응답 1건 점검: 오류 객체 > 데이터셋 키 > 행 존재 > 필수 필드"""
    if "_SUBMIT_ERROR_" in body:
        error = body["_SUBMIT_ERROR_"] or {}
        # STACK_TRACE 는 내부 정보라 출력하지 않음
        return [f"{dataset_key}: _SUBMIT_ERROR_ type={error.get('ERRTYPE')} code={error.get('ERRCODE')}"]
    if dataset_key not in body or not isinstance(body[dataset_key], list):
        return [f"{dataset_key}: dataset missing (keys={sorted(body)[:5]})"]
    rows = body[dataset_key]
    if not rows:
        return [] if is_empty_allowed else [f"{dataset_key}: no rows"]
    missing_fields = [field for field in required_fields if field not in rows[0]]
    return [f"{dataset_key}: first row lacks {missing_fields}"] if missing_fields else []


def find_department_problems(rows: List[dict]) -> List[str]:
    """학과 행 추가 점검: DEPT_CD 중복 없음, 수준 값 2·3, 수준별 코드 대응(2: M, 3: S)"""
    problems = []
    codes = [row.get("DEPT_CD") for row in rows]
    if len(codes) != len(set(codes)):
        problems.append("dl_code_dept: DEPT_CD not unique")
    for row in rows:
        level = row.get("DEPT_LEVEL_CD")
        if level not in ("2", "3"):
            problems.append(f"dl_code_dept: unknown DEPT_LEVEL_CD {level!r}")
            break
        own_code_field = "DEPT_M_CD" if level == "2" else "DEPT_S_CD"
        if row.get("DEPT_CD") != row.get(own_code_field):
            problems.append(f"dl_code_dept: level {level} DEPT_CD != {own_code_field}")
            break
    return problems


def inspect_all(orgn_body: dict, term_body: dict, dept_body: dict) -> List[str]:
    problems = find_dataset_problems(orgn_body, "dl_code_orgn", REQUIRED_ORGN_FIELDS, is_empty_allowed=False)
    problems += find_dataset_problems(term_body, "dl_code_yearSmt", REQUIRED_TERM_FIELDS, is_empty_allowed=False)
    problems += find_dataset_problems(dept_body, "dl_code_dept", REQUIRED_DEPT_FIELDS, is_empty_allowed=False)
    if not problems:
        orgn_codes = {row["COM_CD"] for row in orgn_body["dl_code_orgn"]}
        if UNDERGRADUATE_ORGN_CD not in orgn_codes:
            problems.append(f"dl_code_orgn: category {UNDERGRADUATE_ORGN_CD} missing")
        problems += find_department_problems(dept_body["dl_code_dept"])
    return problems


def post_json(session: UrllibSession, endpoint: str, body: dict) -> dict:
    response = session.request(
        "POST", BASE_URL + endpoint, data=json.dumps(body).encode("utf-8"),
        headers={"Content-Type": 'application/json; charset="UTF-8"', "Accept": "application/json"})
    if not response.is_successful:
        raise RuntimeError(f"{endpoint}: HTTP {response.status}")
    time.sleep(REQUEST_INTERVAL_SECONDS)
    return json.loads(response.body)


def fetch_live_bodies() -> Dict[str, dict]:
    """요청 3건: 조직분류 > 학기(학부) > 학과전공(학부, 최신 학기)"""
    session = UrllibSession(create_ssl_context(True), TIMEOUT_SECONDS, USER_AGENT)
    orgn_body = post_json(session, "doOnload.do", {"dm_search": {}})
    term_body = post_json(session, "doYearsmtlist.do", {"dm_search": {"ORGN_CLSF_CD": UNDERGRADUATE_ORGN_CD}})
    latest_term = (term_body.get("dl_code_yearSmt") or [{}])[0]
    dept_body = post_json(session, "doLesndeptgroup.do", {"dm_search": {
        "ORGN_CLSF_CD": UNDERGRADUATE_ORGN_CD, "YEAR": latest_term.get("YEAR", ""),
        "SMT_CD": latest_term.get("SMT_CD", ""), "YEAR_SMT": latest_term.get("YEAR_SMT_CD", "")}})
    return {"orgn": orgn_body, "term": term_body, "dept": dept_body}


def load_fixture_bodies() -> Dict[str, dict]:
    fixtures = ROOT / "spec" / "fixtures"
    return {name: json.loads((fixtures / f"reference_{file}.json").read_text(encoding="utf-8"))
            for name, file in (("orgn", "orgn"), ("term", "yearsmt"), ("dept", "dept"))}


def main(argv: Optional[List[str]] = None) -> int:
    is_offline = "--offline" in (argv if argv is not None else sys.argv[1:])
    try:
        bodies = load_fixture_bodies() if is_offline else fetch_live_bodies()
    except (OSError, RuntimeError, ValueError, TimeoutError) as error:
        print(f"[probe] request failed: {type(error).__name__}: {error}")
        return 1
    problems = inspect_all(bodies["orgn"], bodies["term"], bodies["dept"])
    print(f"[probe] mode={'offline' if is_offline else 'live'} "
          f"orgn={len(bodies['orgn'].get('dl_code_orgn') or [])} "
          f"terms={len(bodies['term'].get('dl_code_yearSmt') or [])} "
          f"depts={len(bodies['dept'].get('dl_code_dept') or [])}")
    for problem in problems:
        print(f"[probe] DRIFT {problem}")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
