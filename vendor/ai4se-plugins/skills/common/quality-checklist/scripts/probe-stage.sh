#!/usr/bin/env bash
set -euo pipefail

ROOT=""
STAGE="all"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --root) ROOT="${2:-}"; shift 2 ;;
    --stage) STAGE="${2:-}"; shift 2 ;;
    -h|--help)
      echo "Usage: probe-stage.sh --root <path> [--stage all|p0|p1|p2|p3|p4|p5|p0,p1,...]"
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

ALL_STAGES=(p0 p1 p2 p3 p4 p5)
stage_is_valid() {
  case "$1" in
    p0|p1|p2|p3|p4|p5) return 0 ;;
    *) return 1 ;;
  esac
}

STAGES=()
if [[ -z "$STAGE" || "$STAGE" == "all" ]]; then
  STAGES=("${ALL_STAGES[@]}")
else
  IFS=',' read -r -a raw_stages <<<"$STAGE"
  for s in "${raw_stages[@]}"; do
    s="$(echo "$s" | tr -d '[:space:]')"
    [[ -z "$s" ]] && continue
    if ! stage_is_valid "$s"; then
      echo "ERROR: invalid stage: $s (allowed: all,p0,p1,p2,p3,p4,p5 or comma-separated)" >&2
      exit 2
    fi
    STAGES+=("$s")
  done
fi
if [[ "${#STAGES[@]}" -eq 0 ]]; then
  STAGES=("${ALL_STAGES[@]}")
fi
joined="$(IFS=,; echo "${STAGES[*]}")"
echo "NOTE stages:$joined"

emit_path() {
  local rel="$1" kind="$2"
  if [[ "$kind" == "dir" ]]; then
    if [[ -d "$ROOT_ABS/${rel%/}" ]]; then
      echo "FOUND $rel"
    else
      echo "MISSING $rel"
    fi
  else
    if [[ -f "$ROOT_ABS/$rel" ]]; then
      echo "FOUND $rel"
    else
      echo "MISSING $rel"
    fi
  fi
}

probe_common() {
  # Common
  for d in docs/ docs/ai4se-plugins/ docs/knowledge/ docs/knowledge/rules/ docs/feedback/ \
    openspec/ openspec/changes/ requirement-workspace/; do
    emit_path "$d" dir
  done
  emit_path "AGENTS.md" file
  emit_path "README.md" file

  if command -v rg >/dev/null 2>&1; then
    echo "NOTE tool:rg:yes"
  else
    echo "NOTE tool:rg:no"
  fi
}

load_active_changes() {
  ACTIVE=()
  if [[ -d "$ROOT_ABS/openspec/changes" ]]; then
    shopt -s nullglob
    for ch in "$ROOT_ABS/openspec/changes"/*/; do
      name="$(basename "$ch")"
      [[ "$name" == "archive" ]] && continue
      echo "NOTE active-change:$name"
      ACTIVE+=("$name")
    done
    shopt -u nullglob
  fi
}

probe_under_change() {
  local rel="$1" kind="$2"
  local name
  for name in "${ACTIVE[@]+"${ACTIVE[@]}"}"; do
    emit_path "openspec/changes/$name/$rel" "$kind"
  done
}

probe_one_stage() {
  local stage="$1"
  echo "NOTE stage:$stage"
  case "$stage" in
  p0)
    emit_path "requirement-workspace/" dir
    ;;
  p1)
    emit_path "requirement-workspace/analysis/" dir
    emit_path "requirement-workspace/ready/" dir
    ;;
  p2)
    probe_under_change "dov-ac-review.md" file
    probe_under_change "g1-decision.md" file
    if [[ "${#ACTIVE[@]}" -gt 0 ]]; then
      for name in "${ACTIVE[@]}"; do
        if [[ -d "$ROOT_ABS/openspec/changes/$name/specs" ]]; then
          echo "FOUND openspec/changes/$name/specs/"
        else
          echo "MISSING openspec/changes/$name/specs/"
        fi
      done
    fi
    ;;
  p3)
    probe_under_change "design.md" file
    probe_under_change "tasks.md" file
    ;;
  p4)
    if [[ -d "$ROOT_ABS/docs/ai4se-plugins" ]]; then
      shopt -s nullglob
      local_reports=("$ROOT_ABS"/docs/ai4se-plugins/*-local-verify-report.md)
      pipe_reports=("$ROOT_ABS"/docs/ai4se-plugins/*-pipeline-report.md)
      if [[ ${#local_reports[@]} -gt 0 && -e "${local_reports[0]}" ]]; then
        for f in "${local_reports[@]}"; do
          echo "NOTE local-verify-report:$(basename "$f")"
        done
      else
        echo "NOTE local-verify-report:none"
      fi
      if [[ ${#pipe_reports[@]} -gt 0 && -e "${pipe_reports[0]}" ]]; then
        for f in "${pipe_reports[@]}"; do
          echo "NOTE pipeline-report:$(basename "$f")"
        done
      else
        echo "NOTE pipeline-report:none"
      fi
      shopt -u nullglob
    else
      echo "NOTE local-verify-report:none"
      echo "NOTE pipeline-report:none"
    fi
    probe_under_change "g2-decision.md" file
    ;;
  p5)
    probe_under_change "g3-decision.md" file
    probe_under_change "change-summary.md" file
    ;;
  esac
}

probe_common
load_active_changes
for stage in "${STAGES[@]}"; do
  probe_one_stage "$stage"
done
