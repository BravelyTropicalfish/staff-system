# agents/

Stage-level agent **fleet** stays deferred until P0–P5 recipes are stable and promotion rules (capability-asset design §5.1) are met.

**First-edition samples** prove Recipe → Agent without a full fleet. Contract `/sdd:*` and stage facades `/sdd:pN` remain the primary menu; agents lower skipped-step cost.

Host-facing names use the `sdd-agent-` prefix (invoke / projected filenames).

| Agent | Path | status | Facade | Notes |
|---|---|---|---|---|
| `agent.sdd-agent-spec-align` | [`sdd-agent-spec-align/AGENT.md`](sdd-agent-spec-align/AGENT.md) | ready | `/sdd:p2` | P2 / G1 **draft**; **required** `consistency-lint` |
| `agent.sdd-agent-apply-execute` | [`sdd-agent-apply-execute/AGENT.md`](sdd-agent-apply-execute/AGENT.md) | ready | `/sdd:p4` front half | WAIT external `local-verify-report`; deploy optional |

Shared definition contract: [`_TEMPLATE.md`](_TEMPLATE.md).

Do **not** merge apply execution and verify adjudication into one agent. `agent.verify-gate` and other §5.2.2 candidates stay deferred.

---

## How to use (host)

1. Install the pack (`node install/bin/ai4se-plugins.mjs install`) so skills, `/sdd`, and ready agents are projected.
2. Invoke the projected subagent (`sdd-agent-spec-align` / `sdd-agent-apply-execute`) from the host agent picker / Task tool (files under `.cursor/agents/`, `.claude/agents/`, `.codex/agents/`, `.github/agents/*.agent.md`, or `.qoder/agents/`).
3. Give the agent a change context (change-id / Proposal / ATDD+tasks as required).
4. Let it orchestrate the `/sdd` sequence in `AGENT.md`; keep Gate sign-off **human-explicit**.

Agents do **not** replace `/sdd:p2` / `/sdd:p4` or contract commands — use those for single-step debug.

**Install note:** only `status: ready` agents are projected. All five hosts get agent files (Copilot uses `<name>.agent.md`).

---

## `agent.sdd-agent-spec-align` (P2)

**Goal:** Proposal → verifiable Delta Spec → G1 decision-record **draft**; humans close G1.

**Orchestration (sample policy):**

```text
/sdd:delta-craft
  → /sdd:consistency-lint          # always in this sample; Fail → stop → SA
  → ambiguity? stop → IO/SA
  → /sdd:g1-decision-record        # draft only
  → HITL human sign-off closes G1
```

| | |
|---|---|
| Preload | `delta-craft`, `consistency-lint`, `g1-decision-record` |
| Allow | read repo/specs; write Delta Spec + G1 **draft** |
| Deny | business code; self-close G1; invent requirements under ambiguity |
| Manual test | Check the [agent's completion criteria](sdd-agent-spec-align/AGENT.md) in the selected host. |

Catalog may still mark `consistency-lint` as an optional **layer**; this sample **requires** it.

---

## `agent.sdd-agent-apply-execute` (P4 implement half)

**Goal:** ATDD/tasks → code/tests → **wait** local-verify → PR; optional existing CD after merge. Never adjudicate G2.

**Orchestration:**

```text
/sdd:atdd-apply
  → WAIT /sdd:local-verify-report  # orchestrate; do not preload; Fail → stop
                                   # Pass OR human-signed waiver → continue
  → /sdd:pr-submit
  → (optional) deploy=true | human asked + merge confirmed
       → /sdd:deploy-artifacts
  → STOP before pipeline-report / g2-decision-record
```

| | |
|---|---|
| Preload | `atdd-apply`, `pr-submit`; optional `deploy-artifacts` |
| External wait | `local-verify-report` (invoke; not preload) |
| Allow | write code/tests; open/update PR; trigger **existing** CD when allowed |
| Deny | self-waive; treat Fail as Pass; close G2; own `pipeline-report` / `g2-decision-record` |
| Manual test | Check the [agent's completion criteria](sdd-agent-apply-execute/AGENT.md) in the selected host. |

Facade aligns with `/sdd:p4` **front half** only — not the full P4 recipe.

---

## Promotion rules

SKILL → Recipe/`/sdd:pN` → Agent. Mark `ready` only when Design §5.1 and dep skills are `ready`.

New sample agents: copy `_TEMPLATE.md`, use `sdd-agent-<kebab>` for `name` / dir / id, fill Goal / Preload / Deny / HITL / Orchestration, add a MANUAL-TEST checklist row, update this inventory and the package README Agents table.
