#!/usr/bin/env bash
# Local-only helper: print suggested GitLab MR remote commands; do not run them.
# Does not call glab, push, fetch, or configure credentials.
# Usage:
#   prompt-remote-mr.sh [--repo-root DIR] [--title TEXT] [--description TEXT]
#                       [--target-branch BRANCH] [--source-branch BRANCH]
#
# Exit 0 always when local context is readable (hints printed).
# Does NOT check local-verify waiver — the skill must do that before remote steps.
set -euo pipefail

ROOT="."
TITLE=""
DESCRIPTION=""
TARGET_BRANCH=""
SOURCE_BRANCH=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --repo-root)
      ROOT="${2:?}"
      shift 2
      ;;
    --title)
      TITLE="${2:?}"
      shift 2
      ;;
    --description)
      DESCRIPTION="${2:?}"
      shift 2
      ;;
    --target-branch)
      TARGET_BRANCH="${2:?}"
      shift 2
      ;;
    --source-branch)
      SOURCE_BRANCH="${2:?}"
      shift 2
      ;;
    -h|--help)
      cat <<'EOF'
Local-only: print suggested GitLab MR remote commands (does not execute them).
Usage:
  prompt-remote-mr.sh [--repo-root DIR] [--title TEXT] [--description TEXT]
                      [--target-branch BRANCH] [--source-branch BRANCH]
Does not push, auth, or invoke glab. Operator/agent runs remote steps separately.
EOF
      exit 0
      ;;
    *)
      echo "ERROR: unknown argument: $1" >&2
      exit 2
      ;;
  esac
done

cd "$ROOT"

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "ERROR: not a git repository: $ROOT" >&2
  exit 1
fi

if [[ -z "${SOURCE_BRANCH}" ]]; then
  SOURCE_BRANCH="$(git branch --show-current 2>/dev/null || true)"
fi
if [[ -z "${SOURCE_BRANCH}" ]]; then
  echo "ERROR: cannot determine source branch (detached HEAD?)" >&2
  exit 1
fi

if [[ -z "${TITLE}" ]]; then
  TITLE="$(git log -1 --pretty=%s)"
fi

remote="$(git remote 2>/dev/null | head -n1 || true)"
if [[ -z "${remote}" ]]; then
  remote="<remote>"
fi

echo "scope: local-only (hints only; no network / no glab invocation)"
echo "source-branch: ${SOURCE_BRANCH}"
echo "title: ${TITLE}"
[[ -n "${TARGET_BRANCH}" ]] && echo "target-branch: ${TARGET_BRANCH}"
[[ -n "${DESCRIPTION}" ]] && echo "description: ${DESCRIPTION}"
echo

echo "REMOTE HINT — run these outside this script when ready:"
echo
echo "1) Ensure credentials are configured on the machine (not by this pack):"
echo "     glab auth login"
echo
echo "2) Publish the branch if needed:"
echo "     git push -u ${remote} ${SOURCE_BRANCH}"
echo
echo "3) Create an MR (if none exists for this branch):"
create_cmd=(glab mr create --source-branch "${SOURCE_BRANCH}" --title "${TITLE}" --yes)
if [[ -n "${DESCRIPTION}" ]]; then
  create_cmd+=(--description "${DESCRIPTION}")
fi
if [[ -n "${TARGET_BRANCH}" ]]; then
  create_cmd+=(--target-branch "${TARGET_BRANCH}")
fi
printf '     '
printf '%q ' "${create_cmd[@]}"
echo
echo
echo "4) Or update an existing open MR (replace <iid>):"
update_cmd=(glab mr update "<iid>" --title "${TITLE}")
if [[ -n "${DESCRIPTION}" ]]; then
  update_cmd+=(--description "${DESCRIPTION}")
fi
if [[ -n "${TARGET_BRANCH}" ]]; then
  update_cmd+=(--target-branch "${TARGET_BRANCH}")
fi
printf '     '
printf '%q ' "${update_cmd[@]}"
echo
echo "     glab mr view <iid>"
echo
echo "This script does not perform steps 1–4."
exit 0
