package kr.suhsaechan.sejongpeia.publicdata;

import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRequestSupport.cacheKeyOf;
import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRequestSupport.nullToEmpty;
import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRequestSupport.pairsOf;
import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRowReader.listOf;
import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRowReader.text;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import kr.suhsaechan.sejongpeia.publicdata.model.ExtracurricularEvent;

/**
 * 비교과 프로그램 일정 (요청 조립과 행 > 모델 변환)
 */
final class ExtracurricularSource {

  // 주소·요청 키·고정 값의 원본은 spec/contract.json 의 publicData.extracurricular
  static final String EXTRACURRICULAR_EVENTS_URL = "https://do.sejong.ac.kr/ko/process/schedule/getEvents";
  static final List<String> EXTRACURRICULAR_FORM_KEYS = List.of("cid", "category", "start", "end", "start_year", "end_year");
  static final String EXTRACURRICULAR_CID = "eco";
  private static final int YEAR_TEXT_LENGTH = 4;

  private final PublicDataFetcher fetcher;

  ExtracurricularSource(PublicDataFetcher fetcher) {
    this.fetcher = fetcher;
  }

  /** 기간·분류 > 비교과 일정 목록 */
  List<ExtracurricularEvent> extracurricularEvents(String startDate, String endDate, String category) {
    String form = pairsOf(EXTRACURRICULAR_FORM_KEYS, EXTRACURRICULAR_CID, category, startDate, endDate,
        yearOf(startDate), yearOf(endDate));
    return fetcher.fetch(cacheKeyOf("extracurricularEvents", startDate, endDate, category),
        new PublicDataRequest("POST", EXTRACURRICULAR_EVENTS_URL, form, OkHttpPublicDataHttpCaller.FORM_CONTENT_TYPE),
        listOf("events", ExtracurricularSource::toExtracurricularEvent), List.of());
  }

  private static String yearOf(String date) {
    String text = nullToEmpty(date);
    return text.length() >= YEAR_TEXT_LENGTH ? text.substring(0, YEAR_TEXT_LENGTH) : text;
  }

  private static ExtracurricularEvent toExtracurricularEvent(JsonNode row) {
    JsonNode detail = row.path("data");
    return ExtracurricularEvent.builder().id(text(row, "id")).title(text(row, "title")).start(text(row, "start"))
        .end(text(row, "end")).categoryTitle(text(row, "category_title")).url(text(detail, "url"))
        .description(text(detail, "description")).location(text(detail, "location")).build();
  }
}
