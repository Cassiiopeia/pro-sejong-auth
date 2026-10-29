// 의존 방향 규칙: 인증 코드와 공개 데이터 소스는 서로 임포트하지 않고 공통 계층(transport, errors)만 함께 쓴다
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const sourceDirectory = join(dirname(fileURLToPath(import.meta.url)), '..', 'src');
const authenticationModules = ['client', 'parsers', 'portalMainParser', 'models'];
const publicDataModules = ['publicData', 'publicDataModels'];

function importedModules(moduleName) {
  const text = readFileSync(join(sourceDirectory, `${moduleName}.ts`), 'utf8');
  return [...text.matchAll(/from '\.\/(\w+)\.js'/g)].map((match) => match[1]);
}

test('public data never imports authentication modules', () => {
  for (const moduleName of publicDataModules) {
    const forbidden = importedModules(moduleName).filter((name) => authenticationModules.includes(name));
    assert.deepEqual(forbidden, [], `${moduleName} imports ${forbidden}: share through transport or errors`);
  }
});

test('authentication never imports public data', () => {
  for (const moduleName of authenticationModules) {
    const forbidden = importedModules(moduleName).filter((name) => publicDataModules.includes(name));
    assert.deepEqual(forbidden, [], `${moduleName} imports ${forbidden}: keep sources independent`);
  }
});
