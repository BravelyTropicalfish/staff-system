# typescript-security — TypeScript/JavaScript Security

## Metadata

| Field | Value |
|---|---|
| **ID** | typescript-security |
| **Category** | security |
| **Priority** | Critical |
| **Activation** | always |
| **Applies To** | all |
| **Stack** | typescript |

## Rule

## Secret Management

```typescript
// NEVER: Hardcoded secrets
const apiKey = "sk-proj-xxxxx"

// ALWAYS: Environment variables
const apiKey = process.env.API_KEY

if (!apiKey) {
  throw new Error('API_KEY not configured')
}
```

## Agent Support

- Use **security-reviewer** skill for comprehensive security audits

## Rationale

Stack-specific security norms for `typescript` keep agent output aligned with the project's chosen stack without duplicating common Rules.

## Verification

| Check | Blocks |
|---|---|
| PR review against this rule when touching `typescript` sources | pr-merge |

## Examples

See Good/Bad (or CORRECT/WRONG) samples in the Rule section when present.

## Exceptions

- None unless recorded in the project decision log / org policy.
