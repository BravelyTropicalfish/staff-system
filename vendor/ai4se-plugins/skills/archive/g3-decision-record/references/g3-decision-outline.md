# G3 decision record outline (pack default)

Use when writing `/sdd:g3-decision-record`. Fill every required field.\
Decision must be one of **Pass | Fail | Conditional**.

## Required fields (common Gate set)

| Field | Content |
|---|---|
| Change id | OpenSpec / project change identifier |
| Gate | `G3` (Merge & Archive) |
| Input artifacts | Living-merge result, change-summary, archive completeness evidence |
| Check conclusions | Merge outcome, conflicts, archive completeness, harness/rules evolve needed? |
| Decision | Pass \| Fail \| Conditional |
| Conditions + owners | Required when Conditional; include deadlines when the project requires them |
| Approvers | Named **SA** (required). AO AI4SE asks may attach — they do not replace SA |
| Timestamp | When the decision was recorded |

## Conditional / Pass rules

- Conditional **must** list remediation items with owners (and deadlines if required).
- Conditional without remediation is **invalid** — not Verified.
- Archive incomplete with Decision=Pass → **reject**.
- AO improvement asks are attachments, not a substitute for the SA archive decision.

## Suggested record shape

```markdown
# G3 decision — <change-id>

- Gate: G3
- Inputs: …
- Findings: merge / conflicts / archive completeness / evolve needed? …
- Decision: Pass | Fail | Conditional
- Conditions + owners: …
- Approvers: SA=…; AO asks (optional attachment): …
- Timestamp: …
```

## Storage

Prefer the change folder: project Rules path if set, else `g3-decision.md`.\
If the path is unclear or the user declines a file write → emit the full record in the reply.
