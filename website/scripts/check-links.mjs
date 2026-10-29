// 빌드 결과(dist) 내부 링크 점검: href/src 가 dist 안의 파일로 이어지는지, #앵커가 대상 페이지에 있는지 확인
import { readdirSync, readFileSync, existsSync, statSync } from 'node:fs';
import { join, posix } from 'node:path';
import { fileURLToPath } from 'node:url';

const DIST_DIRECTORY = fileURLToPath(new URL('../dist/', import.meta.url));
const BASE_PATH = '/pro-sejongpeia';

function listHtmlFiles(directory) {
  return readdirSync(directory).flatMap((entryName) => {
    const entryPath = join(directory, entryName);
    if (statSync(entryPath).isDirectory()) return listHtmlFiles(entryPath);
    return entryPath.endsWith('.html') ? [entryPath] : [];
  });
}

// 공개 주소 경로(/pro-sejongpeia/x/) > dist 안의 실제 파일
function resolveDistFile(urlPath) {
  const relativePath = decodeURIComponent(urlPath.slice(BASE_PATH.length)).replace(/^\//, '');
  const candidates = [relativePath, join(relativePath, 'index.html'), `${relativePath}.html`];
  return candidates.map((candidate) => join(DIST_DIRECTORY, candidate)).find((candidate) => existsSync(candidate) && statSync(candidate).isFile());
}

const anchorCache = new Map();
function collectAnchors(htmlFile) {
  if (!anchorCache.has(htmlFile)) {
    const html = readFileSync(htmlFile, 'utf8');
    anchorCache.set(htmlFile, new Set([...html.matchAll(/\sid="([^"]+)"/g)].map((match) => match[1])));
  }
  return anchorCache.get(htmlFile);
}

const problems = [];
const htmlFiles = listHtmlFiles(DIST_DIRECTORY);
for (const htmlFile of htmlFiles) {
  const pageUrlPath = BASE_PATH + '/' + htmlFile.slice(DIST_DIRECTORY.length).replace(/index\.html$/, '');
  const html = readFileSync(htmlFile, 'utf8');
  for (const match of html.matchAll(/\s(?:href|src)="([^"]+)"/g)) {
    const reference = match[1];
    if (/^(https?:|mailto:|data:|tel:|javascript:)/.test(reference) || reference.startsWith('//')) continue;
    const [pathPart, anchor] = reference.split('#');
    const targetUrlPath = pathPart === '' ? pageUrlPath : pathPart.startsWith('/') ? pathPart : posix.join(pageUrlPath, pathPart);
    if (!targetUrlPath.startsWith(BASE_PATH)) {
      problems.push(`${pageUrlPath}: ${reference} (outside base ${BASE_PATH})`);
      continue;
    }
    const targetFile = resolveDistFile(targetUrlPath);
    if (!targetFile) {
      problems.push(`${pageUrlPath}: ${reference} (target not found)`);
      continue;
    }
    if (anchor && targetFile.endsWith('.html') && !collectAnchors(targetFile).has(decodeURIComponent(anchor))) {
      problems.push(`${pageUrlPath}: ${reference} (anchor not found)`);
    }
  }
}

console.log(`checked ${htmlFiles.length} pages`);
if (problems.length > 0) {
  console.error(`${problems.length} broken references:\n${[...new Set(problems)].join('\n')}`);
  process.exit(1);
}
