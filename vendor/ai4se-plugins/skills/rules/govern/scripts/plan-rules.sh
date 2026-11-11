#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RULEPACK="$(cd "$SCRIPT_DIR/../references/rulepack" && pwd)"
# shellcheck source=lib-langs.sh
source "$SCRIPT_DIR/lib-langs.sh"

ROOT=""
MODE="init"
LANGS=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --root) ROOT="${2:-}"; shift 2 ;;
    --mode) MODE="${2:-}"; shift 2 ;;
    --langs) LANGS="${2:-}"; shift 2 ;;
    -h|--help)
      echo "Usage: plan-rules.sh --root <path> [--mode init|check-merge] [--langs python,typescript,...]"
      echo "Allowed langs: $ALLOWED_LANGS"
      exit 0
      ;;
    *) echo "ERROR: unknown arg: $1" >&2; exit 2 ;;
  esac
done

if [[ -z "$ROOT" ]]; then
  echo "ERROR: --root is required" >&2
  exit 2
fi
if [[ ! -d "$ROOT" ]]; then
  echo "ERROR: root is not a directory: $ROOT" >&2
  exit 1
fi
case "$MODE" in
  init|check-merge) ;;
  *) echo "ERROR: --mode must be init|check-merge" >&2; exit 2 ;;
esac

validate_langs "$LANGS"

ROOT_ABS="$(cd "$ROOT" && pwd)"
AUTH="$ROOT_ABS/docs/knowledge/rules"

emit() {
  if [[ "$1" == "SKIP" ]]; then
    echo "SKIP $2 ${3:-exists}"
  elif [[ "$1" == "CONFLICT" ]]; then
    echo "CONFLICT $2 ${3:-differs}"
  else
    echo "CREATE $2"
  fi
}

consider_path() {
  local rel="$1" src="$2"
  local dest="$AUTH/$rel"
  if [[ -f "$dest" ]]; then
    if [[ "$MODE" == "check-merge" ]] && ! cmp -s "$src" "$dest"; then
      emit CONFLICT "$rel" "differs-from-rulepack"
    else
      emit SKIP "$rel" "exists"
    fi
  else
    emit CREATE "$rel"
  fi
}

# Always ensure MECE roots
for d in process architecture quality testing security documentation modules; do
  if [[ -d "$AUTH/$d" ]]; then
    echo "SKIP $d/ exists"
  else
    echo "CREATE $d/"
  fi
done

# Common tree files
while IFS= read -r src; do
  [[ -z "$src" ]] && continue
  rel="${src#"$RULEPACK/common/"}"
  consider_path "$rel" "$src"
done < <(find "$RULEPACK/common" -type f | LC_ALL=C sort)

# Root schema/readme from rulepack root
for f in _SCHEMA.md README.md; do
  if [[ -f "$RULEPACK/$f" ]]; then
    consider_path "$f" "$RULEPACK/$f"
  fi
done

# Placeholder _INDEX/_LOG if missing
for f in _INDEX.md _LOG.md; do
  if [[ -f "$AUTH/$f" ]]; then
    echo "SKIP $f exists"
  else
    echo "CREATE $f"
  fi
done

# Selected langs → MECE rewrite plan (langs/<lang>/<category>/<id>.md)
IFS=',' read -r -a LANG_ARR <<< "$LANGS"
for lang in "${LANG_ARR[@]:-}"; do
  lang="$(echo "$lang" | tr -d '[:space:]')"
  [[ -z "$lang" ]] && continue
  while IFS=$'\t' read -r rel src; do
    [[ -z "${rel:-}" || -z "${src:-}" ]] && continue
    consider_path "$rel" "$src"
  done < <(list_lang_rule_entries "$lang")
done

echo "SUMMARY root=$ROOT_ABS mode=$MODE langs=${LANGS:-none} auth=$AUTH"
