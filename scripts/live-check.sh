#!/usr/bin/env bash
# 실제 포털 정상 인증 점검: 세 언어 통합 인증 성공 + 필드 해시 일치 (포털 변경 감지, 정기 워크플로우가 실행)
#   틀린 비밀번호 시도 없음 (계정 잠금 방지), 언어당 포털 로그인 1회, 개인정보 출력 없음
#   사용: SEJONG_TEST_STUDENT_ID=... SEJONG_TEST_PASSWORD=... scripts/live-check.sh
set -euo pipefail
cd "$(dirname "$0")/.."

: "${SEJONG_TEST_STUDENT_ID:?SEJONG_TEST_STUDENT_ID not set}"
: "${SEJONG_TEST_PASSWORD:?SEJONG_TEST_PASSWORD not set}"

# 1. 언어별 정상 인증 결과 수집 (Java 테스트는 SEJONG_TEST_INCLUDE_WRONG_PASSWORD 없으면 정상 인증만)
env -u SEJONG_TEST_INCLUDE_WRONG_PASSWORD ./gradlew -q :sejongpeia:test --tests '*CrossLanguageSummaryTest' --rerun-tasks
(cd node && npm run --silent build)
OUT=java/build/cross-language
python3 scripts/cross-language/summarize.py > "$OUT/python.json"
node scripts/cross-language/summarize.mjs > "$OUT/node.json"

# 2. 비교 (실패·차이 있으면 exit 1)
python3 scripts/cross-language/compare.py "$OUT"
echo "live check passed"
