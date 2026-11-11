# swift-hooks — Swift Hooks

## Metadata

| Field | Value |
|---|---|
| **ID** | swift-hooks |
| **Category** | process |
| **Priority** | Medium |
| **Activation** | auto |
| **Applies To** | all |
| **Stack** | swift |

## Rule

## PostToolUse Hooks

Configure in `host agent hook settings`:

- **SwiftFormat**: Auto-format `.swift` files after edit
- **SwiftLint**: Run lint checks after editing `.swift` files
- **swift build**: Type-check modified packages after edit

## Warning

Flag `print()` statements — use `os.Logger` or structured logging instead for production code.

## Rationale

Stack-specific hooks and automation norms for `swift` keep agent output aligned with the project's chosen stack without duplicating common Rules.

## Verification

| Check | Blocks |
|---|---|
| PR review against this rule when touching `swift` sources | pr-merge |

## Examples

See Good/Bad (or CORRECT/WRONG) samples in the Rule section when present.

## Exceptions

- None unless recorded in the project decision log / org policy.
