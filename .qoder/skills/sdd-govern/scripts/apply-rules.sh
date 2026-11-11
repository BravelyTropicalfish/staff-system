#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RULEPACK="$(cd "$SCRIPT_DIR/../references/rulepack" && pwd)"
# shellcheck source=lib-langs.sh
source "$SCRIPT_DIR/lib-langs.sh"

ROOT=""
DO_APPLY=0
MODE="init"
LANGS=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --root) ROOT="${2:-}"; shift 2 ;;
    --apply) DO_APPLY=1; shift ;;
    --mode) MODE="${2:-}"; shift 2 ;;
    --langs) LANGS="${2:-}"; shift 2 ;;
    -h|--help)
      echo "Usage: apply-rules.sh --root <path> --apply [--mode init|check-merge] [--langs python,...]"
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
if [[ "$DO_APPLY" -ne 1 ]]; then
  echo "ERROR: refusing to write without --apply" >&2
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
mkdir -p "$AUTH"/{process,architecture,quality,testing,security,documentation,modules}

copy_if_absent_or_empty() {
  local dest="$1" src="$2" rel="$3"
  if [[ -f "$dest" && -s "$dest" ]]; then
    if [[ "$MODE" == "check-merge" ]]; then
      echo "SKIP $rel non-empty (merge-manual)"
      return
    fi
    echo "SKIP $rel non-empty"
    return
  fi
  mkdir -p "$(dirname "$dest")"
  cp "$src" "$dest"
  echo "CREATE $rel"
}

write_stub() {
  local dest="$1" rel="$2" body="$3"
  if [[ -f "$dest" && -s "$dest" ]]; then
    echo "SKIP $rel non-empty"
    return
  fi
  mkdir -p "$(dirname "$dest")"
  printf '%s\n' "$body" >"$dest"
  echo "CREATE $rel"
}

# Copy common files
while IFS= read -r src; do
  [[ -z "$src" ]] && continue
  rel="${src#"$RULEPACK/common/"}"
  copy_if_absent_or_empty "$AUTH/$rel" "$src" "$rel"
done < <(find "$RULEPACK/common" -type f | LC_ALL=C sort)

cp_if() {
  local f="$1"
  [[ -f "$RULEPACK/$f" ]] || return 0
  copy_if_absent_or_empty "$AUTH/$f" "$RULEPACK/$f" "$f"
}
cp_if _SCHEMA.md
cp_if README.md

write_stub "$AUTH/_INDEX.md" "_INDEX.md" "# Knowledge rules index (placeholder)

Compiled by kb-lint when available. Do not hand-maintain long-term."
write_stub "$AUTH/_LOG.md" "_LOG.md" "# Rules change log

| date | id | change | author |
|---|---|---|---|
| | | | |"

# AGENTS pointer segment (root)
AGENTS="$ROOT_ABS/AGENTS.md"
POINTER=$'\n## Rules pointers\n\n- Authority: `docs/knowledge/rules/` (MECE categories)\n- Prefer `@rule:<id>` references; do not paste rule bodies here.\n- Host projections are derived copies — authority wins on conflict.\n'
if [[ -f "$AGENTS" ]]; then
  if rg -q '## Rules pointers' "$AGENTS"; then
    echo "SKIP AGENTS.md rules-pointers-exists"
  else
    printf '%s\n' "$POINTER" >>"$AGENTS"
    echo "CREATE AGENTS.md#Rules-pointers"
  fi
else
  printf '%s\n' "# AGENTS.md" "$POINTER" >"$AGENTS"
  echo "CREATE AGENTS.md"
fi

# Selected langs → rewrite into MECE (no langs/ under authority)
IFS=',' read -r -a LANG_ARR <<< "$LANGS"
for lang in "${LANG_ARR[@]:-}"; do
  lang="$(echo "$lang" | tr -d '[:space:]')"
  [[ -z "$lang" ]] && continue
  while IFS=$'\t' read -r rel src; do
    [[ -z "${rel:-}" || -z "${src:-}" ]] && continue
    copy_if_absent_or_empty "$AUTH/$rel" "$src" "$rel"
  done < <(list_lang_rule_entries "$lang")
done

echo "SUMMARY applied authority=$AUTH mode=$MODE langs=${LANGS:-none}"
