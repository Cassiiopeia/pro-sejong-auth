package kr.suhsaechan.sejongpeia.publicdata;

import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRequestSupport.cacheKeyOf;
import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRequestSupport.nullToEmpty;
import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRequestSupport.pairsOf;
import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRowReader.extraOf;
import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRowReader.listOf;
import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRowReader.text;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import kr.suhsaechan.sejongpeia.common.exception.SejongAuthErrorCode;
import kr.suhsaechan.sejongpeia.common.exception.SejongAuthException;
import kr.suhsaechan.sejongpeia.common.util.CommonUtil;
import kr.suhsaechan.sejongpeia.common.util.JsonMapperSupport;
import kr.suhsaechan.sejongpeia.publicdata.model.CourseType;
import kr.suhsaechan.sejongpeia.publicdata.model.CurriculumCourse;
import kr.suhsaechan.sejongpeia.publicdata.model.Department;
import kr.suhsaechan.sejongpeia.publicdata.model.Lecture;
import kr.suhsaechan.sejongpeia.publicdata.model.OrganizationCategory;
import kr.suhsaechan.sejongpeia.publicdata.model.SelectiveDomain;
import kr.suhsaechan.sejongpeia.publicdata.model.Term;

/**
 * 학사 기준 정보·강의·학과 교육과정 (SIS 시간표 + 교육과정 조회): 요청 조립과 행 > 모델 변환
 * 요청 > 데이터셋 배열 > 모델 목록
 */
final class AcademicSource {

  // 주소·요청 키·고정 값의 원본은 spec/contract.json 의 publicData.sisTimetable·curriculum
  static final String SIS_OPEN_TIME_URL = "https://sjpt.sejong.ac.kr/sch/sch/ext/SueOpenTimeQ/";
  static final String SIS_CONTENT_TYPE = "application/json; charset=\"UTF-8\"";
  static final String SIS_SEARCH_KEY = "dm_search";
  static final String SIS_ONLOAD_ENDPOINT = "doOnload.do";
  static final String SIS_TERM_LIST_ENDPOINT = "doYearsmtlist.do";
  static final String SIS_DEPARTMENT_LIST_ENDPOINT = "doLesndeptgroup.do";
  static final String SIS_LECTURE_LIST_ENDPOINT = "doList.do";
  // 강좌 조회는 나머지 필터 키가 모두 빈 값으로 있어야 학교가 받아들임 (하나라도 빠지면 _SUBMIT_ERROR_)
  static final List<String> LECTURE_EMPTY_FILTER_KEYS = List.of(
      "SMT_NM", "CURI_TYPE_CD", "SLT_DOMAIN_CD", "CURI_NO", "CLASS", "SLT_DEPT_CD", "PLAN_REG_YN", "CORS_SCHE_GRP_CD");
  // 학과 교육과정 과목 1건: deptNo 는 SIS 학과 코드(DEPT_CD)와 같은 값
  static final String CURRICULUM_COURSE_URL = "https://www.sejong.ac.kr/curriculum/getCurriculumOneKor.do";
  static final List<String> CURRICULUM_QUERY_KEYS = List.of("curiNo", "deptNo");

  private final PublicDataFetcher fetcher;

  AcademicSource(PublicDataFetcher fetcher) {
    this.fetcher = fetcher;
  }

  /** 기관 구분 목록 */
  List<OrganizationCategory> organizationCategories() {
    return sisDataset(SIS_ONLOAD_ENDPOINT, new LinkedHashMap<>(), "dl_code_orgn", AcademicSource::toOrganizationCategory);
  }

  /** 기관 구분 코드 > 학기 목록 */
  List<Term> terms(String categoryCode) {
    Map<String, String> search = new LinkedHashMap<>();
    search.put("ORGN_CLSF_CD", nullToEmpty(categoryCode));
    return sisDataset(SIS_TERM_LIST_ENDPOINT, search, "dl_code_yearSmt", AcademicSource::toTerm);
  }

  /** 기관 구분·연도·학기 코드 > 학과 목록 */
  List<Department> departments(String categoryCode, String year, String semesterCode) {
    return sisDataset(SIS_DEPARTMENT_LIST_ENDPOINT, termSearch(categoryCode, year, semesterCode), "dl_code_dept",
        AcademicSource::toDepartment);
  }

  /** 이수 구분 목록 */
  List<CourseType> courseTypes() {
    return sisDataset(SIS_ONLOAD_ENDPOINT, new LinkedHashMap<>(), "dl_code_curiType", AcademicSource::toCourseType);
  }

