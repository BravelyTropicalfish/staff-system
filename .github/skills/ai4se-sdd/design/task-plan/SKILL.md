---
name: task-plan
description: >-
  Generate design/tasks and run Spec/Design/ATDD alignment self-check. Use after
  ATDD design. Do not implement production code or approve Gates.
disable-model-invocation: true
status: ready
id: skill.design.task-plan
command: /sdd:task-plan
impl: wrap-openspec
layer: core
gate: false
---

# Task plan

## Instructions

Generate the design/tasks plan for a change and self-check alignment to Spec /
Design / ATDD. **Anti-trigger:** Not implementation (`/sdd:atdd-apply`); not Gate
approval.

1. Confirm `change-id`, Delta Spec context, and ATDD design outputs. If ATDD
   design is missing → stop and run `/sdd:atdd-design` first (or produce it in
   session, then continue).
2. Wrap OpenSpec `design.md` / `tasks.md` artifacts when the backend is OpenSpec.
   If those paths are unavailable and no agreed fallback exists → stop; see
   [openspec integration](../../../../../vendor/ai4se-plugins/integrations/openspec.md).
3. Ensure tasks obey ATDD design constraints (acceptance-driven slices; do not
   contradict ATDD). Keep design doc and ATDD design both linked from tasks.
4. **Final required step:** run alignment self-check (Spec ↔ Design ↔ Tasks ↔
   ATDD). If the self-check **Fails** → list gaps and **do not** mark the plan
   complete.
5. On Pass: summarize artifacts + self-check result. **Next:**
   `/sdd:quality-checklist` (P3), then later Apply/Verify.

- **In:** change-id; Delta Spec; ATDD design outputs
- **Out:** OpenSpec `design.md` / `tasks.md` (when applicable); alignment
  self-check report
- **Not:** production code; skipping ATDD design; marking complete on Failed
  alignment
- **Escalate:** SA when alignment Fail needs Spec or ATDD changes

**Gotchas:** Dual naming — OpenSpec design doc vs ATDD design; keep both linked.

## Examples

```text
User: /sdd:task-plan
Agent: Wraps OpenSpec design/tasks; runs Spec↔Design↔Tasks↔ATDD self-check;
stops without complete on Fail; on Pass suggests /sdd:quality-checklist.
```

## Additional resources

- [OpenSpec integration](../../../../../vendor/ai4se-plugins/integrations/openspec.md)
