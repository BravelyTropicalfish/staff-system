#!/usr/bin/env bash
set -euo pipefail

ROOT=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --root) ROOT="${2:-}"; shift 2 ;;
    -h|--help)
      echo "Usage: ensure-evaluation-dir.sh --root <workdir>"
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

ROOT_ABS="$(cd "$ROOT" && pwd)"
EVAL_DIR="$ROOT_ABS/evaluation"
mkdir -p "$EVAL_DIR"

README="$EVAL_DIR/README.md"
if [[ ! -f "$README" ]]; then
  cat >"$README" <<'EOF'
# Evaluation scheme artifacts

Optional outputs from `/sdd:agent-evalset`. Scheme documents only — not Living
Spec authority, not OpenSpec authority, and not a pack-local golden dataset.
Do not check golden sample corpora into the AI4SE plugin pack.
EOF
fi

echo "DIR $EVAL_DIR"
