package kr.suhsaechan.sejongpeia.publicdata;

import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRequestSupport.cacheKeyOf;
import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRequestSupport.pairsOf;
import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRowReader.extraOf;
import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRowReader.listOf;
import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRowReader.requireArray;
import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRowReader.text;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import kr.suhsaechan.sejongpeia.common.exception.SejongAuthErrorCode;
import kr.suhsaechan.sejongpeia.common.exception.SejongAuthException;
import kr.suhsaechan.sejongpeia.publicdata.model.CafeteriaMenuItem;
import kr.suhsaechan.sejongpeia.publicdata.model.CafeteriaPlace;

/**
 * 학식: 식당 한 곳의 메뉴, 식당 목록 (식당 목록 API가 없어 번호별 메뉴 응답에서 모음)
 */
final class CafeteriaSource {

  // 주소·요청 키·고정 값의 원본은 spec/contract.json 의 publicData.cafeteria
  static final String CAFETERIA_URL = "https://www.sejong.ac.kr/kor/unilife/cafeteria-info.do";
  static final List<String> CAFETERIA_QUERY_KEYS = List.of("mode", "placeId");
  static final String CAFETERIA_MENU_MODE = "getMenuList";
  // 학교에 식당 목록 API가 없어 번호 1부터 차례로 조회해 식당을 모음 (학교 화면도 번호를 스크립트에 고정해 씀, spec/PUBLIC_DATA.md)
  // 식당 번호는 1부터 호출자가 정한 상한까지 조회 (기본 10, 최대 50: 한 번 갱신의 요청 수를 제한)
  static final int CAFETERIA_DEFAULT_MAX_PLACE_ID = 10;
  static final int CAFETERIA_LIMIT_MAX_PLACE_ID = 50;
  // 메뉴 응답 행의 알려진 필드 (식당 extra 에는 이 밖의 필드만)
  private static final Set<String> CAFETERIA_MENU_FIELDS = Set.of("placeId", "placeName", "buildingName", "location",
      "menuDate", "day", "mealTypeId", "mealName", "menuName", "price");

  private final PublicDataFetcher fetcher;

  CafeteriaSource(PublicDataFetcher fetcher) {
    this.fetcher = fetcher;
  }

  /** 식당 번호 > 메뉴 목록 */
  List<CafeteriaMenuItem> cafeteriaMenu(String placeId) {
    return fetcher.fetch(cacheKeyOf("cafeteriaMenu", placeId), menuRequestOf(placeId),
        listOf("items", CafeteriaSource::toCafeteriaMenuItem), List.of());
  }

  /** 번호 1~maxPlaceId 메뉴 응답 > 식당 목록 (범위 검증 후 요청, 틀리면 INVALID_INPUT) */
  List<CafeteriaPlace> cafeteriaPlaces(int maxPlaceId) {
    if (maxPlaceId < 1 || maxPlaceId > CAFETERIA_LIMIT_MAX_PLACE_ID) {
      throw new SejongAuthException(SejongAuthErrorCode.INVALID_INPUT,
          "Max place id must be an integer from 1 to " + CAFETERIA_LIMIT_MAX_PLACE_ID);
    }
    List<String> probePlaceIds = IntStream.rangeClosed(1, maxPlaceId).mapToObj(String::valueOf).toList();
    List<PublicDataRequest> requests = probePlaceIds.stream().map(CafeteriaSource::menuRequestOf).toList();
    return fetcher.fetchInSequence(cacheKeyOf("cafeteriaPlaces", String.valueOf(maxPlaceId)), requests,
        responses -> toCafeteriaPlaces(probePlaceIds, responses), List.of());
  }

  private static PublicDataRequest menuRequestOf(String placeId) {
    String query = pairsOf(CAFETERIA_QUERY_KEYS, CAFETERIA_MENU_MODE, placeId);
    return new PublicDataRequest("GET", CAFETERIA_URL + "?" + query, null, null);
  }

  private static CafeteriaMenuItem toCafeteriaMenuItem(JsonNode row) {
    return CafeteriaMenuItem.builder().buildingName(text(row, "buildingName")).location(text(row, "location"))
        .placeId(text(row, "placeId")).placeName(text(row, "placeName")).date(text(row, "menuDate")).day(text(row, "day"))
        .mealTypeId(text(row, "mealTypeId")).mealName(text(row, "mealName")).menuName(text(row, "menuName"))
        .price(text(row, "price")).build();
  }

  /** 번호별 메뉴 응답 > 식당 목록 (메뉴가 있는 번호만, 첫 행의 식당 정보, 같은 식당 번호는 한 번만) */
  private static List<CafeteriaPlace> toCafeteriaPlaces(List<String> requestedPlaceIds, List<JsonNode> responses) {
    Map<String, CafeteriaPlace> placeById = new LinkedHashMap<>();
    for (int index = 0; index < responses.size(); index++) {
      JsonNode firstRow = firstObjectRow(requireArray(responses.get(index), "items"));
      if (firstRow == null) {
        continue;
      }
      String placeId = text(firstRow, "placeId").isEmpty() ? requestedPlaceIds.get(index) : text(firstRow, "placeId");
      placeById.putIfAbsent(placeId, CafeteriaPlace.builder().placeId(placeId).placeName(text(firstRow, "placeName"))
          .buildingName(text(firstRow, "buildingName")).location(text(firstRow, "location"))
          .extra(extraOf(firstRow, CAFETERIA_MENU_FIELDS)).build());
    }
    return List.copyOf(placeById.values());
  }

  private static JsonNode firstObjectRow(JsonNode rows) {
    for (JsonNode row : rows) {
      if (row.isObject()) {
        return row;
      }
    }
    return null;
  }
}
