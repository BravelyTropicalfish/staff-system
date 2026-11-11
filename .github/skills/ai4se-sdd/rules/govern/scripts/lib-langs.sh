# Hosted language allowlist for /sdd:rules-govern
# shellcheck shell=bash

# Non-common rulepack langs — only these folders are supported.
# Order matches pack docs; do not add languages without an explicit allowlist change.
ALLOWED_LANGS="cpp golang java kotlin python react react-native rust swift typescript vue web"

lang_is_allowed() {
  local needle="$1" tok
  for tok in $ALLOWED_LANGS; do
    if [[ "$tok" == "$needle" ]]; then
      return 0
    fi
  done
  return 1
}

# Validate comma-separated langs; print ERROR and return 2 on unknown.
validate_langs() {
  local raw="${1:-}" tok
  [[ -z "$raw" ]] && return 0
  IFS=',' read -r -a arr <<< "$raw"
  for tok in "${arr[@]:-}"; do
    tok="$(echo "$tok" | tr -d '[:space:]')"
    [[ -z "$tok" ]] && continue
    if ! lang_is_allowed "$tok"; then
      echo "ERROR: unsupported lang '$tok' (allowed: $ALLOWED_LANGS)" >&2
      return 2
    fi
  done
  return 0
}

# List rule source files for a lang under RULEPACK/langs/<lang>/ (skip README*).
# Layout: langs/<lang>/<mece-category>/<rule-id>.md
# Prints "rel<TAB>abs" where rel is path under authority (e.g. quality/async-only.md).
# Requires RULEPACK set by caller.
list_lang_rule_entries() {
  local lang="$1" dir
  dir="$RULEPACK/langs/$lang"
  [[ -d "$dir" ]] || return 0
  while IFS= read -r src; do
    [[ -z "$src" ]] && continue
    local rel="${src#"$dir"/}"
    # skip README at any depth
    case "$(basename "$src")" in
      README.md|README.*.md) continue ;;
    esac
    # must live under a MECE category folder (not flat under lang/)
    case "$rel" in
      */*) ;;
      *)
        echo "ERROR: lang rule must be under langs/$lang/<category>/: $rel" >&2
        return 1
        ;;
    esac
    printf '%s\t%s\n' "$rel" "$src"
  done < <(find "$dir" -type f -name '*.md' | LC_ALL=C sort)
}
