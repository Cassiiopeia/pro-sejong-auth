package kr.suhsaechan.sejongpeia.publicdata;

import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRequestSupport.cacheKeyOf;
import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRequestSupport.pairsOf;
import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRowReader.decimal;
import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRowReader.extraOf;
import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRowReader.listOf;
import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRowReader.text;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Set;
import kr.suhsaechan.sejongpeia.publicdata.model.CampusPlace;

/**
 * 캠퍼스 지도의 장소 (요청 조립과 행 > 모델 변환)
 */
final class CampusSource {

  // 주소·요청 키·고정 값의 원본은 spec/contract.json 의 publicData.campusMap
  static final String CAMPUS_MAP_URL = "https://www.sejong.ac.kr/kor/intro/campus-map.do";
  static final List<String> CAMPUS_MAP_QUERY_KEYS = List.of("mode", "placeType");
  static final String CAMPUS_PLACE_LIST_MODE = "getPlaceUserList";

  private final PublicDataFetcher fetcher;

  CampusSource(PublicDataFetcher fetcher) {
    this.fetcher = fetcher;
  }

  /** 장소 종류 > 장소 목록 (비우면 전체) */
  List<CampusPlace> campusPlaces(String placeType) {
    String query = pairsOf(CAMPUS_MAP_QUERY_KEYS, CAMPUS_PLACE_LIST_MODE, placeType);
    return fetcher.fetch(cacheKeyOf("campusPlaces", placeType),
        new PublicDataRequest("GET", CAMPUS_MAP_URL + "?" + query, null, null),
        listOf("items", CampusSource::toCampusPlace), List.of());
  }

  private static CampusPlace toCampusPlace(JsonNode row) {
    return CampusPlace.builder().id(text(row, "id")).name(text(row, "placeName")).nameEn(text(row, "placeNameEn"))
        .placeType(text(row, "placeType")).category(text(row, "welfareCategory"))
        .latitude(decimal(row, "latitude")).longitude(decimal(row, "longitude"))
        .info(text(row, "placeInfo")).phone(text(row, "telNum")).description(text(row, "description"))
        .extra(extraOf(row, Set.of("id", "placeName", "placeNameEn", "placeType", "welfareCategory", "latitude", "longitude",
            "placeInfo", "telNum", "description"))).build();
  }
}
