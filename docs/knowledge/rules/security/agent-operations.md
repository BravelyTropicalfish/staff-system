# agent-operations — What agents may and may not do

## Metadata

| Field | Value |
|---|---|
| **ID** | agent-operations |
| **Category** | security |
| **Priority** | Critical |
| **Activation** | always |
| **Applies To** | all |
| **Stack** | — |

## Rule

1. Agents must not push directly to `main` or merge their own pull requests.
2. Agents must not delete or disable tests, skip CI checks, or change branch protection.
3. Agents must not commit secrets, tokens, or personal data.
   If they find one, stop and report it.
4. Agents must ask before a destructive or hard-to-reverse action, such as:
   - deleting files;
   - resetting or force-pushing branches;
   - changing shared infrastructure or permissions.
5. Agents must not upload repository content to an external service unless a human asks for that service.
6. Agents must follow the project rules and say when a request conflicts with them.
   They must not silently ignore a rule.
7. When an agent is unsure, it should stop and ask a human instead of guessing.

## Rationale

AI help is useful only when its limits are clear.
These rules keep one mistake from becoming a shared or permanent problem.

## Verification

| Check | Blocks |
|---|---|
| Branch protection is enabled | pr-merge |
| No secrets in commits | pr-merge |
| Destructive actions have a human confirmation | operation |

## Examples

Good: the agent asks before deleting a folder or resetting a branch.

Bad: the agent force-pushes to `main` to "clean up" history.

## Exceptions

- A human may give a written, task-specific approval.
  The approval does not cover other tasks or later actions.
