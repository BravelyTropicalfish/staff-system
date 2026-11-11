# pre-spec-req brainstorming agenda

Use Superpowers brainstorming: **one question at a time**; prefer multiple
choice. If Superpowers is unavailable, keep the same discipline and note the
missing wrap.

Frame prompts at **application architecture** level (services/modules/
capabilities, integration boundaries, data ownership, inter-app contracts).
See [open-questions.md](./open-questions.md).

## When to brainstorm (blocking — mandatory resolve)

Run as soon as a **blocking** open question is identified — do not invent an
answer to keep drafting smooth. Exit only with `status: resolved` or escalate
to IO/SA (**hard stop**). Do **not** mark blocking items `deferred`.

| id | topic | Typical choices / prompts |
|---|---|---|
| B1 | Actor / permission boundary (which app enforces) | named roles + owning service / “unknown — escalate IO/SA” |
| B2 | In vs out of scope for a disputed capability | in-scope / out-of-scope / escalate IO |
| B3 | Observable acceptance outcome | concrete result options / “need stakeholder input” |
| B4 | Exception or failure-mode expectation across app boundary | must-handle now / N/A with reason / escalate |
| B5 | Blocking dependency or integration boundary | sync API / event / batch / shared store + owner / escalate |
| B6 | Data ownership / authoritative store for a capability | named app or store / unknown + escalate |
| B7 | Runtime topology at app level (who hosts the change) | named deployable / shared platform / escalate |
| B8 | Data-contract grain / fields / lifecycle (deep-dive ①) | options from impact concepts / `{TBD}` + escalate |
| B9 | Boundary branch in vs out (deep-dive ②) | in-scope / out-of-scope / escalate |
| B10 | Notification or UI display fields (deep-dive ④) | surface + field list options / defer non-blocking |

Skip only when the user already answered in this session; mark
`skipped-preanswered` on that item. After each answer, set
`status: resolved` (or escalate) and update the requirement entry.

## After generation

| id | Ask | On yes | On no |
|---|---|---|---|
| N1 | Clarify non-blocking open questions now? | One-at-a-time clarify; mark `resolved` | Mark each remaining **non-blocking** item `deferred` |
| D1-BLOCKER | Resolve DoR BLOCKER findings | Mandatory one-at-a-time; re-check until BLOCKER=0 | Not allowed — escalate IO/SA and hard stop |
| D1-WARN | Clarify DoR WARNING / SUGGESTION now? | One-at-a-time; re-check touched entries | Mark each unresolved WARNING/SUGGESTION `deferred`; note residual risk |

Do not re-ask N1 / D1-WARN in the same run after the user answers. SUGGESTION-only
DoR lists may be summarized briefly before D1-WARN; still ask once.
D1-BLOCKER is not optional.
