#!/usr/bin/env bash
set -euo pipefail

ROOT=""
APPLY=0
BODY_FILE=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --root) ROOT="${2:-}"; shift 2 ;;
    --apply) APPLY=1; shift ;;
    --body-file) BODY_FILE="${2:-}"; shift 2 ;;
    -h|--help)
      echo "Usage: write-scheme.sh --root <workdir> --apply --body-file <path>"
      exit 0
      ;;
    *) echo "ERROR: unknown arg: $1" >&2; exit 2 ;;
  esac
done

if [[ -z "$ROOT" ]]; then
  echo "ERROR: --root is required" >&2
  exit 2
fi
if [[ "$APPLY" -ne 1 ]]; then
  echo "ERROR: --apply is required to write artifact (dry-run refuses writes)" >&2
  exit 2
fi
if [[ -z "$BODY_FILE" || ! -f "$BODY_FILE" ]]; then
  echo "ERROR: --body-file must point to an existing file" >&2
  exit 2
fi
if [[ ! -d "$ROOT" ]]; then
  echo "ERROR: root is not a directory: $ROOT" >&2
  exit 1
fi

ROOT_ABS="$(cd "$ROOT" && pwd)"
EVAL_DIR="$ROOT_ABS/evaluation"
if [[ ! -d "$EVAL_DIR" ]]; then
  echo "ERROR: evaluation dir missing; run ensure-evaluation-dir.sh first: $EVAL_DIR" >&2
  exit 1
fi

TS="$(date +%Y%m%d-%H%M%S)"
OUT="$EVAL_DIR/${TS}-agent-eval-scheme.md"
n=0
while [[ -e "$OUT" ]]; do
  n=$((n + 1))
  OUT="$EVAL_DIR/${TS}-${n}-agent-eval-scheme.md"
done

cp "$BODY_FILE" "$OUT"
echo "FILE $OUT"
