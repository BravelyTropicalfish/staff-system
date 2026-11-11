# Explore methods (pack default)

Select with `/sdd:explore method=<id>`.

| method | Outcome |
|---|---|
| `elevator-pitch` | Short value proposition draft |
| `journey-map` | User journey stages + pains/opportunities |
| `story-map` | Backbone + walking skeleton story outline (exploration — not tracker export) |
| `service-blueprint` | Frontstage/backstage sketch (optional depth) |

Each run should state method, participants/assumptions, and output paths.
Tag findings as Confirmed / Assumption / Candidate. Exploration may stop
without proceeding to requirements.

## elevator-pitch

**Outcome:** A short value proposition draft stakeholders can react to.

**Minimum deliverable:** One pitch block: who / problem / offer / why now
(plus any open Assumptions).

**Facilitation (3–5 steps):**

1. Who is the primary beneficiary?
2. What painful problem do they face today?
3. What do we offer in one sentence?
4. Why now (trigger / constraint)?
5. What must stay out of scope for this pitch?

## journey-map

**Outcome:** User journey stages with pains and opportunities.

**Minimum deliverable:** Ordered stages; per stage: actor action, pain,
opportunity (mark each note Confirmed / Assumption / Candidate).

**Facilitation (3–5 steps):**

1. Name the journey (trigger → desired end state).
2. List 4–8 stages in order.
3. For each stage: what the actor does and sees.
4. Capture pains and opportunities per stage.
5. Highlight the top 1–3 intervention points (Candidate until confirmed).

## story-map

**Outcome:** Backbone + walking-skeleton outline for exploration only.

**Minimum deliverable:** User activities (backbone) + a thin walking-skeleton
slice of stories under them. **Not** Jira/ADO export; not `/sdd:story-split`.

**Facilitation (3–5 steps):**

1. Name the primary user and goal.
2. List backbone activities left-to-right.
3. Under each activity, list candidate stories (thin).
4. Mark a walking-skeleton path (must-have vertical slice).
5. Park nice-to-haves explicitly; do not treat them as committed backlog.

## service-blueprint

**Outcome:** Frontstage/backstage sketch (thinner depth is OK).

**Minimum deliverable:** One journey slice with frontstage steps and matching
backstage / support notes (Assumptions allowed).

**Facilitation (3–5 steps):**

1. Pick one journey slice (not the whole enterprise).
2. List frontstage steps the user perceives.
3. Note backstage actions that enable each step.
4. Note support processes / systems if known (else Assumption).
5. Stop when the sketch clarifies handoffs — do not expand into full consulting.
