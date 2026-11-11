# no-hardcoded-secrets — No hardcoded secrets

## Metadata

| Field | Value |
|---|---|
| **ID** | no-hardcoded-secrets |
| **Category** | security |
| **Priority** | Critical |
| **Activation** | always |
| **Applies To** | all |
| **Stack** | — |

## Rule

1. Never commit API keys, tokens, passwords, or private keys in source or docs.
2. Use secret stores / env injection documented in Environment Rules.
3. Rotate immediately if a secret is exposed.

## Rationale

Hardcoded secrets are a common agent/codegen failure mode with severe impact.

## Verification

| Check | Blocks |
|---|---|
| Secret scanning | pre-commit / pr-merge |

## Examples

### Good

- `api_key = os.environ["SERVICE_API_KEY"]`

### Bad

- `api_key = "sk-live-..."` in repo.

## Exceptions

- None.
