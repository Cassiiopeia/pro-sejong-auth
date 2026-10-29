package kr.suhsaechan.sejongpeia.model;

import java.util.Map;

/**
 * SIS 학사일정조회 1행 (학사 업무별 허용 기간): 학교 원문, 없으면 빈 문자열
 * extra = 이름 붙이지 않은 나머지 필드, 민감 키 제외
 */
public record SisBusinessPeriod(
    String period, // PERIOD, 예: "2026-03-01 ∼ 2026-03-15"
    String startDate, // START_DT, yyyyMMdd
    String endDate, // TO_DT, yyyyMMdd
    String scheduleName, // SCHEDULE
    String courseGroupName, // CORS_SCHE_GRP_CD_NM
    String target, // TARGET_AUTH
    String businessName, // SYS_NM
    String yearSemester, // YEAR_SMT
    String timeText, // TMS
    String categoryCode, // ORGN_CLSF_CD
    String sortOrder, // PRT_ORD
    Map<String, String> extra) {
}
