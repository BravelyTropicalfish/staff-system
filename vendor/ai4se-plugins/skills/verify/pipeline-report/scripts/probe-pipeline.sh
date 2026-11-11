#!/usr/bin/env bash
set -euo pipefail

ROOT=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --root) ROOT="${2:-}"; shift 2 ;;
    -h|--help)
      echo "Usage: probe-pipeline.sh --root <path>"
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

emit_path ".git/" dir
emit_path "openspec/" dir
emit_path "openspec/changes/" dir
emit_path "docs/ai4se-plugins/" dir
emit_path "AGENTS.md" file
emit_path "README.md" file

host="unknown"
if [[ -d "$ROOT_ABS/.git" ]]; then
  while IFS= read -r line; do
    [[ -z "$line" ]] && continue
    # format: name url (fetch|push)
    name="$(echo "$line" | awk '{print $1}')"
    url="$(echo "$line" | awk '{print $2}')"
    echo "NOTE remote:${name} ${url}"
    lower="$(echo "$url" | tr '[:upper:]' '[:lower:]')"
    if [[ "$lower" == *gitlab* ]]; then
      host="gitlab"
    elif [[ "$lower" == *github.com* || "$lower" == *github.* ]]; then
      [[ "$host" == "unknown" || "$host" == "other" ]] && host="github"
    elif [[ "$host" == "unknown" ]]; then
      host="other"
    fi
  done < <(git -C "$ROOT_ABS" remote -v 2>/dev/null || true)

  branch="$(git -C "$ROOT_ABS" rev-parse --abbrev-ref HEAD 2>/dev/null || true)"
  sha="$(git -C "$ROOT_ABS" rev-parse --short HEAD 2>/dev/null || true)"
  [[ -n "$branch" ]] && echo "NOTE branch:$branch"
  [[ -n "$sha" ]] && echo "NOTE head_sha:$sha"
fi
echo "NOTE host:$host"

if command -v glab >/dev/null 2>&1; then
  echo "NOTE tool:glab:yes"
else
  echo "NOTE tool:glab:no"
fi
if command -v gh >/dev/null 2>&1; then
  echo "NOTE tool:gh:yes"
else
  echo "NOTE tool:gh:no"
fi

if [[ -d "$ROOT_ABS/openspec/changes" ]]; then
  shopt -s nullglob
  for ch in "$ROOT_ABS/openspec/changes"/*/; do
    name="$(basename "$ch")"
    [[ "$name" == "archive" ]] && continue
    echo "NOTE active-change:$name"
  done
  shopt -u nullglob
fi
