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
cleanup() { rm -rf "$tmp" "${tmp_p:-}" "${tmp_w:-}" "${tmp_s:-}"; }
trap cleanup EXIT

log_out="$("$SCRIPTS/init-checkpoint-log.sh" --root "$tmp")"
echo "$log_out" | rg -q '^LOG ' || { echo "FAIL init-log-stdout"; fail=1; }
log_path="$(echo "$log_out" | awk '/^LOG /{print $2; exit}')"
[[ -f "$log_path" ]] || { echo "FAIL init-log-file"; fail=1; }
rg -q 'skill: skill.common.quality-checklist' "$log_path" || { echo "FAIL init-log-header"; fail=1; }
rg -q '\| C1 \|' "$log_path" || { echo "FAIL init-log-c1"; fail=1; }
rg -q '\| C3 \|' "$log_path" && { echo "FAIL init-log-no-c3"; fail=1; }
echo "$log_path" | rg -q 'quality-checklist-checkpoints\.md$' || { echo "FAIL init-log-name"; fail=1; }

probe_all_default="$("$SCRIPTS/probe-stage.sh" --root "$tmp")"
echo "$probe_all_default" | rg -q '^NOTE stages:p0,p1,p2,p3,p4,p5$' || { echo "FAIL probe-default-all-stages"; fail=1; }
echo "$probe_all_default" | rg -q '^NOTE stage:p0$' || { echo "FAIL probe-default-p0"; fail=1; }
echo "$probe_all_default" | rg -q '^NOTE stage:p5$' || { echo "FAIL probe-default-p5"; fail=1; }

set +e
"$SCRIPTS/probe-stage.sh" --root "$tmp" --stage p9 >/dev/null 2>&1
rc=$?
set -e
assert_eq "probe-bad-stage-rc" "$rc" "2"

tmp_p="$(mktemp -d)"
probe_out="$("$SCRIPTS/probe-stage.sh" --root "$tmp_p" --stage p0)"
echo "$probe_out" | rg -q '^NOTE stages:p0$' || { echo "FAIL probe-single-stage-list"; fail=1; }
echo "$probe_out" | rg -q '^NOTE stage:p0$' || { echo "FAIL probe-stage-note"; fail=1; }
echo "$probe_out" | rg -q '^MISSING openspec/$' || { echo "FAIL probe-missing-openspec"; fail=1; }
before="$(find "$tmp_p" | wc -l | tr -d ' ')"
"$SCRIPTS/probe-stage.sh" --root "$tmp_p" --stage p0 >/dev/null
after="$(find "$tmp_p" | wc -l | tr -d ' ')"
assert_eq "probe-no-writes" "$after" "$before"

probe_multi="$("$SCRIPTS/probe-stage.sh" --root "$tmp_p" --stage p0,p4)"
echo "$probe_multi" | rg -q '^NOTE stages:p0,p4$' || { echo "FAIL probe-multi-stages-list"; fail=1; }
echo "$probe_multi" | rg -q '^NOTE stage:p0$' || { echo "FAIL probe-multi-p0"; fail=1; }
echo "$probe_multi" | rg -q '^NOTE stage:p4$' || { echo "FAIL probe-multi-p4"; fail=1; }

tmp_s="$(mktemp -d)"
mkdir -p "$tmp_s/openspec/changes/demo/specs" "$tmp_s/docs/ai4se-plugins"
echo '# t' >"$tmp_s/openspec/changes/demo/tasks.md"
echo '# r' >"$tmp_s/docs/ai4se-plugins/20260804-120000-local-verify-report.md"
probe_p4="$("$SCRIPTS/probe-stage.sh" --root "$tmp_s" --stage p4)"
echo "$probe_p4" | rg -q '^NOTE active-change:demo$' || { echo "FAIL probe-active"; fail=1; }
echo "$probe_p4" | rg -q '^NOTE local-verify-report:20260804-120000-local-verify-report.md$' || { echo "FAIL probe-lvr"; fail=1; }
echo "$probe_p4" | rg -q '^MISSING openspec/changes/demo/g2-decision.md$' || { echo "FAIL probe-g2-missing"; fail=1; }

set +e
"$SCRIPTS/write-report.sh" --root "$tmp" --body-file /dev/null >/dev/null 2>&1
rc=$?
set -e
assert_eq "write-requires-apply-rc" "$rc" "2"

tmp_w="$(mktemp -d)"
bodyf="$tmp_w/body.md"
printf '%s\n' "# Quality Checklist Report: p0" "" "Overall: Pass" >"$bodyf"
w_out="$("$SCRIPTS/write-report.sh" --root "$tmp_w" --apply --body-file "$bodyf")"
echo "$w_out" | rg -q '^REPORT ' || { echo "FAIL write-stdout"; fail=1; }
rp="$(echo "$w_out" | awk '/^REPORT /{print $2; exit}')"
echo "$rp" | rg -q 'quality-checklist-report\.md$' || { echo "FAIL write-name"; fail=1; }
w_out2="$("$SCRIPTS/write-report.sh" --root "$tmp_w" --apply --body-file "$bodyf")"
rp2="$(echo "$w_out2" | awk '/^REPORT /{print $2; exit}')"
[[ "$rp" != "$rp2" ]] || { echo "FAIL write-suffix"; fail=1; }

# checklists present
for s in p0 p1 p2 p3 p4 p5; do
  [[ -f "$SCRIPTS/../references/checklists/${s}.md" ]] || { echo "FAIL checklist-$s"; fail=1; }
done

if [[ "$fail" -ne 0 ]]; then
  echo "test-quality-checklist-scripts: FAILED"
  exit 1
fi
echo "test-quality-checklist-scripts: PASSED"
