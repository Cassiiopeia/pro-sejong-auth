// 캠퍼스 지도의 장소 (요청 조립과 행 > 모델 변환)
import { pairsOf, type PublicDataFetcher } from './fetcher.js';
import type { CampusPlace } from './models.js';
import { extraOf, rowsOf, toNumber, toText } from './rowReader.js';

// 주소·요청 키·고정 값의 원본은 spec/contract.json 의 publicData.campusMap
export const CAMPUS_MAP_URL = 'https://www.sejong.ac.kr/kor/intro/campus-map.do';
export const CAMPUS_MAP_QUERY_KEYS = ['mode', 'placeType'];
export const CAMPUS_PLACE_LIST_MODE = 'getPlaceUserList';
const CAMPUS_PLACE_USED_KEYS = ['id', 'placeName', 'placeNameEn', 'placeType', 'welfareCategory', 'latitude', 'longitude', 'placeInfo', 'telNum', 'description'];

// 캠퍼스 지도 장소
export class CampusSource {
  constructor(private readonly fetcher: PublicDataFetcher) {}

  // 장소 종류 > 장소 목록 (비우면 전체)
  campusPlaces(placeType = ''): Promise<CampusPlace[]> {
    const url = `${CAMPUS_MAP_URL}?${pairsOf(CAMPUS_MAP_QUERY_KEYS, CAMPUS_PLACE_LIST_MODE, placeType)}`;
    return this.fetcher.fetchCached('campusPlaces', [placeType], () => [], () => this.fetcher.requestJson('GET', url), (body) =>
      rowsOf(body, 'items').map((row) => ({
        id: toText(row['id']), name: toText(row.placeName), nameEn: toText(row.placeNameEn), placeType: toText(row.placeType),
        category: toText(row.welfareCategory), latitude: toNumber(row.latitude), longitude: toNumber(row.longitude), info: toText(row.placeInfo), phone: toText(row.telNum),
        description: toText(row.description), extra: extraOf(row, CAMPUS_PLACE_USED_KEYS),
      })));
  }
}
