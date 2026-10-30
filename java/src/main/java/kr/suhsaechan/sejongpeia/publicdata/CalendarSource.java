package kr.suhsaechan.sejongpeia.publicdata;

import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRequestSupport.cacheKeyOf;
import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRequestSupport.pairsOf;
import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRowReader.extraOf;
import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRowReader.listOf;
import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRowReader.text;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import kr.suhsaechan.sejongpeia.common.exception.SejongAuthErrorCode;
import kr.suhsaechan.sejongpeia.common.exception.SejongAuthException;
import kr.suhsaechan.sejongpeia.common.util.JsonMapperSupport;
import kr.suhsaechan.sejongpeia.publicdata.model.CalendarEvent;

/**
 * 학사일정: 학부·대학원 홈페이지 달력, 대학원 사이트별 달력 (요청 조립과 행 > 모델 변환)
 */
final class CalendarSource {

  // 주소·요청 키·고정 값의 원본은 spec/contract.json 의 publicData.calendar·graduateCalendar
  static final String ACADEMIC_CALENDAR_URL = "https://www.sejong.ac.kr/kor/academics/academic-calendar.do";
  static final List<String> CALENDAR_QUERY_KEYS = List.of("mode", "start", "end", "collDiv");
  static final String CALENDAR_MODE = "getCalendarData";
  static final String DEFAULT_CALENDAR_CATEGORY_CODE = "0001"; // 학부 일정
  // 대학원 사이트별 학사일정: {siteId} 는 graduate, gsi, mba 같은 사이트 식별자 (코드표 없이 호출자가 전달)
  static final String GRADUATE_CALENDAR_URL = "https://{siteId}.sejong.ac.kr/_custom/sejong/_app/calendar-graduate/proc/getCalendarData.jsp";
  static final List<String> GRADUATE_CALENDAR_QUERY_KEYS = List.of("year", "month", "siteId");
  // 사이트 식별자 허용 형태 (호스트 이름에 들어가므로 소문자·숫자·하이픈만)
  static final Pattern GRADUATE_SITE_ID_PATTERN = Pattern.compile("[a-z0-9][a-z0-9-]{0,30}");

  private final PublicDataFetcher fetcher;

  CalendarSource(PublicDataFetcher fetcher) {
    this.fetcher = fetcher;
  }

  /** 기간·분류 > 홈페이지 학사일정 */
  List<CalendarEvent> calendar(String startDate, String endDate, String categoryCode) {
    String query = pairsOf(CALENDAR_QUERY_KEYS, CALENDAR_MODE, startDate, endDate, categoryCode);
    return fetcher.fetch(cacheKeyOf("calendar", categoryCode, startDate, endDate),
        new PublicDataRequest("GET", ACADEMIC_CALENDAR_URL + "?" + query, null, null),
        listOf("data", CalendarSource::toCalendarEvent), List.of());
  }

  /** 대학원 사이트·연·월 > 한 달 학사일정 (사이트 이름 형태 검증 후 요청, 틀리면 INVALID_INPUT) */
  List<CalendarEvent> graduateCalendar(String siteId, int year, int month) {
    if (siteId == null || !GRADUATE_SITE_ID_PATTERN.matcher(siteId).matches()) {
      throw new SejongAuthException(SejongAuthErrorCode.INVALID_INPUT,
          "Graduate site id must be lowercase letters, digits and hyphens");
    }
    String query = pairsOf(GRADUATE_CALENDAR_QUERY_KEYS, String.valueOf(year), String.valueOf(month), siteId);
    return fetcher.fetch(cacheKeyOf("graduateCalendar", siteId, String.valueOf(year), String.valueOf(month)),
        new PublicDataRequest("GET", GRADUATE_CALENDAR_URL.replace("{siteId}", siteId) + "?" + query, null, null),
        CalendarSource::toGraduateCalendarEvents, List.of());
  }

  private static CalendarEvent toCalendarEvent(JsonNode row) {
    return CalendarEvent.builder().year(text(row, "scmYear")).month(text(row, "scmMonth")).categoryCode(text(row, "collDiv"))
        .categoryName(text(row, "collDivNm")).title(text(row, "scmSubject")).titleEn(text(row, "scmSubjectEng"))
        .startDate(text(row, "frDt")).endDate(text(row, "toDt"))
        .extra(extraOf(row, Set.of("scmYear", "scmMonth", "collDiv", "collDivNm", "scmSubject", "scmSubjectEng", "frDt", "toDt")))
        .build();
  }

  /** 대학원 달력: success 거짓·없음은 실패, items 는 JSON 문자열이라 한 번 더 해석 (문자열도 배열도 아니면 실패) */
  private static List<CalendarEvent> toGraduateCalendarEvents(JsonNode root) {
    if (!"true".equalsIgnoreCase(text(root, "success"))) {
      throw new IllegalStateException("graduate calendar did not report success");
    }
    JsonNode items = root.get("items");
    try {
      if (items != null && items.isTextual()) {
        items = JsonMapperSupport.PLAIN_JSON_MAPPER.readTree(items.asText());
      }
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("graduate calendar items is not json", e);
    }
    if (items == null || !items.isArray()) {
      throw new IllegalStateException("expected array missing : items");
    }
    List<CalendarEvent> events = new ArrayList<>();
    for (JsonNode row : items) {
      events.add(toCalendarEvent(row));
    }
    return List.copyOf(events);
  }
}
