// @ts-check
import { readFileSync } from 'node:fs';
import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';
import { BASE_PATH, REPOSITORY_URL, SIDEBAR, SITE_DESCRIPTION_KO, SITE_TITLE, SITE_URL } from './site.config.mjs';

// 공유 미리보기 이미지: public/og.png (원본 docs/assets/og.png, scripts/sync-brand-assets.mjs 가 빌드 때 복사)
const OPEN_GRAPH_IMAGE_URL = `${SITE_URL}${BASE_PATH}/og.png`;

// 브라우저 테마 색 (meta theme-color): 브랜드 토큰 --sejong-crimson 을 src/styles/custom.css 에서 읽음 (색 정의는 그 파일 한 곳)
const BRAND_COLOR_HEX = readFileSync(new URL('./src/styles/custom.css', import.meta.url), 'utf8')
  .match(/--sejong-crimson:\s*(#[0-9a-fA-F]{6})\s*;/)?.[1]
  .toUpperCase();
if (!BRAND_COLOR_HEX) throw new Error('--sejong-crimson (6자리 16진 색) not found in src/styles/custom.css');

// 표를 .table-scroll 상자로 감싸는 스크립트: 표는 본문 폭을 채우고, 좁은 화면에서는 상자만 가로로 스크롤 (src/styles/custom.css)
// Astro 7 의 기본 마크다운 처리기는 rehype 플러그인을 쓰려면 별도 패키지가 필요해서, 의존성을 늘리지 않고 브라우저에서 감싼다
const WRAP_TABLES_SCRIPT = `
document.addEventListener('DOMContentLoaded', () => {
  for (const table of document.querySelectorAll('.sl-markdown-content table')) {
    if (table.closest('.not-content') || table.parentElement.classList.contains('table-scroll')) continue;
    const scrollBox = document.createElement('div');
    scrollBox.className = 'table-scroll';
    table.replaceWith(scrollBox);
    scrollBox.append(table);
  }
});
`;

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
        { tag: 'meta', attrs: { name: 'theme-color', content: BRAND_COLOR_HEX } },
        { tag: 'script', content: WRAP_TABLES_SCRIPT },
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
