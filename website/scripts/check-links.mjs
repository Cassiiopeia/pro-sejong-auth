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

// 에이전트용 파일: llms.txt 의 사이트 링크와 api.json 의 docs 링크가 빌드된 페이지로 이어지고, api.json 이 읽히는지 확인
const SITE_PREFIX = `https://cassiiopeia.github.io${BASE_PATH}`;
function checkSiteUrl(sourceName, url) {
  if (!url.startsWith(SITE_PREFIX)) return;
  const urlPath = decodeURIComponent(url.slice('https://cassiiopeia.github.io'.length)).split('#')[0];
  if (!resolveDistFile(urlPath)) problems.push(`${sourceName}: ${url} (target not found)`);
}
const llmsText = readFileSync(join(DIST_DIRECTORY, 'llms.txt'), 'utf8');
const llmsLinks = [...llmsText.matchAll(/\]\((https?:[^)\s]+)\)/g)].map((match) => match[1]);
llmsLinks.forEach((url) => checkSiteUrl('llms.txt', url));
const apiIndex = JSON.parse(readFileSync(join(DIST_DIRECTORY, 'api.json'), 'utf8'));
if (!Array.isArray(apiIndex.methods) || apiIndex.methods.length === 0) problems.push('api.json: methods is empty');
apiIndex.methods?.forEach((method) => Object.values(method.docs).forEach((url) => checkSiteUrl(`api.json ${method.id}`, url)));
for (const fullTextName of ['llms-full.txt', 'llms-full.en.txt']) {
  [...readFileSync(join(DIST_DIRECTORY, fullTextName), 'utf8').matchAll(/\]\((https?:[^)\s]+)\)/g)].forEach((match) => checkSiteUrl(fullTextName, match[1]));
}
console.log(`checked ${htmlFiles.length} pages, ${llmsLinks.length} llms.txt links, ${apiIndex.methods?.length ?? 0} api.json methods`);
if (problems.length > 0) {
  console.error(`${problems.length} broken references:\n${[...new Set(problems)].join('\n')}`);
  process.exit(1);
}
