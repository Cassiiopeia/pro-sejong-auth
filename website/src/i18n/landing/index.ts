// 랜딩 페이지 문구와 공통 상수: 컴포넌트는 언어별 문구를 여기서만 읽는다 (단일 원본)
// 섹션별 문구는 같은 폴더의 *Copy.ts, 모양은 types.ts, 상수는 constants.ts
// 브라우저 스크립트(LiveDemo)는 문구 묶음이 번들에 딸려 오지 않도록 constants.ts 를 직접 임포트
import { DEMO_COPY } from './demoCopy';
import { FEATURES_COPY } from './featuresCopy';
import { HERO_COPY } from './heroCopy';
import type { LandingCopy, LandingLocale } from './types';

export * from './constants';
export type { LandingCopy, LandingLocale } from './types';

export const LANDING_COPY: Record<LandingLocale, LandingCopy> = {
  ko: { ...HERO_COPY.ko, ...DEMO_COPY.ko, ...FEATURES_COPY.ko },
  en: { ...HERO_COPY.en, ...DEMO_COPY.en, ...FEATURES_COPY.en },
};
