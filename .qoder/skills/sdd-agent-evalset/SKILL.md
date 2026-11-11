---
name: sdd-agent-evalset
description: >-
  Produce an evaluation-dataset scheme for Agent products (domains, schema,
  judge strategy). Use when the change is agent-oriented. Do not create
  pack-local golden datasets.
disable-model-invocation: true
status: ready
id: skill.eval.agent-evalset
command: /sdd:agent-evalset
impl: native
layer: optional
gate: false
---

# Agent evalset scheme

## Instructions

Optional P1 step: draft an **evaluation-dataset scheme** for an Agent product,
assistant, or automated agent. Outputs land in the customer Pre-SDD workdir.
**Anti-trigger:** Not every P1; not for dumping golden sample corpora; not for
creating a pack-local `eval/` dataset directory; not a live harness run.

Resolve `$SKILL_DIR` as the directory containing this `SKILL.md`. Invoke pack
scripts via `"$SKILL_DIR/scripts/..."` — do not run bare `scripts/...` from the
target repo cwd.

1. **Agent-oriented gate** — From change goals / Spec excerpts, decide whether
   the change targets an Agent product, assistant, or automated agent.
   - If **no** → explain why, recommend skipping this skill, and **stop**. Do
     **not** write files.
2. **Resolve persist root** — user path parameter if provided; else an existing
   Pre-SDD workdir (default `requirement-workspace/` at repo root, or a
   session-known path); else ask once. If the user declines file write →
   **session-only** (skip scripts). Do not invent a write path. Never create
   `ai4se-plugins/eval/` (or any pack-local eval dataset tree).
3. Prefer Superpowers brainstorming as the process wrap when installed (one
   question at a time; prefer multiple choice) to triage: capability domains,
   primary task type(s), risk tier, whether L2/L3 layers are required. If
   unavailable → still draft and note missing wrap.
4. Draft the scheme from
   [Scheme outline](../../../vendor/ai4se-plugins/skills/eval/agent-evalset/references/scheme-outline.md). Tag content as Confirmed /
   Assumption / Gap. Never fabricate business facts or invent threshold
   numbers — leave Gaps. Schema without sample floods: define fields and
   grading criteria only; **do not** write Golden sample bodies or agent run
   outputs.
5. If persisting: write the body to a temp file, then:

   ```bash
   "$SKILL_DIR/scripts/ensure-evaluation-dir.sh" --root "<workdir>"
   "$SKILL_DIR/scripts/write-scheme.sh" --root "<workdir>" --apply \
     --body-file "<path-to-agent-written-body.md>"
   ```

   Scripts are additive; never delete or silently overwrite user content beyond
   creating missing `evaluation/` + README. On script failure → report error;
   keep the session draft; do not claim written.
6. Summarize file path or session-only result. **Next:** continue P1 (e.g.
   `/sdd:proposal-split` / `/sdd:story-split` / `/sdd:quality-checklist`) as
   appropriate. This skill **never** writes Gate Pass/Fail.

- **In:** Agent-oriented change context / goals; optional workdir/path; Spec
  excerpts describing agent behaviors
- **Out:** Scheme document under `<workdir>/evaluation/` (timestamped) and/or
  session; next-command hint
- **Not:** Pack-local `eval/` datasets; Golden sample bodies; running the
  evaluation harness; Gate decisions; inventing thresholds
- **Escalate:** human when the change is not agent-oriented (hard stop); when
  persist path is unclear and the user refuses both a path and session-only

**Gotchas:** Scheme ≠ dataset dump. Refuse requests to create
`ai4se-plugins/eval/` or to fill golden rows in-pack. Do not paste long
evaluation-org playbooks into the skill body — keep detail in the outline.

## Examples

```text
User: /sdd:agent-evalset
Agent: Confirms agent-oriented change; triages domains/risk/task type via
brainstorming (or notes missing wrap); drafts scheme from outline; ensure +
write under <workdir>/evaluation/<ts>-agent-eval-scheme.md.
```

```text
User: /sdd:agent-evalset (CRUD API only, no agent)
Agent: Explains not agent-oriented; recommends skip; stops; writes no files.
```

```text
User: /sdd:agent-evalset (thin Spec, declines file write)
Agent: Session-only Partial scheme with Gaps; does not invent thresholds.
```

## Additional resources

- [Scheme outline](../../../vendor/ai4se-plugins/skills/eval/agent-evalset/references/scheme-outline.md)
- [ensure-evaluation-dir.sh](../../../vendor/ai4se-plugins/skills/eval/agent-evalset/scripts/ensure-evaluation-dir.sh)
- [write-scheme.sh](../../../vendor/ai4se-plugins/skills/eval/agent-evalset/scripts/write-scheme.sh)
- [Superpowers integration](../../../vendor/ai4se-plugins/integrations/superpowers.md)
