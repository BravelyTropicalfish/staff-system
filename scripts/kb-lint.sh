#!/usr/bin/env bash
# Knowledge-base consistency check: every entry has a title and every index link resolves.
# The implementation is `scripts/kb-lint.mjs` so the same check runs on Windows and in CI.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
exec node "$ROOT/scripts/kb-lint.mjs"
