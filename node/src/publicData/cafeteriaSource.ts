// 학식: 식당 한 곳의 메뉴, 식당 목록 (식당 목록 API가 없어 번호별 메뉴 응답에서 모음)
import { ErrorCode, SejongAuthError } from '../common/errors.js';
import { pairsOf, type PublicDataFetcher } from './fetcher.js';
import type { CafeteriaMenuItem, CafeteriaPlace } from './models.js';
import type { CafeteriaPlacesQuery } from './queries.js';
import { extraOf, rowsOf, toText, type JsonObject } from './rowReader.js';

// 주소·요청 키·고정 값의 원본은 spec/contract.json 의 publicData.cafeteria
export const CAFETERIA_URL = 'https://www.sejong.ac.kr/kor/unilife/cafeteria-info.do';
export const CAFETERIA_QUERY_KEYS = ['mode', 'placeId'];
export const CAFETERIA_MENU_MODE = 'getMenuList';
// 학교에 식당 목록 API가 없어 번호 1부터 차례로 조회해 식당을 모음 (학교 화면도 번호를 스크립트에 고정해 씀, spec/PUBLIC_DATA.md)
// 식당 번호는 1부터 호출자가 정한 상한까지 차례로 조회 (기본 10, 최대 50: 한 번 갱신의 요청 수를 제한)
export const CAFETERIA_DEFAULT_MAX_PLACE_ID = 10;
export const CAFETERIA_LIMIT_MAX_PLACE_ID = 50;
// 메뉴 응답 행의 알려진 필드 (식당 extra 에는 이 밖의 필드만)
const CAFETERIA_MENU_USED_KEYS = ['placeId', 'placeName', 'buildingName', 'location', 'menuDate', 'day', 'mealTypeId', 'mealName', 'menuName', 'price'];

const menuUrlOf = (placeId: string): string => `${CAFETERIA_URL}?${pairsOf(CAFETERIA_QUERY_KEYS, CAFETERIA_MENU_MODE, placeId)}`;

// 학식 메뉴와 식당 목록
export class CafeteriaSource {
  constructor(private readonly fetcher: PublicDataFetcher) {}

  // 식당 번호 > 메뉴 목록
  cafeteriaMenu(placeId: string | number): Promise<CafeteriaMenuItem[]> {
    const url = menuUrlOf(String(placeId));
    return this.fetcher.fetchCached('cafeteriaMenu', [String(placeId)], () => [], () => this.fetcher.requestJson('GET', url), (body) =>
      rowsOf(body, 'items').map((row) => ({
        buildingName: toText(row.buildingName), location: toText(row.location), placeId: toText(row.placeId), placeName: toText(row.placeName),
        date: toText(row.menuDate), day: toText(row.day), mealTypeId: toText(row.mealTypeId), mealName: toText(row.mealName),
        menuName: toText(row.menuName), price: toText(row.price),
      })));
  }

  // 번호 1~maxPlaceId 메뉴 응답 > 식당 목록 (범위·정수 검증 후 요청, 틀리면 INVALID_INPUT)
  cafeteriaPlaces({ maxPlaceId = CAFETERIA_DEFAULT_MAX_PLACE_ID }: CafeteriaPlacesQuery = {}): Promise<CafeteriaPlace[]> {
    if (!Number.isInteger(maxPlaceId) || maxPlaceId < 1 || maxPlaceId > CAFETERIA_LIMIT_MAX_PLACE_ID) {
      throw new SejongAuthError(ErrorCode.INVALID_INPUT, `Max place id must be an integer from 1 to ${CAFETERIA_LIMIT_MAX_PLACE_ID}.`);
    }
    const probePlaceIds = Array.from({ length: maxPlaceId }, (_, index) => String(index + 1));
    const requestAllPlaceIds = async (): Promise<JsonObject> => {
      const responses: JsonObject[] = [];
      for (const placeId of probePlaceIds) { // 순서대로 한 건씩, 병렬 요청 없음
        responses.push(await this.fetcher.requestJson('GET', menuUrlOf(placeId)));
      }
      return { responses };
    };
    return this.fetcher.fetchCached('cafeteriaPlaces', [String(maxPlaceId)], () => [], requestAllPlaceIds, (body) => {
      // 번호 순서대로 첫 행의 식당 정보, 같은 식당 번호는 한 번만
      const placeById = new Map<string, CafeteriaPlace>();
      (body.responses as JsonObject[]).forEach((response, index) => {
        const firstRow = rowsOf(response, 'items').find((row) => Object.keys(row).length > 0);
        if (!firstRow) return;
        const placeId = toText(firstRow.placeId) || probePlaceIds[index];
        if (placeById.has(placeId)) return;
        placeById.set(placeId, {
          placeId, placeName: toText(firstRow.placeName), buildingName: toText(firstRow.buildingName), location: toText(firstRow.location),
          extra: extraOf(firstRow, CAFETERIA_MENU_USED_KEYS),
        });
      });
      return [...placeById.values()];
    });
  }
}
