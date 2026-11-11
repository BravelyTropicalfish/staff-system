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
cleanup() { rm -rf "$tmp" "${tmp_p:-}" "${tmp_g:-}" "${tmp_w:-}"; }
trap cleanup EXIT

log_out="$("$SCRIPTS/init-checkpoint-log.sh" --root "$tmp" 2>&1)"
echo "$log_out" | rg -q '^LOG ' || { echo "FAIL init-log-stdout"; fail=1; }
log_path="$(echo "$log_out" | awk '/^LOG /{print $2; exit}')"
[[ -f "$log_path" ]] || { echo "FAIL init-log-missing-file"; fail=1; }
rg -q 'skill: skill.harness.pipeline-iac' "$log_path" || { echo "FAIL init-log-header"; fail=1; }
rg -q '\| C1 \|' "$log_path" || { echo "FAIL init-log-c1"; fail=1; }
rg -q '\| C2 \|' "$log_path" || { echo "FAIL init-log-c2"; fail=1; }
rg -q '\| C3 \|' "$log_path" || { echo "FAIL init-log-c3"; fail=1; }
rg -q '\| C4 \|' "$log_path" && { echo "FAIL init-log-must-not-have-c4"; fail=1; }
echo "$log_path" | rg -q 'pipeline-iac-checkpoints\.md$' || { echo "FAIL init-log-filename"; fail=1; }

set +e
"$SCRIPTS/probe-pipeline-iac.sh" >/dev/null 2>&1
rc=$?
set -e
assert_eq "probe-no-root-rc" "$rc" "2"

tmp_p="$(mktemp -d)"
probe_out="$("$SCRIPTS/probe-pipeline-iac.sh" --root "$tmp_p")"
echo "$probe_out" | rg -q '^MISSING \.github/workflows/$' || { echo "FAIL probe-missing-gha"; fail=1; }
echo "$probe_out" | rg -q '^MISSING \.gitlab-ci\.yml$' || { echo "FAIL probe-missing-gitlab-ci"; fail=1; }
echo "$probe_out" | rg -q '^MISSING terraform/$' || { echo "FAIL probe-missing-tf"; fail=1; }
echo "$probe_out" | rg -q '^MISSING infra/$' || { echo "FAIL probe-missing-infra"; fail=1; }
echo "$probe_out" | rg -q '^MISSING iac/$' || { echo "FAIL probe-missing-iac"; fail=1; }
echo "$probe_out" | rg -q '^NOTE host:unknown$' || { echo "FAIL probe-host-unknown"; fail=1; }

before="$(find "$tmp_p" | wc -l | tr -d ' ')"
"$SCRIPTS/probe-pipeline-iac.sh" --root "$tmp_p" >/dev/null
after="$(find "$tmp_p" | wc -l | tr -d ' ')"
assert_eq "probe-no-writes" "$after" "$before"

tmp_g="$(mktemp -d)"
mkdir -p "$tmp_g/.github/workflows" "$tmp_g/terraform"
: >"$tmp_g/.gitlab-ci.yml"
git -C "$tmp_g" init -q
git -C "$tmp_g" remote add origin "https://github.com/org/proj.git"
probe_g="$("$SCRIPTS/probe-pipeline-iac.sh" --root "$tmp_g")"
echo "$probe_g" | rg -q '^FOUND \.github/workflows/$' || { echo "FAIL probe-found-gha"; fail=1; }
echo "$probe_g" | rg -q '^FOUND \.gitlab-ci\.yml$' || { echo "FAIL probe-found-gitlab-ci"; fail=1; }
echo "$probe_g" | rg -q '^FOUND terraform/$' || { echo "FAIL probe-found-tf"; fail=1; }
echo "$probe_g" | rg -q '^NOTE host:github$' || { echo "FAIL probe-host-github"; fail=1; }

set +e
"$SCRIPTS/write-plan.sh" --root "$tmp" --body-file /dev/null >/dev/null 2>&1
rc=$?
set -e
assert_eq "write-requires-apply-rc" "$rc" "2"

tmp_w="$(mktemp -d)"
bodyf="$tmp_w/body.md"
printf '%s\n' "# Pipeline IaC plan" "" "scope: both" >"$bodyf"
w_out="$("$SCRIPTS/write-plan.sh" --root "$tmp_w" --apply --body-file "$bodyf")"
echo "$w_out" | rg -q '^REPORT ' || { echo "FAIL write-plan-stdout"; fail=1; }
plan_path="$(echo "$w_out" | awk '/^REPORT /{print $2; exit}')"
[[ -f "$plan_path" ]] || { echo "FAIL write-plan-missing"; fail=1; }
echo "$plan_path" | rg -q 'pipeline-iac-plan\.md$' || { echo "FAIL write-plan-filename"; fail=1; }

if [[ "$fail" -ne 0 ]]; then
  echo "test-pipeline-iac-scripts: FAILED"
  exit 1
fi
echo "test-pipeline-iac-scripts: PASSED"
