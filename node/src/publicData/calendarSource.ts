// 학사일정: 학부·대학원 홈페이지 달력, 대학원 사이트별 달력 (요청 조립과 행 > 모델 변환)
import { ErrorCode, SejongAuthError } from '../common/errors.js';
import { pairsOf, type PublicDataFetcher } from './fetcher.js';
import type { CalendarEvent } from './models.js';
import type { CalendarQuery, GraduateCalendarQuery } from './queries.js';
import { asRows, extraOf, rowsOf, toText, type SchoolRow } from './rowReader.js';

// 주소·요청 키·고정 값의 원본은 spec/contract.json 의 publicData.calendar·graduateCalendar
export const ACADEMIC_CALENDAR_URL = 'https://www.sejong.ac.kr/kor/academics/academic-calendar.do';
export const CALENDAR_QUERY_KEYS = ['mode', 'start', 'end', 'collDiv'];
export const CALENDAR_MODE = 'getCalendarData';
export const DEFAULT_CALENDAR_CATEGORY_CODE = '0001'; // 학부 일정
// 대학원 사이트별 학사일정: {siteId} 는 graduate, gsi, mba 같은 사이트 식별자 (코드표 없이 호출자가 전달)
export const GRADUATE_CALENDAR_URL = 'https://{siteId}.sejong.ac.kr/_custom/sejong/_app/calendar-graduate/proc/getCalendarData.jsp';
export const GRADUATE_CALENDAR_QUERY_KEYS = ['year', 'month', 'siteId'];
// 사이트 식별자 허용 형태 (호스트 이름에 들어가므로 소문자·숫자·하이픈만)
export const GRADUATE_SITE_ID_PATTERN_SOURCE = '[a-z0-9][a-z0-9-]{0,30}';
const GRADUATE_SITE_ID_PATTERN = new RegExp(`^${GRADUATE_SITE_ID_PATTERN_SOURCE}$`);

const CALENDAR_EVENT_USED_KEYS = ['scmYear', 'scmMonth', 'collDiv', 'collDivNm', 'scmSubject', 'scmSubjectEng', 'frDt', 'toDt'];

// 학사일정 행 > 일정 (홈페이지와 대학원 사이트 공통)
const toCalendarEvent = (row: SchoolRow): CalendarEvent => ({
  year: toText(row.scmYear), month: toText(row.scmMonth), categoryCode: toText(row['collDiv']), categoryName: toText(row.collDivNm),
  title: toText(row.scmSubject), titleEn: toText(row.scmSubjectEng), startDate: toText(row['frDt']), endDate: toText(row['toDt']),
  extra: extraOf(row, CALENDAR_EVENT_USED_KEYS),
});

// 홈페이지 학사일정과 대학원 사이트별 학사일정
export class CalendarSource {
  constructor(private readonly fetcher: PublicDataFetcher) {}

  // 기간·분류 > 홈페이지 학사일정
  calendar({ start, end, categoryCode = DEFAULT_CALENDAR_CATEGORY_CODE }: CalendarQuery): Promise<CalendarEvent[]> {
    const url = `${ACADEMIC_CALENDAR_URL}?${pairsOf(CALENDAR_QUERY_KEYS, CALENDAR_MODE, start, end, categoryCode)}`;
    return this.fetcher.fetchCached('calendar', [start, end, categoryCode], () => [], () => this.fetcher.requestJson('GET', url), (body) =>
      rowsOf(body, 'data').map(toCalendarEvent));
  }

  // 대학원 사이트·연·월 > 한 달 학사일정 (사이트 이름 형태 검증 후 요청, 틀리면 INVALID_INPUT)
  graduateCalendar({ siteId, year, month }: GraduateCalendarQuery): Promise<CalendarEvent[]> {
    if (!GRADUATE_SITE_ID_PATTERN.test(String(siteId ?? ''))) {
      throw new SejongAuthError(ErrorCode.INVALID_INPUT, 'Graduate site id must be lowercase letters, digits and hyphens.');
    }
    const url = `${GRADUATE_CALENDAR_URL.replace('{siteId}', siteId)}?${pairsOf(GRADUATE_CALENDAR_QUERY_KEYS, String(year), String(month), siteId)}`;
    return this.fetcher.fetchCached('graduateCalendar', [siteId, String(year), String(month)], () => [], () => this.fetcher.requestJson('GET', url), (body) => {
      // success 거짓·없음은 실패, items 는 JSON 문자열이라 한 번 더 해석
      if (toText(body.success).toLowerCase() !== 'true') throw new Error('graduate calendar did not report success');
      const items = typeof body.items === 'string' ? JSON.parse(body.items) : body.items;
      return asRows(items).map(toCalendarEvent);
    });
  }
}
