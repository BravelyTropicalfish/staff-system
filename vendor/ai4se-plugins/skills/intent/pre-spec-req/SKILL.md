---
name: pre-spec-req
description: >-
  Turn fuzzy intent into structured, splittable requirement entries via light
  (or upgraded) Discovery, optional deep-dive (data contract / boundary /
  display / cross-REQ), 12-item DoR, then impact analysis. Use after workdir
  exists. Do not create OpenSpec changes or agile story exports here.
disable-model-invocation: true
status: ready
id: skill.intent.pre-spec-req
command: /sdd:pre-spec-req
impl: native
layer: core
gate: false
---

# Pre-spec requirements

## Instructions

Turn fuzzy intent into structured, splittable requirement entries in the
pre-spec workdir, **hard-clear** blocking open questions and DoR BLOCKERs, then
run impact analysis (embeds `/sdd:impact-analysis`). **Anti-trigger:** Not
OpenSpec propose (`/sdd:proposal-split`); not agile story splitting
(`/sdd:story-split`). Standalone re-run of impact only → use
`/sdd:impact-analysis` directly.

1. Confirm the pre-spec workdir exists (user path parameter if provided; else the
   default used by `/sdd:pre-spec-workdir`). If missing → stop; run or
   request `/sdd:pre-spec-workdir` first.
2. **Discovery** per [discovery.md](./references/discovery.md): default **light
   intake** (Go decision on the entry/note); escalate to full Discovery on the
   three triggers (no candidate / coverage unclear / needs explore). No-Go →
   stop without inventing Ready entries.
3. Collect intent sources (notes, interviews, tickets, `intents/` captures).
4. Write structured entries under `requirements/`. Each entry must include the
   six-field default: **id**, **statement**, **actors**, **rationale**,
   **acceptance hints**, **links to evidence**. Mark unknowns explicitly; do not
   invent stakeholder intent.
5. **Optionally** enrich entries toward a Ready shape using
   [ready-outline.md](./references/ready-outline.md) when the user wants fuller
   sections (Intent, Actors, In/Out scope, Scenarios+AC hints, Constraints,
   Context, Affected capabilities, Open questions). Gaps OK; no invented facts.
6. Classify open questions as **blocking** vs **non-blocking** at **application
   architecture** granularity (see
   [open-questions.md](./references/open-questions.md)).
   - **Granularity:** services / modules / capabilities, integration boundaries,
     data ownership, inter-app API or event contracts, runtime topology at app
     level. Too coarse (pure business policy with no system touchpoint) →
     refine; too fine (class/method/code design) → push down or mark
     non-blocking for later design.
   - **Blocking:** do not invent answers. Prefer Superpowers brainstorming as
     the process wrap (one question at a time; prefer multiple choice) using
     [brainstorming-agenda.md](./references/brainstorming-agenda.md). Discuss
     with the user until each blocker is **`resolved`** or the user explicitly
     escalates to IO/SA (session stops — do not continue). **Must not** leave
     blocking items `open` or `deferred`.
   - **Non-blocking:** record them; do not block drafting. Clarification is
     asked only after generation (step 9).
7. Run the light DoR / entry self-check in
   [dor-checklist.md](./references/dor-checklist.md) (12 mandatory when
   deep-dive in scope; BLOCKER / WARNING / SUGGESTION). This is **not** a Gate
   — do not mint fake acceptance criteria to “pass”.
8. **After generation — non-blocking open questions:** ask once whether to
   clarify non-blocking open questions now.
   - Yes → facilitate one-at-a-time (prefer Superpowers brainstorming when
     installed); update entries with Confirmed answers.
   - No / defer → mark each remaining non-blocking item
     `status: deferred` (后续解决) with owner hint when known; keep them in the
     open-questions list. Do not invent answers.
