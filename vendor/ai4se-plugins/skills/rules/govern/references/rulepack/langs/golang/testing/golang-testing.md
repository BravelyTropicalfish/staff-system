# golang-testing — Go Testing

## Metadata

| Field | Value |
|---|---|
| **ID** | golang-testing |
| **Category** | testing |
| **Priority** | High |
| **Activation** | auto |
| **Applies To** | all |
| **Stack** | golang |

## Rule

## Framework

Use the standard `go test` with **table-driven tests**.

## Race Detection

Always run with the `-race` flag:

```bash
go test -race ./...
```

## Coverage

```bash
go test -cover ./...
```

## Reference

## Rationale

Stack-specific testing norms for `golang` keep agent output aligned with the project's chosen stack without duplicating common Rules.

## Verification

| Check | Blocks |
|---|---|
| PR review against this rule when touching `golang` sources | pr-merge |

## Examples

See Good/Bad (or CORRECT/WRONG) samples in the Rule section when present.

## Exceptions

- None unless recorded in the project decision log / org policy.
