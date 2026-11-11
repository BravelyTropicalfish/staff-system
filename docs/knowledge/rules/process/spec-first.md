# spec-first — Write the spec before the code

## Metadata

| Field | Value |
|---|---|
| **ID** | spec-first |
| **Category** | process |
| **Priority** | Critical |
| **Activation** | always |
| **Applies To** | all |
| **Stack** | — |

## Rule

1. Do not write production code before the spec for that change is agreed.
2. Keep the order: intent → spec → design and tasks → code → verify → archive.
3. A pull request must cite the spec or change ID and link its artifacts.
4. Keep change artifacts under `openspec/changes/<id>/`.
5. A small change still needs a short spec.
   A docs-only change may be exempt if the team agrees.
6. If the code and the spec disagree, stop and fix the spec first.

## Rationale

A clear spec makes the change testable and keeps code from drifting.

## Verification

| Check | Blocks |
|---|---|
| Pull request cites a change ID | pr-merge |
| Change folder exists under `openspec/changes/` | pr-merge |
| No production code without a spec | pr-merge |

## Examples

Good: the pull request cites `Spec ID: add-login` and includes `openspec/changes/add-login/`.

Bad: feature code only, with no spec or change ID.

## Exceptions

- An urgent fix may write a short spec after the fix, but before the next release.
