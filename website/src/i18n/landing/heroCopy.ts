// 랜딩 문구: 첫 화면, 첫 호출 예시, 마무리 (ko/en)

import { SCHOOL_DATA_SNAPSHOT } from './constants';
import type { LandingCopy, LandingLocale } from './types';

type HeroSectionsCopy = Pick<LandingCopy, 'hero' | 'firstCall' | 'closing'>;

export const HERO_COPY: Record<LandingLocale, HeroSectionsCopy> = {
  ko: {
    hero: {
      eyebrow: 'Pro Sejongpeia · 세종대 개발자들의 작은 유토피아',
      titleLine: '세종대 개발에 필요한',
      titleEmphasis: '모든 것을 한 번에',
      lead: '학번 인증부터 학과·강의·학사일정·학식까지. 학교 공개 데이터 15가지를 Java · Python · Node.js에서 같은 메서드, 같은 결과로 받습니다.',
      tagline: '가볍고 빠르게 · 요청은 아끼고 · 비밀번호는 저장하지 않고 · 매일 실제 포털로 점검',
      visualRowsComment: `${SCHOOL_DATA_SNAPSHOT.lectureRowCount}행`,
      tryDemo: '바로 체험하기',
      quickStart: '빠른 시작',
      github: 'GitHub',
    },
    firstCall: {
      title: '30초면 첫 호출',
      lead: '설치하고 두 줄이면 됩니다. 아래 코드를 그대로 붙여 넣으세요.',
      authLabel: '학번 인증',
      publicLabel: '로그인 없이 학과 코드 받기',
      note: '서버에서만 호출하세요. 학번과 비밀번호를 브라우저나 앱에서 직접 보내면 제3자 코드에 노출됩니다.',
      passwordPlaceholder: '비밀번호',
    },
    closing: {
      title: '더 알아보기',
      lead: '이 프로젝트는 세종대 학생이 만든 개인 오픈소스입니다. 하위 호환을 보장하지 않으니 버전을 고정해서 쓰세요.',
      docs: '문서 소개',
      stability: 'API 안정성',
      contribute: '기여하기',
      agents: 'AI 에이전트와 쓰기',
      disclaimer:
        '이 프로젝트는 세종대학교의 공식 제품이 아니며 세종대학교와 제휴 관계가 없습니다. 세종대학교 교표의 권리는 세종대학교에 있습니다.',
    },
  },
  en: {
    hero: {
      eyebrow: 'Pro Sejongpeia · A little utopia for Sejong developers',
      titleLine: 'Everything you need',
      titleEmphasis: 'to build on Sejong University',
      lead: 'From student login to departments, lectures, the academic calendar and cafeterias. 15 kinds of public school data, with the same methods and the same results in Java · Python · Node.js.',
      tagline: 'Fast and light · gentle on requests · never stores a password · checked against the real portal daily',
      visualRowsComment: `${SCHOOL_DATA_SNAPSHOT.lectureRowCount} rows`,
      tryDemo: 'Try it live',
      quickStart: 'Quick start',
      github: 'GitHub',
    },
    firstCall: {
      title: 'Your first call in 30 seconds',
      lead: 'Install it and write two lines. Paste the code below as it is.',
      authLabel: 'Student verification',
      publicLabel: 'Department codes without login',
      note: 'Call it from your server only. Sending a student ID and password straight from a browser or an app exposes them to third-party code.',
      passwordPlaceholder: 'password',
    },
    closing: {
      title: 'Read on',
      lead: 'This is a personal open-source project by a Sejong student. It does not guarantee backward compatibility, so pin the version.',
      docs: 'Introduction',
      stability: 'API stability',
      contribute: 'Contribute',
      agents: 'With coding agents',
      disclaimer:
        'This project is not an official product of Sejong University and is not affiliated with it. The Sejong University seal belongs to Sejong University.',
    },
  },
};
