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

# --- init-checkpoint-log: missing --root fails ---
set +e
out="$("$SCRIPTS/init-checkpoint-log.sh" 2>&1)"
rc=$?
set -e
assert_eq "init-log-no-root-rc" "$rc" "2"

# --- init-checkpoint-log: creates file ---
tmp="$(mktemp -d)"
cleanup() { rm -rf "$tmp" "${tmp_p:-}" "${tmp_b:-}" "${tmp_r:-}" "${tmp_w:-}"; }
trap cleanup EXIT

log_out="$("$SCRIPTS/init-checkpoint-log.sh" --root "$tmp" 2>&1)"
echo "$log_out" | rg -q '^LOG ' || { echo "FAIL init-log-stdout"; fail=1; }
log_path="$(echo "$log_out" | awk '/^LOG /{print $2; exit}')"
[[ -f "$log_path" ]] || { echo "FAIL init-log-missing-file"; fail=1; }
rg -q 'skill: skill.verify.local-verify-report' "$log_path" || { echo "FAIL init-log-header"; fail=1; }
rg -q "$tmp" "$log_path" || { echo "FAIL init-log-root-field"; fail=1; }
rg -q '## Checkpoints' "$log_path" || { echo "FAIL init-log-checkpoints"; fail=1; }
rg -q '\| C1 \|' "$log_path" || { echo "FAIL init-log-c1"; fail=1; }
rg -q '\| C2 \|' "$log_path" || { echo "FAIL init-log-c2"; fail=1; }
rg -q '\| C3 \|' "$log_path" && { echo "FAIL init-log-must-not-have-c3"; fail=1; }
echo "$log_path" | rg -q 'local-verify-report-checkpoints\.md$' || { echo "FAIL init-log-filename"; fail=1; }

# --- probe-verify: missing --root ---
set +e
"$SCRIPTS/probe-verify.sh" >/dev/null 2>&1
rc=$?
set -e
assert_eq "probe-no-root-rc" "$rc" "2"

# --- probe-verify: empty whitelist ---
tmp_p="$(mktemp -d)"
probe_out="$("$SCRIPTS/probe-verify.sh" --root "$tmp_p")"
echo "$probe_out" | rg -q '^MISSING docs/feedback/verification-commands.md$' || { echo "FAIL probe-missing-vc"; fail=1; }
echo "$probe_out" | rg -q '^NOTE whitelist_empty:yes$' || { echo "FAIL probe-whitelist-empty"; fail=1; }
# probe with empty whitelist should not create commands file under docs (may create nothing)
# ensure no business mutation beyond optional docs — empty root should stay file-count stable except if mkdir docs
before_files="$(find "$tmp_p" -type f | wc -l | tr -d ' ')"
"$SCRIPTS/probe-verify.sh" --root "$tmp_p" >/dev/null
after_files="$(find "$tmp_p" -type f | wc -l | tr -d ' ')"
assert_eq "probe-empty-no-file-writes" "$after_files" "$before_files"

# --- probe-verify: marker commands ---
tmp_b="$(mktemp -d)"
mkdir -p "$tmp_b/docs/feedback" "$tmp_b/openspec/changes/add-demo"
printf '%s\n' '# Verify' '' '<!-- ai4se-verify-commands -->' 'echo ok-one' 'echo ok-two' '' '# more' >"$tmp_b/docs/feedback/verification-commands.md"
echo '# t' >"$tmp_b/openspec/changes/add-demo/tasks.md"
probe_b="$("$SCRIPTS/probe-verify.sh" --root "$tmp_b" --change-id add-demo)"
echo "$probe_b" | rg -q '^FOUND docs/feedback/verification-commands.md$' || { echo "FAIL probe-found-vc"; fail=1; }
echo "$probe_b" | rg -q '^COMMAND echo ok-one$' || { echo "FAIL probe-cmd-one"; fail=1; }
echo "$probe_b" | rg -q '^COMMAND echo ok-two$' || { echo "FAIL probe-cmd-two"; fail=1; }
echo "$probe_b" | rg -q '^COMMANDS_FILE ' || { echo "FAIL probe-commands-file"; fail=1; }
echo "$probe_b" | rg -q '^NOTE whitelist_empty:no$' || { echo "FAIL probe-whitelist-no"; fail=1; }
echo "$probe_b" | rg -q '^FOUND openspec/changes/add-demo/$' || { echo "FAIL probe-change-dir"; fail=1; }
echo "$probe_b" | rg -q '^FOUND openspec/changes/add-demo/tasks.md$' || { echo "FAIL probe-tasks"; fail=1; }
cmds_file="$(echo "$probe_b" | awk '/^COMMANDS_FILE /{print $2; exit}')"
[[ -f "$cmds_file" ]] || { echo "FAIL commands-file-missing"; fail=1; }

