# branching — Branch and merge rules

## Metadata

| Field | Value |
|---|---|
| **ID** | branching |
| **Category** | process |
| **Priority** | High |
| **Activation** | always |
| **Applies To** | all |
| **Stack** | — |

## Rule

1. `main` is the only long-lived branch. Do not push directly to it.
2. Start each change from `main` on a short branch named `<type>/<short-name>`.
   Use `feat`, `fix`, `docs`, `test`, or `chore` as the type.
3. Keep one change per branch. If two changes are unrelated, use two branches.
4. Keep each pull request small. As a default, stay under 400 changed lines.
   Split larger work before review.
5. A pull request may merge only when:
   - CI passes;
   - at least one other person has reviewed it;
   - no blocking comments are open.
6. Delete the branch after merge. Do not reuse it.

## Rationale

Small, short-lived branches are easier to review and less likely to conflict.

## Verification

| Check | Blocks |
|---|---|
| Branch name and target branch | pr-merge |
| CI result | pr-merge |
| Reviewer approval | pr-merge |

## Examples

Good: `feat/add-login`, `fix/null-total`

Bad: `my-work`, direct commits on `main`

## Exceptions

- A release branch may live longer. Name it `release/<version>` and record its owner.
- An emergency fix may use `hotfix/<short-name>`. It still needs review after merge.
