#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=lib-skeleton.sh
source "$SCRIPT_DIR/lib-skeleton.sh"
# shellcheck source=lib-stubs.sh
source "$SCRIPT_DIR/lib-stubs.sh"

AGENTS_STUB="$SCRIPT_DIR/../references/agents-stub.md"
OPENSPEC_README_SRC="$SCRIPT_DIR/../references/openspec-placeholders.md"

ROOT=""
DO_APPLY=0
SPEC_BACKEND="openspec"
AGENTS_MD="create-stub"
EXTRA_KNOWLEDGE="docs-only"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --root) ROOT="${2:-}"; shift 2 ;;
    --apply) DO_APPLY=1; shift ;;
    --spec-backend) SPEC_BACKEND="${2:-}"; shift 2 ;;
    --agents-md) AGENTS_MD="${2:-}"; shift 2 ;;
    --extra-knowledge) EXTRA_KNOWLEDGE="${2:-}"; shift 2 ;;
    -h|--help)
      echo "Usage: apply-skeleton.sh --root <path> --apply [same decision flags as plan-skeleton.sh]"
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
  echo "ERROR: refusing to write without --apply (run plan-skeleton.sh first, then re-run with --apply)" >&2
  exit 2
fi
if [[ ! -d "$ROOT" ]]; then
  echo "ERROR: root is not a directory: $ROOT" >&2
  exit 1
fi
case "$SPEC_BACKEND" in
  openspec|defer|other) ;;
  *) echo "ERROR: --spec-backend must be openspec|defer|other" >&2; exit 2 ;;
esac
case "$AGENTS_MD" in
  create-stub|skip-if-exists|defer) ;;
  *) echo "ERROR: --agents-md must be create-stub|skip-if-exists|defer" >&2; exit 2 ;;
esac
case "$EXTRA_KNOWLEDGE" in
  docs-only|docs/knowledge) ;;
  *) echo "ERROR: --extra-knowledge must be docs-only|docs/knowledge" >&2; exit 2 ;;
esac

ROOT_ABS="$(cd "$ROOT" && pwd)"

mk_dir() {
  local rel="$1"
  if [[ -d "$ROOT_ABS/$rel" ]]; then
    echo "SKIP $rel exists"
  else
    mkdir -p "$ROOT_ABS/$rel"
    echo "CREATE $rel"
  fi
}

write_file_if_absent_or_empty() {
  local rel="$1"
  local src="$2"
  local dest="$ROOT_ABS/$rel"
  if [[ -f "$dest" && -s "$dest" ]]; then
    echo "SKIP $rel non-empty"
    return
  fi
  if [[ -e "$dest" && ! -f "$dest" ]]; then
    echo "SKIP $rel exists-non-file"
    return
  fi
  mkdir -p "$(dirname "$dest")"
  cat "$src" >"$dest"
  echo "CREATE $rel"
}

write_stub_if_absent_or_empty() {
  local rel="$1"
  local dest="$ROOT_ABS/$rel"
  if [[ -f "$dest" && -s "$dest" ]]; then
    echo "SKIP $rel non-empty"
    return
  fi
  if [[ -e "$dest" && ! -f "$dest" ]]; then
    echo "SKIP $rel exists-non-file"
    return
  fi
  mkdir -p "$(dirname "$dest")"
  if ! stub_content "$rel" >"$dest"; then
    echo "ERROR: no stub body for $rel" >&2
    rm -f "$dest"
    exit 1
  fi
  echo "CREATE $rel"
}

write_minimal_readme() {
  local dest="$ROOT_ABS/README.md"
  if [[ -e "$dest" ]]; then
    echo "SKIP README.md exists"
    return
  fi
  printf '%s\n' "# Repository" "" "Bootstrapped for AI-assisted SDD. See AGENTS.md and /sdd:bootstrap notes." >"$dest"
  echo "CREATE README.md"
}

for d in "${SKELETON_ALWAYS_DIRS[@]}"; do
  mk_dir "$d"
done
for f in "${SKELETON_ALWAYS_FILES[@]}"; do
  write_stub_if_absent_or_empty "$f"
done

if [[ "$AGENTS_MD" == "create-stub" || "$AGENTS_MD" == "skip-if-exists" ]]; then
  if [[ "$AGENTS_MD" == "skip-if-exists" && -e "$ROOT_ABS/AGENTS.md" ]]; then
    echo "SKIP AGENTS.md skip-if-exists"
  else
    write_file_if_absent_or_empty "AGENTS.md" "$AGENTS_STUB"
  fi
fi

write_minimal_readme

if [[ "$SPEC_BACKEND" == "openspec" ]]; then
  for d in "${SKELETON_OPENSPEC_DIRS[@]}"; do
    mk_dir "$d"
  done
  write_file_if_absent_or_empty "openspec/README.md" "$OPENSPEC_README_SRC"
  for f in "${SKELETON_OPENSPEC_FILES[@]}"; do
    if [[ "$f" == "openspec/README.md" ]]; then
      continue
    fi
    write_stub_if_absent_or_empty "$f"
  done
fi

if [[ "$EXTRA_KNOWLEDGE" == "docs/knowledge" ]]; then
  for d in "${SKELETON_KB_DIRS[@]}"; do
    mk_dir "$d"
  done
  for f in "${SKELETON_KB_FILES[@]}"; do
    write_stub_if_absent_or_empty "$f"
  done
fi

echo "SUMMARY applied root=$ROOT_ABS"
