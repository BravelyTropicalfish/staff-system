#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
COMMON="$(cd "$SCRIPT_DIR/.." && pwd)"

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

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
tmpl="$tmp/checkpoint-log-template.md"
cat >"$tmpl" <<'EOF'
# Checkpoint log — test
- skill: skill.test
- root: {{ROOT}}
- started_at: {{STARTED_AT}}
- host_agent: {{HOST_AGENT}}

## Checkpoints

| id | topic | choice | rationale | status |
|---|---|---|---|---|
| C1 | root | | | pending |
EOF

# --- init: missing args ---
set +e
"$COMMON/init-checkpoint-log.sh" >/dev/null 2>&1
rc=$?
set -e
assert_eq "init-no-args-rc" "$rc" "2"

set +e
"$COMMON/init-checkpoint-log.sh" --root "$tmp" --template "$tmpl" >/dev/null 2>&1
rc=$?
set -e
assert_eq "init-no-token-rc" "$rc" "2"

# --- init: creates file ---
log_out="$("$COMMON/init-checkpoint-log.sh" --root "$tmp" --template "$tmpl" --skill-token demo)"
echo "$log_out" | rg -q '^LOG ' || { echo "FAIL init-stdout"; fail=1; }
log_path="$(echo "$log_out" | awk '/^LOG /{print $2; exit}')"
[[ -f "$log_path" ]] || { echo "FAIL init-missing-file"; fail=1; }
echo "$log_path" | rg -q 'demo-checkpoints\.md$' || { echo "FAIL init-filename"; fail=1; }
rg -q "$tmp" "$log_path" || { echo "FAIL init-root-field"; fail=1; }

# --- write-artifact ---
bodyf="$tmp/body.md"
printf '%s\n' "hello" >"$bodyf"
set +e
"$COMMON/write-artifact.sh" --root "$tmp" --body-file "$bodyf" --artifact-name demo-report >/dev/null 2>&1
rc=$?
set -e
assert_eq "write-requires-apply-rc" "$rc" "2"

w_out="$("$COMMON/write-artifact.sh" --root "$tmp" --apply --body-file "$bodyf" --artifact-name demo-report --label REPORT)"
echo "$w_out" | rg -q '^REPORT ' || { echo "FAIL write-stdout"; fail=1; }
rep_path="$(echo "$w_out" | awk '/^REPORT /{print $2; exit}')"
[[ -f "$rep_path" ]] || { echo "FAIL write-missing-file"; fail=1; }
echo "$rep_path" | rg -q 'demo-report\.md$' || { echo "FAIL write-filename"; fail=1; }
rg -q 'hello' "$rep_path" || { echo "FAIL write-content"; fail=1; }

if [[ "$fail" -ne 0 ]]; then
  echo "test-common-scripts: FAILED"
  exit 1
fi
echo "test-common-scripts: PASSED"
