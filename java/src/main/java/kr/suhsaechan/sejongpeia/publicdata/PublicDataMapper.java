package kr.suhsaechan.sejongpeia.publicdata;

import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRowReader.decimal;
import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRowReader.extraOf;
import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRowReader.integer;
import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRowReader.requireArray;
import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRowReader.text;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import kr.suhsaechan.sejongpeia.publicdata.model.*;

/**
 * 학교 응답 > 모델 변환: 필드 대응은 spec/PUBLIC_DATA.md 와 같음
 * 반환 목록은 변경 불가 (캐시 공유)
 */
final class PublicDataMapper {

  private static final int DORMITORY_MENU_DAY_COUNT = 7;

  private PublicDataMapper() {
  }

  /** 응답의 배열 필드 > 행마다 변환한 변경 불가 목록 */
  static <T> Function<JsonNode, List<T>> listOf(String arrayName, Function<JsonNode, T> rowMapper) {
    return root -> {
      List<T> models = new ArrayList<>();
      for (JsonNode row : requireArray(root, arrayName)) {
        models.add(rowMapper.apply(row));
      }
      return List.copyOf(models);
    };
  }

  static OrganizationCategory toOrganizationCategory(JsonNode row) {
    return OrganizationCategory.builder().code(text(row, "COM_CD")).name(text(row, "CD_NM")).nameEn(text(row, "CD_NM_ENG"))
        .extra(extraOf(row, Set.of("COM_CD", "CD_NM", "CD_NM_ENG"))).build();
  }

  static SelectiveDomain toSelectiveDomain(JsonNode row) {
    return SelectiveDomain.builder().code(text(row, "COM_CD")).name(text(row, "CD_NM")).nameEn(text(row, "CD_NM_ENG"))
        .extra(extraOf(row, Set.of("COM_CD", "CD_NM", "CD_NM_ENG"))).build();
  }

  static Term toTerm(JsonNode row) {
    return Term.builder().year(text(row, "YEAR")).semesterCode(text(row, "SMT_CD")).code(text(row, "YEAR_SMT_CD"))
        .name(text(row, "YEAR_SMT_NM"))
        .extra(extraOf(row, Set.of("YEAR", "SMT_CD", "YEAR_SMT_CD", "YEAR_SMT_NM"))).build();
  }

  static Department toDepartment(JsonNode row) {
    return Department.builder().code(text(row, "DEPT_CD")).level(text(row, "DEPT_LEVEL_CD"))
        .collegeCode(text(row, "DEPT_L_CD")).collegeName(text(row, "DEPT_L_PRINT"))
        .unitCode(text(row, "DEPT_M_CD")).unitName(text(row, "DEPT_M_PRINT"))
        .majorCode(text(row, "DEPT_S_CD")).majorName(text(row, "DEPT_S_PRINT")).nameEn(text(row, "DEPT_NM_ENG"))
        .extra(extraOf(row, Set.of("DEPT_CD", "DEPT_LEVEL_CD", "DEPT_L_CD", "DEPT_L_PRINT", "DEPT_M_CD", "DEPT_M_PRINT",
            "DEPT_S_CD", "DEPT_S_PRINT", "DEPT_NM_ENG"))).build();
  }

  static CourseType toCourseType(JsonNode row) {
    return CourseType.builder().code(text(row, "CURI_TYPE_CD")).name(text(row, "CURI_TYPE_CD_NM"))
        .categoryCode(text(row, "ORGN_CLSF_CD"))
        .extra(extraOf(row, Set.of("CURI_TYPE_CD", "CURI_TYPE_CD_NM", "ORGN_CLSF_CD"))).build();
  }

