# Ready detail-completeness gate (A0)

Run **before** proposing OpenSpec Changes from Ready / structured
requirements. Mirrors DoR items ⑨–⑫ from
`skills/intent/pre-spec-req/references/dor-checklist.md`.

## When A0 applies

| Entry class | Rule |
|---|---|
| **New** (deep-dive in scope / file present, or project cutover date says new) | Must pass A0 before split |
| **Legacy** (no deep-dive; cutover says stock) | Bypass A0 with an explicit note — do not block progress |

If the project documents a cutover / “新流程上线日”, prefer Ready Date ≥ cutover
⇒ new; else treat deep-dive presence as the proxy.

## Checks

1. Read workdir `deep-dive.md` (or project equivalent) for each target REQ.
2. Evaluate DoR ⑨ data contract / ⑩ boundary / ⑪ cross-REQ / ⑫
   notification-display.
3. All Pass → continue proposal-split heuristics.
4. Any Fail → **refuse split**; point BA back to `/sdd:pre-spec-req` deep-dive
   with the failing concept / branch / ✗ / notification named.

## Fail output shape

```text
A0 FAIL | REQ-id | item ⑨|⑩|⑪|⑫ | gap location | fix: return to deep-dive
```
