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
trap 'rm -rf "$tmp"' EXIT
log_out="$("$SCRIPTS/init-checkpoint-log.sh" --root "$tmp" 2>&1)"
echo "$log_out" | rg -q '^LOG ' || { echo "FAIL init-log-stdout"; fail=1; }
log_path="$(echo "$log_out" | awk '/^LOG /{print $2; exit}')"
[[ -f "$log_path" ]] || { echo "FAIL init-log-missing-file"; fail=1; }
rg -q 'skill: skill.common.repo-understand' "$log_path" || { echo "FAIL init-log-header"; fail=1; }
rg -q "$tmp" "$log_path" || { echo "FAIL init-log-root-field"; fail=1; }
rg -q '## Checkpoints' "$log_path" || { echo "FAIL init-log-checkpoints"; fail=1; }
rg -q '\| C1 \|' "$log_path" || { echo "FAIL init-log-c1"; fail=1; }
rg -q '\| C2 \|' "$log_path" || { echo "FAIL init-log-c2"; fail=1; }
rg -q '\| C3 \|' "$log_path" && { echo "FAIL init-log-must-not-have-c3"; fail=1; }
echo "$log_path" | rg -q 'repo-understand-checkpoints\.md$' || { echo "FAIL init-log-filename"; fail=1; }

# --- probe-layout: missing --root ---
set +e
"$SCRIPTS/probe-layout.sh" >/dev/null 2>&1
rc=$?
set -e
assert_eq "probe-no-root-rc" "$rc" "2"

# --- probe-layout: empty root → greenfield suspected ---
tmp_p="$(mktemp -d)"
trap 'rm -rf "$tmp" "$tmp_p"' EXIT
probe_out="$("$SCRIPTS/probe-layout.sh" --root "$tmp_p")"
echo "$probe_out" | rg -q '^MISSING AGENTS.md$' || { echo "FAIL probe-missing-agents"; fail=1; }
echo "$probe_out" | rg -q '^MISSING openspec/$' || { echo "FAIL probe-missing-openspec"; fail=1; }
echo "$probe_out" | rg -q '^NOTE greenfield_suspected:yes$' || { echo "FAIL probe-greenfield-yes"; fail=1; }

# --- probe-layout: brownfield signals ---
tmp_b="$(mktemp -d)"
trap 'rm -rf "$tmp" "$tmp_p" "$tmp_b"' EXIT
mkdir -p "$tmp_b/openspec/changes/add-x" "$tmp_b/docs/knowledge/rules"
echo '{}' >"$tmp_b/package.json"
echo '# a' >"$tmp_b/AGENTS.md"
probe_b="$("$SCRIPTS/probe-layout.sh" --root "$tmp_b")"
echo "$probe_b" | rg -q '^FOUND AGENTS.md$' || { echo "FAIL probe-found-agents"; fail=1; }
echo "$probe_b" | rg -q '^FOUND package.json$' || { echo "FAIL probe-found-pkg"; fail=1; }
echo "$probe_b" | rg -q '^FOUND openspec/$' || { echo "FAIL probe-found-openspec"; fail=1; }
echo "$probe_b" | rg -q '^NOTE active-change:add-x$' || { echo "FAIL probe-active-change"; fail=1; }
echo "$probe_b" | rg -q '^NOTE greenfield_suspected:no$' || { echo "FAIL probe-greenfield-no"; fail=1; }

# --- probe-layout must not create files ---
before="$(find "$tmp_p" | wc -l | tr -d ' ')"
"$SCRIPTS/probe-layout.sh" --root "$tmp_p" >/dev/null
after="$(find "$tmp_p" | wc -l | tr -d ' ')"
assert_eq "probe-no-writes" "$after" "$before"

# --- write-brief without --apply → rc 2 ---
set +e
"$SCRIPTS/write-brief.sh" --root "$tmp_b" --body-file /dev/null >/dev/null 2>&1
rc=$?
set -e
assert_eq "write-requires-apply-rc" "$rc" "2"

# --- write-brief with --apply ---
tmp_w="$(mktemp -d)"
trap 'rm -rf "$tmp" "$tmp_p" "$tmp_b" "$tmp_w"' EXIT
bodyf="$tmp_w/body.md"
printf '%s\n' "# Repo understand brief" "" "test body" >"$bodyf"
w_out="$("$SCRIPTS/write-brief.sh" --root "$tmp_w" --apply --body-file "$bodyf")"
echo "$w_out" | rg -q '^BRIEF ' || { echo "FAIL write-brief-stdout"; fail=1; }
brief_path="$(echo "$w_out" | awk '/^BRIEF /{print $2; exit}')"
[[ -f "$brief_path" ]] || { echo "FAIL write-brief-missing-file"; fail=1; }
echo "$brief_path" | rg -q 'repo-understand-brief\.md$' || { echo "FAIL write-brief-filename"; fail=1; }
rg -q 'test body' "$brief_path" || { echo "FAIL write-brief-content"; fail=1; }

# --- write-brief does not overwrite non-empty collision (uses suffix) ---
w_out2="$("$SCRIPTS/write-brief.sh" --root "$tmp_w" --apply --body-file "$bodyf")"
brief_path2="$(echo "$w_out2" | awk '/^BRIEF /{print $2; exit}')"
[[ -f "$brief_path2" ]] || { echo "FAIL write-brief-second-missing"; fail=1; }
[[ "$brief_path" != "$brief_path2" ]] || { echo "FAIL write-brief-must-suffix-on-collision"; fail=1; }

if [[ "$fail" -ne 0 ]]; then
  echo "test-repo-understand-scripts: FAILED"
  exit 1
fi
echo "test-repo-understand-scripts: PASSED"
