// 빌드 전 package.json 버전 > src/common/version.ts (버전 단일 원본: package.json, projectops 가 갱신)
import { readFileSync, writeFileSync } from 'node:fs';
const { version } = JSON.parse(readFileSync(new URL('../package.json', import.meta.url), 'utf8'));
writeFileSync(new URL('../src/common/version.ts', import.meta.url), `// 자동 생성 (scripts/write-version.mjs), 직접 수정 금지\nexport const VERSION = '${version}';\n`);
