#!/usr/bin/env bash
# List MR targets from local git config only (primary + submodules).
# Does not fetch, push, or configure credentials.
# Usage: list-pr-targets.sh [repo-root]
# Output: TSV — kind	path	remote	fetch_url
set -euo pipefail

ROOT="${1:-.}"
cd "$ROOT"

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "ERROR: not a git repository: $ROOT" >&2
  exit 1
fi

TOP="$(git rev-parse --show-toplevel)"
cd "$TOP"

print_row() {
  local kind="$1" path="$2"
  local remote fetch
  remote="$(git -C "$path" remote 2>/dev/null | head -n1 || true)"
  if [[ -z "${remote}" ]]; then
    fetch="(no remote in local config)"
    remote="-"
  else
    fetch="$(git -C "$path" remote get-url "$remote" 2>/dev/null || echo "(unknown)")"
  fi
  printf '%s\t%s\t%s\t%s\n' "$kind" "$path" "$remote" "$fetch"
}

echo "scope: local-only (remote URLs from local config; no network)"
echo -e "kind\tpath\tremote\tfetch_url"
print_row "primary" "$TOP"

if [[ -f .gitmodules ]]; then
  while IFS= read -r path; do
    [[ -z "$path" ]] && continue
    if [[ -d "$path/.git" ]] || [[ -f "$path/.git" ]]; then
      print_row "submodule" "$TOP/$path"
    else
      printf '%s\t%s\t%s\t%s\n' "submodule" "$TOP/$path" "-" "(not checked out)"
    fi
  done < <(git config --file .gitmodules --get-regexp path | awk '{print $2}')
fi

echo
echo "REMOTE HINT (not run by this script): for each target, push the branch then"
echo "  open/update the GitLab MR with glab (see prompt-remote-mr.sh)."
