#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SCRIPTS="$(cd "$SCRIPT_DIR/.." && pwd)"
# shellcheck source=../lib-hosts.sh
source "$SCRIPTS/lib-hosts.sh"

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

assert_file() {
  local name="$1" path="$2"
  if [[ -f "$path" ]]; then
    echo "OK   $name"
  else
    echo "FAIL $name: missing $path"
    fail=1
  fi
}

assert_absent() {
  local name="$1" path="$2"
  if [[ -e "$path" ]]; then
    echo "FAIL $name: unexpected $path"
    fail=1
  else
    echo "OK   $name"
  fi
}

# --- lib-hosts ---
assert_eq "host-claude" "$(host_rules_dest claude-code)" ".claude/rules/ai4se"
assert_eq "host-cursor" "$(host_rules_dest cursor)" ".cursor/rules/ai4se"
assert_eq "host-codex" "$(host_rules_dest codex)" ".codex/rules/ai4se"
assert_eq "host-qoder" "$(host_rules_dest qoder)" ".qoder/rules/ai4se"
assert_eq "host-copilot" "$(host_rules_dest github-copilot)" ".github/ai4se-rules"
assert_eq "host-none" "$(host_rules_dest none)" ""

# --- lib-langs allowlist ---
# shellcheck source=../lib-langs.sh
source "$SCRIPTS/lib-langs.sh"
lang_is_allowed python || { echo "FAIL lang-allow-python"; fail=1; }
lang_is_allowed typescript || { echo "FAIL lang-allow-typescript"; fail=1; }
lang_is_allowed react-native || { echo "FAIL lang-allow-rn"; fail=1; }
lang_is_allowed angular && { echo "FAIL lang-must-reject-angular"; fail=1; }
lang_is_allowed php && { echo "FAIL lang-must-reject-php"; fail=1; }
set +e
validate_langs "python,csharp" >/dev/null 2>&1
rc=$?
set -e
assert_eq "validate-reject-csharp-rc" "$rc" "2"
set +e
"$SCRIPTS/plan-rules.sh" --root "$(mktemp -d)" --langs php >/dev/null 2>&1
rc=$?
set -e
assert_eq "plan-reject-php-rc" "$rc" "2"
set +e
"$SCRIPTS/apply-rules.sh" --root "$(mktemp -d)" --apply --mode bad >/dev/null 2>&1
rc=$?
set -e
assert_eq "apply-reject-bad-mode-rc" "$rc" "2"

# --- init-checkpoint-log: missing --root ---
set +e
"$SCRIPTS/init-checkpoint-log.sh" >/dev/null 2>&1
rc=$?
set -e
assert_eq "init-log-no-root-rc" "$rc" "2"

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
log_out="$("$SCRIPTS/init-checkpoint-log.sh" --root "$tmp" 2>&1)"
echo "$log_out" | rg -q '^LOG ' || { echo "FAIL init-log-stdout"; fail=1; }
log_path="$(echo "$log_out" | awk '/^LOG /{print $2; exit}')"
[[ -f "$log_path" ]] || { echo "FAIL init-log-missing-file"; fail=1; }
rg -q 'skill: skill.rules.govern' "$log_path" || { echo "FAIL init-log-header"; fail=1; }
rg -q '## Checkpoints' "$log_path" || { echo "FAIL init-log-checkpoints"; fail=1; }
rg -q '\| C2 \|' "$log_path" || { echo "FAIL init-log-c2"; fail=1; }
rg -q '\| D[0-9] \|' "$log_path" && { echo "FAIL init-log-must-not-use-D-ids"; fail=1; }
rg -q 'apply \|' "$log_path" && { echo "FAIL init-log-must-not-have-apply-checkpoint"; fail=1; }

# --- plan: missing --root ---
set +e
"$SCRIPTS/plan-rules.sh" >/dev/null 2>&1
rc=$?
set -e
assert_eq "plan-no-root-rc" "$rc" "2"

# --- plan greenfield CREATE ---
tmp2="$(mktemp -d)"
trap 'rm -rf "$tmp" "$tmp2"' EXIT
plan_out="$("$SCRIPTS/plan-rules.sh" --root "$tmp2" --mode init)"
echo "$plan_out" | rg -q '^CREATE process/spec-first.md$' || { echo "FAIL plan-create-spec-first"; fail=1; }
echo "$plan_out" | rg -q '^CREATE quality/coding-style.md$' || { echo "FAIL plan-create-coding-style"; fail=1; }
echo "$plan_out" | rg -q 'code-review' && { echo "FAIL plan-must-not-include-code-review"; fail=1; }
echo "$plan_out" | rg -q '^CREATE security/security-baseline.md$' || { echo "FAIL plan-create-sec-baseline"; fail=1; }
echo "$plan_out" | rg -q '^CREATE architecture/layer-boundaries.md$' || { echo "FAIL plan-create-layer"; fail=1; }
echo "$plan_out" | rg -q '^CREATE security/no-hardcoded-secrets.md$' || { echo "FAIL plan-create-secrets"; fail=1; }
echo "$plan_out" | rg -q 'async-only' && { echo "FAIL plan-must-not-include-async-without-lang"; fail=1; }
echo "$plan_out" | rg -q '^CREATE quality/$|^SKIP quality/|^CREATE quality/README.md$' || { echo "FAIL plan-quality-slot"; fail=1; }

# --- plan with python lang includes async-only ---
plan_py="$("$SCRIPTS/plan-rules.sh" --root "$tmp2" --mode init --langs python)"
echo "$plan_py" | rg -q '^CREATE quality/async-only.md$' || { echo "FAIL plan-python-async"; fail=1; }

