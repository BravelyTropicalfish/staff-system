---
name: g1-decision-record
description: >-
  Record the G1 Align decision with human sign-off. Use only when explicitly
  invoked. Do not use for G2/G3 or auto-approve.
disable-model-invocation: true
status: ready
id: skill.spec.g1-decision-record
command: /sdd:g1-decision-record
impl: native
layer: core
gate: true
---

# G1 decision record

## Instructions

Human explicitly records the G1 Align decision after Delta Spec review.
**Anti-trigger:** Model must not auto-invoke; do not use for G2/G3; do not
rewrite Delta Specs or merge Living Spec.

1. Confirm explicit `/sdd:g1-decision-record` (or named invoke). Collect
   `change-id`, Delta Spec paths + AC/DoV review results, and **named** human
   approvers (**IO + SA**). Missing sign-off roles → **stop**.
2. Draft the record using
   [g1-decision-outline.md](./references/g1-decision-outline.md): change id,
   gate=G1, inputs, findings (scope / ambiguity / conflict), decision
   Pass|Fail|Conditional, conditions+owners, timestamp.
3. Validate: Conditional without remediation items/owners → **invalid**, require
   completion. Unresolved ambiguities with Decision=Pass → **reject**; require
   Fail or Conditional with owners.
4. Persist: project Rules path if set, else `g1-decision.md` under the change
   folder; if the path is unclear or the user declines a file write → emit the
   full record in the reply. Summarize the path in the reply.
5. **Next:** on Pass → `/sdd:p3` or design skills; on Fail → return to
   `/sdd:delta-craft` / human clarify.

- **In:** change-id; Delta Spec + AC/DoV review outputs; named IO + SA
- **Out:** G1 decision record (Pass|Fail|Conditional) with required fields;
  optional `g1-decision.md` (or Rules-agreed path)
- **Not:** rewriting Delta Specs; G2/G3 decisions; Living Spec merge
- **Escalate:** stop when approvers missing; IO/SA when Pass is requested with
  unresolved blocking gaps

**Gotchas:** Conditional without remediation is not Verified. This skill records
the human decision — it does not invent approval.

## Examples

```text
User: /sdd:g1-decision-record
Agent: Confirms IO+SA names and Delta/AC-DoV inputs; writes g1-decision.md
(or reply); Conditional only with remediation+owners; on Pass suggests /sdd:p3.
```

```text
User: /sdd:g1-decision-record (Pass, open ambiguities)
Agent: Rejects Pass; requires Fail or Conditional with owners before recording.
```

## Additional resources

- [G1 decision outline](./references/g1-decision-outline.md)