9. **After generation — DoR self-check:** present DoR findings. **BLOCKER
   findings must all be resolved** before step 10 — facilitate one-at-a-time
   (prefer Superpowers brainstorming); update entries; re-run the light
   self-check on touched entries until BLOCKER count is 0. If IO/SA escalate
   without resolve → **hard stop** (do not run impact analysis or suggest
   split). WARNING / SUGGESTION: ask once whether to clarify now; on defer mark
   `status: deferred` with severity retained.
10. **Hard gate before impact / split:** verify (a) blocking open questions =
    0 with status other than `resolved`, and (b) DoR BLOCKER findings for
    items **①–⑧** = 0 unresolved. Items ⑨–⑫ apply only after deep-dive (step
    12). If either fails → stop; do not proceed.
11. **Embed impact analysis:** execute the full contract of
    `/sdd:impact-analysis` (same workdir; follow
    `skills/intent/impact-analysis/SKILL.md` and its references). Persist
    `impact-brief.md`. If impact reports critical blockers that invalidate
    requirements → return to steps 6–9; do not suggest split until cleared.
12. **Optional deep-dive (Phase 2.5):** when impact solution direction is add /
    extend / enable-by-config, run
    [deep-dive.md](./references/deep-dive.md) (data contract, boundary,
    cross-REQ consistency, notification/display). Then re-run DoR items
    **⑨–⑫** until BLOCKER=0 before suggesting split. Already-covered /
    continue-explore / re-split → follow discovery / authoring exit paths
    instead.
13. If intent is still too vague to form testable entries after the blocking
    loop → stop; escalate to IO/SA. Keep entries backend-agnostic enough to feed
    both OpenSpec and agile tracks. **Next:** `/sdd:proposal-split` and/or
    `/sdd:story-split`. (Re-run impact alone later with `/sdd:impact-analysis`.)

- **In:** pre-spec workdir; intent sources
- **Out:** structured requirement entries under `requirements/`; Discovery
  decision; optional deep-dive tables; open questions (blocking all `resolved`;
  non-blocking resolved / deferred); DoR findings (BLOCKERs all `resolved`;
  WARNING/SUGGESTION resolved / deferred); impact brief from embedded
  `/sdd:impact-analysis`
- **Not:** writing `openspec/changes/**/proposal.md`; Jira/ADO story export;
  Gate Pass/Fail decisions; skipping impact when blockers remain
- **Escalate:** IO/SA when blocking questions or DoR BLOCKERs cannot be resolved
  without inventing scope — session stops before impact/split

**Gotchas:** Do not silently convert unknowns into fake acceptance criteria.
Deferred is allowed **only** for non-blocking OQs and non-BLOCKER DoR findings.
Blocking OQ / DoR BLOCKER `deferred` is out of contract. Do not treat
`status: deferred` as Confirmed. Business data contracts (fields, grain,
lifecycle) belong in Ready/deep-dive; tech-stack choices do not.

## Examples

```text
User: /sdd:pre-spec-req
Agent: Reads intents; writes six-field entries; resolves blocking app-arch open
questions one-at-a-time; runs DoR until BLOCKERs=0; asks about non-blocking OQs
and WARNING/SUGGESTION DoR; embeds /sdd:impact-analysis; writes impact-brief.md;
suggests proposal-split and/or story-split.
```

```text
User: /sdd:pre-spec-req (blocking OQ found mid-draft)
Agent: Stops inventing; brainstorms the blocker with the user; resumes drafting
only after resolve. Escalation without resolve → hard stop (no impact, no split).
```

## Additional resources

- [Discovery (light / upgrade)](./references/discovery.md)
- [Detail deep-dive](./references/deep-dive.md)
- [Ready-shaped outline](./references/ready-outline.md)
- [DoR self-check (12 items)](./references/dor-checklist.md)
- [Open questions](./references/open-questions.md)
- [Brainstorming agenda](./references/brainstorming-agenda.md)
- [Impact analysis skill](../impact-analysis/SKILL.md)
