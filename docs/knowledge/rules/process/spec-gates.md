# spec-gates — Human sign-off points

## Metadata

| Field | Value |
|---|---|
| **ID** | spec-gates |
| **Category** | process |
| **Priority** | Critical |
| **Activation** | always |
| **Applies To** | all |
| **Stack** | — |

## Rule

1. The project has three decision points:
   - G1: the specification is agreed;
   - G2: the change is verified;
   - G3: the change is archived.
2. A human signs each decision. An agent may prepare the record but cannot sign it.
3. Each record must include:
   - what was decided;
   - evidence links;
   - open risks;
   - the signer's name and date.
4. Do not mark a decision as passed when required evidence is missing.
   Use `partial` or `blocked` instead.
5. Do not change an earlier decision without a new record that explains why.
6. Agents must stop at a gate and ask a human. Silence is not approval.

## Rationale

The gates keep important decisions with people who own the outcome.

## Verification

| Check | Blocks |
|---|---|
| A signed record exists for each gate | archive |
| Evidence links are present | archive |
| The signer is not the agent that made the change | archive |

## Examples

Good: a G2 record with test results, a risk list, and a named signer.

Bad: an agent writing "G2 passed" without a human signer.

## Exceptions

- A hotfix may use an emergency G2 record after the fix.
  It must be signed before the next release.
