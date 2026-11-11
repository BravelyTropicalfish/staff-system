# no-circular-dependency — No circular module dependencies

## Metadata

| Field | Value |
|---|---|
| **ID** | no-circular-dependency |
| **Category** | architecture |
| **Priority** | Critical |
| **Activation** | auto |
| **Applies To** | all |
| **Stack** | — |

## Rule

1. Module dependency graph must be acyclic.
2. Break cycles via ports/interfaces or extracting a shared kernel module.
3. Do not hide cycles with dynamic imports as a permanent fix.

## Rationale

Cycles block incremental delivery and confuse both humans and agents.

## Verification

| Check | Blocks |
|---|---|
| Cycle detection in package graph | pre-commit / pr-merge |

## Examples

### Good

- Shared types live in a leaf module imported by both sides.

### Bad

- `A → B → A` package imports.

## Exceptions

- None.
