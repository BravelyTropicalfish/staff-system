# Pack stub bodies for apply-skeleton.sh

stub_content() {
  # $1 = relative path under root → prints body to stdout
  case "$1" in
    docs/feedback/README.md)
      cat <<'EOF'
# Feedback docs

Verification matrix, commands, and evaluator rubric stubs for the harness
feedback area. Fill with project-specific checks; do not treat this folder as
Living Spec authority.
EOF
      ;;
    docs/feedback/verification-matrix.md)
      cat <<'EOF'
# Verification matrix

| Area | Check | Command / evidence | Owner |
|---|---|---|---|
| (pending) | | | |
EOF
      ;;
    docs/feedback/verification-commands.md)
      cat <<'EOF'
# Verification commands

Document the three-layer stop checks used by this repo (static → runtime →
system). Replace placeholders with real commands.

## Static

```bash
# e.g. lint / typecheck
```

## Runtime

```bash
# e.g. unit / integration tests
```

## System

```bash
# e.g. smoke / e2e
```
EOF
      ;;
    docs/feedback/evaluator-rubric.md)
      cat <<'EOF'
# Evaluator rubric

Short scoring dimensions for agent/harness outcomes. Customize per project.

| Dimension | Pass criteria |
|---|---|
| (pending) | |
EOF
      ;;
    requirement-workspace/README.md)
      cat <<'EOF'
# Requirement workspace

Pre-spec requirements area: clarify intent here, then hand off stabilized
requirements into OpenSpec `openspec/changes/<id>/`.

This workspace is **not** the long-term Living Spec authority.

Shared layout with `/sdd:pre-spec-workdir` (additive; existing paths are skipped).

## Layout

- `intents/` — raw intent captures
- `requirements/` — structured requirement entries
- `handoff.md` — map into `openspec/changes/<id>/`
- `_templates/` — templates for briefs, stories, checklists
- `analysis/` — in-progress REQ folders
- `ready/` — input-ready requirements (+ stories)
- `archive/` — archived after Spec-ready handoff
- `registry.md` — traceability registry

Next: `/sdd:pre-spec-workdir` / `/sdd:pre-spec-req` as needed.
EOF
      ;;
    requirement-workspace/handoff.md)
      cat <<'EOF'
# Handoff — requirement workspace → OpenSpec

- **Workdir path:** `requirement-workspace/` (override if relocated)
- **OpenSpec changes root:** `openspec/changes/`
- **Rule:** Copy or link stabilized requirements into a change package. This
  workdir is never the long-term Living Spec authority (`openspec/specs/`).

| Artifact | Destination | Notes |
|---|---|---|
| `requirements/` entries | `openspec/changes/<id>/` proposal inputs | After `/sdd:pre-spec-req` |
| Ready / analysis packs | change package or archive | Project convention |
EOF
      ;;
    requirement-workspace/registry.md)
      cat <<'EOF'
# Requirement registry

| id | title | status | openspec change | notes |
|---|---|---|---|---|
| | | | | |
EOF
      ;;
    scripts/README.md)
      cat <<'EOF'
# Harness scripts

Bootstrap creates **placeholder** shells below. Running a placeholder prints a
customize prompt and exits until the using team replaces the body.

| Script | Role |
|---|---|
| `init.sh` | Environment / “start of day” setup |
| `kb-lint.sh` | KB consistency + compile `docs/knowledge/_INDEX.md` |
| `verify-links.sh` | Bidirectional link checks |
| `archive-change.sh` | Archive helper (e.g. wrap OpenSpec archive + KB checks) |

Do not invent a second Spec authority tree from these scripts. Re-running
`/sdd:bootstrap` will not overwrite non-empty shells (including customized
implementations).
EOF
      ;;
    scripts/init.sh)
      cat <<'EOF'
#!/usr/bin/env bash
# Placeholder from /sdd:bootstrap — replace with this repo's implementation.
set -euo pipefail
echo "自定义提示: scripts/init.sh 仍是 bootstrap 占位脚本，请由使用团队补全后再运行。" >&2
echo "Role: 环境 / 开工初始化（environment / start-of-day setup）。" >&2
echo "See scripts/README.md. Exiting." >&2
exit 1
EOF
      ;;
    scripts/kb-lint.sh)
      cat <<'EOF'
