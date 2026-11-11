---
name: proposal-split
description: >-
  Split requirements into OpenSpec Change/Proposal packages with A0 deep-dive
  gate, vertical-slice heuristics, light dependency/phase ordering, and effort
  notes. Use after /sdd:pre-spec-req (includes impact) or a fresh
  /sdd:impact-analysis. Do not produce agile story backlogs here.
disable-model-invocation: true
status: ready
id: skill.intent.proposal-split
command: /sdd:proposal-split
impl: wrap-openspec
layer: core
gate: false
---

# Proposal split

## Instructions

Split requirements into OpenSpec Change/Proposal packages with effort
assessment. **Anti-trigger:** Do not use for agile user-story backlog
(`/sdd:story-split`); do not craft Delta Spec AC/DoV (`/sdd:delta-craft`); do
not write business code.

1. Confirm structured requirements and impact brief are ready (brief usually
   from embedded impact inside `/sdd:pre-spec-req`, or a standalone
   `/sdd:impact-analysis` refresh). If requirements are not split-ready
   (blocking open questions or DoR BLOCKERs remain) → return to
   `/sdd:pre-spec-req`. Optional change-name / change-id hint from the user.
2. **A0 deep-dive gate** for new REQs — see
   [a0-deep-dive-gate.md](./references/a0-deep-dive-gate.md). Fail → refuse
   split; send back to pre-spec-req deep-dive. Legacy without deep-dive may
   bypass with an explicit note.
3. Consult [split-heuristics.md](./references/split-heuristics.md) before and
   while proposing: vertical slices, scenario-count tree, light dependency /
   phase ordering, team-shape, and quality self-check. Present the split plan
   (Change list + DAG sketch + phases) and wait for user confirm before
   wrapping propose.
4. Wrap `/opsx:propose` (and continue as needed) so artifacts land under the
   authoritative `openspec/changes/` tree. If OpenSpec is unavailable → stop;
   see [openspec integration](../../../integrations/openspec.md). Do **not**
   create a second changes root.
5. Overlay: ensure the proposal cites requirement ids and impact highlights; add
   effort/size notes for the proposal. Update Pre-SDD `handoff.md` (or session
   handoff note) when a workdir was used. Story files / registry rows belong to
   `/sdd:story-split` (run in parallel or after).
6. Summarize change path(s) + effort notes. **Completion handoff — `/sdd` only:**
   suggest next steps exclusively as `/sdd:*` (typically `/sdd:delta-craft`,
   and/or parallel `/sdd:story-split`, and/or `/sdd:quality-checklist` /
   continue `/sdd:p1`). **Do not** prompt `/opsx:apply`, any other `/opsx:*`,
   Superpowers skills, or non-`/sdd:*` commands as the next action — even if
   upstream OpenSpec text after `/opsx:propose` suggests apply. Ignore or
   rewrite that upstream nudge into the `/sdd` menu above.

- **In:** structured requirements + impact brief; optional deep-dive; optional
  change-name/id
- **Out:** OpenSpec change under `openspec/changes/`; effort/size notes; light
  dependency/phase sketch; handoff note from Pre-SDD workdir if used; next-step
  suggestions limited to `/sdd:*`
- **Not:** Delta Spec craft; story map / Jira stories; business code; second
  changes tree; heavyweight dependency-graph tooling; recommending `/opsx:apply`
  or other non-`/sdd:*` skills on completion
- **Escalate:** human when OpenSpec install/auth is blocked; BA when A0 fails

**Gotchas:** Upstream OpenSpec first, pack overlay second. Never duplicate the
changes tree. Wrap may invoke `/opsx:propose` mid-run; completion surfacing
stays `/sdd`-only — never echo OpenSpec’s “run apply” default.

## Examples

```text
User: /sdd:proposal-split
Agent: Validates requirements + impact; applies split heuristics; wraps
/opsx:propose; overlays req ids / effort; updates handoff; suggests only
/sdd:delta-craft and/or /sdd:story-split (or /sdd:p1 checklist) — never
/opsx:apply or other non-/sdd skills.
```

## Additional resources

- [A0 deep-dive gate](./references/a0-deep-dive-gate.md)
- [Split heuristics](./references/split-heuristics.md)
- [OpenSpec integration](../../../integrations/openspec.md)
