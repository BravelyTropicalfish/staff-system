#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SCRIPTS="$(cd "$SCRIPT_DIR/.." && pwd)"
REF="$(cd "$SCRIPTS/../references" && pwd)"

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
trap 'rm -rf "$tmp"' EXIT
log_out="$("$SCRIPTS/init-checkpoint-log.sh" --root "$tmp" 2>&1)"
echo "$log_out" | rg -q '^LOG ' || { echo "FAIL init-log-stdout"; fail=1; }
log_path="$(echo "$log_out" | awk '/^LOG /{print $2; exit}')"
[[ -f "$log_path" ]] || { echo "FAIL init-log-missing-file"; fail=1; }
rg -q 'skill: skill.harness.bootstrap' "$log_path" || { echo "FAIL init-log-header"; fail=1; }
rg -q "$tmp" "$log_path" || { echo "FAIL init-log-root-field"; fail=1; }
rg -q '## Checkpoints' "$log_path" || { echo "FAIL init-log-checkpoints"; fail=1; }
rg -q '\| C1 \|' "$log_path" || { echo "FAIL init-log-c1"; fail=1; }
rg -q '\| D[0-9] \|' "$log_path" && { echo "FAIL init-log-must-not-use-D-ids"; fail=1; }
echo "$log_path" | rg -q 'checkpoints\.md$' || { echo "FAIL init-log-filename"; fail=1; }

# --- plan-skeleton: missing --root ---
set +e
"$SCRIPTS/plan-skeleton.sh" >/dev/null 2>&1
rc=$?
set -e
assert_eq "plan-no-root-rc" "$rc" "2"

tmp_invalid="$(mktemp -d)"
trap 'rm -rf "$tmp" "$tmp_invalid"' EXIT
set +e
"$SCRIPTS/plan-skeleton.sh" --root "$tmp_invalid" --spec-backend bad >/dev/null 2>&1
rc=$?
set -e
assert_eq "plan-bad-spec-backend-rc" "$rc" "2"
set +e
"$SCRIPTS/plan-skeleton.sh" --root "$tmp_invalid" --agents-md bad >/dev/null 2>&1
rc=$?
set -e
assert_eq "plan-bad-agents-md-rc" "$rc" "2"
set +e
"$SCRIPTS/plan-skeleton.sh" --root "$tmp_invalid" --extra-knowledge custom >/dev/null 2>&1
rc=$?
set -e
assert_eq "plan-bad-extra-knowledge-rc" "$rc" "2"

# --- plan-skeleton: greenfield lists CREATE ---
tmp2="$(mktemp -d)"
trap 'rm -rf "$tmp" "$tmp_invalid" "$tmp2"' EXIT
plan_out="$("$SCRIPTS/plan-skeleton.sh" --root "$tmp2" --spec-backend openspec --agents-md create-stub --extra-knowledge docs-only)"
echo "$plan_out" | rg -q '^CREATE AGENTS.md$' || { echo "FAIL plan-create-agents"; fail=1; }
echo "$plan_out" | rg -q '^CREATE openspec/$' || { echo "FAIL plan-create-openspec"; fail=1; }
echo "$plan_out" | rg -q '\.github' && { echo "FAIL plan-must-not-create-github"; fail=1; }

# --- plan-skeleton: existing file → SKIP ---
mkdir -p "$tmp2"
echo "# existing" >"$tmp2/AGENTS.md"
plan_out2="$("$SCRIPTS/plan-skeleton.sh" --root "$tmp2" --spec-backend openspec --agents-md create-stub --extra-knowledge docs-only)"
echo "$plan_out2" | rg -q '^SKIP AGENTS.md' || { echo "FAIL plan-skip-agents"; fail=1; }

# --- apply without --apply → rc 2 ---
set +e
"$SCRIPTS/apply-skeleton.sh" --root "$tmp2" >/dev/null 2>&1
rc=$?
set -e
assert_eq "apply-requires-flag-rc" "$rc" "2"

set +e
"$SCRIPTS/apply-skeleton.sh" --root "$tmp2" --apply --spec-backend bad >/dev/null 2>&1
rc=$?
set -e
assert_eq "apply-bad-spec-backend-rc" "$rc" "2"

# --- apply greenfield ---
tmp3="$(mktemp -d)"
trap 'rm -rf "$tmp" "$tmp2" "$tmp3"' EXIT
"$SCRIPTS/apply-skeleton.sh" --root "$tmp3" --apply \
  --spec-backend openspec \
  --agents-md create-stub --extra-knowledge docs/knowledge >/dev/null
