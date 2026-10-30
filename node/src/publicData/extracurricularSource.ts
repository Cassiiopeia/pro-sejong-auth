// 비교과 프로그램 일정 (요청 조립과 행 > 모델 변환)
import { pairsOf, type PublicDataFetcher } from './fetcher.js';
import type { ExtracurricularEvent } from './models.js';
import type { ExtracurricularEventsQuery } from './queries.js';
import { asObject, rowsOf, toText } from './rowReader.js';

// 주소·요청 키·고정 값의 원본은 spec/contract.json 의 publicData.extracurricular
export const EXTRACURRICULAR_URL = 'https://do.sejong.ac.kr/ko/process/schedule/getEvents';
export const EXTRACURRICULAR_FORM_KEYS = ['cid', 'category', 'start', 'end', 'start_year', 'end_year'];
export const EXTRACURRICULAR_CID = 'eco';

const yearOf = (date: string): string => date.slice(0, 4);

// 비교과 프로그램 일정
export class ExtracurricularSource {
  constructor(private readonly fetcher: PublicDataFetcher) {}

  // 기간·분류 > 비교과 일정 목록
  extracurricularEvents({ start, end, category = '' }: ExtracurricularEventsQuery): Promise<ExtracurricularEvent[]> {
    const form = pairsOf(EXTRACURRICULAR_FORM_KEYS, EXTRACURRICULAR_CID, category, start, end, yearOf(start), yearOf(end));
    return this.fetcher.fetchCached('extracurricularEvents', [start, end, category], () => [], () => this.fetcher.requestJson('POST', EXTRACURRICULAR_URL, { form }), (body) =>
      rowsOf(body, 'events').map((row) => {
        const detail = asObject(row.data);
        return {
          id: toText(row['id']), title: toText(row.title), start: toText(row.start), end: toText(row.end), categoryTitle: toText(row.category_title),
          url: toText(detail.url), description: toText(detail.description), location: toText(detail.location),
        };
      }));
  }
}
