# Host projection path map for /sdd:rules-govern
# shellcheck shell=bash

host_rules_dest() {
  # $1=host $2=root_abs → prints relative dest dir or special token
  case "$1" in
    claude-code) echo ".claude/rules/ai4se" ;;
    cursor) echo ".cursor/rules/ai4se" ;;
    codex) echo ".codex/rules/ai4se" ;;
    qoder) echo ".qoder/rules/ai4se" ;;
    github-copilot) echo ".github/ai4se-rules" ;;
    none) echo "" ;;
    *) return 1 ;;
  esac
}

host_is_valid() {
  case "$1" in
    claude-code|cursor|codex|qoder|github-copilot|none) return 0 ;;
    *) return 1 ;;
  esac
}
