// @ts-check
import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';
import { BASE_PATH, REPOSITORY_URL, SIDEBAR, SITE_DESCRIPTION_KO, SITE_TITLE, SITE_URL } from './site.config.mjs';

// 공유 미리보기 이미지: public/og.png (docs/assets/og.png 사본)
const OPEN_GRAPH_IMAGE_URL = `${SITE_URL}${BASE_PATH}/og.png`;

export default defineConfig({
  site: SITE_URL,
  base: BASE_PATH,
  integrations: [
    starlight({
      title: SITE_TITLE,
      description: SITE_DESCRIPTION_KO,
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
        // 코딩 에이전트용 문서 목차 (scripts/generate-llms.mjs 가 빌드 때 생성)
        { tag: 'link', attrs: { rel: 'alternate', type: 'text/plain', href: `${BASE_PATH}/llms.txt`, title: 'llms.txt' } },
      ],
      sidebar: SIDEBAR,
    }),
  ],
});
