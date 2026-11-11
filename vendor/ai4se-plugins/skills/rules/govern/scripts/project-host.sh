#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=lib-hosts.sh
source "$SCRIPT_DIR/lib-hosts.sh"

ROOT=""
HOST=""
DO_APPLY=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --root) ROOT="${2:-}"; shift 2 ;;
    --host) HOST="${2:-}"; shift 2 ;;
    --apply) DO_APPLY=1; shift ;;
    -h|--help)
      echo "Usage: project-host.sh --root <path> --host <claude-code|cursor|codex|github-copilot|qoder|none> [--apply]"
      exit 0
      ;;
    *) echo "ERROR: unknown arg: $1" >&2; exit 2 ;;
  esac
done

if [[ -z "$ROOT" || -z "$HOST" ]]; then
  echo "ERROR: --root and --host are required" >&2
  exit 2
fi
if ! host_is_valid "$HOST"; then
  echo "ERROR: unsupported host: $HOST" >&2
  exit 2
fi
if [[ ! -d "$ROOT" ]]; then
  echo "ERROR: root is not a directory: $ROOT" >&2
  exit 1
fi

ROOT_ABS="$(cd "$ROOT" && pwd)"
AUTH="$ROOT_ABS/docs/knowledge/rules"

if [[ "$HOST" == "none" ]]; then
  echo "PROJECT none (skipped)"
  exit 0
fi

if [[ ! -d "$AUTH" ]]; then
  echo "ERROR: authority tree missing: $AUTH (run apply-rules first)" >&2
  exit 1
fi

DEST_REL="$(host_rules_dest "$HOST")"
DEST="$ROOT_ABS/$DEST_REL"

echo "PROJECT $HOST $DEST_REL"

if [[ "$DO_APPLY" -ne 1 ]]; then
  echo "SUMMARY dry-run host=$HOST dest=$DEST_REL"
  exit 0
fi

mkdir -p "$DEST"
# Sync authority tree into host namespace (directory copy, not flatten)
# Prefer rsync if available; else cp -R
HEADER="# Projected from docs/knowledge/rules/ — NOT the authority SoT.
# Edit authority under docs/knowledge/rules/; then re-run /sdd:rules-govern projection.
"
printf '%s\n' "$HEADER" >"$DEST/README.projection.md"

if command -v rsync >/dev/null 2>&1; then
  rsync -a --delete --exclude 'README.projection.md' "$AUTH"/ "$DEST"/
else
  # portable fallback without delete of extras
  find "$AUTH" -type f | while read -r f; do
    rel="${f#"$AUTH"/}"
    mkdir -p "$DEST/$(dirname "$rel")"
    cp "$f" "$DEST/$rel"
  done
fi

if [[ "$HOST" == "github-copilot" ]]; then
  COPILOT="$ROOT_ABS/.github/copilot-instructions.md"
  mkdir -p "$ROOT_ABS/.github"
  MARKER="<!-- ai4se-rules-pointer -->"
  BLOCK="$MARKER
Follow project development rules under \`docs/knowledge/rules/\` (authority).
A mirrored projection also exists at \`.github/ai4se-rules/\` for convenience — authority wins on conflict.
$MARKER"
  if [[ -f "$COPILOT" ]] && rg -q "$MARKER" "$COPILOT"; then
    echo "SKIP .github/copilot-instructions.md pointer-exists"
  elif [[ -f "$COPILOT" ]]; then
    printf '\n%s\n' "$BLOCK" >>"$COPILOT"
    echo "CREATE .github/copilot-instructions.md#pointer"
  else
    printf '%s\n' "# Copilot instructions" "" "$BLOCK" >"$COPILOT"
    echo "CREATE .github/copilot-instructions.md"
  fi
fi

echo "SUMMARY projected host=$HOST dest=$DEST_REL"
