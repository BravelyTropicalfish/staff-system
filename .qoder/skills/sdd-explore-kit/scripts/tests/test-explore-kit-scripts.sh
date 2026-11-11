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

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

# --- ensure: missing --root ---
set +e
"$SCRIPTS/ensure-exploration-dir.sh" >/dev/null 2>&1
rc=$?
set -e
assert_eq "ensure-no-root-rc" "$rc" "2"

# --- ensure: creates exploration/ + README ---
out="$("$SCRIPTS/ensure-exploration-dir.sh" --root "$tmp")"
echo "$out" | rg -q '^DIR ' || { echo "FAIL ensure-dir-stdout"; fail=1; }
dir_path="$(echo "$out" | awk '/^DIR /{print $2; exit}')"
[[ -d "$dir_path" ]] || { echo "FAIL ensure-dir-missing"; fail=1; }
[[ -f "$tmp/exploration/README.md" ]] || { echo "FAIL ensure-readme-missing"; fail=1; }

# --- ensure: additive (second run keeps README content) ---
echo "keep-me" >>"$tmp/exploration/README.md"
"$SCRIPTS/ensure-exploration-dir.sh" --root "$tmp" >/dev/null
rg -q 'keep-me' "$tmp/exploration/README.md" || { echo "FAIL ensure-must-not-overwrite-readme"; fail=1; }

# --- write: missing --apply ---
body="$tmp/body.md"
printf '%s\n' '# Exploration — elevator-pitch' 'ok' >"$body"
set +e
"$SCRIPTS/write-exploration.sh" --root "$tmp" --method elevator-pitch --body-file "$body" >/dev/null 2>&1
rc=$?
set -e
assert_eq "write-no-apply-rc" "$rc" "2"

# --- write: bad method ---
set +e
"$SCRIPTS/write-exploration.sh" --root "$tmp" --apply --method not-a-method --body-file "$body" >/dev/null 2>&1
rc=$?
set -e
assert_eq "write-bad-method-rc" "$rc" "2"

# --- write: success ---
"$SCRIPTS/ensure-exploration-dir.sh" --root "$tmp" >/dev/null
w_out="$("$SCRIPTS/write-exploration.sh" --root "$tmp" --apply --method elevator-pitch --body-file "$body")"
echo "$w_out" | rg -q '^FILE ' || { echo "FAIL write-file-stdout"; fail=1; }
fpath="$(echo "$w_out" | awk '/^FILE /{print $2; exit}')"
[[ -f "$fpath" ]] || { echo "FAIL write-file-missing"; fail=1; }
echo "$fpath" | rg -q 'exploration/.*-elevator-pitch\.md$' || { echo "FAIL write-filename"; fail=1; }
rg -q 'elevator-pitch' "$fpath" || { echo "FAIL write-content"; fail=1; }

# --- write: collision gets suffix ---
w_out2="$("$SCRIPTS/write-exploration.sh" --root "$tmp" --apply --method elevator-pitch --body-file "$body")"
fpath2="$(echo "$w_out2" | awk '/^FILE /{print $2; exit}')"
[[ -f "$fpath2" ]] || { echo "FAIL write-second-missing"; fail=1; }
[[ "$fpath" != "$fpath2" ]] || { echo "FAIL write-must-suffix-on-collision"; fail=1; }

if [[ "$fail" -ne 0 ]]; then
  echo "test-explore-kit-scripts: FAILED"
  exit 1
fi
echo "test-explore-kit-scripts: PASSED"
