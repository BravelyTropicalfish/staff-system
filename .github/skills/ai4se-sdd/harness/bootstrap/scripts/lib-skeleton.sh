# Shared skeleton node lists for plan-skeleton.sh / apply-skeleton.sh
# Sourced by those scripts. Paths are relative to --root.
# Trailing slash = directory; otherwise = file to create from a stub or empty.

# Always (independent of knowledge/openspec decisions)
SKELETON_ALWAYS_DIRS=(
  "docs/"
  "docs/ai4se-plugins/"
  "docs/feedback/"
  "requirement-workspace/"
  "requirement-workspace/intents/"
  "requirement-workspace/requirements/"
  "requirement-workspace/_templates/"
  "requirement-workspace/analysis/"
  "requirement-workspace/ready/"
  "requirement-workspace/archive/"
  "scripts/"
)

SKELETON_ALWAYS_FILES=(
  "docs/feedback/README.md"
  "docs/feedback/verification-matrix.md"
  "docs/feedback/verification-commands.md"
  "docs/feedback/evaluator-rubric.md"
  "requirement-workspace/README.md"
  "requirement-workspace/handoff.md"
  "requirement-workspace/registry.md"
  "scripts/README.md"
  "scripts/init.sh"
  "scripts/kb-lint.sh"
  "scripts/verify-links.sh"
  "scripts/archive-change.sh"
)

# When --extra-knowledge docs/knowledge
SKELETON_KB_DIRS=(
  "docs/knowledge/"
  "docs/knowledge/architecture/"
  "docs/knowledge/domain/"
  "docs/knowledge/domain/shared/"
  "docs/knowledge/rules/"
  "docs/knowledge/interfaces/"
  "docs/knowledge/operations/"
  "docs/knowledge/external/"
)

SKELETON_KB_FILES=(
  "docs/knowledge/_SCHEMA.md"
  "docs/knowledge/_INDEX.md"
  "docs/knowledge/_LOG.md"
  "docs/knowledge/architecture/README.md"
  "docs/knowledge/domain/README.md"
  "docs/knowledge/domain/shared/README.md"
  "docs/knowledge/rules/README.md"
  "docs/knowledge/interfaces/README.md"
  "docs/knowledge/operations/README.md"
  "docs/knowledge/external/README.md"
)

# When --spec-backend openspec
SKELETON_OPENSPEC_DIRS=(
  "openspec/"
  "openspec/templates/"
  "openspec/specs/"
  "openspec/changes/"
  "openspec/changes/archive/"
)

SKELETON_OPENSPEC_FILES=(
  "openspec/README.md"
  "openspec/config.yaml"
  "openspec/templates/change-spec.md"
  "openspec/templates/main-spec.md"
)
