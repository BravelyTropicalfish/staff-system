#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PACK_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
exec "$PACK_ROOT/scripts/common/write-artifact.sh" \
  --artifact-name local-verify-report \
  --label REPORT \
  "$@"
