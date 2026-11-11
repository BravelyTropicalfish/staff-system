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

# --- init-checkpoint-log: missing --root ---
set +e
"$SCRIPTS/init-checkpoint-log.sh" >/dev/null 2>&1
rc=$?
set -e
assert_eq "init-log-no-root-rc" "$rc" "2"

tmp="$(mktemp -d)"
cleanup() { rm -rf "$tmp" "${tmp_p:-}" "${tmp_g:-}" "${tmp_w:-}"; }
trap cleanup EXIT

log_out="$("$SCRIPTS/init-checkpoint-log.sh" --root "$tmp" 2>&1)"
echo "$log_out" | rg -q '^LOG ' || { echo "FAIL init-log-stdout"; fail=1; }
log_path="$(echo "$log_out" | awk '/^LOG /{print $2; exit}')"
[[ -f "$log_path" ]] || { echo "FAIL init-log-missing-file"; fail=1; }
rg -q 'skill: skill.verify.pipeline-report' "$log_path" || { echo "FAIL init-log-header"; fail=1; }
rg -q '\| C1 \|' "$log_path" || { echo "FAIL init-log-c1"; fail=1; }
rg -q '\| C2 \|' "$log_path" || { echo "FAIL init-log-c2"; fail=1; }
rg -q '\| C3 \|' "$log_path" && { echo "FAIL init-log-must-not-have-c3"; fail=1; }
echo "$log_path" | rg -q 'pipeline-report-checkpoints\.md$' || { echo "FAIL init-log-filename"; fail=1; }

# --- probe: missing --root ---
set +e
"$SCRIPTS/probe-pipeline.sh" >/dev/null 2>&1
rc=$?
set -e
assert_eq "probe-no-root-rc" "$rc" "2"

# --- probe: empty root, no .git ---
tmp_p="$(mktemp -d)"
probe_out="$("$SCRIPTS/probe-pipeline.sh" --root "$tmp_p")"
echo "$probe_out" | rg -q '^MISSING \.git/$' || { echo "FAIL probe-missing-git"; fail=1; }
echo "$probe_out" | rg -q '^NOTE host:unknown$' || { echo "FAIL probe-host-unknown"; fail=1; }
echo "$probe_out" | rg -q '^NOTE tool:glab:' || { echo "FAIL probe-glab-note"; fail=1; }
echo "$probe_out" | rg -q '^NOTE tool:gh:' || { echo "FAIL probe-gh-note"; fail=1; }

before="$(find "$tmp_p" | wc -l | tr -d ' ')"
"$SCRIPTS/probe-pipeline.sh" --root "$tmp_p" >/dev/null
after="$(find "$tmp_p" | wc -l | tr -d ' ')"
assert_eq "probe-no-writes" "$after" "$before"

# --- probe: gitlab remote heuristic ---
tmp_g="$(mktemp -d)"
git -C "$tmp_g" init -q
git -C "$tmp_g" remote add origin "https://gitlab.example.com/group/proj.git"
git -C "$tmp_g" checkout -q -b main 2>/dev/null || git -C "$tmp_g" checkout -q -b master
# need a commit for rev-parse — allow failure on empty
probe_g="$("$SCRIPTS/probe-pipeline.sh" --root "$tmp_g")"
echo "$probe_g" | rg -q '^FOUND \.git/$' || { echo "FAIL probe-found-git"; fail=1; }
echo "$probe_g" | rg -q '^NOTE host:gitlab$' || { echo "FAIL probe-host-gitlab"; fail=1; }
echo "$probe_g" | rg -q '^NOTE remote:origin ' || { echo "FAIL probe-remote-origin"; fail=1; }

# --- write-report without --apply ---
set +e
"$SCRIPTS/write-report.sh" --root "$tmp" --body-file /dev/null >/dev/null 2>&1
rc=$?
set -e
assert_eq "write-requires-apply-rc" "$rc" "2"

# --- write-report with --apply + collision ---
tmp_w="$(mktemp -d)"
bodyf="$tmp_w/body.md"
printf '%s\n' "# Pipeline Report" "" "Overall: Partial" >"$bodyf"
w_out="$("$SCRIPTS/write-report.sh" --root "$tmp_w" --apply --body-file "$bodyf")"
echo "$w_out" | rg -q '^REPORT ' || { echo "FAIL write-report-stdout"; fail=1; }
report_path="$(echo "$w_out" | awk '/^REPORT /{print $2; exit}')"
[[ -f "$report_path" ]] || { echo "FAIL write-report-missing"; fail=1; }
echo "$report_path" | rg -q 'pipeline-report\.md$' || { echo "FAIL write-report-filename"; fail=1; }
w_out2="$("$SCRIPTS/write-report.sh" --root "$tmp_w" --apply --body-file "$bodyf")"
report_path2="$(echo "$w_out2" | awk '/^REPORT /{print $2; exit}')"
[[ "$report_path" != "$report_path2" ]] || { echo "FAIL write-report-must-suffix"; fail=1; }

if [[ "$fail" -ne 0 ]]; then
  echo "test-pipeline-report-scripts: FAILED"
  exit 1
fi
echo "test-pipeline-report-scripts: PASSED"
