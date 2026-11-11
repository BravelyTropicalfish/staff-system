#!/usr/bin/env bash
set -euo pipefail

ROOT=""
APPLY=0
METHOD=""
BODY_FILE=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --root) ROOT="${2:-}"; shift 2 ;;
    --apply) APPLY=1; shift ;;
    --method) METHOD="${2:-}"; shift 2 ;;
    --body-file) BODY_FILE="${2:-}"; shift 2 ;;
    -h|--help)
      echo "Usage: write-exploration.sh --root <workdir> --apply --method <id> --body-file <path>"
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
if [[ -z "$METHOD" ]]; then
  echo "ERROR: --method is required" >&2
  exit 2
fi
case "$METHOD" in
  elevator-pitch|journey-map|story-map|service-blueprint) ;;
  *)
    echo "ERROR: unknown method: $METHOD (elevator-pitch|journey-map|story-map|service-blueprint)" >&2
    exit 2
    ;;
esac
if [[ -z "$BODY_FILE" || ! -f "$BODY_FILE" ]]; then
  echo "ERROR: --body-file must point to an existing file" >&2
  exit 2
fi
if [[ ! -d "$ROOT" ]]; then
  echo "ERROR: root is not a directory: $ROOT" >&2
  exit 1
fi

ROOT_ABS="$(cd "$ROOT" && pwd)"
EXP="$ROOT_ABS/exploration"
if [[ ! -d "$EXP" ]]; then
  echo "ERROR: exploration dir missing; run ensure-exploration-dir.sh first: $EXP" >&2
  exit 1
fi

TS="$(date +%Y%m%d-%H%M%S)"
OUT="$EXP/${TS}-${METHOD}.md"
n=0
while [[ -e "$OUT" ]]; do
  n=$((n + 1))
  OUT="$EXP/${TS}-${n}-${METHOD}.md"
done

cp "$BODY_FILE" "$OUT"
echo "FILE $OUT"
