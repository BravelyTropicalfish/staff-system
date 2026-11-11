# environments — Environments, configuration, and secrets

## Metadata

| Field | Value |
|---|---|
| **ID** | environments |
| **Category** | process |
| **Priority** | High |
| **Activation** | always |
| **Applies To** | all |
| **Stack** | — |

## Rule

1. Use at least these environments: local, test, production.
   Add staging only if the team needs it.
2. Each environment has one owner and one purpose. Record both in the project README.
3. Configuration comes from environment variables or a config file.
   Do not hardcode environment values in source code.
4. Secrets (passwords, tokens, keys) live in the team's secret store.
   Never commit them. Never print them in logs.
5. A human must trigger a production deploy. An agent may prepare it but must ask first.
6. Test data must be fake or masked. Do not copy production personal data.
7. If an environment is unavailable, stop and report it.
   Do not point a test at another environment as a workaround.

## Rationale

Clear environment rules prevent test data leaks and accidental production changes.

## Verification

| Check | Blocks |
|---|---|
| No secrets in the repository | pr-merge |
| No hardcoded environment values in source | pr-merge |
| Production deploy has a human approver | deploy |

## Examples

Good: `DATABASE_URL` comes from the environment.

Bad: a password in `config.yaml`, or a test that writes to production.

## Exceptions

- A short-lived preview environment may be created for a pull request.
  It must use fake data and be removed after the pull request closes.
