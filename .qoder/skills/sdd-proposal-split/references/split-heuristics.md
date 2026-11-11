# Proposal split heuristics

Lightweight guides while wrapping `/opsx:propose`. Still one authoritative
`openspec/changes/` tree — **never** create a second changes root.

## Vertical slice first

Each Change should answer: “what new user-visible capability does this add?”
Prefer end-to-end user value over horizontal tech layers (API-only, UI-only,
“notification tool alone”) unless the team explicitly needs a contract-first
shared piece — and even then keep a demo-able verification path.

## Scenario-count decision tree

For each structured requirement (or Ready-shaped entry):

```
Key scenarios / AC hints ≤ 3 and small scope → prefer 1 Change
4–6 scenarios → consider 2 Changes (e.g. happy path + enrichment)
> 6 scenarios or multi-subsystem → consider up to 3 (e.g. intake / exec /
  exception)
> 3 Changes from one requirement → warn; may need requirement re-split upstream
```

Adjust with impact-brief split boundaries when present. Prefer fewer Changes
that remain independently verifiable over micro-splits.

## Light dependency / phase ordering

Keep this **light** — a short DAG sketch in the handoff, not a separate product:

1. Note cross-REQ depends-on when one entry consumes another’s output.
2. Shared components used by ≥2 Changes → Contract-First owner Change in an
   early phase; freeze the interface; others register only.
3. Independent REQs may parallel Phase 1; downstream dependents follow.
4. Present to the user before `/opsx:propose`: Change list + one-line sub-goal,
   text DAG, suggested Phase 1/2/3, shared-component strategy.

## Light team-shape note

Ask once if unclear; keep it short:

| Shape | Bias |
|---|---|
| Full-stack pairs | Vertical slices per scenario / sub-goal |
| Front/back split teams | Still prefer vertical Changes; sequence backend-heavy
  first when needed |
| Mixed | Phase order matters more than extra Changes |

Do not redesign org structure here — only bias slice size and order.

## Pattern hints (when splitting one requirement)

- **Workflow** — journey stages (visible → runnable → resilient).
- **Happy / Sad** — happy path first; exception paths follow.
- **Simple / Complex** — simple case first; complex variants later.

(See story-split pattern card for the short definitions used on the agile
track.)

## Quality self-check (per proposed Change)

Before finishing overlays / handoff:

1. **Sub-goal in one sentence** — user-perceivable capability, not a tech
   component name alone.
2. **Not an activity cross-cut** — finishing the Change leaves something a user
   or operator can observe.
3. **E2E-verifiable** — ≥1 path from action to observable result (not only
   “API returns 200” or “unit tests pass”).
4. **Independently shippable** — can run/demo without waiting on unfinished
   sibling Changes (may be incomplete feature, must be runnable).
5. **Capability names real** — Modified Capabilities match living
   `openspec/specs/` when those specs exist; do not invent names.
6. **No dependency cycles** — if depends-on hints are recorded, keep a DAG;
   flag cycles for human fix.

Failing 1–4 → discuss re-split with the user. Fix wrong capability names when
the listing is available.

## Anti-patterns

- Tech-layer slices (“email receive Change”, “notify Change”) with no user goal.
- Shared infra as a Change with nothing demo-able.
- Verification only “API green” / “component renders”.
- A Change that cannot start verification until another finishes (serial ≠
  parallel).
