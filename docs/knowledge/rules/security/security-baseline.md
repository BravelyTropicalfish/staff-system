# security-baseline — Basic security rules

## Metadata

| Field | Value |
|---|---|
| **ID** | security-baseline |
| **Category** | security |
| **Priority** | Critical |
| **Activation** | always |
| **Applies To** | all |
| **Stack** | — |

## Rule

1. Check login and permission on the server for every protected action.
   Do not rely on the UI to hide a button.
2. Validate and limit all input at the boundary.
3. Give each account, token, and service only the access it needs.
4. Do not put secrets in code, config, logs, or error messages.
   Use the team's secret store.
5. Do not log passwords, tokens, personal data, or full request bodies.
6. Return safe error messages.
   Do not show stack traces or internal paths to users.
7. Keep dependencies updated and scan them.
   Review a new package before adding it.
8. Use an encrypted connection (TLS) for network traffic.
   Do not turn off certificate checks.

## Rationale

Most incidents start with a missing check, a leaked secret, or an old dependency.

## Verification

| Check | Blocks |
|---|---|
| Permission tests for protected actions | pr-merge |
| Secret scan finds nothing | pr-merge |
| Dependency scan finds no critical issue | pr-merge |

## Examples

Good: the server checks the user's role before an admin action.

Bad: a hidden admin button as the only protection.

## Exceptions

- A local demo may use fake data, but it must still keep secrets out of the repository.
