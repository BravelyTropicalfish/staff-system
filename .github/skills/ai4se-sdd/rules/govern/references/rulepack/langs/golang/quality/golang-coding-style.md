# golang-coding-style — Go Coding Style

## Metadata

| Field | Value |
|---|---|
| **ID** | golang-coding-style |
| **Category** | quality |
| **Priority** | High |
| **Activation** | auto |
| **Applies To** | all |
| **Stack** | golang |

## Rule

## Formatting

- **gofmt** and **goimports** are mandatory — no style debates

## Design Principles

- Accept interfaces, return structs
- Keep interfaces small (1-3 methods)

## Error Handling

Always wrap errors with context:

```go
if err != nil {
    return fmt.Errorf("failed to create user: %w", err)
}
```

## Reference

## Rationale

Stack-specific coding style norms for `golang` keep agent output aligned with the project's chosen stack without duplicating common Rules.

## Verification

| Check | Blocks |
|---|---|
| PR review against this rule when touching `golang` sources | pr-merge |

## Examples

See Good/Bad (or CORRECT/WRONG) samples in the Rule section when present.

## Exceptions

- None unless recorded in the project decision log / org policy.
