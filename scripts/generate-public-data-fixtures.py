#!/usr/bin/env python3
"""공개 데이터 소스 합성 fixture와 기대 결과 생성: spec/fixtures/public_data/*.json

  fixture(학교 응답 모양, 값은 전부 가상) > 참조 매핑(spec/PUBLIC_DATA.md 의 필드 대응) > expected_public_data.json
  세 언어 테스트는 같은 fixture 를 읽어 같은 expected 와 비교한다
  사용: python3 scripts/generate-public-data-fixtures.py
"""

import json
from pathlib import Path

OUT_DIR = Path(__file__).resolve().parent.parent / "spec" / "fixtures" / "public_data"

# 결과의 extra 에서 제외하는 필드 (교직원 이름 등 개인 식별, 서버 내부 정보)
EXTRA_DENYLIST = {"CD_ALIAS1", "CD_ALIAS2", "CD_ALIAS3", "CD_ALIAS1_ENG", "CD_ALIAS2_ENG", "CD_ALIAS3_ENG",
                  "regname", "regid", "STACK_TRACE"}


def text(value) -> str:
    return "" if value is None else str(value)


def extra_of(row: dict, used: set) -> dict:
    """매핑에 쓰지 않은 필드를 원문 문자열로 (빈 값과 제외 목록 필드는 뺌)"""
    return {k: text(v) for k, v in sorted(row.items()) if k not in used and k not in EXTRA_DENYLIST and v not in (None, "")}


