# python-hooks — Python Hooks

## Metadata

| Field | Value |
|---|---|
| **ID** | python-hooks |
| **Category** | process |
| **Priority** | Medium |
| **Activation** | auto |
| **Applies To** | all |
| **Stack** | python |

## Rule

## PostToolUse Hooks

Configure in `host agent hook settings`:

- **black/ruff**: Auto-format `.py` files after edit
- **mypy/pyright**: Run type checking after editing `.py` files

## Warnings

- Warn about `print()` statements in edited files (use `logging` module instead)

## Rationale

Stack-specific hooks and automation norms for `python` keep agent output aligned with the project's chosen stack without duplicating common Rules.

## Verification

| Check | Blocks |
|---|---|
| PR review against this rule when touching `python` sources | pr-merge |

## Examples

See Good/Bad (or CORRECT/WRONG) samples in the Rule section when present.

## Exceptions

- None unless recorded in the project decision log / org policy.
