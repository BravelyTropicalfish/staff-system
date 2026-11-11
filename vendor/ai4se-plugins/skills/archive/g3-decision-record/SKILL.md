---
name: g3-decision-record
description: >-
  Record the G3 Merge & Archive decision with SA sign-off. Use only when
  explicitly invoked. Do not perform archive itself or auto-approve.
disable-model-invocation: true
status: ready
id: skill.archive.g3-decision-record
command: /sdd:g3-decision-record
impl: native
layer: core
gate: true
---

# G3 decision record

## Instructions

Human explicitly records the G3 Merge & Archive decision.
**Anti-trigger:** Model must not auto-invoke; not for G1/G2; do not run
`/opsx:archive` here (that is `/sdd:living-merge`).

1. Confirm explicit `/sdd:g3-decision-record` (or named invoke). Collect
   `change-id`, living-merge result, change-summary, archive completeness
   evidence, and **named SA** sign-off. AO may attach AI4SE improvement asks.
   Missing SA → **stop**.
2. Draft the record using
   [g3-decision-outline.md](./references/g3-decision-outline.md): change id,
   gate=G3, inputs, conclusions (merge, conflicts, archive completeness,
   harness/rules evolve needed?), decision Pass|Fail|Conditional,
   conditions+owners, timestamp.
3. Validate: Conditional without remediation items/owners → **invalid**. Archive
   incomplete with Decision=Pass → **reject**. AO asks are attachments only —
   they do not replace the SA decision.
4. Persist: project Rules path if set, else `g3-decision.md` under the change
   folder; if the path is unclear or the user declines a file write → emit the
   full record in the reply. Summarize the path in the reply.
5. Note whether Harness/Rules should evolve; point AO asks to asset governance
   when present. Do **not** implement `/sdd:evolve` here (later wave when ready).

- **In:** change-id; living-merge + change-summary + archive evidence; named SA;
  optional AO asks
- **Out:** G3 decision record (Pass|Fail|Conditional); optional `g3-decision.md`
  (or Rules-agreed path)
- **Not:** performing `/opsx:archive`; implementing harness evolve; substituting
  AO asks for SA sign-off
- **Escalate:** stop when SA missing; SA when Pass is requested with incomplete
  archive

**Gotchas:** AO asks are attachments, not a substitute for SA archive decision.

## Examples

```text
User: /sdd:g3-decision-record
Agent: Confirms SA + living-merge/summary/archive inputs; writes g3-decision.md
(or reply); Conditional only with remediation+owners; notes evolve/AO asks
without performing archive or evolve.
```

```text
User: /sdd:g3-decision-record (Pass, archive incomplete)
Agent: Rejects Pass; requires Fail or Conditional with owners before recording.
```

## Additional resources

- [G3 decision outline](./references/g3-decision-outline.md)
