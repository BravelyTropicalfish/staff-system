#!/usr/bin/env bash
# Shared checkpoint-log init. Skill wrappers pass --template and --skill-token.
set -euo pipefail

ROOT=""
HOST_AGENT="${HOST_AGENT:-}"
TEMPLATE=""
SKILL_TOKEN=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --root) ROOT="${2:-}"; shift 2 ;;
    --host-agent) HOST_AGENT="${2:-}"; shift 2 ;;
    --template) TEMPLATE="${2:-}"; shift 2 ;;
    --skill-token) SKILL_TOKEN="${2:-}"; shift 2 ;;
    -h|--help)
      echo "Usage: init-checkpoint-log.sh --root <path> --template <file> --skill-token <token> [--host-agent <name>]"
      exit 0
      ;;
    *) echo "ERROR: unknown arg: $1" >&2; exit 2 ;;
  esac
done

if [[ -z "$ROOT" ]]; then
  echo "ERROR: --root is required" >&2
  exit 2
fi
if [[ -z "$TEMPLATE" ]]; then
  echo "ERROR: --template is required" >&2
  exit 2
fi
if [[ -z "$SKILL_TOKEN" ]]; then
  echo "ERROR: --skill-token is required" >&2
  exit 2
fi
if [[ ! -d "$ROOT" ]]; then
  echo "ERROR: root is not a directory: $ROOT" >&2
  exit 1
fi
if [[ ! -f "$TEMPLATE" ]]; then
  echo "ERROR: missing template: $TEMPLATE" >&2
  exit 1
fi

ROOT_ABS="$(cd "$ROOT" && pwd)"
TS="$(date +%Y%m%d-%H%M%S)"
STARTED_AT="$(date +%Y-%m-%dT%H:%M:%S%z)"
OUT_DIR="$ROOT_ABS/docs/ai4se-plugins"
mkdir -p "$OUT_DIR"

OUT="$OUT_DIR/${TS}-${SKILL_TOKEN}-checkpoints.md"
n=0
while [[ -e "$OUT" ]]; do
  n=$((n + 1))
  OUT="$OUT_DIR/${TS}-${n}-${SKILL_TOKEN}-checkpoints.md"
done

body="$(cat "$TEMPLATE")"
body="${body//\{\{ROOT\}\}/$ROOT_ABS}"
body="${body//\{\{STARTED_AT\}\}/$STARTED_AT}"
body="${body//\{\{HOST_AGENT\}\}/${HOST_AGENT:-unknown}}"
printf '%s\n' "$body" >"$OUT"

echo "LOG $OUT"
