# golang-hooks — Go Hooks

## Metadata

| Field | Value |
|---|---|
| **ID** | golang-hooks |
| **Category** | process |
| **Priority** | Medium |
| **Activation** | auto |
| **Applies To** | all |
| **Stack** | golang |

## Rule

## PostToolUse Hooks

Configure in `host agent hook settings`:

- **gofmt/goimports**: Auto-format `.go` files after edit
- **go vet**: Run static analysis after editing `.go` files
- **staticcheck**: Run extended static checks on modified packages

## Rationale

Stack-specific hooks and automation norms for `golang` keep agent output aligned with the project's chosen stack without duplicating common Rules.

## Verification

| Check | Blocks |
|---|---|
| PR review against this rule when touching `golang` sources | pr-merge |

## Examples

See Good/Bad (or CORRECT/WRONG) samples in the Rule section when present.

## Exceptions

- None unless recorded in the project decision log / org policy.
