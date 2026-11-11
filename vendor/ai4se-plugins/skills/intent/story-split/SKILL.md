---
name: story-split
description: >-
  Split requirements into agile user stories with AC summaries, requirement
  traceability, and optional change/story registry rows. Use after
  /sdd:pre-spec-req (includes impact) or a fresh /sdd:impact-analysis. Do not
  substitute for OpenSpec proposal-split.
disable-model-invocation: true
status: ready
id: skill.intent.story-split
command: /sdd:story-split
impl: native
layer: core
gate: false
---

# Story split

## Instructions

Split requirements into agile user stories (and optional Epic/AC summaries) for
backlog tools. **Anti-trigger:** Not a substitute for OpenSpec
`/sdd:proposal-split`; not the Wave-3 explore toolbox (`/sdd:explore`).

1. Confirm structured requirements (+ impact brief when available — usually from
   embedded impact inside `/sdd:pre-spec-req`). If requirements are missing or
   not split-ready (blocking open questions / DoR BLOCKERs remain) → **hard
   stop** / return to `/sdd:pre-spec-req`.
2. When splitting one requirement into multiple stories, consult
   [splitting-patterns.md](./references/splitting-patterns.md) (Workflow /
   Happy-Sad / Simple-Complex) and prefer vertical, user-valuable slices.
3. Apply the team story template / tracker convention when the user provides one;
   otherwise use a minimal story shape: title, user voice, AC summary, size hint,
   links to requirement ids. Optional Epic grouping is allowed.
4. **Attribution when Changes exist:** if `/sdd:proposal-split` (or OpenSpec
   changes) already defined Change boundaries, attribute each Ready scenario /
   AC theme to **exactly one** story (no duplicate coverage; no orphan
   scenarios). Derive Story Statement from the Change sub-goal when available.
5. Persist the story list to `<workdir>/stories.md` by default (user may override
   the path parameter). When the project uses per-change story files, also write
   `<workdir>/stories/<change-name>.story.md` (or project template path).
   Summarize in the session as well.
6. **Optional registry row:** if the workdir has a Change & Story tracking table
   (e.g. `registry.md`), insert an idempotent row per change/story: Change Name,
   Parent REQ, Story File ✅, ALM Sync Pending, Story ID —, Updated date. Group
   by Parent REQ; skip duplicate Change Names.
7. If a tracker API is unavailable → still produce the markdown story list; do
   **not** block on API access. Do **not** wrap this skill as `/opsx:propose`.
8. Keep traceability to requirement ids. Stories serve sprint planning; when both
   tracks run, OpenSpec remains delivery-spec authority. May run in parallel with
   `/sdd:proposal-split`. **Next:** `/sdd:quality-checklist` (P1) and/or continue
   Spec lane after proposal exists.

- **In:** structured requirements + impact brief; optional Change boundaries from
  proposal-split; optional story template / tracker convention; optional output
  path
- **Out:** story list file (+ optional per-change story files) + session summary
  (title, user voice, AC summary, size hint, requirement links; optional Epics);
  optional registry rows
- **Not:** writing OpenSpec `proposal.md`; exploration method toolkit; ALM
  product upsert pipelines
- **Escalate:** human when story sizing policy is unclear and blocks planning

**Gotchas:** Dual-track — do not collapse this into proposal-split. Scenario
attribution must be 1:1 with stories when Changes already exist.

## Examples

```text
User: /sdd:story-split
Agent: Reads requirements + impact; applies Workflow / Happy-Sad /
Simple-Complex patterns as needed; writes <workdir>/stories.md with traced
stories; notes tracker API skipped if unavailable; may run parallel to
proposal-split.
```

## Additional resources

- [Splitting patterns](./references/splitting-patterns.md)
