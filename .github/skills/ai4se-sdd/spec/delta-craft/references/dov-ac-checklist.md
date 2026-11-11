# AC / DoV review checklist (pack default)

Use during `/sdd:delta-craft` review step (`step=review` or `all`).\
Mark each item **Pass / Fail / Partial** with evidence paths.

## Acceptance criteria (AC)

1. Each requirement has at least one testable AC
2. ACs are observable (no vague “works well”)
3. Negative / edge cases called out where risk is high
4. ACs are not disguised implementation detail; vague words (e.g. appropriately, quickly, correctly) → Fail or Partial with location

## Definition of verified (DoV) for Delta Spec

1. Scope matches the change proposal (no silent scope expansion)
2. Ambiguities listed or resolved
3. Role concerns noted (IO / SA) for residual risk
4. No business implementation claimed as done in this skill
5. Requirements in the Delta Spec trace to the proposal / approved requirements; unmatched items → Fail or Partial

## How to record

| Item | Result (Pass/Fail/Partial) | Evidence | Notes |
|---|---|---|---|
| AC-1 … | | | |
| DoV-1 … | | | |

- Prefer storing the table under the change folder: path from project Rules if set, else `dov-ac-review.md` beside the change artifacts.
- If the change path is unclear or the user declines a file write → put the same table in the reply.
- Any blocking Fail → do not imply G1-ready; point humans to clarify or re-run generate after fixes.
