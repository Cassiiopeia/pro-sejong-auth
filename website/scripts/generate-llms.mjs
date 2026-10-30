// 코딩 에이전트용 파일 생성 (npm run build 의 prebuild 단계): public/ 에 git 에 올리지 않는 산출물 4개를 만든다
//   llms.txt            문서 목차 + 한 줄 설명 (llmstxt.org 형식, 한국어 목록 뒤에 영어 목록)
//   llms-full.txt       한국어 문서 전체를 이어 붙인 것
//   llms-full.en.txt    영어 문서 전체
//   api.json            공개 메서드 목록 (원본: spec/api-index.json, 모델 필드: python/.../models.py)
// 원본은 사이드바(site.config.mjs), 문서 본문(src/content/docs), spec/api-index.json 뿐이고 여기서는 손으로 쓴 사본을 두지 않는다
import { existsSync, mkdirSync, readFileSync, readdirSync, statSync, writeFileSync } from 'node:fs';
import { dirname, join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';
import {
  BASE_PATH, PACKAGE_NAME, REPOSITORY_URL, SIDEBAR, SITE_DESCRIPTION_EN, SITE_DESCRIPTION_KO, SITE_TITLE, SITE_URL,
} from '../site.config.mjs';

const WEBSITE_DIRECTORY = fileURLToPath(new URL('..', import.meta.url));
const REPOSITORY_DIRECTORY = join(WEBSITE_DIRECTORY, '..');
const DOCS_DIRECTORY = join(WEBSITE_DIRECTORY, 'src/content/docs');
const PUBLIC_DIRECTORY = join(WEBSITE_DIRECTORY, 'public');
const API_INDEX_PATH = join(REPOSITORY_DIRECTORY, 'spec/api-index.json');
const PYTHON_MODELS_PATH = join(REPOSITORY_DIRECTORY, 'python/src/sejongpeia/public_data/models.py');
const PYTHON_ERRORS_PATH = join(REPOSITORY_DIRECTORY, 'python/src/sejongpeia/common/errors.py');
const VERSION_FILE_PATH = join(REPOSITORY_DIRECTORY, 'version.yml');
const LOCALES = ['ko', 'en'];
const SITE_ROOT_URL = `${SITE_URL}${BASE_PATH}/`;
// 목차에는 없지만 llms-full 에 넣지 않는 문서 (랜딩 페이지는 컴포넌트뿐이라 본문이 없음)
const LANDING_DOCUMENT = 'index';

// ---------- 문서 읽기
function listDocumentSlugs(directory, prefix = '') {
  return readdirSync(directory).flatMap((entryName) => {
    const entryPath = join(directory, entryName);
    if (statSync(entryPath).isDirectory()) return entryName === 'en' && prefix === '' ? [] : listDocumentSlugs(entryPath, `${prefix}${entryName}/`);
    const match = entryName.match(/^(.+)\.mdx?$/);
    return match ? [`${prefix}${match[1]}`] : [];
  });
}

function documentPath(locale, slug) {
  const base = locale === 'en' ? join(DOCS_DIRECTORY, 'en') : DOCS_DIRECTORY;
  return ['.mdx', '.md'].map((extension) => join(base, slug + extension)).find((candidate) => existsSync(candidate));
}

function documentUrl(locale, slug) {
  return `${SITE_ROOT_URL}${locale === 'en' ? 'en/' : ''}${slug === LANDING_DOCUMENT ? '' : `${slug}/`}`;
}

// 프론트매터 title, description 과 본문 분리 (따옴표로 감싼 값 허용)
function readDocument(locale, slug) {
  const filePath = documentPath(locale, slug);
  if (!filePath) throw new Error(`document not found: ${locale} ${slug}`);
  const text = readFileSync(filePath, 'utf8');
  const match = text.match(/^---\n([\s\S]*?)\n---\n?([\s\S]*)$/);
  if (!match) throw new Error(`frontmatter missing: ${filePath}`);
  const field = (name) => (match[1].match(new RegExp(`^${name}:\\s*(.+)$`, 'm'))?.[1] ?? '').trim().replace(/^(['"])(.*)\1$/, '$2');
  return { slug, locale, title: field('title'), description: field('description'), body: match[2], url: documentUrl(locale, slug) };
}

// ---------- MDX > 평문 마크다운 (llms-full 용)
function convertMdxToMarkdown(document) {
  const pageUrl = document.url;
  const output = [];
  let isInCodeFence = false;
  let isInAside = false;
  for (const line of document.body.split('\n')) {
    const isFenceLine = /^\s*```/.test(line);
    if (isFenceLine) isInCodeFence = !isInCodeFence;
    if (isFenceLine || isInCodeFence) {
      output.push(line); // 코드 안은 그대로
      continue;
    }
    if (/^import\s.+from\s/.test(line)) continue;
    const trimmed = line.trim();
    const tab = trimmed.match(/^<TabItem label="([^"]+)"/);
    if (tab) { output.push(`**${tab[1]}**`, ''); continue; }
    if (/^<\/?(Tabs|TabItem|Steps|CardGrid|Card)\b/.test(trimmed)) continue;
    const link = trimmed.match(/^<LinkCard title="([^"]+)"(?: description="([^"]*)")? href="([^"]+)"/);
    if (link) { output.push(`- [${link[1]}](${resolveLink(link[3], pageUrl)})${link[2] ? `: ${link[2]}` : ''}`); continue; }
    const asideOpen = trimmed.match(/^<Aside(?:\s+type="[^"]*")?(?:\s+title="([^"]*)")?>$/);
    if (asideOpen) { isInAside = true; output.push(asideOpen[1] ? `> **${asideOpen[1]}**` : '>'); continue; }
    if (trimmed === '</Aside>') { isInAside = false; output.push(''); continue; }
    const content = line.replace(/^ {2,}/, (indent) => (isInAside ? '' : indent)).replace(/\]\(([^)\s]+)\)/g, (_, href) => `](${resolveLink(href, pageUrl)})`);
    output.push(isInAside ? `> ${content}` : content);
  }
  return output.join('\n').replace(/\n{3,}/g, '\n\n').trim();
}

// 상대 링크 > 절대 주소 (이어 붙인 문서에서도 열리도록)
function resolveLink(href, pageUrl) {
  if (/^(https?:|mailto:|tel:)/.test(href)) return href;
  return new URL(href, pageUrl).toString();
}

// ---------- 문서 모음
function collectDocuments(locale) {
  return SIDEBAR.map((group) => ({
    label: locale === 'en' ? group.translations.en : group.label,
    documents: group.items.map((slug) => readDocument(locale, slug)),
  }));
}

function assertEverySlugIsInSidebar() {
  const listed = new Set(SIDEBAR.flatMap((group) => group.items).concat(LANDING_DOCUMENT));
  const unlisted = listDocumentSlugs(DOCS_DIRECTORY).filter((slug) => !listed.has(slug));
  const missing = [...listed].filter((slug) => LOCALES.some((locale) => !documentPath(locale, slug)));
  if (unlisted.length > 0) throw new Error(`docs not in site.config.mjs SIDEBAR: ${unlisted.join(', ')}`);
  if (missing.length > 0) throw new Error(`SIDEBAR docs without a file in every language: ${missing.join(', ')}`);
}

// ---------- llms.txt
const MACHINE_READABLE_FILES = [
  { file: 'api.json', description: { ko: '공개 메서드 목록: 세 언어 이름, 인자와 기본값, 반환 모델 필드, 실패 동작, 캐시, 로그인 필요 여부', en: 'Public method list: names in three languages, parameters and defaults, return model fields, failure behavior, cache, login requirement' } },
  { file: 'llms-full.txt', description: { ko: '한국어 문서 전체를 한 파일로', en: 'All Korean docs in one file' } },
  { file: 'llms-full.en.txt', description: { ko: '영어 문서 전체를 한 파일로', en: 'All English docs in one file' } },
];
const REPOSITORY_FILES = [
  { path: 'AGENTS.md', description: { ko: '이 저장소에 기여하는 에이전트용 규칙: 구조, 의존 규칙, 테스트 명령, 새 출처 추가 절차', en: 'Rules for agents contributing to this repository: layout, dependency rule, test commands, how to add a source' } },
  { path: 'spec/api-index.json', description: { ko: 'api.json 의 원본 (손으로 관리, 구현과 어긋나면 CI 가 실패)', en: 'Source of api.json (hand-maintained; CI fails if it drifts from the implementations)' } },
  { path: 'spec/PUBLIC_DATA.md', description: { ko: '공개 데이터 계약: 요청, 필드 대응, 값 규칙 (영어)', en: 'Public data contract: requests, field mapping, value rules' } },
];

function buildLlmsTxt() {
  const lines = [
    `# ${SITE_TITLE}`,
    '',
    `> ${SITE_DESCRIPTION_KO}`,
    `> ${SITE_DESCRIPTION_EN}`,
    '',
    '하위 호환을 보장하지 않으니 버전을 정확히 고정하세요. 비밀번호는 서버에서만 다루고 저장하거나 로그에 남기지 마세요. 아래는 한국어 문서 목록이고, 영어 문서는 "English" 구역에 있습니다.',
    '',
    'No backward-compatibility promise: pin the exact version. Handle passwords on a server only and never store or log them. The Korean docs are listed first, the English docs follow in the "English" section.',
    '',
  ];
  for (const group of collectDocuments('ko')) {
    lines.push(`## ${group.label}`, '');
    group.documents.forEach((document) => lines.push(`- [${document.title}](${document.url}): ${document.description}`));
    lines.push('');
  }
  lines.push('## English', '');
  for (const group of collectDocuments('en')) {
    lines.push(`### ${group.label}`, '');
    group.documents.forEach((document) => lines.push(`- [${document.title}](${document.url}): ${document.description}`));
    lines.push('');
  }
  lines.push('## 기계가 읽는 파일 (Machine-readable)', '');
  MACHINE_READABLE_FILES.forEach((entry) => lines.push(`- [${entry.file}](${SITE_ROOT_URL}${entry.file}): ${entry.description.ko} / ${entry.description.en}`));
  lines.push('', '## 저장소 (Repository)', '');
  REPOSITORY_FILES.forEach((entry) => lines.push(`- [${entry.path}](${REPOSITORY_URL}/blob/main/${entry.path}): ${entry.description.ko} / ${entry.description.en}`));
  return lines.join('\n') + '\n';
}

// ---------- llms-full
function buildLlmsFull(locale) {
  const intro = locale === 'en' ? SITE_DESCRIPTION_EN : SITE_DESCRIPTION_KO;
  const sections = [`# ${SITE_TITLE}\n\n> ${intro}\n\nSource: ${SITE_ROOT_URL}${locale === 'en' ? 'en/' : ''}`];
  for (const group of collectDocuments(locale)) {
    for (const document of group.documents) {
      sections.push(`# ${document.title}\n\nSource: ${document.url}\nSection: ${group.label}\n\n${convertMdxToMarkdown(document)}`);
    }
  }
  return sections.join('\n\n---\n\n') + '\n';
}

// ---------- api.json
function camelCaseOf(snakeName) {
  const [head, ...tail] = snakeName.split('_');
  return head + tail.map((word) => word.charAt(0).toUpperCase() + word.slice(1)).join('');
}

const PYTHON_TYPE_TO_JSON_TYPE = { str: 'string', int: 'integer', float: 'number', bool: 'boolean' };

// models.py 의 @dataclass 필드 > { 모델: [{ python, node, java, type }] } (Node·Java 이름은 camelCase, 일치는 check-api-index.py 가 검사)
function readModelFields() {
  const models = {};
  let current = null;
  for (const line of readFileSync(PYTHON_MODELS_PATH, 'utf8').split('\n')) {
    const classMatch = line.match(/^class (\w+)/);
    if (classMatch) { current = models[classMatch[1]] = []; continue; }
    const fieldMatch = current && line.match(/^    (\w+): (.+?)(?: = .+)?$/);
    if (!fieldMatch) continue;
    const pythonType = fieldMatch[2];
    const type = PYTHON_TYPE_TO_JSON_TYPE[pythonType] ?? (pythonType.startsWith('Dict') ? 'object<string,string>' : pythonType.replace(/^List\[(\w+)\]$/, '$1[]'));
    current.push({ python: fieldMatch[1], node: camelCaseOf(fieldMatch[1]), java: camelCaseOf(fieldMatch[1]), type });
  }
  return models;
}

function readErrorCodes() {
  const enumBlock = readFileSync(PYTHON_ERRORS_PATH, 'utf8').match(/class ErrorCode[\s\S]*?(?=\n\n\n|\n# )/)[0];
  return [...enumBlock.matchAll(/^    (\w+) = "(\w+)"/gm)].map((match) => match[1]);
}

function readLibraryVersion() {
  return readFileSync(VERSION_FILE_PATH, 'utf8').match(/^version:\s*"([^"]+)"/m)?.[1] ?? '';
}

function pythonValueText(value) {
  return typeof value === 'string' ? JSON.stringify(value) : typeof value === 'boolean' ? (value ? 'True' : 'False') : String(value);
}

function nodeValueText(value) {
  return typeof value === 'string' ? `'${value}'` : String(value);
}

// 인자 목록 > 언어별 호출 형태 (손으로 쓰지 않고 파라미터 표에서 만든다)
function buildUsage(method) {
  const pythonArguments = method.parameters.map((parameter) => (parameter.required ? parameter.name : `${parameter.name}=${pythonValueText(parameter.default)}`));
  const nodeNames = method.parameters.map((parameter) => `${camelCaseOf(parameter.name)}${parameter.required ? '' : '?'}`);
  let nodeArguments;
  if (method.nodeStyle === 'options') nodeArguments = nodeNames.length > 0 ? `{ ${nodeNames.join(', ')} }` : '';
  else if (method.nodeStyle === 'credentials-then-options') nodeArguments = `${nodeNames.slice(0, 2).join(', ')}, { ${nodeNames.slice(2).join(', ')} }`;
  else nodeArguments = nodeNames.join(', ');
  const nodeSuffix = method.nodeStyle === 'options' && method.parameters.every((parameter) => !parameter.required) ? ' = {}' : '';
  return {
    python: `${method.names.python}(${pythonArguments.join(', ')})`,
    node: `await ${method.names.node}(${nodeArguments}${nodeSuffix})`,
    java: method.javaSignatures,
  };
}

function buildFailure(method, behavior) {
  if (method.group === 'auth') {
    return { mode: 'raises', description: behavior.auth.failure, invalidInput: method.invalidInput };
  }
  const emptyValue = method.returns.isList ? '[]' : '{ total: 0, pinned: [], items: [] }';
  return { mode: 'returns-empty-value', emptyValue, description: behavior.publicData.emptyValue, invalidInput: method.invalidInput };
}

function buildApiJson() {
  const index = JSON.parse(readFileSync(API_INDEX_PATH, 'utf8'));
  const models = readModelFields();
  const methods = index.methods.map((method) => {
    const behaviorKey = method.group === 'auth' ? 'auth' : 'publicData';
    const returnsPublicDataModel = method.group === 'public_data';
    return {
      id: method.id,
      group: method.group,
      requiresLogin: method.requiresLogin,
      summary: method.summary,
      names: method.names,
      usage: buildUsage(method),
      parameters: method.parameters.map((parameter) => ({ ...parameter, nodeName: camelCaseOf(parameter.name) })),
      returns: {
        model: method.returns.model,
        isList: method.returns.isList,
        fields: returnsPublicDataModel ? models[method.returns.model] : undefined,
        fieldsDocs: returnsPublicDataModel ? undefined : `${SITE_ROOT_URL}auth/result-fields/`,
      },
      failure: buildFailure(method, index.behavior),
      cache: method.group === 'public_data' ? index.behavior.publicData.cache : undefined,
      requestCount: method.requestCount,
      notes: method.notes,
      docs: { ko: `${SITE_ROOT_URL}${method.docs}`, en: `${SITE_ROOT_URL}en/${method.docs}` },
    };
  });
  return {
    $comment: 'Generated at website build time by website/scripts/generate-llms.mjs from spec/api-index.json and the Python models. Do not edit.',
    name: PACKAGE_NAME,
    libraryVersion: readLibraryVersion(),
    site: SITE_ROOT_URL,
    repository: REPOSITORY_URL,
    packages: index.packages,
    clients: index.clients,
    behavior: index.behavior,
    errorCodes: readErrorCodes(),
    methods,
    models,
    modelNaming: {
      ko: 'Python 은 snake_case, Node.js 는 camelCase 필드. Java 모델은 같은 camelCase 이름의 getter (getCourseNumber())',
      en: 'Python uses snake_case fields, Node.js camelCase fields. Java models expose the same camelCase names as getters (getCourseNumber())',
    },
  };
}

// ---------- 쓰기
function writePublicFile(fileName, content) {
  mkdirSync(PUBLIC_DIRECTORY, { recursive: true });
  writeFileSync(join(PUBLIC_DIRECTORY, fileName), content);
  console.log(`generated ${relative(REPOSITORY_DIRECTORY, join(PUBLIC_DIRECTORY, fileName))} (${content.length} chars)`);
}

assertEverySlugIsInSidebar();
writePublicFile('llms.txt', buildLlmsTxt());
writePublicFile('llms-full.txt', buildLlmsFull('ko'));
writePublicFile('llms-full.en.txt', buildLlmsFull('en'));
writePublicFile('api.json', JSON.stringify(buildApiJson(), null, 2) + '\n');
