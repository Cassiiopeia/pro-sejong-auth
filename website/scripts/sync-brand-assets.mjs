// 브랜드 이미지 복사 (npm run dev·build 의 pre 단계): docs/assets 의 원본을 사이트 폴더로 복사한다
// 원본은 docs/assets 한 곳 (README 도 같은 파일을 참조). 복사본은 .gitignore 대상이라 직접 고치거나 커밋하지 않는다
import { copyFileSync, existsSync, mkdirSync, readdirSync } from 'node:fs';
import { dirname, join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';

const WEBSITE_DIRECTORY = fileURLToPath(new URL('..', import.meta.url));
const REPOSITORY_DIRECTORY = join(WEBSITE_DIRECTORY, '..');
const BRAND_ASSET_DIRECTORY = join(REPOSITORY_DIRECTORY, 'docs/assets');

// 복사 대상: [원본 파일명, 사이트 안 복사 위치들]
// public/ 은 파비콘·공유 이미지(og)·랜딩 URL 용, src/assets/ 는 Starlight 로고(빌드 때 번들) 용
// 위치를 바꾸면 .gitignore 의 "문서 사이트 브랜드 이미지 복사본" 항목도 같이 바꾼다
const BRAND_ASSET_COPIES = [
  ['sejong-seal.png', ['public/sejong-seal.png', 'src/assets/sejong-seal.png']],
  ['og.png', ['public/og.png']],
];

// 기능 아이콘(docs/assets/icons/*.svg)은 폴더째 public/icons 로 복사
const ICON_DIRECTORY = join(BRAND_ASSET_DIRECTORY, 'icons');
for (const iconFileName of readdirSync(ICON_DIRECTORY).filter((name) => name.endsWith('.svg'))) {
  BRAND_ASSET_COPIES.push([join('icons', iconFileName), [join('public/icons', iconFileName)]]);
}

for (const [sourceFileName, targetPaths] of BRAND_ASSET_COPIES) {
  const sourcePath = join(BRAND_ASSET_DIRECTORY, sourceFileName);
  // 원본이 없으면 조용히 넘어가지 않고 빌드를 멈춘다 (이미지 없는 사이트 배포 방지)
  if (!existsSync(sourcePath)) {
    console.error(`[brand assets] missing source : ${relative(REPOSITORY_DIRECTORY, sourcePath)}`);
    process.exit(1);
  }
  for (const targetPath of targetPaths) {
    const absoluteTargetPath = join(WEBSITE_DIRECTORY, targetPath);
    mkdirSync(dirname(absoluteTargetPath), { recursive: true });
    copyFileSync(sourcePath, absoluteTargetPath);
    console.log(`[brand assets] ${relative(REPOSITORY_DIRECTORY, sourcePath)} > website/${targetPath}`);
  }
}
