import type { LandingLocale } from './landing';

// 언어별 문서 주소: base(/pro-sejongpeia) + 영어면 /en + 상대 경로 (끝 슬래시 포함)
export function documentUrl(locale: LandingLocale, documentPath = ''): string {
  const basePath = import.meta.env.BASE_URL.replace(/\/$/, '');
  const localePath = locale === 'en' ? '/en' : '';
  return `${basePath}${localePath}/${documentPath}`;
}