# ---- fixture (학교 응답 모양, 가상 값)
FIXTURES = {
    "reference_onload.json": {
        "dl_code_orgn": [
            {"COM_CD": "20", "CD_NM": "예시학부", "CD_NM_ENG": "Example Undergraduate", "CD_ALIAS3": "가상담당자", "PRT_ORD": "1"},
            {"COM_CD": "30", "CD_NM": "예시대학원", "CD_NM_ENG": "Example Graduate School", "CD_ALIAS3": "가상담당자", "PRT_ORD": "2"},
        ],
        "dl_code_sltDomain": [
            {"COM_CD": "10", "CD_NM": "예시영역", "CD_NM_ENG": "Example Domain", "PRT_ORD": "1"},
        ],
        "dl_code_curiType": [
            {"CURI_TYPE_CD": "1E", "CURI_TYPE_CD_NM": "공필", "ORGN_CLSF_CD": "20", "PRT_ORD": "1", "NEW_FIELD": "x"},
        ],
    },
    "reference_terms.json": {
        "dl_code_yearSmt": [
            {"YEAR": "2099", "SMT_CD": "20", "YEAR_SMT_CD": "2099/20", "YEAR_SMT_NM": "2099/2학기"},
            {"YEAR": "2099", "SMT_CD": "10", "YEAR_SMT_CD": "2099/10", "YEAR_SMT_NM": "2099/1학기"},
        ],
    },
    "reference_departments.json": {
        "dl_code_dept": [
            {"DEPT_CD": "9101", "DEPT_LEVEL_CD": "2", "DEPT_L_CD": "9100", "DEPT_L_PRINT": "예시대학", "DEPT_M_CD": "9101",
             "DEPT_M_PRINT": "예시공학과", "DEPT_S_CD": None, "DEPT_S_PRINT": None, "DEPT_NM_ENG": "Department of Example Engineering",
             "DEPT_NM": "예시대학 예시공학과【9101 학부】", "CLOSE_DT": None},
            {"DEPT_CD": "9201", "DEPT_LEVEL_CD": "3", "DEPT_L_CD": "9100", "DEPT_L_PRINT": "예시대학", "DEPT_M_CD": "9200",
             "DEPT_M_PRINT": "", "DEPT_S_CD": "9201", "DEPT_S_PRINT": "예시전공A", "DEPT_NM_ENG": None,
             "DEPT_NM": "예시대학 예시학부 예시전공A【9201 학부】", "CLOSE_DT": None},
        ],
    },
    "lectures.json": {
        "dl_main": [
            {"CURI_NO": "000001", "CLASS": "001", "CURI_NM": "예시과목", "CURI_TYPE_CD": "1E", "CURI_TYPE_CD_NM": "공필",
             "SLT_DOMAIN_CD": None, "SLT_DOMAIN_CD_NM": None, "CDT": "3.0", "TM_NUM": "3.0 / 3 / 0", "TOT_TM_NUM": 3,
             "LESN_TIME": "월09:00-10:30", "LESN_ROOM": "예시101", "LESN_EMP": "가상교수", "STUDENT_YEAR": 1,
             "CURI_LANG_CD": "1", "CURI_LANG_NM": None, "DEPT_CD": "9101", "SCH_DEPT_ALIAS": "예시공학과",
             "MANAGE_DEPT_NM": "예시공학과", "REMARK": None, "YEAR": "2099", "SMT_CD": "20", "NEW_FIELD": "y"},
        ],
    },
    "calendar.json": {
        "data": [
            {"scmYear": 2099, "scmMonth": 10, "collDiv": "0001", "collDivNm": "학부", "scmSubject": "예시 시험 기간",
             "scmSubjectEng": "Example Exam Period", "frDt": "2099-10-20", "toDt": "2099-10-26"},
        ],
    },
    # 대학원 사이트별 달력: items 는 JSON 문자열 (한 번 더 해석), frDt·toDt 는 MM.dd
    "graduate_calendar.json": {
        "success": True, "startDay": 4, "lastDay": 31, "dayList": [{"mon": 1, "tue": 2}],
        "items": json.dumps([
            {"scmYear": 2099, "scmMonth": 10, "collDiv": "0002", "collDivNm": "일반대학원", "scmSubject": "예시 종합시험",
             "scmSubjectEng": "Example Comprehensive Exam", "frDt": "10.08", "toDt": "10.15", "frDtKo": "목", "toDtKo": "목"},
        ], ensure_ascii=False),
    },
    "dormitory_menu.json": {
        "root": [{**{f"fo_date{i}": f"2099-10-{19 + i}" for i in range(1, 8)},
                  **{f"fo_menu_mor{i}": f"조식{i}" for i in range(1, 8)}, **{f"fo_menu_lun{i}": f"중식{i}" for i in range(1, 8)},
                  **{f"fo_menu_eve{i}": f"석식{i}" for i in range(1, 8)}, **{f"fo_sub_menu{i}": f"대체{i}" if i == 1 else "" for i in range(1, 8)},
                  "seq": "1", "locgbn": "SJ", "today": "2099-10-20"}],
    },
    "dormitory_notices.json": {
        "root": [{
            "topList": [{"seq": "10", "subject": "고정 공지", "regdate": "2099-10-01", "visitcnt": "5", "file_name1": "a.pdf", "regname": "가상", "regid": "x", "topflag": "Y"}],
            "list": [{"seq": "11", "subject": "일반 공지", "regdate": "2099-10-02", "visitcnt": "7", "file_name1": None, "regname": "가상", "regid": "x", "topflag": "N"}],
            "totalCount": [{"cnt": 2}],
        }],
    },
    "cafeteria.json": {
        "items": [
            {"buildingName": "예시관", "location": "6층", "placeId": 1, "placeName": "예시식당", "menuDate": "2099-10-20", "day": "월",
             "mealTypeId": 2, "mealName": "중식", "menuName": "예시메뉴", "price": 5500},
            {"buildingName": "예시관", "location": "6층", "placeId": 1, "placeName": "예시식당", "menuDate": None, "day": None,
             "mealTypeId": None, "mealName": None, "menuName": "고정메뉴", "price": 4000},
        ],
    },
    "curriculum_course.json": {
        "authorization": True, "redirectUrl": None, "success": True, "errorTrace": None, "errorMsgCode": None, "errorMsgTitle": None,
        "authentication": True,
        "items": [
            {"orgnClsfCd": "20", "curiYear": "2099", "year": "2099", "studentYear": 1, "stuYearCd": 1, "deptNm": "예시학과", "deptCd": "9101",
             "smtDanNm": "1학기", "smtDanCd": 1, "curiTypeNm": "전공기초", "curiTypeCd": "23", "curiNm": "예시프로그래밍", "curiNo": "900001",
             "cdt": 3, "exeTmNum": 2, "lecTmNum": 2, "curiTm": 4, "remark": "예시 교과목 개요 본문입니다. 두 문장입니다.", "sameSlcCnt": 1},
        ],
    },
    "campus_places.json": {
        "items": [
            {"id": 1, "placeName": "예시관", "placeNameEn": "Example Hall", "placeType": "BUILDING", "welfareCategory": None,
             "latitude": 37.5, "longitude": 127.07, "placeInfo": "예시 안내", "telNum": "02-0000-0000", "description": "",
             "orderNo": 1, "useYn": "Y"},
        ],
    },
    "extracurricular_events.json": {
        "success": True, "year": 2099, "years": [2099], "category": [], "site": "eco", "loading": False,
        "events": [
            {"id": "e1", "title": "예시 특강", "start": "2099-10-20T14:00:00", "end": "2099-10-20T16:00:00", "category_title": "특강",
             "data": {"url": "https://example.invalid/e1", "description": "예시 설명", "location": "예시관", "uid": "u1"}},
        ],
    },
}


