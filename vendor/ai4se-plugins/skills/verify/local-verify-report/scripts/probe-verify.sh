#!/usr/bin/env bash
set -euo pipefail

ROOT=""
CHANGE_ID=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --root) ROOT="${2:-}"; shift 2 ;;
    --change-id) CHANGE_ID="${2:-}"; shift 2 ;;
    -h|--help)
      echo "Usage: probe-verify.sh --root <path> [--change-id <id>]"
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

emit_path "docs/feedback/" dir
emit_path "docs/feedback/verification-commands.md" file
emit_path "docs/feedback/verification-matrix.md" file
emit_path "docs/feedback/evaluator-rubric.md" file
emit_path "docs/knowledge/rules/" dir
emit_path "README.md" file
emit_path "AGENTS.md" file
emit_path "openspec/" dir
emit_path "openspec/changes/" dir

# --- extract whitelist commands ---
CMDS=()
FORMAT="none"
VC="$ROOT_ABS/docs/feedback/verification-commands.md"

extract_after_marker() {
  local file="$1"
  local in_block=0
  while IFS= read -r line || [[ -n "$line" ]]; do
    if [[ "$line" == *"<!-- ai4se-verify-commands -->"* ]]; then
      in_block=1
      continue
    fi
    if [[ "$in_block" -eq 1 ]]; then
      if [[ -z "$line" || "$line" == "<!--"* || "$line" =~ ^#{1,6}[[:space:]] ]]; then
        break
      fi
      CMDS+=("$line")
    fi
  done <"$file"
}

extract_bash_fence() {
  local file="$1"
  local in_fence=0
  while IFS= read -r line || [[ -n "$line" ]]; do
    if [[ "$in_fence" -eq 0 && "$line" =~ ^\`\`\`(bash|sh)[[:space:]]*$ ]]; then
      in_fence=1
      continue
    fi
    if [[ "$in_fence" -eq 1 ]]; then
      if [[ "$line" =~ ^\`\`\`[[:space:]]*$ ]]; then
        break
      fi
      [[ -z "$line" || "$line" =~ ^# ]] && continue
      CMDS+=("$line")
    fi
  done <"$file"
}

if [[ -f "$VC" ]]; then
  extract_after_marker "$VC"
  if [[ "${#CMDS[@]}" -gt 0 ]]; then
    FORMAT="marker"
  else
    extract_bash_fence "$VC"
    if [[ "${#CMDS[@]}" -gt 0 ]]; then
      FORMAT="bash-fence"
    else
      FORMAT="ambiguous"
      echo "NOTE whitelist_format:ambiguous"
    fi
  fi
fi

# README last-resort: only bash/sh fences that look verify-related nearby — keep
# simple: if still empty and README has a fence under a heading containing
# verify|test|build|check (case insensitive), extract first such fence.
if [[ "${#CMDS[@]}" -eq 0 && -f "$ROOT_ABS/README.md" ]]; then
  # shellcheck disable=SC2016
  if rg -i -q 'verif|test|build|lint|check' "$ROOT_ABS/README.md"; then
    extract_bash_fence "$ROOT_ABS/README.md"
    if [[ "${#CMDS[@]}" -gt 0 ]]; then
      FORMAT="readme-fence"
      echo "NOTE command_source:README.md"
    fi
  fi
fi

OUT_DIR="$ROOT_ABS/docs/ai4se-plugins"
COMMANDS_FILE=""
if [[ "${#CMDS[@]}" -gt 0 ]]; then
  mkdir -p "$OUT_DIR"
  TS="$(date +%Y%m%d-%H%M%S)"
  COMMANDS_FILE="$OUT_DIR/${TS}-local-verify-commands.txt"
  n=0
  while [[ -e "$COMMANDS_FILE" ]]; do
    n=$((n + 1))
    COMMANDS_FILE="$OUT_DIR/${TS}-${n}-local-verify-commands.txt"
  done
  : >"$COMMANDS_FILE"
  for c in "${CMDS[@]}"; do
    echo "COMMAND $c"
    printf '%s\n' "$c" >>"$COMMANDS_FILE"
  done
  echo "COMMANDS_FILE $COMMANDS_FILE"
  echo "NOTE whitelist_empty:no"
  echo "NOTE whitelist_format:$FORMAT"
else
  echo "NOTE whitelist_empty:yes"
  if [[ "$FORMAT" == "none" || "$FORMAT" == "ambiguous" ]]; then
    :
  fi
fi

# Tools
for t in node npm pnpm yarn mvn gradle go cargo python3 pytest git; do
  if command -v "$t" >/dev/null 2>&1; then
    echo "NOTE tool:${t}:yes"
  else
    echo "NOTE tool:${t}:no"
  fi
done

# Active changes / change artifacts
if [[ -n "$CHANGE_ID" ]]; then
  CH="openspec/changes/$CHANGE_ID"
  if [[ -d "$ROOT_ABS/$CH" ]]; then
    echo "FOUND $CH/"
    for f in tasks.md design.md proposal.md; do
      emit_path "$CH/$f" file
    done
    emit_path "$CH/specs/" dir
    if [[ -d "$ROOT_ABS/$CH/specs" ]]; then
      spec_count=0
      while IFS= read -r spec; do
        [[ -z "$spec" ]] && continue
        rel="${spec#"$ROOT_ABS/"}"
        echo "NOTE change-spec:$rel"
        spec_count=$((spec_count + 1))
        [[ "$spec_count" -ge 40 ]] && break
      done < <(find "$ROOT_ABS/$CH/specs" -type f \( -name '*.md' -o -name '*.mdx' \) 2>/dev/null | sort)
    fi
    echo "NOTE change_artifacts:found"
  else
    echo "MISSING $CH/"
    echo "NOTE change_artifacts:missing"
  fi
elif [[ -d "$ROOT_ABS/openspec/changes" ]]; then
  shopt -s nullglob
  for ch in "$ROOT_ABS/openspec/changes"/*/; do
    name="$(basename "$ch")"
    [[ "$name" == "archive" ]] && continue
    echo "NOTE active-change:$name"
  done
  shopt -u nullglob
  echo "NOTE change_artifacts:n/a"
fi