# --- apply without --apply ---
set +e
"$SCRIPTS/apply-rules.sh" --root "$tmp2" >/dev/null 2>&1
rc=$?
set -e
assert_eq "apply-requires-flag-rc" "$rc" "2"

# --- apply greenfield Critical 9 ---
tmp3="$(mktemp -d)"
trap 'rm -rf "$tmp" "$tmp2" "$tmp3"' EXIT
"$SCRIPTS/apply-rules.sh" --root "$tmp3" --apply --mode init >/dev/null
AUTH="$tmp3/docs/knowledge/rules"
assert_file "apply-spec-first" "$AUTH/process/spec-first.md"
assert_file "apply-coding-style" "$AUTH/quality/coding-style.md"
assert_absent "apply-no-code-review" "$AUTH/process/code-review.md"
assert_file "apply-layer" "$AUTH/architecture/layer-boundaries.md"
assert_file "apply-nocirc" "$AUTH/architecture/no-circular-dependency.md"
assert_file "apply-orchex" "$AUTH/architecture/orchestration-execution-separation.md"
assert_file "apply-sec-baseline" "$AUTH/security/security-baseline.md"
assert_file "apply-secrets" "$AUTH/security/no-hardcoded-secrets.md"
assert_file "apply-prompt" "$AUTH/security/prompt-injection-defense.md"
assert_file "apply-pii" "$AUTH/security/pii-scrubbing.md"
assert_file "apply-quality-readme" "$AUTH/quality/README.md"
assert_file "apply-testing-readme" "$AUTH/testing/README.md"
assert_file "apply-docs-readme" "$AUTH/documentation/README.md"
assert_file "apply-schema" "$AUTH/_SCHEMA.md"
assert_file "apply-agents" "$tmp3/AGENTS.md"
assert_absent "apply-no-async-common" "$AUTH/quality/async-only.md"
assert_absent "apply-no-langs-dir" "$AUTH/langs"
rg -q '## Rules pointers' "$tmp3/AGENTS.md" || { echo "FAIL apply-agents-pointers"; fail=1; }
rg -q 'MECE G1' "$tmp3/AGENTS.md" && { echo "FAIL agents-must-not-say-g1g6"; fail=1; }

critical_count=0
for f in \
  process/spec-first.md \
  quality/coding-style.md \
  architecture/layer-boundaries.md \
  architecture/no-circular-dependency.md \
  architecture/orchestration-execution-separation.md \
  security/security-baseline.md \
  security/no-hardcoded-secrets.md \
  security/prompt-injection-defense.md \
  security/pii-scrubbing.md
do
  [[ -f "$AUTH/$f" ]] && critical_count=$((critical_count + 1))
done
assert_eq "apply-critical-9" "$critical_count" "9"

# --- apply python lang rewrites async-only into quality ---
tmp4="$(mktemp -d)"
trap 'rm -rf "$tmp" "$tmp2" "$tmp3" "$tmp4"' EXIT
"$SCRIPTS/apply-rules.sh" --root "$tmp4" --apply --mode init --langs python >/dev/null
assert_file "apply-python-async" "$tmp4/docs/knowledge/rules/quality/async-only.md"
assert_absent "apply-python-no-langs" "$tmp4/docs/knowledge/rules/langs"

# --- apply does not overwrite non-empty rule ---
echo "KEEP" >"$AUTH/process/spec-first.md"
"$SCRIPTS/apply-rules.sh" --root "$tmp3" --apply --mode init >/dev/null
got="$(cat "$AUTH/process/spec-first.md")"
assert_eq "apply-no-overwrite" "$got" "KEEP"

# --- check-merge CONFLICT when file differs ---
plan_cm="$("$SCRIPTS/plan-rules.sh" --root "$tmp3" --mode check-merge)"
echo "$plan_cm" | rg -q '^CONFLICT process/spec-first.md' || { echo "FAIL check-merge-conflict"; fail=1; }

# --- project-host dry-run + apply ---
set +e
"$SCRIPTS/project-host.sh" --root "$tmp3" >/dev/null 2>&1
rc=$?
set -e
assert_eq "project-needs-host-rc" "$rc" "2"

proj_dry="$("$SCRIPTS/project-host.sh" --root "$tmp3" --host claude-code)"
echo "$proj_dry" | rg -q '^PROJECT claude-code' || { echo "FAIL project-dry"; fail=1; }
[[ ! -d "$tmp3/.claude/rules/ai4se" ]] || { echo "FAIL project-dry-wrote"; fail=1; }

"$SCRIPTS/project-host.sh" --root "$tmp3" --host claude-code --apply >/dev/null
assert_file "project-claude-spec" "$tmp3/.claude/rules/ai4se/process/spec-first.md"
assert_file "project-claude-readme" "$tmp3/.claude/rules/ai4se/README.projection.md"

"$SCRIPTS/project-host.sh" --root "$tmp3" --host cursor --apply >/dev/null
assert_file "project-cursor" "$tmp3/.cursor/rules/ai4se/architecture/layer-boundaries.md"

"$SCRIPTS/project-host.sh" --root "$tmp3" --host github-copilot --apply >/dev/null
assert_file "project-copilot-mirror" "$tmp3/.github/ai4se-rules/security/pii-scrubbing.md"
assert_file "project-copilot-instr" "$tmp3/.github/copilot-instructions.md"
rg -q 'ai4se-rules-pointer' "$tmp3/.github/copilot-instructions.md" || { echo "FAIL copilot-pointer"; fail=1; }

"$SCRIPTS/project-host.sh" --root "$tmp3" --host none --apply >/dev/null
echo "OK   project-none"

if [[ "$fail" -ne 0 ]]; then
  echo "test-rules-govern-scripts: FAILED"
  exit 1
fi
echo "test-rules-govern-scripts: PASSED"
