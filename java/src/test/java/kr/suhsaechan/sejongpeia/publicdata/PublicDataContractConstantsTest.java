package kr.suhsaechan.sejongpeia.publicdata;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 세 언어 공통 값: spec/contract.json 의 publicData <-> Java 공개 데이터 상수 (어긋나면 실패)
 */
class PublicDataContractConstantsTest {

  private final JsonNode contract = readContract();
  private final JsonNode publicData = contract.get("publicData");

  @Test
  void 요청_공통_값이_contract와_같다() {
    assertEquals(publicData.get("jsonAccept").asText(), OkHttpPublicDataHttpCaller.JSON_ACCEPT);
    assertEquals(publicData.get("formContentType").asText(), OkHttpPublicDataHttpCaller.FORM_CONTENT_TYPE);
    assertEquals(publicData.get("formRequestedWith").asText(), OkHttpPublicDataHttpCaller.FORM_REQUESTED_WITH);
    assertEquals(contract.get("sis").get("submitErrorKey").asText(), PublicDataFetcher.SUBMIT_ERROR_KEY);
  }

  @Test
  void SIS_시간표와_교육과정_값이_contract와_같다() {
    JsonNode sisTimetable = publicData.get("sisTimetable");
    JsonNode curriculum = publicData.get("curriculum");

    assertEquals(sisTimetable.get("baseUrl").asText(), SejongPublicData.SIS_OPEN_TIME_URL);
    assertEquals(sisTimetable.get("contentType").asText(), SejongPublicData.SIS_CONTENT_TYPE);
    assertEquals(sisTimetable.get("searchKey").asText(), SejongPublicData.SIS_SEARCH_KEY);
    assertEquals(sisTimetable.get("onloadEndpoint").asText(), SejongPublicData.SIS_ONLOAD_ENDPOINT);
    assertEquals(sisTimetable.get("termListEndpoint").asText(), SejongPublicData.SIS_TERM_LIST_ENDPOINT);
    assertEquals(sisTimetable.get("departmentListEndpoint").asText(), SejongPublicData.SIS_DEPARTMENT_LIST_ENDPOINT);
    assertEquals(sisTimetable.get("lectureListEndpoint").asText(), SejongPublicData.SIS_LECTURE_LIST_ENDPOINT);
    assertEquals(textsOf(sisTimetable.get("lectureEmptyFilterKeys")), SejongPublicData.LECTURE_EMPTY_FILTER_KEYS);
    assertEquals(curriculum.get("url").asText(), SejongPublicData.CURRICULUM_COURSE_URL);
    assertEquals(textsOf(curriculum.get("queryKeys")), SejongPublicData.CURRICULUM_QUERY_KEYS);
  }

  @Test
  void 학사일정_값이_contract와_같다() {
    JsonNode calendar = publicData.get("calendar");
    JsonNode graduateCalendar = publicData.get("graduateCalendar");

    assertEquals(calendar.get("url").asText(), SejongPublicData.ACADEMIC_CALENDAR_URL);
    assertEquals(calendar.get("mode").asText(), SejongPublicData.CALENDAR_MODE);
    assertEquals(calendar.get("defaultCategoryCode").asText(), SejongPublicData.DEFAULT_CALENDAR_CATEGORY_CODE);
    assertEquals(textsOf(calendar.get("queryKeys")), SejongPublicData.CALENDAR_QUERY_KEYS);
    assertEquals(graduateCalendar.get("urlTemplate").asText(), SejongPublicData.GRADUATE_CALENDAR_URL);
    assertEquals(textsOf(graduateCalendar.get("queryKeys")), SejongPublicData.GRADUATE_CALENDAR_QUERY_KEYS);
    assertEquals(graduateCalendar.get("siteIdPattern").asText(), SejongPublicData.GRADUATE_SITE_ID_PATTERN.pattern());
  }

  @Test
  void 기숙사_값이_contract와_같다() {
    JsonNode dormitory = publicData.get("dormitory");

    assertEquals(dormitory.get("menuUrl").asText(), SejongPublicData.DORMITORY_MENU_URL);
    assertEquals(dormitory.get("noticeUrl").asText(), SejongPublicData.DORMITORY_NOTICE_URL);
    assertEquals(dormitory.get("defaultLocation").asText(), SejongPublicData.DEFAULT_DORMITORY_LOCATION);
    assertEquals(dormitory.get("noticeBoardId").asText(), SejongPublicData.DORMITORY_NOTICE_BOARD_ID);
    assertEquals(dormitory.get("noticeSearchType").asText(), SejongPublicData.DORMITORY_NOTICE_SEARCH_TYPE);
    assertEquals(textsOf(dormitory.get("menuFormKeys")), SejongPublicData.DORMITORY_MENU_FORM_KEYS);
    assertEquals(textsOf(dormitory.get("noticeFormKeys")), SejongPublicData.DORMITORY_NOTICE_FORM_KEYS);
  }

  @Test
  void 학식_캠퍼스_비교과_값이_contract와_같다() {
    JsonNode cafeteria = publicData.get("cafeteria");
    JsonNode campusMap = publicData.get("campusMap");
    JsonNode extracurricular = publicData.get("extracurricular");

    assertEquals(cafeteria.get("url").asText(), SejongPublicData.CAFETERIA_URL);
    assertEquals(cafeteria.get("menuMode").asText(), SejongPublicData.CAFETERIA_MENU_MODE);
    assertEquals(cafeteria.get("defaultMaxPlaceId").asInt(), SejongPublicData.CAFETERIA_DEFAULT_MAX_PLACE_ID);
    assertEquals(cafeteria.get("limitMaxPlaceId").asInt(), SejongPublicData.CAFETERIA_LIMIT_MAX_PLACE_ID);
    assertEquals(textsOf(cafeteria.get("queryKeys")), SejongPublicData.CAFETERIA_QUERY_KEYS);
    assertEquals(campusMap.get("url").asText(), SejongPublicData.CAMPUS_MAP_URL);
    assertEquals(campusMap.get("mode").asText(), SejongPublicData.CAMPUS_PLACE_LIST_MODE);
    assertEquals(textsOf(campusMap.get("queryKeys")), SejongPublicData.CAMPUS_MAP_QUERY_KEYS);
    assertEquals(extracurricular.get("url").asText(), SejongPublicData.EXTRACURRICULAR_EVENTS_URL);
    assertEquals(extracurricular.get("cid").asText(), SejongPublicData.EXTRACURRICULAR_CID);
    assertEquals(textsOf(extracurricular.get("formKeys")), SejongPublicData.EXTRACURRICULAR_FORM_KEYS);
  }

  private static List<String> textsOf(JsonNode arrayNode) {
    List<String> texts = new ArrayList<>();
    arrayNode.forEach(element -> texts.add(element.asText()));
    return texts;
  }

  private static JsonNode readContract() {
    try {
      return new ObjectMapper().readTree(Path.of("..", "spec", "contract.json").toFile());
    } catch (Exception e) {
      throw new IllegalStateException("spec/contract.json read failed", e);
    }
  }
}