#!/usr/bin/env bash
# Placeholder from /sdd:bootstrap — replace with this repo's implementation.
set -euo pipefail
echo "自定义提示: scripts/kb-lint.sh 仍是 bootstrap 占位脚本，请由使用团队补全后再运行。" >&2
echo "Role: KB 一致性检查 + 编译 docs/knowledge/_INDEX.md。" >&2
echo "See scripts/README.md. Exiting." >&2
exit 1
EOF
      ;;
    scripts/verify-links.sh)
      cat <<'EOF'
#!/usr/bin/env bash
# Placeholder from /sdd:bootstrap — replace with this repo's implementation.
set -euo pipefail
echo "自定义提示: scripts/verify-links.sh 仍是 bootstrap 占位脚本，请由使用团队补全后再运行。" >&2
echo "Role: 双向链接检查（bidirectional link checks）。" >&2
echo "See scripts/README.md. Exiting." >&2
exit 1
EOF
      ;;
    scripts/archive-change.sh)
      cat <<'EOF'
#!/usr/bin/env bash
# Placeholder from /sdd:bootstrap — replace with this repo's implementation.
set -euo pipefail
echo "自定义提示: scripts/archive-change.sh 仍是 bootstrap 占位脚本，请由使用团队补全后再运行。" >&2
echo "Role: 归档辅助（archive helper）。" >&2
echo "See scripts/README.md. Exiting." >&2
exit 1
EOF
      ;;
    docs/knowledge/_SCHEMA.md)
      cat <<'EOF'
# Knowledge base schema

Conventions for Markdown entries under `docs/knowledge/`.

- Human-authored source of truth lives in this tree
- `_INDEX.md` is a compiled artifact — prefer regenerating via `scripts/kb-lint.sh`
- `_LOG.md` is append-only

## Suggested frontmatter

```yaml
---
id: kb.<area>.<slug>
title: …
area: architecture|domain|rules|interfaces|operations|external
---
```
EOF
      ;;
    docs/knowledge/_INDEX.md)
      cat <<'EOF'
# Knowledge index (placeholder)

Compiled by `scripts/kb-lint.sh` when available. Do not maintain a long-form
hand-authored index here long-term.
EOF
      ;;
    docs/knowledge/_LOG.md)
      cat <<'EOF'
# Knowledge log

Append-only timeline of KB changes.

| date | id | change | author |
|---|---|---|---|
| | | | |
EOF
      ;;
    docs/knowledge/architecture/README.md)
      printf '%s\n' '# Architecture knowledge' '' 'System architecture, C4, and topology notes.'
      ;;
    docs/knowledge/domain/README.md)
      printf '%s\n' '# Domain knowledge' '' 'Bounded contexts. Add one directory per BC; keep shared vocabulary under `shared/`.'
      ;;
    docs/knowledge/domain/shared/README.md)
      printf '%s\n' '# Shared domain vocabulary' '' 'Cross-BC terms and ubiquitous language.'
      ;;
    docs/knowledge/rules/README.md)
      printf '%s\n' '# Rules knowledge' '' 'Business and compliance rules.'
      ;;
    docs/knowledge/interfaces/README.md)
      printf '%s\n' '# Interfaces knowledge' '' 'API, event, and integration contracts.'
      ;;
    docs/knowledge/operations/README.md)
      printf '%s\n' '# Operations knowledge' '' 'Ops, monitoring, incident, and release notes.'
      ;;
    docs/knowledge/external/README.md)
      printf '%s\n' '# External systems knowledge' '' 'Access notes for external systems (MCP, APIs, vendors).'
      ;;
    openspec/config.yaml)
      cat <<'EOF'
# Minimal OpenSpec config placeholder — extend after OpenSpec install.
# Do not treat this file as a second Living Spec authority.
schema: openspec.placeholder
notes:
  - Living specs: openspec/specs/
  - Changes: openspec/changes/
  - Templates: openspec/templates/
EOF
      ;;
    openspec/templates/change-spec.md)
      cat <<'EOF'
# Change spec template

Use under `openspec/changes/<change-id>/` (and delta specs as required by OpenSpec).

## Summary

## Requirements

## Clarifications
EOF
      ;;
    openspec/templates/main-spec.md)
      cat <<'EOF'
# Main spec template

Use under `openspec/specs/<capability-id>/spec.md` for living capabilities.

## Purpose

## Requirements

## Non-goals
EOF
      ;;
    *)
      return 1
      ;;
  esac
  return 0
}
