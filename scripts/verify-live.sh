#!/usr/bin/env bash
# 세 언어(Java, Python, Node) 실제 포털 통합 인증 결과 교차 검증
#   필드별 해시만 비교 (개인정보 출력 없음), 틀린 비밀번호는 세 언어 모두 AUTHENTICATION_FAILED 여야 함
#   사용: SEJONG_TEST_STUDENT_ID=... SEJONG_TEST_PASSWORD=... scripts/verify-live.sh
#   틀린 비밀번호 없이 정상 인증만: scripts/live-check.sh
set -euo pipefail
cd "$(dirname "$0")/.."

: "${SEJONG_TEST_STUDENT_ID:?SEJONG_TEST_STUDENT_ID not set}"
: "${SEJONG_TEST_PASSWORD:?SEJONG_TEST_PASSWORD not set}"

# 1. 언어별 결과 수집
SEJONG_TEST_INCLUDE_WRONG_PASSWORD=true ./gradlew -q :sejongpeia:test --tests '*CrossLanguageSummaryTest' --rerun-tasks
(cd node && npm run --silent build)
OUT=java/build/cross-language
# 언어마다 정상 > 틀림 > SIS 틀림 순서 (포털 잠금 방지: 연속 실패 최대 2회, 정상 로그인이 사이에 끼도록)
python3 scripts/cross-language/summarize.py > "$OUT/python.json"
python3 scripts/cross-language/summarize.py --wrong-password > "$OUT/python-wrong.json"
python3 -c "import os,sys,json; sys.path.insert(0,'python/src'); from sejongpeia import authenticate_sis, SejongAuthError
try: authenticate_sis(os.environ['SEJONG_TEST_STUDENT_ID'], os.environ['SEJONG_TEST_PASSWORD']+'-wrong'); print(json.dumps({}))
except SejongAuthError as e: print(json.dumps({'error': e.code.value}))" > "$OUT/python-sis-wrong.json"
node scripts/cross-language/summarize.mjs > "$OUT/node.json"
node scripts/cross-language/summarize.mjs --wrong-password > "$OUT/node-wrong.json"
node --input-type=module -e "import { authenticateSis } from './node/dist/esm/index.js';
try { await authenticateSis(process.env.SEJONG_TEST_STUDENT_ID, process.env.SEJONG_TEST_PASSWORD + '-wrong'); console.log('{}'); }
catch (error) { console.log(JSON.stringify({ error: error.code })); }" > "$OUT/node-sis-wrong.json"

# 2. 비교 (다르면 다른 필드 이름만 출력 후 exit 1)
python3 scripts/cross-language/compare.py "$OUT" --wrong-password
echo "cross-language verification passed"
