# testing-strategy — What to test and when

## Metadata

| Field | Value |
|---|---|
| **ID** | testing-strategy |
| **Category** | testing |
| **Priority** | High |
| **Activation** | always |
| **Applies To** | all |
| **Stack** | — |

## Rule

1. Write unit tests together with the code. One test should check one behavior.
2. Acceptance tests (API or UI) must come from the agreed scenarios.
   Do not add new scope only in tests.
3. Before merge, the unit tests and the acceptance tests for the changed behavior must pass.
4. Do not delete, skip, or weaken a test to make a build pass.
   If a test is wrong, fix it in a separate change that explains why.
5. Test data must be fake or masked. Never copy production data into tests.
6. Coverage must be 100%. Do not lower the target to pass a build.
   If a line cannot be tested, record why in the pull request and get reviewer approval.
7. Tests must pass in any order and must not depend on each other.

## Rationale

Tests are the evidence that a change works.
Coverage shows what was not tested. A high number does not replace a good check.

## Verification

| Check | Blocks |
|---|---|
| Unit and acceptance tests pass in CI | pr-merge |
| No new skipped or deleted tests without a reason | pr-merge |
| Coverage report shows 100% | pr-merge |
| Every excluded line has a reason and reviewer approval | pr-merge |

## Examples

Good: a test for "zero weight returns a validation error".

Bad: deleting a failing test, changing the expected value to match a bug, or adding empty tests only to raise the number.

## Exceptions

- A temporary skip is allowed for a known external outage. Record the reason, owner, and end date.
- Generated code may be excluded from coverage only with a written reason and reviewer approval.
