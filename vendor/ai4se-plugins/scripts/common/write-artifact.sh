#!/usr/bin/env bash
# Shared docs/ai4se-plugins artifact writer. Skill wrappers pass --artifact-name and --label.
set -euo pipefail

ROOT=""
APPLY=0
BODY_FILE=""
ARTIFACT_NAME=""
LABEL="REPORT"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --root) ROOT="${2:-}"; shift 2 ;;
    --apply) APPLY=1; shift ;;
    --body-file) BODY_FILE="${2:-}"; shift 2 ;;
    --artifact-name) ARTIFACT_NAME="${2:-}"; shift 2 ;;
    --label) LABEL="${2:-}"; shift 2 ;;
    -h|--help)
      echo "Usage: write-artifact.sh --root <path> --apply --body-file <path> --artifact-name <name> [--label REPORT|BRIEF]"
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
if [[ -z "$ARTIFACT_NAME" ]]; then
  echo "ERROR: --artifact-name is required" >&2
  exit 2
fi
if [[ ! -d "$ROOT" ]]; then
  echo "ERROR: root is not a directory: $ROOT" >&2
  exit 1
fi

ROOT_ABS="$(cd "$ROOT" && pwd)"
TS="$(date +%Y%m%d-%H%M%S)"
OUT_DIR="$ROOT_ABS/docs/ai4se-plugins"
mkdir -p "$OUT_DIR"

OUT="$OUT_DIR/${TS}-${ARTIFACT_NAME}.md"
n=0
while [[ -e "$OUT" ]]; do
  n=$((n + 1))
  OUT="$OUT_DIR/${TS}-${n}-${ARTIFACT_NAME}.md"
done

cp "$BODY_FILE" "$OUT"
echo "$LABEL $OUT"
