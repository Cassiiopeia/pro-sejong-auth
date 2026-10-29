// Node 통합 인증 결과 > 필드별 해시 JSON (개인정보 출력 없이 세 언어 비교용, node/dist 빌드 필요)
//   사용: SEJONG_TEST_STUDENT_ID, SEJONG_TEST_PASSWORD 설정 후 node summarize.mjs [--wrong-password]
import { createHash } from 'node:crypto';
import { SejongAuthError, authenticate } from '../../node/dist/esm/index.js';

const COMPARED_FIELDS = ['studentId', 'name', 'major', 'grade', 'status', 'email', 'phoneNumber', 'englishName', 'userCategory', 'parentUnitName', 'classicReading'];

// 키 정렬 JSON (Python json.dumps(sort_keys=True, separators=(",", ":")) 와 같은 모양)
const canonicalJson = (value) => {
  if (Array.isArray(value)) return `[${value.map(canonicalJson).join(',')}]`;
  if (value && typeof value === 'object') {
    return `{${Object.keys(value).sort().map((key) => `${JSON.stringify(key)}:${canonicalJson(value[key])}`).join(',')}}`;
  }
  return JSON.stringify(value);
};
const fieldHash = (value) => createHash('sha256').update(canonicalJson(value), 'utf8').digest('hex').slice(0, 16);

const password = process.env.SEJONG_TEST_PASSWORD + (process.argv.includes('--wrong-password') ? '-wrong' : '');
try {
  const result = await authenticate(process.env.SEJONG_TEST_STUDENT_ID, password);
  const fields = Object.fromEntries(COMPARED_FIELDS.sort().map((name) => [name, fieldHash(result[name])]));
  console.log(JSON.stringify({ fields }));
} catch (error) {
  if (!(error instanceof SejongAuthError)) throw error;
  console.log(JSON.stringify({ error: error.code }));
}