  static Lecture toLecture(JsonNode row) {
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

  static CalendarEvent toCalendarEvent(JsonNode row) {
    return CalendarEvent.builder().year(text(row, "scmYear")).month(text(row, "scmMonth")).categoryCode(text(row, "collDiv"))
        .categoryName(text(row, "collDivNm")).title(text(row, "scmSubject")).titleEn(text(row, "scmSubjectEng"))
        .startDate(text(row, "frDt")).endDate(text(row, "toDt"))
        .extra(extraOf(row, Set.of("scmYear", "scmMonth", "collDiv", "collDivNm", "scmSubject", "scmSubjectEng", "frDt", "toDt")))
        .build();
  }

  /** 기숙사 식단: root[0] 의 fo_*1..7 > 7일 */
  static List<DormitoryMenuDay> toDormitoryMenu(JsonNode root) {
    JsonNode row = firstRootRow(root);
    List<DormitoryMenuDay> days = new ArrayList<>();
    for (int dayNumber = 1; dayNumber <= DORMITORY_MENU_DAY_COUNT; dayNumber++) {
      days.add(DormitoryMenuDay.builder().date(text(row, "fo_date" + dayNumber)).breakfast(text(row, "fo_menu_mor" + dayNumber))
          .lunch(text(row, "fo_menu_lun" + dayNumber)).dinner(text(row, "fo_menu_eve" + dayNumber))
          .alternative(text(row, "fo_sub_menu" + dayNumber)).build());
    }
    return List.copyOf(days);
  }

  /** 기숙사 공지: 고정(topList) + 일반(list) + 전체 건수(totalCount[0].cnt), 작성자 제외 */
  static DormitoryNotices toDormitoryNotices(JsonNode root) {
    JsonNode block = firstRootRow(root);
    JsonNode totalRows = requireArray(block, "totalCount");
    List<DormitoryNotice> pinned = new ArrayList<>();
    if (block.get("topList") != null && block.get("topList").isArray()) {
      block.get("topList").forEach(row -> pinned.add(toDormitoryNotice(row)));
    }
    List<DormitoryNotice> items = new ArrayList<>();
    requireArray(block, "list").forEach(row -> items.add(toDormitoryNotice(row)));
    return DormitoryNotices.builder().total(totalRows.size() == 0 ? 0 : integer(totalRows.get(0), "cnt"))
        .pinned(List.copyOf(pinned)).items(List.copyOf(items)).build();
  }

  private static DormitoryNotice toDormitoryNotice(JsonNode row) {
    return DormitoryNotice.builder().id(text(row, "seq")).title(text(row, "subject")).date(text(row, "regdate"))
        .views(integer(row, "visitcnt")).fileName(text(row, "file_name1")).build();
  }

  static CafeteriaMenuItem toCafeteriaMenuItem(JsonNode row) {
    return CafeteriaMenuItem.builder().buildingName(text(row, "buildingName")).location(text(row, "location"))
        .placeId(text(row, "placeId")).placeName(text(row, "placeName")).date(text(row, "menuDate")).day(text(row, "day"))
        .mealTypeId(text(row, "mealTypeId")).mealName(text(row, "mealName")).menuName(text(row, "menuName"))
        .price(text(row, "price")).build();
  }

  static CampusPlace toCampusPlace(JsonNode row) {
    return CampusPlace.builder().id(text(row, "id")).name(text(row, "placeName")).nameEn(text(row, "placeNameEn"))
        .placeType(text(row, "placeType")).category(text(row, "welfareCategory"))
        .latitude(decimal(row, "latitude")).longitude(decimal(row, "longitude"))
        .info(text(row, "placeInfo")).phone(text(row, "telNum")).description(text(row, "description"))
        .extra(extraOf(row, Set.of("id", "placeName", "placeNameEn", "placeType", "welfareCategory", "latitude", "longitude",
            "placeInfo", "telNum", "description"))).build();
  }

  static ExtracurricularEvent toExtracurricularEvent(JsonNode row) {
    JsonNode detail = row.path("data");
    return ExtracurricularEvent.builder().id(text(row, "id")).title(text(row, "title")).start(text(row, "start"))
        .end(text(row, "end")).categoryTitle(text(row, "category_title")).url(text(detail, "url"))
        .description(text(detail, "description")).location(text(detail, "location")).build();
  }

  private static JsonNode firstRootRow(JsonNode root) {
    JsonNode rows = requireArray(root, "root");
    if (rows.size() == 0) {
      throw new IllegalStateException("expected row missing : root[0]");
    }
    return rows.get(0);
  }
}
