package kr.suhsaechan.sejongpeia.publicdata;

import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRequestSupport.cacheKeyOf;
import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRequestSupport.pairsOf;
import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRowReader.integer;
import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRowReader.requireArray;
import static kr.suhsaechan.sejongpeia.publicdata.PublicDataRowReader.text;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import kr.suhsaechan.sejongpeia.publicdata.model.DormitoryMenuDay;
import kr.suhsaechan.sejongpeia.publicdata.model.DormitoryNotice;
import kr.suhsaechan.sejongpeia.publicdata.model.DormitoryNotices;

/**
 * 기숙사: 일주일 식단, 공지 (요청 조립과 행 > 모델 변환, 작성자 필드는 읽지 않음)
 */
final class DormitorySource {

  // 주소·요청 키·고정 값의 원본은 spec/contract.json 의 publicData.dormitory
  static final String DORMITORY_MENU_URL = "https://happydorm.sejong.ac.kr/food/indexFoodList.do";
  static final List<String> DORMITORY_MENU_FORM_KEYS = List.of("locgbn");
  static final String DORMITORY_NOTICE_URL = "https://happydorm.sejong.ac.kr/bbs/getBbsList.do";
  static final List<String> DORMITORY_NOTICE_FORM_KEYS = List.of("cPage", "rows", "bbs_locgbn", "bbs_id", "sType", "sWord");
  static final String DORMITORY_NOTICE_BOARD_ID = "notice";
  static final String DORMITORY_NOTICE_SEARCH_TYPE = "subject"; // 제목 검색
  static final String DEFAULT_DORMITORY_LOCATION = "SJ";
  private static final int DORMITORY_MENU_DAY_COUNT = 7;

  private final PublicDataFetcher fetcher;

  DormitorySource(PublicDataFetcher fetcher) {
    this.fetcher = fetcher;
  }

  /** 위치 코드 > 일주일 식단 7일 */
  List<DormitoryMenuDay> dormitoryMenu(String location) {
    return fetcher.fetch(cacheKeyOf("dormitoryMenu", location),
        new PublicDataRequest("POST", DORMITORY_MENU_URL, pairsOf(DORMITORY_MENU_FORM_KEYS, location),
            OkHttpPublicDataHttpCaller.FORM_CONTENT_TYPE),
        DormitorySource::toDormitoryMenu, List.of());
  }

  /** 쪽·건수·제목 검색어·위치 > 고정 공지와 일반 공지, 전체 건수 */
  DormitoryNotices dormitoryNotices(int page, int rows, String keyword, String location) {
    String form = pairsOf(DORMITORY_NOTICE_FORM_KEYS, String.valueOf(page), String.valueOf(rows), location,
        DORMITORY_NOTICE_BOARD_ID, DORMITORY_NOTICE_SEARCH_TYPE, keyword);
    DormitoryNotices emptyNotices = DormitoryNotices.builder().total(0).pinned(List.of()).items(List.of()).build();
    return fetcher.fetch(cacheKeyOf("dormitoryNotices", String.valueOf(page), String.valueOf(rows), keyword, location),
        new PublicDataRequest("POST", DORMITORY_NOTICE_URL, form, OkHttpPublicDataHttpCaller.FORM_CONTENT_TYPE),
        DormitorySource::toDormitoryNotices, emptyNotices);
  }

  /** 기숙사 식단: root[0] 의 fo_*1..7 > 7일 */
  private static List<DormitoryMenuDay> toDormitoryMenu(JsonNode root) {
    JsonNode row = firstRootRow(root);
    List<DormitoryMenuDay> days = new ArrayList<>();
    for (int dayNumber = 1; dayNumber <= DORMITORY_MENU_DAY_COUNT; dayNumber++) {
      days.add(DormitoryMenuDay.builder().date(text(row, "fo_date" + dayNumber)).breakfast(text(row, "fo_menu_mor" + dayNumber))
          .lunch(text(row, "fo_menu_lun" + dayNumber)).dinner(text(row, "fo_menu_eve" + dayNumber))
          .alternative(text(row, "fo_sub_menu" + dayNumber)).build());
    }
    return List.copyOf(days);
  }

  /** 기숙사 공지: 고정(topList) + 일반(list) + 전체 건수(totalCount[0].cnt), 작성자 제외 (없거나 비면 결과 없음: 빈 목록, 0) */
  private static DormitoryNotices toDormitoryNotices(JsonNode root) {
    JsonNode block = firstRootRow(root);
    List<DormitoryNotice> pinned = noticesOf(block.get("topList"));
    List<DormitoryNotice> items = noticesOf(block.get("list"));
    JsonNode totalRows = block.get("totalCount");
    boolean hasTotalRow = totalRows != null && totalRows.isArray() && totalRows.size() > 0;
    return DormitoryNotices.builder().total(hasTotalRow ? integer(totalRows.get(0), "cnt") : 0)
        .pinned(pinned).items(items).build();
  }

  private static List<DormitoryNotice> noticesOf(JsonNode rows) {
    List<DormitoryNotice> notices = new ArrayList<>();
    if (rows != null && rows.isArray()) {
      rows.forEach(row -> notices.add(toDormitoryNotice(row)));
    }
    return List.copyOf(notices);
  }

  private static DormitoryNotice toDormitoryNotice(JsonNode row) {
    return DormitoryNotice.builder().id(text(row, "seq")).title(text(row, "subject")).date(text(row, "regdate"))
        .views(integer(row, "visitcnt")).fileName(text(row, "file_name1")).build();
  }

  private static JsonNode firstRootRow(JsonNode root) {
    JsonNode rows = requireArray(root, "root");
    if (rows.size() == 0) {
      throw new IllegalStateException("expected row missing : root[0]");
    }
    return rows.get(0);
  }
}
