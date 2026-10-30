// 기숙사: 일주일 식단, 공지 (요청 조립과 행 > 모델 변환, 작성자 필드는 읽지 않음)
import { pairsOf, type PublicDataFetcher } from './fetcher.js';
import type { DormitoryMenuDay, DormitoryNotice, DormitoryNotices } from './models.js';
import type { DormitoryNoticesQuery } from './queries.js';
import { asRowsOrEmpty, rowsOf, toCount, toText, type SchoolRow } from './rowReader.js';

// 주소·요청 키·고정 값의 원본은 spec/contract.json 의 publicData.dormitory
export const DORMITORY_MENU_URL = 'https://happydorm.sejong.ac.kr/food/indexFoodList.do';
export const DORMITORY_MENU_FORM_KEYS = ['locgbn'];
export const DORMITORY_NOTICE_URL = 'https://happydorm.sejong.ac.kr/bbs/getBbsList.do';
export const DORMITORY_NOTICE_FORM_KEYS = ['cPage', 'rows', 'bbs_locgbn', 'bbs_id', 'sType', 'sWord'];
export const DORMITORY_NOTICE_BOARD_ID = 'notice';
export const DORMITORY_NOTICE_SEARCH_TYPE = 'subject'; // 제목 검색
export const DEFAULT_DORMITORY_LOCATION = 'SJ';
const DORMITORY_DAYS_PER_WEEK = 7;

const toNotice = (row: SchoolRow): DormitoryNotice => ({
  id: toText(row['seq']), title: toText(row.subject), date: toText(row.regdate), views: toCount(row.visitcnt), fileName: toText(row.file_name1),
});

// 기숙사 식단과 공지
export class DormitorySource {
  constructor(private readonly fetcher: PublicDataFetcher) {}

  // 위치 코드 > 일주일 식단 7일
  dormitoryMenu(location = DEFAULT_DORMITORY_LOCATION): Promise<DormitoryMenuDay[]> {
    const form = pairsOf(DORMITORY_MENU_FORM_KEYS, location);
    return this.fetcher.fetchCached('dormitoryMenu', [location], () => [], () => this.fetcher.requestJson('POST', DORMITORY_MENU_URL, { form }), (body) => {
      const row = rowsOf(body, 'root')[0];
      if (!row) throw new Error('empty root');
      return Array.from({ length: DORMITORY_DAYS_PER_WEEK }, (_, index) => {
        const dayNumber = index + 1;
        return {
          date: toText(row[`fo_date${dayNumber}`]), breakfast: toText(row[`fo_menu_mor${dayNumber}`]), lunch: toText(row[`fo_menu_lun${dayNumber}`]),
          dinner: toText(row[`fo_menu_eve${dayNumber}`]), alternative: toText(row[`fo_sub_menu${dayNumber}`]),
        };
      });
    });
  }

  // 쪽·건수·제목 검색어·위치 > 고정 공지와 일반 공지, 전체 건수
  dormitoryNotices({ page = 1, rows = 10, keyword = '', location = DEFAULT_DORMITORY_LOCATION }: DormitoryNoticesQuery = {}): Promise<DormitoryNotices> {
    const form = pairsOf(DORMITORY_NOTICE_FORM_KEYS, String(page), String(rows), location, DORMITORY_NOTICE_BOARD_ID, DORMITORY_NOTICE_SEARCH_TYPE, keyword);
    return this.fetcher.fetchCached('dormitoryNotices', [page, rows, keyword, location], () => ({ total: 0, pinned: [], items: [] }),
      () => this.fetcher.requestJson('POST', DORMITORY_NOTICE_URL, { form }), (body) => {
        // root 만 필수, 나머지가 없으면 결과 없음
        const block = rowsOf(body, 'root')[0] ?? {};
        return {
          total: toCount(asRowsOrEmpty(block.totalCount)[0]?.cnt),
          pinned: asRowsOrEmpty(block.topList).map(toNotice),
          items: asRowsOrEmpty(block.list).map(toNotice),
        };
      });
  }
}
