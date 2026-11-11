#!/usr/bin/env bash
set -euo pipefail

ROOT=""
FOCUS=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --root) ROOT="${2:-}"; shift 2 ;;
    --focus) FOCUS="${2:-}"; shift 2 ;;
    -h|--help)
      echo "Usage: probe-layout.sh --root <path> [--focus path,path]"
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

ALWAYS_DIRS=(
  "docs/"
  "docs/ai4se-plugins/"
  "docs/knowledge/"
  "docs/knowledge/rules/"
  "docs/feedback/"
  "requirement-workspace/"
  "openspec/"
  "openspec/specs/"
  "openspec/changes/"
)
ALWAYS_FILES=(
  "AGENTS.md"
  "README.md"
  "package.json"
  "pnpm-workspace.yaml"
  "go.mod"
  "Cargo.toml"
  "pom.xml"
  "build.gradle"
  "build.gradle.kts"
  "pyproject.toml"
  "requirements.txt"
)

for d in "${ALWAYS_DIRS[@]}"; do
  if [[ -d "$ROOT_ABS/${d%/}" ]]; then
    echo "FOUND $d"
  else
    echo "MISSING $d"
  fi
done

for f in "${ALWAYS_FILES[@]}"; do
  if [[ -f "$ROOT_ABS/$f" ]]; then
    echo "FOUND $f"
  else
    echo "MISSING $f"
  fi
done

# Active changes
if [[ -d "$ROOT_ABS/openspec/changes" ]]; then
  shopt -s nullglob
  for ch in "$ROOT_ABS/openspec/changes"/*/; do
    name="$(basename "$ch")"
    [[ "$name" == "archive" ]] && continue
    echo "NOTE active-change:$name"
  done
  shopt -u nullglob
fi

# Focus overlays
if [[ -n "$FOCUS" ]]; then
  IFS=',' read -r -a focus_paths <<<"$FOCUS"
  for fp in "${focus_paths[@]}"; do
    fp="${fp#"${fp%%[![:space:]]*}"}"
    fp="${fp%"${fp##*[![:space:]]}"}"
    [[ -z "$fp" ]] && continue
    if [[ -e "$ROOT_ABS/$fp" ]]; then
      if [[ -d "$ROOT_ABS/$fp" ]]; then
        echo "FOUND focus:$fp/"
        for name in AGENTS.md package.json go.mod openspec; do
          if [[ -e "$ROOT_ABS/$fp/$name" ]]; then
            if [[ -d "$ROOT_ABS/$fp/$name" ]]; then
              echo "FOUND focus:$fp/$name/"
            else
              echo "FOUND focus:$fp/$name"
            fi
          else
            if [[ "$name" == "openspec" ]]; then
              echo "MISSING focus:$fp/openspec/"
            else
              echo "MISSING focus:$fp/$name"
            fi
          fi
        done
      else
        echo "FOUND focus:$fp"
      fi
    else
      echo "MISSING focus:$fp"
    fi
  done
fi

# Greenfield heuristic
gf=yes
[[ -f "$ROOT_ABS/AGENTS.md" ]] && gf=no
[[ -d "$ROOT_ABS/openspec" ]] && gf=no
[[ -d "$ROOT_ABS/docs/knowledge" ]] && gf=no
for m in package.json go.mod Cargo.toml pom.xml build.gradle build.gradle.kts pyproject.toml requirements.txt; do
  [[ -f "$ROOT_ABS/$m" ]] && gf=no
done
echo "NOTE greenfield_suspected:$gf"
