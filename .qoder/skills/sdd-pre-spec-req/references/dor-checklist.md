# Definition of Ready — light self-check

Run after drafting requirement entries (six-field and/or Ready-shaped), and
after deep-dive when that phase is in scope. This is an **entry self-check**,
not a Gate skill — do not write Pass/Fail Gate decisions.

## Severity mindset for findings

| Severity | Use when |
|---|---|
| **BLOCKER** | Entry is not testable or not split-ready without inventing scope; missing
  intent/actors/scope; blocking open question remains; app-arch touchpoints
  unknown; deep-dive ⑨–⑫ fail when deep-dive is in scope |
| **WARNING** | Ambiguous wording, thin scenarios, weak evidence links — proceed only after
  BLOCKERs are cleared; may defer WARNING with explicit risk noted |
| **SUGGESTION** | Polish (references, advisory completeness) that does not block downstream |

## Mandatory 12 items (fails → BLOCKER — must resolve before impact / split)

### Base 8 (always)

1. **Intent clear** — outsider understands what + why from the statement/intent.
2. **Actors defined** — roles (and permission boundaries when relevant) listed.
3. **Scope bounded** — in-scope present; out-of-scope ≥1 or explained “None”.
4. **Scenarios / AC hints concrete** — at least one concrete, observable outcome;
   prefer ≥2 when the entry claims non-trivial behavior.
5. **Exception / boundary** — at least one exception or boundary when failure
   modes matter; else note N/A with reason.
6. **Results verifiable** — expected results are observable (no vague “works
   well”).
7. **Dependencies / app-arch touchpoints identified** — known constraints and
   affected apps/services/capabilities / integration boundaries listed or marked
   as blocking open questions (then resolved). “Unknown” without a resolved
   answer is a BLOCKER.
8. **Open-questions gate** — blocking open questions = 0 not-`resolved` before
   calling the entry ready for impact/split; non-blocking items are `resolved`
   or explicitly `deferred` (后续解决) with owner hints when known.

### Deep-dive ⑨–⑫ (when Phase 2.5 is in scope)

Check against workdir `deep-dive.md` (or project equivalent). For **new**
entries that entered deep-dive: missing file = fail → return to deep-dive (not
“skippable”). Legacy entries without deep-dive: skip ⑨–⑫ with an explicit note
(align with proposal-split A0 cutover rules).

9. **Data contract decided** — every data concept has grain + field list + state
   transitions. Fail: concept in Ready with no deep-dive row, or fields/
   transitions left `{TBD}`.
10. **Boundary enumerated** — every branch of every business rule has in/out
    attribution. Fail: a branch referenced in scenarios is absent from deep-dive.
11. **Cross-REQ aligned** — every ✗ on the cross-REQ table has an anchor assigned
    and backfilled. Fail: any unconverged ✗ remains.
12. **Notification / display contract decided** — every notification and every UI
    display surface has trigger/recipient/content+display-fields/channel. Fail:
    Ready names a notification or UI surface with no matching deep-dive row, or
    display-fields blank.

## Advisory (WARNING / SUGGESTION)

- Evidence / sources / prototypes referenced where claims need support.
- Impact brief produced by embedded `/sdd:impact-analysis` (or standalone
  re-run) cites the same requirement ids.
- Entry looks independently verifiable enough to feed proposal/story split.
- NFR business-policy layer (retention, volume, SLA): record in deep-dive when
  decidable; else non-blocking open-question for Dev.

## How to record

List findings as `severity | status | location | issue | fix hint`.

| status | Meaning | Allowed for |
|---|---|---|
| `open` | Not yet clarified | Transient |
| `resolved` | Clarified in-session; entry updated | All severities |
| `deferred` | 后续解决 — user declined clarify-now after generation | **WARNING / SUGGESTION only** — never BLOCKER |

## Exit gate

1. Present DoR findings after the draft (and deep-dive when in scope).
2. **Resolve every BLOCKER** one-at-a-time (prefer Superpowers brainstorming —
   see brainstorming agenda D1). Re-run the light self-check on touched entries
   until BLOCKER count is **0**.
3. Ask once whether to clarify WARNING / SUGGESTION now (agenda D1 non-blocker
   path). On decline → `status: deferred` with severity retained.
4. If BLOCKERs cannot be resolved without inventing scope → escalate IO/SA and
   **hard stop**. Do not run or accept impact analysis as complete for split,
   and do not suggest `/sdd:proposal-split` / `/sdd:story-split`.

Prefer clarifying with IO/SA over inventing acceptance criteria. A BLOCKER with
`status: deferred` is **out of contract**.
