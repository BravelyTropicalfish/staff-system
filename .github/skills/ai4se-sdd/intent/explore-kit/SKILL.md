---
name: explore-kit
description: >-
  Run an optional intent exploration method (elevator-pitch, journey-map,
  story-map, service-blueprint). Use early in P1. Do not use as story-split
  or OpenSpec propose.
disable-model-invocation: true
status: ready
id: skill.intent.explore-kit
command: /sdd:explore
impl: wrap-superpowers
layer: optional
gate: false
---

# Explore kit

## Instructions

Optional early intent exploration using a selected method card (P1).
**Anti-trigger:** Not `/sdd:story-split` backlog splitting; not OpenSpec
propose (`/sdd:proposal-split`); not formal requirements freeze.

Resolve `$SKILL_DIR` as the directory containing this `SKILL.md`. Invoke pack
scripts via `"$SKILL_DIR/scripts/..."` — do not run bare `scripts/...` from the
target repo cwd.

1. Resolve `method=` — one of `elevator-pitch` | `journey-map` | `story-map` |
   `service-blueprint`. If missing or unknown → list valid methods and **stop**
   (or ask the user to choose, then continue). Always echo the selected method
   in the output header.
2. Resolve persist root: user path parameter if provided; else an existing
   Pre-SDD workdir (default `requirement-workspace/` at repo root, or a
   session-known path); else ask once. If the user declines file write →
   **session-only** (skip scripts). Do not invent a write path.
3. Prefer Superpowers brainstorming as the process wrap when installed
   (one question at a time; prefer multiple choice). If unavailable → still run
   the thin method card and note missing wrap.
4. Facilitate per [Explore methods](../../../../../vendor/ai4se-plugins/skills/intent/explore-kit/references/methods.md). Tag findings as
   Confirmed / Assumption / Candidate; do not treat Candidate as decided. Never
   fabricate business facts — mark unknowns. Exploration may stop without
   proceeding. Story-map exploration ≠ `/sdd:story-split`.
5. If persisting: draft the body from
   [artifact template](../../../../../vendor/ai4se-plugins/skills/intent/explore-kit/references/artifact-template.md) + method outcome, then:

   ```bash
   "$SKILL_DIR/scripts/ensure-exploration-dir.sh" --root "<workdir>"
   "$SKILL_DIR/scripts/write-exploration.sh" --root "<workdir>" --apply \
     --method "<method>" --body-file "<path-to-agent-written-body.md>"
   ```

   Scripts are additive; never delete or silently overwrite user content beyond
   creating missing `exploration/` + README. On script failure → report error;
   keep the session draft; do not claim written.
6. Summarize file path or session-only result. **Next:**
   `/sdd:pre-spec-workdir` and/or `/sdd:pre-spec-req`.

- **In:** `method=`; optional workdir/path; intent context / stakeholders
- **Out:** method-specific exploration artifact (file under
  `<workdir>/exploration/` and/or session); next-command hint
- **Not:** formal requirements freeze; OpenSpec change creation; story-split /
  proposal-split; full service-blueprint consulting engagement
- **Escalate:** human when stakeholders conflict and block a useful draft; when
  persist path is unclear and the user refuses both a path and session-only

**Gotchas:** Do not dump a parallel long “new brainstorming” essay into this
skill — keep overlays short. Do not collapse explore into story-split.

## Examples

```text
User: /sdd:explore method=elevator-pitch
Agent: Echoes method; wraps brainstorming (or notes missing wrap); drafts
pitch; ensure + write under <workdir>/exploration/; suggests pre-sdd-spec-*.
```

```text
User: /sdd:explore
Agent: Lists valid methods; stops or waits for method= choice.
```

```text
User: /sdd:explore method=story-map
Agent: Produces backbone + walking-skeleton outline; reminds this is not
/sdd:story-split; may session-only if no workdir.
```

## Additional resources

- [Explore methods](../../../../../vendor/ai4se-plugins/skills/intent/explore-kit/references/methods.md)
- [Artifact template](../../../../../vendor/ai4se-plugins/skills/intent/explore-kit/references/artifact-template.md)
- [ensure-exploration-dir.sh](../../../../../vendor/ai4se-plugins/skills/intent/explore-kit/scripts/ensure-exploration-dir.sh)
- [write-exploration.sh](../../../../../vendor/ai4se-plugins/skills/intent/explore-kit/scripts/write-exploration.sh)
- [Superpowers integration](../../../../../vendor/ai4se-plugins/integrations/superpowers.md)
