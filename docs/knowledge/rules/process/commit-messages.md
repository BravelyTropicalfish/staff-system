# commit-messages — Commit message format

## Metadata

| Field | Value |
|---|---|
| **ID** | commit-messages |
| **Category** | process |
| **Priority** | High |
| **Activation** | always |
| **Applies To** | all |
| **Stack** | — |

## Rule

1. Every commit message starts with a type, then a colon, then a short summary:
   `<type>: <summary>`
2. Allowed types:
   - `feat`: a new feature;
   - `fix`: a bug fix;
   - `docs`: documentation only;
   - `test`: tests only;
   - `refactor`: a code change that adds no feature and fixes no bug;
   - `chore`: build, tools, or dependency work.
3. Use a lowercase type and one space after the colon.
4. Keep the summary short. Use an action phrase, for example `add login page`,
   not `added login page`. Do not end it with a period.
5. One commit does one thing. Do not mix types in one commit; split it instead.
6. If the reason is not obvious, add a short body that explains why.
7. Agents must use the same format.
8. Add a new type only when the team agrees, and update this rule.

## Rationale

A clear prefix makes history easy to scan and to group for a release.

## Verification

| Check | Blocks |
|---|---|
| Message matches `<type>: <summary>` | commit |
| Type is in the allowed list | commit |
| One commit has one type | pr-merge |

## Examples

Good: `feat: add login page`, `fix: return an error for zero weight`

Bad: `update`, `feat add login page`, `Fix: login page`

## Exceptions

- Merge commits created by Git may keep their default message.
- A revert commit may start with `revert:`.