# --- run-verify: missing args ---
set +e
"$SCRIPTS/run-verify.sh" --root "$tmp_b" >/dev/null 2>&1
rc=$?
set -e
assert_eq "run-no-commands-file-rc" "$rc" "2"

# --- run-verify: executes and continues after fail ---
tmp_r="$(mktemp -d)"
mkdir -p "$tmp_r/docs/ai4se-plugins"
cf="$tmp_r/cmds.txt"
printf '%s\n' 'echo pass-a' 'false' 'echo pass-b' >"$cf"
run_out="$("$SCRIPTS/run-verify.sh" --root "$tmp_r" --commands-file "$cf")"
echo "$run_out" | rg -q '^PASS_CMD echo pass-a$' || { echo "FAIL run-pass-a"; fail=1; }
echo "$run_out" | rg -q '^FAIL_CMD false$' || { echo "FAIL run-fail-false"; fail=1; }
echo "$run_out" | rg -q '^PASS_CMD echo pass-b$' || { echo "FAIL run-pass-b-after-fail"; fail=1; }
echo "$run_out" | rg -q '^NOTE commands_attempted:3$' || { echo "FAIL run-attempted"; fail=1; }
echo "$run_out" | rg -q '^NOTE commands_failed:1$' || { echo "FAIL run-failed-count"; fail=1; }
echo "$run_out" | rg -q '^RUN_LOG ' || { echo "FAIL run-log-stdout"; fail=1; }

# --- write-report without --apply → rc 2 ---
set +e
"$SCRIPTS/write-report.sh" --root "$tmp_b" --body-file /dev/null >/dev/null 2>&1
rc=$?
set -e
assert_eq "write-requires-apply-rc" "$rc" "2"

# --- write-report with --apply ---
tmp_w="$(mktemp -d)"
bodyf="$tmp_w/body.md"
printf '%s\n' "# Local Verify Report" "" "Overall: Pass" >"$bodyf"
w_out="$("$SCRIPTS/write-report.sh" --root "$tmp_w" --apply --body-file "$bodyf")"
echo "$w_out" | rg -q '^REPORT ' || { echo "FAIL write-report-stdout"; fail=1; }
report_path="$(echo "$w_out" | awk '/^REPORT /{print $2; exit}')"
[[ -f "$report_path" ]] || { echo "FAIL write-report-missing-file"; fail=1; }
echo "$report_path" | rg -q 'local-verify-report\.md$' || { echo "FAIL write-report-filename"; fail=1; }
rg -q 'Overall: Pass' "$report_path" || { echo "FAIL write-report-content"; fail=1; }

# --- write-report collision suffix ---
w_out2="$("$SCRIPTS/write-report.sh" --root "$tmp_w" --apply --body-file "$bodyf")"
report_path2="$(echo "$w_out2" | awk '/^REPORT /{print $2; exit}')"
[[ -f "$report_path2" ]] || { echo "FAIL write-report-second-missing"; fail=1; }
[[ "$report_path" != "$report_path2" ]] || { echo "FAIL write-report-must-suffix-on-collision"; fail=1; }

if [[ "$fail" -ne 0 ]]; then
  echo "test-local-verify-report-scripts: FAILED"
  exit 1
fi
echo "test-local-verify-report-scripts: PASSED"
