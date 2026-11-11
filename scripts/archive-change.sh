#!/usr/bin/env bash
# Archive an OpenSpec change and keep the knowledge base consistent.
#
#   1. OpenSpec merges the change's delta specs into `openspec/specs/` and moves the change folder
#      under `openspec/changes/archive/`.
#   2. `scripts/kb-lint.sh` checks that every knowledge entry still has a title and that the index
#      links still resolve.
#
# Both must succeed: an archive that leaves the knowledge base broken is reported as a failure, and
# the operator restores consistency before the gate is recorded. Set `OPENSPEC` when the CLI is not
# on PATH (e.g. `OPENSPEC="$APPDATA/npm/openspec.cmd"` on Windows).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OPENSPEC="${OPENSPEC:-openspec}"

if [ "$#" -lt 1 ]; then
  echo "usage: scripts/archive-change.sh <change-id> [--skip-specs] [--no-validate]" >&2
  exit 2
fi

CHANGE="$1"
shift

if [ ! -d "$ROOT/openspec/changes/$CHANGE" ]; then
  echo "archive-change: no change named $CHANGE under openspec/changes/" >&2
  exit 1
fi

if ! command -v "$OPENSPEC" >/dev/null 2>&1; then
  echo "archive-change: OpenSpec CLI not found ($OPENSPEC); install it or set OPENSPEC" >&2
  exit 1
fi

cd "$ROOT"
"$OPENSPEC" archive "$CHANGE" --yes "$@"

if ! bash "$ROOT/scripts/kb-lint.sh"; then
  echo "archive-change: the change is archived but the knowledge base is inconsistent;" >&2
  echo "archive-change: fix the reported entries before recording the merge gate" >&2
  exit 1
fi

echo "archive-change: $CHANGE archived and the knowledge base is consistent"
