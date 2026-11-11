# Discovery — light intake vs upgrade

Run before (or as the first pass of) drafting requirement entries. Discovery may
end in **No-Go** — that is a valid outcome; do not invent a Ready entry.

## Default path — light intake

1. Collect intent sources (notes, interviews, tickets, `intents/`).
2. Form one candidate: “suggest adding / changing capability X because …”
3. Record a **Discovery Decision** line on the entry or workdir note:
   `Go | No-Go` + one-line rationale + date.
4. Fold confirmed facts into the six-field / Ready-shaped draft. Do **not**
   invent a parallel explore-log product unless the upgrade path triggers.

## Upgrade path — full Discovery

Escalate to a short explore note (workdir `discovery/` or equivalent) when **any**
of these hold:

1. Cannot list a candidate solution (no one-sentence “add X”).
2. Cannot say how the candidate relates to existing Living Spec /
   `openspec/specs/` capabilities.
3. Needs repo/product exploration (`/sdd:repo-understand` or `/sdd:explore`)
   before a Go decision.

Upgrade outputs (when used):

| Section | Content |
|---|---|
| Confirmed | Facts / rules marked confirmed |
| Assumptions | Explicitly unverified |
| Candidate | Capability suggestion, fit, Included/Excluded, alternatives |
| Decision | Go / No-Go + rationale + date |

## Go / No-Go

| Result | Action |
|---|---|
| Go | Proceed to structured entries + DoR + impact |
| No-Go — already covered | Close / archive intent; no Ready entry |
| No-Go — insufficient info | Stay in Discovery; do not enter impact/split |
| No-Go — withdrawn / deprioritized | Close intent |

## Divergence tool

When candidates or boundary branches are stuck, prefer Superpowers brainstorming
([brainstorming-agenda.md](./brainstorming-agenda.md)) for clarification only —
do not jump to plan/spec generation from Discovery alone.
