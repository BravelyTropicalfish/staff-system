#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=lib-skeleton.sh
source "$SCRIPT_DIR/lib-skeleton.sh"

ROOT=""
SPEC_BACKEND="openspec"
AGENTS_MD="create-stub"
EXTRA_KNOWLEDGE="docs-only"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --root) ROOT="${2:-}"; shift 2 ;;
    --spec-backend) SPEC_BACKEND="${2:-}"; shift 2 ;;
    --agents-md) AGENTS_MD="${2:-}"; shift 2 ;;
    --extra-knowledge) EXTRA_KNOWLEDGE="${2:-}"; shift 2 ;;
    -h|--help)
      echo "Usage: plan-skeleton.sh --root <path> [--spec-backend openspec|defer|other] [--agents-md create-stub|skip-if-exists|defer] [--extra-knowledge docs-only|docs/knowledge]"
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

emit() {
  if [[ "$1" == "SKIP" ]]; then
    echo "SKIP $2 ${3:-exists}"
  else
    echo "CREATE $2"
  fi
}

consider_dir() {
  local rel="$1"
  if [[ -d "$ROOT_ABS/$rel" ]]; then
    emit SKIP "$rel" "exists"
  else
    emit CREATE "$rel"
  fi
}

consider_file() {
  local rel="$1"
  if [[ -f "$ROOT_ABS/$rel" ]]; then
    if [[ -s "$ROOT_ABS/$rel" ]]; then
      emit SKIP "$rel" "non-empty"
    else
      emit CREATE "$rel"
    fi
  elif [[ -e "$ROOT_ABS/$rel" ]]; then
    emit SKIP "$rel" "exists-non-file"
  else
    emit CREATE "$rel"
  fi
}

for d in "${SKELETON_ALWAYS_DIRS[@]}"; do
  consider_dir "$d"
done
for f in "${SKELETON_ALWAYS_FILES[@]}"; do
  consider_file "$f"
done

if [[ "$AGENTS_MD" == "create-stub" ]]; then
  consider_file "AGENTS.md"
elif [[ "$AGENTS_MD" == "skip-if-exists" ]]; then
  if [[ -e "$ROOT_ABS/AGENTS.md" ]]; then
    emit SKIP "AGENTS.md" "skip-if-exists"
  else
    consider_file "AGENTS.md"
  fi
fi

if [[ ! -e "$ROOT_ABS/README.md" ]]; then
  emit CREATE "README.md"
else
  emit SKIP "README.md" "exists"
fi

if [[ "$SPEC_BACKEND" == "openspec" ]]; then
  for d in "${SKELETON_OPENSPEC_DIRS[@]}"; do
    consider_dir "$d"
  done
  for f in "${SKELETON_OPENSPEC_FILES[@]}"; do
    consider_file "$f"
  done
fi

if [[ "$EXTRA_KNOWLEDGE" == "docs/knowledge" ]]; then
  for d in "${SKELETON_KB_DIRS[@]}"; do
    consider_dir "$d"
  done
  for f in "${SKELETON_KB_FILES[@]}"; do
    consider_file "$f"
  done
fi

echo "SUMMARY root=$ROOT_ABS spec=$SPEC_BACKEND agents=$AGENTS_MD knowledge=$EXTRA_KNOWLEDGE"
