#!/usr/bin/env bash
set -euo pipefail

ROOT=""
COMMANDS_FILE=""
LOG_LINES="${LOG_LINES:-40}"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --root) ROOT="${2:-}"; shift 2 ;;
    --commands-file) COMMANDS_FILE="${2:-}"; shift 2 ;;
    --log-lines) LOG_LINES="${2:-}"; shift 2 ;;
    -h|--help)
      echo "Usage: run-verify.sh --root <path> --commands-file <path> [--log-lines N]"
      exit 0
      ;;
    *) echo "ERROR: unknown arg: $1" >&2; exit 2 ;;
  esac
done

if [[ -z "$ROOT" ]]; then
  echo "ERROR: --root is required" >&2
  exit 2
fi
if [[ -z "$COMMANDS_FILE" ]]; then
  echo "ERROR: --commands-file is required" >&2
  exit 2
fi
if [[ ! -d "$ROOT" ]]; then
  echo "ERROR: root is not a directory: $ROOT" >&2
  exit 1
fi
if [[ ! -f "$COMMANDS_FILE" ]]; then
  echo "ERROR: commands file not found: $COMMANDS_FILE" >&2
  exit 1
fi

ROOT_ABS="$(cd "$ROOT" && pwd)"
OUT_DIR="$ROOT_ABS/docs/ai4se-plugins"
mkdir -p "$OUT_DIR"
TS="$(date +%Y%m%d-%H%M%S)"
RUN_LOG="$OUT_DIR/${TS}-local-verify-run.log"
n=0
while [[ -e "$RUN_LOG" ]]; do
  n=$((n + 1))
  RUN_LOG="$OUT_DIR/${TS}-${n}-local-verify-run.log"
done

: >"$RUN_LOG"
echo "RUN_LOG $RUN_LOG"

failed=0
attempted=0
while IFS= read -r cmd || [[ -n "$cmd" ]]; do
  # trim
  cmd="${cmd#"${cmd%%[![:space:]]*}"}"
  cmd="${cmd%"${cmd##*[![:space:]]}"}"
  [[ -z "$cmd" || "$cmd" =~ ^# ]] && continue
  attempted=$((attempted + 1))
  echo "RUN $cmd"
  {
    echo "===== CMD: $cmd ====="
    echo "cwd: $ROOT_ABS"
    echo "started: $(date +%Y-%m-%dT%H:%M:%S%z)"
  } >>"$RUN_LOG"

  set +e
  # Run via bash -lc so simple pipelines in whitelist work; still only declared cmds.
  (cd "$ROOT_ABS" && bash -lc "$cmd") >>"$RUN_LOG" 2>&1
  rc=$?
  set -e

  {
    echo "exit: $rc"
    echo "ended: $(date +%Y-%m-%dT%H:%M:%S%z)"
    echo
  } >>"$RUN_LOG"

  # Summary: last N lines of this command block (best-effort)
  summary="$(tail -n "$LOG_LINES" "$RUN_LOG" | tr '\n' ' ' | sed 's/  */ /g' | cut -c1-240)"
  echo "RESULT exit=$rc cmd=$cmd"
  echo "SUMMARY $summary"

  if [[ "$rc" -ne 0 ]]; then
    failed=$((failed + 1))
    echo "FAIL_CMD $cmd"
  else
    echo "PASS_CMD $cmd"
  fi
done <"$COMMANDS_FILE"

echo "NOTE commands_attempted:$attempted"
echo "NOTE commands_failed:$failed"

if [[ "$attempted" -eq 0 ]]; then
  echo "NOTE runner_empty:yes"
  exit 0
fi

# Always exit 0 so the skill can aggregate; failures are in NOTE/FAIL_CMD lines.
exit 0
