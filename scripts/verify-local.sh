#!/usr/bin/env bash
# Run on a development machine with Docker access. Never reports skipped checks as passed.
set -uo pipefail
root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
cd "$root"
if ! docker info >/dev/null 2>&1; then
  echo 'BLOCKED: Docker daemon is unavailable. PostgreSQL/MinIO integration cannot be verified.' >&2
  exit 2
fi
report=$(mktemp -d "${TMPDIR:-/tmp}/bankops-verification.XXXXXX")
failed=0
run_check() {
  local name=$1 directory=$2
  shift 2
  echo "Running $name"
  if (cd "$root/$directory" && "$@") >"$report/$name.log" 2>&1; then
    echo "PASS $name"
  else
    echo "FAIL $name — see $report/$name.log"
    failed=1
  fi
}
run_check backend backend mvn -B verify
run_check live-profile-compile backend mvn -B -P live-ai -DskipTests compile
run_check frontend-install frontend npm ci
run_check frontend-build frontend npm run build
run_check frontend-unit frontend npm test -- --watch=false --browsers=ChromeHeadless --reporters=progress
# Browser tests expect the development Compose stack to be running already.
# This script does not stop services, overwrite .env, or delete database volumes.
if curl --fail --silent --max-time 10 http://localhost:4200/ >/dev/null; then
  run_check browser frontend npm run e2e
else
  echo 'BLOCKED browser: start the development Compose stack, then rerun.'
  failed=1
fi
echo "Reports: $report"
exit "$failed"
