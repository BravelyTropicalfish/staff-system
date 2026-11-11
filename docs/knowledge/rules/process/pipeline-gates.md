# pipeline-gates — Checks that must pass before merge

## Metadata

| Field | Value |
|---|---|
| **ID** | pipeline-gates |
| **Category** | process |
| **Priority** | Critical |
| **Activation** | always |
| **Applies To** | all |
| **Stack** | — |

## Rule

1. CI runs on every pull request.
2. Required checks:
   - lint;
   - type check;
   - unit tests;
   - acceptance tests for the changed behavior;
   - dependency and security scan.
3. If a required check fails, the pull request cannot merge.
   Do not merge first and fix later.
4. Only a repository admin may skip a required check.
   The skip must include a reason and a follow-up date.
5. Agents and automation must not skip checks or push directly to `main`.
6. Keep CI results long enough to review the change. Default: 30 days.
7. When a required check changes, update this rule and `docs/feedback/verification-commands.md` in the same change.

## Rationale

The same checks must run for every change. This keeps the merge decision based on evidence, not on who is in a hurry.

## Verification

| Check | Blocks |
|---|---|
| Required checks are configured | pr-merge |
| All required checks pass | pr-merge |
| Any skip has a reason and follow-up date | pr-merge |

## Examples

Good: a pull request waits for the failing test to be fixed.

Bad: merging with a red build and "fix it later" in the description.

## Exceptions

- A documentation-only change may skip runtime tests if the team agrees. It still needs the docs check and review.
