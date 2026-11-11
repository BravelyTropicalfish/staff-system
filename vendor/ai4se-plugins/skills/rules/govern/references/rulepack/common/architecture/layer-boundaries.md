# layer-boundaries — Respect declared layer dependency direction

## Metadata

| Field | Value |
|---|---|
| **ID** | layer-boundaries |
| **Category** | architecture |
| **Priority** | Critical |
| **Activation** | auto |
| **Applies To** | all |
| **Stack** | — |

## Rule

1. Dependencies must follow the project-declared layer direction (higher layers may call lower; reverse is forbidden).
2. Cross-layer shortcuts require an explicit ADR / exception record.
3. New modules declare their layer in architecture knowledge before merging.

## Rationale

Layer leaks become systemic coupling and break AI-assisted refactors.

## Verification

| Check | Blocks |
|---|---|
| Import / package dependency direction lint | pre-commit / pr-merge |

## Examples

### Good

- API adapters depend on domain ports; domain does not import adapters.

### Bad

- Domain package imports HTTP framework types directly.

## Exceptions

- None.
