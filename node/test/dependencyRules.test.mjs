// 의존 방향 규칙 3계층: 공통(common)은 소스를 모르고, 소스(auth, publicData)는 공통만 쓰며 서로 임포트하지 않는다
import assert from 'node:assert/strict';
import { readdirSync, readFileSync } from 'node:fs';
import { dirname, join, relative, resolve, sep } from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const sourceDirectory = join(dirname(fileURLToPath(import.meta.url)), '..', 'src');
// 각 계층이 임포트해도 되는 계층 (자기 자신 제외)
const allowedLayersByLayer = {
  common: [],
  auth: ['common'],
  publicData: ['common'],
};

function typeScriptFilesUnder(layer) {
  return readdirSync(join(sourceDirectory, layer), { recursive: true })
    .filter((name) => name.endsWith('.ts'))
    .map((name) => join(sourceDirectory, layer, name));
}

// 상대 임포트 경로를 src 기준 최상위 폴더(계층)로 변환
function importedLayers(file, ownLayer) {
  const text = readFileSync(file, 'utf8');
  const layers = new Set();
  for (const match of text.matchAll(/(?:from|import)\s+'(\.[^']*)'/g)) {
    const targetPath = relative(sourceDirectory, resolve(dirname(file), match[1]));
    const topLevelName = targetPath.split(sep)[0];
    if (topLevelName !== ownLayer) layers.add(topLevelName);
  }
  return [...layers];
}

test('every layer has files', () => {
  for (const layer of Object.keys(allowedLayersByLayer)) {
    assert.ok(typeScriptFilesUnder(layer).length > 0, `${layer} has no files: rule test would pass without checking anything`);
  }
});

test('layers import only allowed layers', () => {
  for (const [layer, allowedLayers] of Object.entries(allowedLayersByLayer)) {
    for (const file of typeScriptFilesUnder(layer)) {
      const forbidden = importedLayers(file, layer).filter((name) => !allowedLayers.includes(name));
      assert.deepEqual(forbidden, [], `${layer}/${relative(join(sourceDirectory, layer), file)} imports ${forbidden}: sources share only common, common knows no source`);
    }
  }
});