# ---- 참조 매핑 (spec/PUBLIC_DATA.md 와 같은 내용)
def map_all() -> dict:
    f = FIXTURES
    orgn = [{"code": r["COM_CD"], "name": r["CD_NM"], "name_en": text(r.get("CD_NM_ENG")),
             "extra": extra_of(r, {"COM_CD", "CD_NM", "CD_NM_ENG"})} for r in f["reference_onload.json"]["dl_code_orgn"]]
    terms = [{"year": r["YEAR"], "semester_code": r["SMT_CD"], "code": r["YEAR_SMT_CD"], "name": r["YEAR_SMT_NM"],
              "extra": extra_of(r, {"YEAR", "SMT_CD", "YEAR_SMT_CD", "YEAR_SMT_NM"})} for r in f["reference_terms.json"]["dl_code_yearSmt"]]
    depts = [{"code": r["DEPT_CD"], "level": r["DEPT_LEVEL_CD"], "college_code": text(r["DEPT_L_CD"]), "college_name": text(r["DEPT_L_PRINT"]),
              "unit_code": text(r["DEPT_M_CD"]), "unit_name": text(r["DEPT_M_PRINT"]), "major_code": text(r["DEPT_S_CD"]),
              "major_name": text(r["DEPT_S_PRINT"]), "name_en": text(r["DEPT_NM_ENG"]),
              "extra": extra_of(r, {"DEPT_CD", "DEPT_LEVEL_CD", "DEPT_L_CD", "DEPT_L_PRINT", "DEPT_M_CD", "DEPT_M_PRINT", "DEPT_S_CD", "DEPT_S_PRINT", "DEPT_NM_ENG"})}
             for r in f["reference_departments.json"]["dl_code_dept"]]
    course_types = [{"code": r["CURI_TYPE_CD"], "name": r["CURI_TYPE_CD_NM"], "category_code": text(r["ORGN_CLSF_CD"]),
                     "extra": extra_of(r, {"CURI_TYPE_CD", "CURI_TYPE_CD_NM", "ORGN_CLSF_CD"})} for r in f["reference_onload.json"]["dl_code_curiType"]]
    domains = [{"code": r["COM_CD"], "name": r["CD_NM"], "name_en": text(r.get("CD_NM_ENG")),
                "extra": extra_of(r, {"COM_CD", "CD_NM", "CD_NM_ENG"})} for r in f["reference_onload.json"]["dl_code_sltDomain"]]
    lec_used = {"CURI_NO", "CLASS", "CURI_NM", "CURI_TYPE_CD", "CURI_TYPE_CD_NM", "SLT_DOMAIN_CD", "SLT_DOMAIN_CD_NM", "CDT", "TM_NUM", "TOT_TM_NUM",
                "LESN_TIME", "LESN_ROOM", "LESN_EMP", "STUDENT_YEAR", "CURI_LANG_CD", "CURI_LANG_NM", "DEPT_CD", "SCH_DEPT_ALIAS", "MANAGE_DEPT_NM",
                "REMARK", "YEAR", "SMT_CD"}
    lectures = [{"course_number": r["CURI_NO"], "section": r["CLASS"], "name": r["CURI_NM"], "course_type_code": text(r["CURI_TYPE_CD"]),
                 "course_type_name": text(r["CURI_TYPE_CD_NM"]), "domain_code": text(r["SLT_DOMAIN_CD"]), "domain_name": text(r["SLT_DOMAIN_CD_NM"]),
                 "credits": text(r["CDT"]), "hours": text(r["TM_NUM"]), "total_hours": text(r["TOT_TM_NUM"]), "schedule": text(r["LESN_TIME"]),
                 "room": text(r["LESN_ROOM"]), "instructor": text(r["LESN_EMP"]), "grade_level": text(r["STUDENT_YEAR"]),
                 "language_code": text(r["CURI_LANG_CD"]), "language_name": text(r["CURI_LANG_NM"]), "department_code": text(r["DEPT_CD"]),
                 "department_name": text(r["SCH_DEPT_ALIAS"]), "managing_department": text(r["MANAGE_DEPT_NM"]), "remark": text(r["REMARK"]),
                 "year": text(r["YEAR"]), "semester_code": text(r["SMT_CD"]), "extra": extra_of(r, lec_used)} for r in f["lectures.json"]["dl_main"]]
    curriculum_used = {"deptCd", "deptNm", "orgnClsfCd", "year", "curiNo", "curiNm", "curiTypeCd", "curiTypeNm", "cdt", "lecTmNum", "exeTmNum",
                       "curiTm", "stuYearCd", "smtDanCd", "smtDanNm", "remark"}
    curriculum_courses = [{"department_code": text(r["deptCd"]), "department_name": text(r["deptNm"]), "category_code": text(r["orgnClsfCd"]),
                           "year": text(r["year"]), "course_number": text(r["curiNo"]), "name": text(r["curiNm"]),
                           "course_type_code": text(r["curiTypeCd"]), "course_type_name": text(r["curiTypeNm"]), "credits": text(r["cdt"]),
                           "lecture_hours": text(r["lecTmNum"]), "practice_hours": text(r["exeTmNum"]), "total_hours": text(r["curiTm"]),
                           "grade_level": text(r["stuYearCd"]), "semester_code": text(r["smtDanCd"]), "semester_name": text(r["smtDanNm"]),
                           "overview": text(r["remark"]), "extra": extra_of(r, curriculum_used)} for r in f["curriculum_course.json"]["items"]]
    calendar = [{"year": text(r["scmYear"]), "month": text(r["scmMonth"]), "category_code": r["collDiv"], "category_name": text(r["collDivNm"]),
                 "title": text(r["scmSubject"]), "title_en": text(r["scmSubjectEng"]), "start_date": r["frDt"], "end_date": r["toDt"],
                 "extra": extra_of(r, {"scmYear", "scmMonth", "collDiv", "collDivNm", "scmSubject", "scmSubjectEng", "frDt", "toDt"})}
                for r in f["calendar.json"]["data"]]
    graduate_calendar = [{"year": text(r["scmYear"]), "month": text(r["scmMonth"]), "category_code": r["collDiv"], "category_name": text(r["collDivNm"]),
                          "title": text(r["scmSubject"]), "title_en": text(r["scmSubjectEng"]), "start_date": r["frDt"], "end_date": r["toDt"],
                          "extra": extra_of(r, {"scmYear", "scmMonth", "collDiv", "collDivNm", "scmSubject", "scmSubjectEng", "frDt", "toDt"})}
                         for r in json.loads(f["graduate_calendar.json"]["items"])]
    row = f["dormitory_menu.json"]["root"][0]
    dorm_menu = [{"date": row[f"fo_date{i}"], "breakfast": text(row[f"fo_menu_mor{i}"]), "lunch": text(row[f"fo_menu_lun{i}"]),
                  "dinner": text(row[f"fo_menu_eve{i}"]), "alternative": text(row[f"fo_sub_menu{i}"])} for i in range(1, 8)]
    block = f["dormitory_notices.json"]["root"][0]
    def notice(r): return {"id": text(r["seq"]), "title": text(r["subject"]), "date": text(r["regdate"]), "views": int(r["visitcnt"] or 0), "file_name": text(r["file_name1"])}
    dorm_notices = {"total": int(block["totalCount"][0]["cnt"]), "pinned": [notice(r) for r in block["topList"]], "items": [notice(r) for r in block["list"]]}
    cafeteria = [{"building_name": text(r["buildingName"]), "location": text(r["location"]), "place_id": text(r["placeId"]), "place_name": text(r["placeName"]),
                  "date": text(r["menuDate"]), "day": text(r["day"]), "meal_type_id": text(r["mealTypeId"]), "meal_name": text(r["mealName"]),
                  "menu_name": text(r["menuName"]), "price": text(r["price"])} for r in f["cafeteria.json"]["items"]]
    places = [{"id": text(r["id"]), "name": text(r["placeName"]), "name_en": text(r["placeNameEn"]), "place_type": text(r["placeType"]),
               "category": text(r["welfareCategory"]), "latitude": float(r["latitude"]), "longitude": float(r["longitude"]), "info": text(r["placeInfo"]),
               "phone": text(r["telNum"]), "description": text(r["description"]),
               "extra": extra_of(r, {"id", "placeName", "placeNameEn", "placeType", "welfareCategory", "latitude", "longitude", "placeInfo", "telNum", "description"})}
              for r in f["campus_places.json"]["items"]]
    events = [{"id": text(r["id"]), "title": text(r["title"]), "start": text(r["start"]), "end": text(r["end"]), "category_title": text(r["category_title"]),
               "url": text(r["data"].get("url")), "description": text(r["data"].get("description")), "location": text(r["data"].get("location"))}
              for r in f["extracurricular_events.json"]["events"]]
    return {"organization_categories": orgn, "terms": terms, "departments": depts, "course_types": course_types, "selective_domains": domains,
            "lectures": lectures, "curriculum_courses": curriculum_courses, "calendar": calendar, "graduate_calendar": graduate_calendar, "dormitory_menu": dorm_menu, "dormitory_notices": dorm_notices,
            "cafeteria_menu": cafeteria, "campus_places": places, "extracurricular_events": events}


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for name, body in FIXTURES.items():
        (OUT_DIR / name).write_text(json.dumps(body, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (OUT_DIR / "expected_public_data.json").write_text(json.dumps(map_all(), ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"wrote {len(FIXTURES) + 1} files to {OUT_DIR}")


if __name__ == "__main__":
    main()
