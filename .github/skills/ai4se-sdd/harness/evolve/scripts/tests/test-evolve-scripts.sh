#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SCRIPTS="$(cd "$SCRIPT_DIR/.." && pwd)"

fail=0
assert_eq() {
  local name="$1" got="$2" want="$3"
  if [[ "$got" != "$want" ]]; then
    echo "FAIL $name: got=[$got] want=[$want]"
    fail=1
  else
    echo "OK   $name"
  fi
}

set +e
"$SCRIPTS/init-checkpoint-log.sh" >/dev/null 2>&1
rc=$?
set -e
assert_eq "init-log-no-root-rc" "$rc" "2"

tmp="$(mktemp -d)"
cleanup() { rm -rf "$tmp" "${tmp_w:-}"; }
trap cleanup EXIT

log_out="$("$SCRIPTS/init-checkpoint-log.sh" --root "$tmp" 2>&1)"
echo "$log_out" | rg -q '^LOG ' || { echo "FAIL init-log-stdout"; fail=1; }
log_path="$(echo "$log_out" | awk '/^LOG /{print $2; exit}')"
[[ -f "$log_path" ]] || { echo "FAIL init-log-missing-file"; fail=1; }
rg -q 'skill: skill.harness.evolve' "$log_path" || { echo "FAIL init-log-header"; fail=1; }
rg -q '\| C1 \|' "$log_path" || { echo "FAIL init-log-c1"; fail=1; }
rg -q '\| C2 \|' "$log_path" || { echo "FAIL init-log-c2"; fail=1; }
rg -q '\| C3 \|' "$log_path" || { echo "FAIL init-log-c3"; fail=1; }
rg -q '\| C4 \|' "$log_path" && { echo "FAIL init-log-must-not-have-c4"; fail=1; }
echo "$log_path" | rg -q 'evolve-checkpoints\.md$' || { echo "FAIL init-log-filename"; fail=1; }

set +e
"$SCRIPTS/write-proposals.sh" --root "$tmp" --body-file /dev/null >/dev/null 2>&1
rc=$?
set -e
assert_eq "write-requires-apply-rc" "$rc" "2"

tmp_w="$(mktemp -d)"
bodyf="$tmp_w/body.md"
printf '%s\n' "# Evolve proposals" "" "change-id: demo" >"$bodyf"
w_out="$("$SCRIPTS/write-proposals.sh" --root "$tmp_w" --apply --body-file "$bodyf")"
echo "$w_out" | rg -q '^REPORT ' || { echo "FAIL write-proposals-stdout"; fail=1; }
report_path="$(echo "$w_out" | awk '/^REPORT /{print $2; exit}')"
[[ -f "$report_path" ]] || { echo "FAIL write-proposals-missing"; fail=1; }
echo "$report_path" | rg -q 'evolve-proposals\.md$' || { echo "FAIL write-proposals-filename"; fail=1; }

if [[ "$fail" -ne 0 ]]; then
  echo "test-evolve-scripts: FAILED"
  exit 1
fi
echo "test-evolve-scripts: PASSED"
