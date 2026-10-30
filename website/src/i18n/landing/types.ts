// 랜딩 문구 타입: 언어 코드와 섹션별 문구 모양
export type LandingLocale = 'ko' | 'en';

interface FeatureCard {
  title: string;
  body: string;
  methods: string;
  link: string;
}
interface HardPartCard {
  title: string;
  body: string;
  link: string;
}
interface MatrixRow {
  feature: string;
  detail: string;
}

export interface LandingCopy {
  hero: { eyebrow: string; titleLine: string; titleEmphasis: string; lead: string; tryDemo: string; quickStart: string; github: string };
  firstCall: { title: string; lead: string; authLabel: string; publicLabel: string; note: string; passwordPlaceholder: string };
  demo: {
    title: string;
    lead: string;
    studentIdLabel: string;
    studentIdPlaceholder: string;
    passwordLabel: string;
    passwordPlaceholder: string;
    authTypeLabel: string;
    authIntegrated: string;
    authIntegratedDescription: string;
    authDhc: string;
    authDhcDescription: string;
    authSis: string;
    authSisDescription: string;
    submit: string;
    submitting: string;
    privacyBefore: string;
    privacyServerName: string;
    privacyAfter: string;
    resultTitle: string;
    maskToggle: string;
    tabTable: string;
    tabJson: string;
    resultEmpty: string;
    success: string;
    failure: string;
    classicTitle: string;
    rowLabels: Record<string, string>;
    inputRequired: string;
    networkError: string;
    timeoutError: string;
    errorMeanings: Record<string, string>;
  };
  features: { title: string; lead: string; cards: FeatureCard[] };
  matrix: { title: string; lead: string; featureHeader: string; supported: string; note: string; rows: MatrixRow[] };
  hardParts: { title: string; lead: string; cards: HardPartCard[] };
  closing: { title: string; lead: string; docs: string; stability: string; contribute: string; agents: string; disclaimer: string };
}