  /** 선택 교양 영역 목록 */
  List<SelectiveDomain> selectiveDomains() {
    return sisDataset(SIS_ONLOAD_ENDPOINT, new LinkedHashMap<>(), "dl_code_sltDomain", AcademicSource::toSelectiveDomain);
  }

  /** 학기 개설 강좌 (학과·과목명·교수명은 앞뒤 공백 제거, 모두 비우면 그 학기 전체) */
  List<Lecture> lectures(String categoryCode, String year, String semesterCode,
      String departmentCode, String courseName, String instructorName) {
    Map<String, String> search = termSearch(categoryCode, year, semesterCode);
    LECTURE_EMPTY_FILTER_KEYS.forEach(key -> search.put(key, ""));
    // 공백만 있는 값도 학교는 검색어로 받아 결과가 줄어들므로 빈 값으로 정리
    search.put("DEPT_CD", nullToEmpty(departmentCode).trim());
    search.put("CURI_NM", nullToEmpty(courseName).trim());
    search.put("EMP_NM", nullToEmpty(instructorName).trim());
    search.put("LANG_CHK", "KOR");
    return sisDataset(SIS_LECTURE_LIST_ENDPOINT, search, "dl_main", AcademicSource::toLecture);
  }

  /** 학과 코드·학수번호 > 교육과정 과목 (입력 검증 후 요청, 빈 입력은 INVALID_INPUT) */
  List<CurriculumCourse> curriculumCourses(String departmentCode, String courseNumber) {
    if (CommonUtil.isBlank(departmentCode) || CommonUtil.isBlank(courseNumber)) {
      throw new SejongAuthException(SejongAuthErrorCode.INVALID_INPUT, "Department code and course number are required");
    }
    String query = pairsOf(CURRICULUM_QUERY_KEYS, courseNumber, departmentCode);
    return fetcher.fetch(cacheKeyOf("curriculumCourses", departmentCode, courseNumber),
        new PublicDataRequest("GET", CURRICULUM_COURSE_URL + "?" + query, null, null),
        listOf("items", AcademicSource::toCurriculumCourse), List.of());
  }

  /** SIS 조회 1건: {"dm_search": ...} JSON 본문 > 데이터셋 배열 > 모델 목록 */
  private <T> List<T> sisDataset(String endpoint, Map<String, String> search, String datasetName, Function<JsonNode, T> rowMapper) {
    return fetcher.fetch(cacheKeyOf("sis", endpoint, datasetName, search.toString()),
        new PublicDataRequest("POST", SIS_OPEN_TIME_URL + endpoint, sisBodyOf(search), SIS_CONTENT_TYPE),
        listOf(datasetName, rowMapper), List.of());
  }

  private static Map<String, String> termSearch(String categoryCode, String year, String semesterCode) {
    Map<String, String> search = new LinkedHashMap<>();
    search.put("ORGN_CLSF_CD", nullToEmpty(categoryCode));
    search.put("YEAR", nullToEmpty(year));
    search.put("SMT_CD", nullToEmpty(semesterCode));
    search.put("YEAR_SMT", nullToEmpty(year) + "/" + nullToEmpty(semesterCode));
    return search;
  }

  private static String sisBodyOf(Map<String, String> search) {
    try {
      return JsonMapperSupport.PLAIN_JSON_MAPPER.writeValueAsString(Map.of(SIS_SEARCH_KEY, search));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("cannot build SIS body", e);
    }
  }

  private static OrganizationCategory toOrganizationCategory(JsonNode row) {
    return OrganizationCategory.builder().code(text(row, "COM_CD")).name(text(row, "CD_NM")).nameEn(text(row, "CD_NM_ENG"))
        .extra(extraOf(row, Set.of("COM_CD", "CD_NM", "CD_NM_ENG"))).build();
  }

  private static SelectiveDomain toSelectiveDomain(JsonNode row) {
    return SelectiveDomain.builder().code(text(row, "COM_CD")).name(text(row, "CD_NM")).nameEn(text(row, "CD_NM_ENG"))
        .extra(extraOf(row, Set.of("COM_CD", "CD_NM", "CD_NM_ENG"))).build();
  }

  private static Term toTerm(JsonNode row) {
    return Term.builder().year(text(row, "YEAR")).semesterCode(text(row, "SMT_CD")).code(text(row, "YEAR_SMT_CD"))
        .name(text(row, "YEAR_SMT_NM"))
        .extra(extraOf(row, Set.of("YEAR", "SMT_CD", "YEAR_SMT_CD", "YEAR_SMT_NM"))).build();
  }

