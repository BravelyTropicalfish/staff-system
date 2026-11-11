# kotlin-hooks — Kotlin Hooks

## Metadata

| Field | Value |
|---|---|
| **ID** | kotlin-hooks |
| **Category** | process |
| **Priority** | Medium |
| **Activation** | auto |
| **Applies To** | all |
| **Stack** | kotlin |

## Rule

## PostToolUse Hooks

Configure in `host agent hook settings`:

- **ktfmt/ktlint**: Auto-format `.kt` and `.kts` files after edit
- **detekt**: Run static analysis after editing Kotlin files
- **./gradlew build**: Verify compilation after changes

## Rationale

Stack-specific hooks and automation norms for `kotlin` keep agent output aligned with the project's chosen stack without duplicating common Rules.

## Verification

| Check | Blocks |
|---|---|
| PR review against this rule when touching `kotlin` sources | pr-merge |

## Examples

See Good/Bad (or CORRECT/WRONG) samples in the Rule section when present.

## Exceptions

- None unless recorded in the project decision log / org policy.
