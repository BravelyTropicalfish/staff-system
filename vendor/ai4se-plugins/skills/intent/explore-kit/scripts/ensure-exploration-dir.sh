#!/usr/bin/env bash
set -euo pipefail

ROOT=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --root) ROOT="${2:-}"; shift 2 ;;
    -h|--help)
      echo "Usage: ensure-exploration-dir.sh --root <workdir>"
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
EXP="$ROOT_ABS/exploration"
mkdir -p "$EXP"

README="$EXP/README.md"
if [[ ! -f "$README" ]]; then
  cat >"$README" <<'EOF'
# Exploration artifacts

Optional outputs from `/sdd:explore`. Process records only — not Living Spec
or OpenSpec authority. Prefer Confirmed / Assumption / Candidate labels.
EOF
fi

echo "DIR $EXP"
