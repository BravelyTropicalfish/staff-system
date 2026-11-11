# Open questions — classify and defer

Record unknowns explicitly. Prefer labels over invented facts.

## Granularity — application architecture

Open questions in `pre-spec-req` MUST be framed at **application architecture**
level — enough to split proposals/stories and bound impact, not implementation
detail and not pure business policy without system touchpoints.

| In scope (ask / resolve here) | Out of scope (too fine → later design; too coarse → refine) |
|---|---|
| Which apps / services / modules / capabilities are touched | Class, method, SQL, UI widget, or code-path design |
| Integration boundaries between apps (sync API, async event, batch, shared DB) | Protocol byte-level or library choice |
| Data ownership and authoritative store per capability | Table indexes, ORM mapping |
| Runtime / deploy topology at app level (who hosts what; env boundaries) | Pod counts, JVM flags, CI job YAML |
| Cross-cutting NFR that change architecture (authn boundary, tenancy, audit sink) | Vague “make it faster” with no system target |

If a question is only business policy (e.g. “who may approve?”) but has no
architecture implication yet, either (a) pair it with the affected capability /
boundary, or (b) keep it **non-blocking** until architecture impact is known.

## Classification

| Class | Meaning | When drafting |
|---|---|---|
| **blocking** | Entry is not testable / not split-ready without an answer at app-arch level | Pause; clarify via Superpowers brainstorming (or thin one-at-a-time loop). **Must** reach `resolved` (or escalate → hard stop). No `deferred` |
| **non-blocking** | Ambiguity or polish that does not block a useful draft or impact brief | Record; ask clarify-vs-defer only **after** generation |

## Status values

| status | Meaning | Allowed for |
|---|---|---|
| `open` | Still needs an answer; not yet asked or in progress | Transient only — blocking must leave `open` before exit |
| `resolved` | Answered in-session; update the requirement entry | blocking and non-blocking |
| `deferred` | 后续解决 — user declined clarify-now; keep owner hint when known | **non-blocking only** |

## How to record

List as:

```text
class | status | question | owner (if known) | note
```

Examples:

```text
blocking | resolved | Which service owns cut-off rules — Booking or Pricing? | SA | Confirmed: Pricing owns; Booking calls Pricing API
blocking | resolved | Auth boundary for cancel — BFF or domain service? | SA | Confirmed: domain service enforces; BFF forwards identity
non-blocking | deferred | Preferred notification channel for Ops? | — | 后续解决 (does not change app boundaries)
```

## Exit gate (before impact / split)

Before embedded `/sdd:impact-analysis` completes successfully and before
suggesting `/sdd:proposal-split` / `/sdd:story-split`:

1. Every **blocking** open question has `status: resolved` (count of blocking
   `open` or `deferred` = **0**).
2. Unresolved blockers → escalate to IO/SA and **hard stop** — do not invent
   answers and do not proceed downstream.
