---
name: followups
description: >-
  Generate tech-debt and business backlog follow-ups after archive context.
  Use optionally in P5 after change-summary. Do not treat as Living Spec
  authority; do not design harness/rules evolution (use /sdd:evolve).
disable-model-invocation: true
status: ready
id: skill.archive.followups
command: /sdd:followups
impl: native
layer: optional
gate: false
---

# Followups

## Instructions

After archive / change-summary context, generate technical debt and business
backlog follow-ups in **one** work package (optional P5).
**Anti-trigger:** Not the G3 decision; not `/sdd:change-summary`; not
`/sdd:evolve` (Harness/Rules evolution).

1. Confirm `change-id`. Look for a readable change-summary (project Rules path
   if set, else `change-summary.md` under the change folder).
   - If **missing** → tell the user to run `/sdd:change-summary` first. Do
     **not** auto-run it unless the user explicitly asks. If they confirm
     continue anyway → proceed from change folder / verify residuals /
     conversation context and expect **Partial** (possibly empty lists + gaps).
     If they decline both → **stop**.
   - G3 notes are **not** required (G3 normally runs after this skill in `/sdd:p5`).
2. Collect residuals read-only: preferred change-summary (especially Residual
   risks); optional verify reports, PR/task leftovers, consistency-lint findings
   if present. Classify into tech-debt vs business per
   [followups outline](./references/followups-outline.md). Divert Harness /
   Rules / pipeline-template asks to a **Redirect: `/sdd:evolve`** list — do
   not design evolution here.
3. Draft the document from the outline. Insufficient evidence → empty or thin
   sections with Gaps; **never invent** debt or business asks. Do not treat the
   output as Living Spec authority. Do not auto-file tracker tickets unless the
   user explicitly asks and a project integration exists (otherwise list
   candidates and explain).
4. Persist: project Rules path if set; else
   `openspec/changes/<id>/followups.md`; if path unclear or the user declines a
   file write → put the full document in the reply. Never overwrite
   proposal/spec/Living bodies.
5. Summarize counts (tech-debt / business / redirect / gaps). **Next:**
   `/sdd:g3-decision-record` (mention `/sdd:evolve` when Redirect is non-empty).
   This skill **never** writes Gate Pass/Fail.

- **In:** change-id; preferred change-summary; optional verify / residual
  artifacts
- **Out:** follow-ups document (file or reply) with Tech debt + Business
  backlog (+ Gaps + Redirect)
- **Not:** G3 decision; Living Spec authority; harness/rules evolution design;
  inventing items; default tracker ticket creation
- **Escalate:** human when change-id missing, prerequisite declined, or tracker
  filing requested without integration

**Gotchas:** Keep debt vs business labeled separately. Follow-ups are not Living
Spec. Soft prerequisite on change-summary — prompt first, continue only on
confirm.

## Examples

```text
User: /sdd:followups change-id=add-foo
Agent: Finds change-summary; drafts followups.md with TD/BZ tables; redirects
Rules asks to /sdd:evolve; suggests /sdd:g3-decision-record.
```

```text
User: /sdd:followups change-id=add-foo (no change-summary)
Agent: Asks to run /sdd:change-summary first; on “continue anyway”, uses thin
residuals, emits Partial/gaps, does not invent items.
```

## Additional resources

- [Follow-ups outline](./references/followups-outline.md)
