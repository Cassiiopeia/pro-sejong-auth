// 랜딩 문구: 이 라이브러리를 고르는 이유 4가지와 각 근거 (ko/en), README 의 '왜 이 라이브러리인가' 와 같은 내용

import { REPOSITORY_URL } from './constants';
import type { LandingCopy, LandingLocale } from './types';

type PillarsSectionCopy = Pick<LandingCopy, 'pillars'>;

const PUBLIC_DATA_SPEC_URL = `${REPOSITORY_URL}/blob/main/spec/PUBLIC_DATA.md`;
const CONTRACT_URL = `${REPOSITORY_URL}/blob/main/spec/contract.json`;

export const PILLARS_COPY: Record<LandingLocale, PillarsSectionCopy> = {
  ko: {
    pillars: {
      title: '왜 이 라이브러리인가',
      lead: '네 가지를 약속하고, 약속마다 코드나 테스트로 확인할 수 있는 근거를 붙였습니다.',
      cards: [
        {
          icon: '⚡',
          title: '빠르고 가볍게',
          points: [
            'Python, Node는 런타임 의존성 0개 (표준 라이브러리만 사용)',
            '같은 요청은 캐시에서 바로 답하고, 학교 서버를 다시 부르지 않습니다',
            '통합 인증은 포털 로그인 1번으로 DHC와 SIS를 모두 조회합니다',
          ],
          proofLabel: '동작 규칙 보기',
          proofLink: PUBLIC_DATA_SPEC_URL,
        },
        {
          icon: '🧠',
          title: '요청은 아끼고',
          points: [
            '결과를 메모리에 24시간 캐시, 같은 인자의 동시 호출은 요청 1번으로 합침',
            '호출 하나당 요청 1번 (학식 식당 목록만 번호 범위만큼 순서대로)',
            '실패는 60초 기억, 캐시는 최대 500개 (기본값)',
          ],
          proofLabel: '캐시와 실패 격리',
          proofLink: 'understand/failure-isolation-cache/',
        },
        {
          icon: '🔒',
          title: '안전하게',
          points: [
            '비밀번호를 저장하지 않고 오류 메시지에도 담지 않음',
            '첫 실패에서 멈춰 계정 잠금을 피하고, 잠긴 계정은 ACCOUNT_LOCKED로 알림',
            'SIS 부가 필드에서 세션값·접속 IP·주민번호·계좌·주소 등 민감 키를 항상 제외',
          ],
          proofLabel: '로그인 판정과 계정 보호',
          proofLink: 'understand/login-verdict/',
        },
        {
          icon: '✅',
          title: '믿을 수 있게',
          points: [
            '매일 09:00(KST) 세 언어가 실제 포털에 로그인하고, 실패하면 이슈가 자동으로 열림',
            '세 언어 결과를 spec/contract.json 한 곳으로 맞추고, 어긋나면 테스트 실패',
            '공개 메서드 19개 목록이 소스·문서와 같은지 CI가 검사',
          ],
          proofLabel: '매일 도는 실제 포털 점검',
          proofLink: 'understand/daily-live-check/',
        },
      ],
    },
  },
  en: {
    pillars: {
      title: 'Why this library',
      lead: 'Four promises, each with proof you can check in the code or the tests.',
      cards: [
        {
          icon: '⚡',
          title: 'Fast and light',
          points: [
            'Zero runtime dependencies in Python and Node (standard library only)',
            'Repeated requests are answered from the cache without calling the school server again',
            'Integrated verification is one portal login that serves both DHC and SIS',
          ],
          proofLabel: 'See the rules',
          proofLink: PUBLIC_DATA_SPEC_URL,
        },
        {
          icon: '🧠',
          title: 'Gentle on requests',
          points: [
            'Results cached in memory for 24 hours; simultaneous calls with the same arguments share one request',
            'One request per call (only the cafeteria list sends one per number in a range, one after the other)',
            'Failures remembered for 60 seconds, at most 500 cache entries (defaults)',
          ],
          proofLabel: 'Cache and failure isolation',
          proofLink: 'understand/failure-isolation-cache/',
        },
        {
          icon: '🔒',
          title: 'Safe',
          points: [
            'The password is never stored and never put into error messages',
            'Stops at the first failure so the account is not locked, and reports a locked account as ACCOUNT_LOCKED',
            'SIS extra fields always exclude session values, client IP, resident number, bank account, address and similar keys',
          ],
          proofLabel: 'Login verdict and account safety',
          proofLink: 'understand/login-verdict/',
        },
        {
          icon: '✅',
          title: 'Trustworthy',
          points: [
            'At 09:00 KST every day all three languages sign in to the live portal, and a failure opens an issue automatically',
            'Shared values live in spec/contract.json and the tests fail when the languages drift',
            'CI checks that the 19 public methods match the sources and the docs',
          ],
          proofLabel: 'The daily live check',
          proofLink: 'understand/daily-live-check/',
        },
      ],
    },
  },
};
