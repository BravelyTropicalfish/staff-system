#!/usr/bin/env bash
set -euo pipefail

ROOT=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --root) ROOT="${2:-}"; shift 2 ;;
    -h|--help)
      echo "Usage: probe-pipeline-iac.sh --root <path>"
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

emit_path ".github/workflows/" dir
emit_path ".gitlab-ci.yml" file
emit_path "terraform/" dir
emit_path "infra/" dir
emit_path "iac/" dir

host="unknown"
if [[ -d "$ROOT_ABS/.git" ]]; then
  while IFS= read -r line; do
    [[ -z "$line" ]] && continue
    name="$(echo "$line" | awk '{print $1}')"
    url="$(echo "$line" | awk '{print $2}')"
    echo "NOTE remote:${name} ${url}"
    lower="$(echo "$url" | tr '[:upper:]' '[:lower:]')"
    if [[ "$lower" == *gitlab* ]]; then
      host="gitlab"
    elif [[ "$lower" == *github* ]]; then
      host="github"
    elif [[ "$host" == "unknown" ]]; then
      host="other"
    fi
  done < <(git -C "$ROOT_ABS" remote -v 2>/dev/null | awk '/\(fetch\)$/{print $1,$2}')
fi
echo "NOTE host:${host}"
