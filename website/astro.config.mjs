// @ts-check
import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

const REPOSITORY_URL = 'https://github.com/Cassiiopeia/pro-sejongpeia';
const SITE_URL = 'https://cassiiopeia.github.io';
// 프로젝트 Pages는 /<레포명>/ 아래에서 서빙되므로 base를 맞춰야 에셋 경로가 깨지지 않는다.
const BASE_PATH = '/pro-sejongpeia';
// 공유 미리보기 이미지: public/og.png (docs/assets/og.png 사본)
const OPEN_GRAPH_IMAGE_URL = `${SITE_URL}${BASE_PATH}/og.png`;

export default defineConfig({
  site: SITE_URL,
  base: BASE_PATH,
  integrations: [
    starlight({
      title: 'pro-sejongpeia',
      description:
        '세종대학교 통합 라이브러리: 포털 로그인(학번 인증)과 로그인 없이 쓰는 학교 데이터를 Java, Python, Node.js로 한 번에.',
      logo: { src: './src/assets/sejong-seal.png', alt: '세종대학교 교표' },
      favicon: '/sejong-seal.png',
      // 한국어가 루트(/), 영어는 /en/. 모든 페이지가 두 언어로 있다.
      defaultLocale: 'root',
      locales: {
        root: { label: '한국어', lang: 'ko' },
        en: { label: 'English', lang: 'en' },
      },
      social: [{ icon: 'github', label: 'GitHub', href: REPOSITORY_URL }],
      editLink: { baseUrl: `${REPOSITORY_URL}/edit/main/website/` },
      components: { PageTitle: './src/components/PageTitle.astro' },
      customCss: ['pretendard/dist/web/variable/pretendardvariable-dynamic-subset.css', './src/styles/custom.css'],
      head: [
        { tag: 'meta', attrs: { name: 'theme-color', content: '#C20F2F' } },
        { tag: 'meta', attrs: { property: 'og:image', content: OPEN_GRAPH_IMAGE_URL } },
        { tag: 'meta', attrs: { property: 'og:image:width', content: '1200' } },
        { tag: 'meta', attrs: { property: 'og:image:height', content: '630' } },
        { tag: 'meta', attrs: { name: 'twitter:card', content: 'summary_large_image' } },
        { tag: 'meta', attrs: { name: 'twitter:image', content: OPEN_GRAPH_IMAGE_URL } },
        { tag: 'link', attrs: { rel: 'apple-touch-icon', href: `${BASE_PATH}/sejong-seal.png` } },
      ],
      sidebar: [
        {
          label: '시작하기',
          translations: { en: 'Start' },
          items: ['start/introduction', 'start/quickstart'],
        },
        {
          label: '가이드',
          translations: { en: 'Guides' },
          items: [
            'guides/student-verification',
            'guides/department-lecture-search',
            'guides/academic-calendar',
            'guides/dormitory-cafeteria-bot',
          ],
        },
        {
          label: '학교 데이터 레퍼런스',
          translations: { en: 'School data reference' },
          items: [
            'data/overview',
            'data/codes',
            'data/lectures',
            'data/calendar',
            'data/dormitory',
            'data/cafeteria',
            'data/campus-places',
            'data/extracurricular',
            'data/curriculum',
          ],
        },
        {
          label: '인증 레퍼런스',
          translations: { en: 'Authentication reference' },
          items: [
            'auth/methods',
            'auth/result-fields',
            'auth/error-codes',
            'auth/configuration',
            'auth/raw-and-extra',
            'auth/business-periods',
          ],
        },
        {
          label: '이해하기',
          translations: { en: 'Understand' },
          items: [
            'understand/legacy-tls',
            'understand/firewall-user-agent',
            'understand/sso-flow',
            'understand/login-verdict',
            'understand/failure-isolation-cache',
            'understand/daily-live-check',
          ],
        },
        {
          label: '기여하기',
          translations: { en: 'Contribute' },
          items: ['contribute/add-data-source', 'contribute/structure', 'contribute/issues-and-prs'],
        },
        {
          label: '더 보기',
          translations: { en: 'More' },
          items: ['api-stability', 'faq'],
        },
      ],
    }),
  ],
});