[[ -f "$tmp3/AGENTS.md" ]] || { echo "FAIL apply-agents"; fail=1; }
[[ -d "$tmp3/openspec/changes/archive" ]] || { echo "FAIL apply-openspec-archive"; fail=1; }
[[ -f "$tmp3/openspec/templates/change-spec.md" ]] || { echo "FAIL apply-openspec-template"; fail=1; }
[[ ! -d "$tmp3/.github" ]] || { echo "FAIL apply-must-not-create-github"; fail=1; }
[[ -d "$tmp3/docs/knowledge/domain/shared" ]] || { echo "FAIL apply-knowledge-shared"; fail=1; }
[[ -f "$tmp3/docs/knowledge/_SCHEMA.md" ]] || { echo "FAIL apply-knowledge-schema"; fail=1; }
[[ -d "$tmp3/requirement-workspace/analysis" ]] || { echo "FAIL apply-req-ws"; fail=1; }
[[ -d "$tmp3/requirement-workspace/intents" ]] || { echo "FAIL apply-req-intents"; fail=1; }
[[ -d "$tmp3/requirement-workspace/requirements" ]] || { echo "FAIL apply-req-requirements"; fail=1; }
[[ -f "$tmp3/requirement-workspace/handoff.md" ]] || { echo "FAIL apply-req-handoff"; fail=1; }
[[ -f "$tmp3/docs/feedback/verification-matrix.md" ]] || { echo "FAIL apply-feedback"; fail=1; }
[[ -f "$tmp3/scripts/README.md" ]] || { echo "FAIL apply-scripts-readme"; fail=1; }
[[ -f "$tmp3/scripts/init.sh" ]] || { echo "FAIL apply-scripts-init"; fail=1; }
[[ -f "$tmp3/scripts/kb-lint.sh" ]] || { echo "FAIL apply-scripts-kb-lint"; fail=1; }
[[ -f "$tmp3/scripts/verify-links.sh" ]] || { echo "FAIL apply-scripts-verify-links"; fail=1; }
[[ -f "$tmp3/scripts/archive-change.sh" ]] || { echo "FAIL apply-scripts-archive-change"; fail=1; }
rg -q '自定义提示' "$tmp3/scripts/init.sh" || { echo "FAIL apply-scripts-init-prompt"; fail=1; }
rg -q '自定义提示' "$tmp3/scripts/kb-lint.sh" || { echo "FAIL apply-scripts-kb-lint-prompt"; fail=1; }
set +e
out_init="$(bash "$tmp3/scripts/init.sh" 2>&1)"
rc_init=$?
set -e
assert_eq "placeholder-init-rc" "$rc_init" "1"
echo "$out_init" | rg -q '自定义提示' || { echo "FAIL placeholder-init-stdout"; fail=1; }
# customized shells must not be overwritten on re-apply
printf 'KEEP-INIT\n' >"$tmp3/scripts/init.sh"
"$SCRIPTS/apply-skeleton.sh" --root "$tmp3" --apply \
  --spec-backend openspec \
  --agents-md create-stub --extra-knowledge docs/knowledge >/dev/null
rg -q 'KEEP-INIT' "$tmp3/scripts/init.sh" || { echo "FAIL apply-must-not-overwrite-shell"; fail=1; }
head -1 "$tmp3/AGENTS.md" | rg -q '^# AGENTS\.md$' || { echo "FAIL apply-agents-h1"; fail=1; }
rg -q '## Branch' "$tmp3/AGENTS.md" || { echo "FAIL apply-agents-body"; fail=1; }

# --- plan lists KB + requirement-workspace when knowledge enabled ---
tmp4="$(mktemp -d)"
trap 'rm -rf "$tmp" "$tmp2" "$tmp3" "$tmp4" "$tmp5"' EXIT
plan_kb="$("$SCRIPTS/plan-skeleton.sh" --root "$tmp4" --spec-backend openspec --agents-md defer --extra-knowledge docs/knowledge)"
echo "$plan_kb" | rg -q '^CREATE docs/knowledge/domain/shared/$' || { echo "FAIL plan-kb-shared"; fail=1; }
echo "$plan_kb" | rg -q '^CREATE requirement-workspace/$' || { echo "FAIL plan-req-ws"; fail=1; }

# --- existing requirement-workspace paths are skipped (shared with pre-spec-workdir) ---
tmp5="$(mktemp -d)"
mkdir -p "$tmp5/requirement-workspace/intents" "$tmp5/requirement-workspace/analysis"
printf '%s\n' '# keep-handoff' >"$tmp5/requirement-workspace/handoff.md"
plan_skip="$("$SCRIPTS/plan-skeleton.sh" --root "$tmp5" --spec-backend defer --agents-md defer --extra-knowledge docs-only)"
echo "$plan_skip" | rg -q '^SKIP requirement-workspace/intents/' || { echo "FAIL plan-skip-intents"; fail=1; }
echo "$plan_skip" | rg -q '^SKIP requirement-workspace/analysis/' || { echo "FAIL plan-skip-analysis"; fail=1; }
echo "$plan_skip" | rg -q '^SKIP requirement-workspace/handoff.md' || { echo "FAIL plan-skip-handoff"; fail=1; }
echo "$plan_skip" | rg -q '^CREATE requirement-workspace/requirements/' || { echo "FAIL plan-create-missing-requirements"; fail=1; }
"$SCRIPTS/apply-skeleton.sh" --root "$tmp5" --apply \
  --spec-backend defer --agents-md defer --extra-knowledge docs-only >/dev/null
rg -q 'keep-handoff' "$tmp5/requirement-workspace/handoff.md" || { echo "FAIL apply-must-not-overwrite-handoff"; fail=1; }
[[ -d "$tmp5/requirement-workspace/requirements" ]] || { echo "FAIL apply-fill-missing-requirements"; fail=1; }

# --- apply does not overwrite non-empty AGENTS.md ---
echo "KEEP" >"$tmp3/AGENTS.md"
"$SCRIPTS/apply-skeleton.sh" --root "$tmp3" --apply \
  --spec-backend openspec \
  --agents-md create-stub --extra-knowledge docs-only >/dev/null
got="$(cat "$tmp3/AGENTS.md")"
assert_eq "apply-no-overwrite" "$got" "KEEP"

if [[ "$fail" -ne 0 ]]; then
  echo "test-bootstrap-scripts: FAILED"
  exit 1
fi
echo "test-bootstrap-scripts: PASSED"
