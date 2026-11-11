#!/usr/bin/env bash
# Local-only branch preflight for /sdd:pr-submit.
# Does not fetch, push, or configure credentials.
# Usage: preflight-branch.sh [repo-root]
# Exit: 0 clean + ready locally; 2 dirty worktree; 1 hard local error
set -euo pipefail

ROOT="${1:-.}"
cd "$ROOT"

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "ERROR: not a git repository: $ROOT" >&2
  exit 1
fi

branch="$(git branch --show-current 2>/dev/null || true)"
if [[ -z "${branch}" ]]; then
  echo "ERROR: detached HEAD; check out a branch before opening an MR" >&2
  exit 1
fi

echo "scope:    local-only (no fetch/push/auth)"
echo "repo:     $(git rev-parse --show-toplevel)"
echo "branch:   ${branch}"

upstream="$(git rev-parse --abbrev-ref '@{upstream}' 2>/dev/null || true)"
need_remote_hint=0

if [[ -z "${upstream}" ]]; then
  echo "upstream: (none in local config)"
  need_remote_hint=1
else
  echo "upstream: ${upstream} (local tracking ref only; not refreshed)"
  # Uses local refs only — does not contact the network.
  read -r behind ahead <<<"$(git rev-list --left-right --count "${upstream}...HEAD" 2>/dev/null || echo "0 0")"
  echo "ahead:    ${ahead} (vs local upstream ref)"
  echo "behind:   ${behind} (vs local upstream ref)"
  if [[ "${ahead}" != "0" ]]; then
    need_remote_hint=1
  fi
fi

if [[ -n "$(git status --porcelain)" ]]; then
  echo "worktree: dirty"
  git status --short
  echo
  echo "REMOTE HINT: commit or stash local changes before any push / MR create."
  exit 2
fi

echo "worktree: clean"

if [[ "${need_remote_hint}" -eq 1 ]]; then
  echo
  echo "REMOTE HINT (not run by this script):"
  if [[ -z "${upstream}" ]]; then
    echo "  git push -u <remote> ${branch}"
  else
    echo "  git push   # publish local commits that are ahead of upstream"
  fi
  echo "  Then create/update the GitLab MR with glab (see prompt-remote-mr.sh)."
fi

exit 0