  private static Department toDepartment(JsonNode row) {
    return Department.builder().code(text(row, "DEPT_CD")).level(text(row, "DEPT_LEVEL_CD"))
        .collegeCode(text(row, "DEPT_L_CD")).collegeName(text(row, "DEPT_L_PRINT"))
        .unitCode(text(row, "DEPT_M_CD")).unitName(text(row, "DEPT_M_PRINT"))
        .majorCode(text(row, "DEPT_S_CD")).majorName(text(row, "DEPT_S_PRINT")).nameEn(text(row, "DEPT_NM_ENG"))
        .extra(extraOf(row, Set.of("DEPT_CD", "DEPT_LEVEL_CD", "DEPT_L_CD", "DEPT_L_PRINT", "DEPT_M_CD", "DEPT_M_PRINT",
            "DEPT_S_CD", "DEPT_S_PRINT", "DEPT_NM_ENG"))).build();
  }

  private static CourseType toCourseType(JsonNode row) {
    return CourseType.builder().code(text(row, "CURI_TYPE_CD")).name(text(row, "CURI_TYPE_CD_NM"))
        .categoryCode(text(row, "ORGN_CLSF_CD"))
        .extra(extraOf(row, Set.of("CURI_TYPE_CD", "CURI_TYPE_CD_NM", "ORGN_CLSF_CD"))).build();
  }

  private static Lecture toLecture(JsonNode row) {
    return Lecture.builder().courseNumber(text(row, "CURI_NO")).section(text(row, "CLASS")).name(text(row, "CURI_NM"))
        .courseTypeCode(text(row, "CURI_TYPE_CD")).courseTypeName(text(row, "CURI_TYPE_CD_NM"))
        .domainCode(text(row, "SLT_DOMAIN_CD")).domainName(text(row, "SLT_DOMAIN_CD_NM"))
        .credits(text(row, "CDT")).hours(text(row, "TM_NUM")).totalHours(text(row, "TOT_TM_NUM"))
        .schedule(text(row, "LESN_TIME")).room(text(row, "LESN_ROOM")).instructor(text(row, "LESN_EMP"))
        .gradeLevel(text(row, "STUDENT_YEAR")).languageCode(text(row, "CURI_LANG_CD")).languageName(text(row, "CURI_LANG_NM"))
        .departmentCode(text(row, "DEPT_CD")).departmentName(text(row, "SCH_DEPT_ALIAS"))
        .managingDepartment(text(row, "MANAGE_DEPT_NM")).remark(text(row, "REMARK"))
        .year(text(row, "YEAR")).semesterCode(text(row, "SMT_CD"))
        .extra(extraOf(row, Set.of("CURI_NO", "CLASS", "CURI_NM", "CURI_TYPE_CD", "CURI_TYPE_CD_NM", "SLT_DOMAIN_CD",
            "SLT_DOMAIN_CD_NM", "CDT", "TM_NUM", "TOT_TM_NUM", "LESN_TIME", "LESN_ROOM", "LESN_EMP", "STUDENT_YEAR",
            "CURI_LANG_CD", "CURI_LANG_NM", "DEPT_CD", "SCH_DEPT_ALIAS", "MANAGE_DEPT_NM", "REMARK", "YEAR", "SMT_CD")))
        .build();
  }

  private static CurriculumCourse toCurriculumCourse(JsonNode row) {
    return CurriculumCourse.builder().departmentCode(text(row, "deptCd")).departmentName(text(row, "deptNm"))
        .categoryCode(text(row, "orgnClsfCd")).year(text(row, "year")).courseNumber(text(row, "curiNo"))
        .name(text(row, "curiNm")).courseTypeCode(text(row, "curiTypeCd")).courseTypeName(text(row, "curiTypeNm"))
        .credits(text(row, "cdt")).lectureHours(text(row, "lecTmNum")).practiceHours(text(row, "exeTmNum"))
        .totalHours(text(row, "curiTm")).gradeLevel(text(row, "stuYearCd")).semesterCode(text(row, "smtDanCd"))
        .semesterName(text(row, "smtDanNm")).overview(text(row, "remark"))
        .extra(extraOf(row, Set.of("deptCd", "deptNm", "orgnClsfCd", "year", "curiNo", "curiNm", "curiTypeCd", "curiTypeNm",
            "cdt", "lecTmNum", "exeTmNum", "curiTm", "stuYearCd", "smtDanCd", "smtDanNm", "remark"))).build();
  }
}
