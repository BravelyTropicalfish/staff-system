# G1 decision record outline (pack default)

Use when writing `/sdd:g1-decision-record`. Fill every required field.\
Decision must be one of **Pass | Fail | Conditional**.

## Required fields (common Gate set)

| Field | Content |
|---|---|
| Change id | OpenSpec / project change identifier |
| Gate | `G1` (Align) |
| Input artifacts | Delta Spec paths + AC/DoV review results (paths or reply refs) |
| Check conclusions | Scope / ambiguity / conflict findings |
| Decision | Pass \| Fail \| Conditional |
| Conditions + owners | Required when Conditional; empty only if Pass/Fail with none |
| Approvers | Named **IO** and **SA** (both required) |
| Timestamp | When the decision was recorded |

## Conditional rules

- Conditional **must** list remediation items with owners (and deadlines if the project requires them).
- Conditional without remediation is **invalid** — not Verified.
- Unresolved ambiguities with Decision=Pass → **reject**; use Fail or Conditional with owners.

## Suggested record shape

```markdown
# G1 decision — <change-id>

- Gate: G1
- Inputs: …
- Findings: …
- Decision: Pass | Fail | Conditional
- Conditions + owners: …
- Approvers: IO=…; SA=…
- Timestamp: …
```

## Storage

Prefer the change folder: project Rules path if set, else `g1-decision.md`.\
If the path is unclear or the user declines a file write → emit the full record in the reply.
