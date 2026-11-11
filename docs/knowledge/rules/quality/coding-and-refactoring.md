# coding-and-refactoring — Keep changes small and safe

## Metadata

| Field | Value |
|---|---|
| **ID** | coding-and-refactoring |
| **Category** | quality |
| **Priority** | High |
| **Activation** | always |
| **Applies To** | all |
| **Stack** | — |

## Rule

1. One change should do one thing.
   Do not mix a refactor with a behavior change unless the team agrees.
2. Refactor only when tests are in place. Run the tests before and after.
3. Reuse existing code before adding a new helper.
4. Do not add an abstraction for a future need that does not exist yet.
5. Remove code your change makes unused. Do not leave commented-out code.
6. If you find an unrelated problem, record it as a separate task.
   Do not fix it in this change.
7. Before you finish, read your own changed lines and remove noise.

## Rationale

Small changes are easier to review, test, and undo.

## Verification

| Check | Blocks |
|---|---|
| Change size stays reviewable | pr-merge |
| Tests pass before and after a refactor | pr-merge |
| No dead code or unrelated edits | pr-merge |

## Examples

Good: move a function, run the tests, and change nothing else.

Bad: rename files, change behavior, and reformat code in one pull request.

## Exceptions

- A large mechanical rename may be one change if it does not change behavior and tests pass.
