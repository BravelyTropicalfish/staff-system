#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PACK_ROOT="$(cd "$SCRIPT_DIR/../../../../../../vendor/ai4se-plugins" && pwd)"
exec "$PACK_ROOT/scripts/common/init-checkpoint-log.sh" \
  --template "$SCRIPT_DIR/../references/checkpoint-log-template.md" \
  --skill-token bootstrap \
  "$@"
