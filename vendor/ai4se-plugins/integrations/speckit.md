# Integration: Spec Kit (optional Spec backend)

## Role

Alternate Spec pipeline backend for customers already on [Spec Kit](https://github.com/github/spec-kit). **Not a coding-agent host.** Host wiring stays under `adapters/`; backend switch stays here + catalog `upstream_ref`.

## Status (Wave-0)

Contract skeleton only. Full `/speckit.*` mapping is a later compatibility wave.

## Invariant

- External `/sdd` names do **not** change when backend=speckit.
- Adapter hosts still register `/sdd:*`; routing selects OpenSpec vs Spec Kit per project config.

## Mapping skeleton (to complete later)

| /sdd | Spec Kit analogue (indicative) |
|---|---|
| `/sdd:delta-craft` | `/speckit.specify` (+ clarify/review points) |
| `/sdd:task-plan` | `/speckit.tasks` |
| `/sdd:atdd-apply` | `/speckit.implement` |

## Config hook

Projects opt in with a documented `backend: speckit` (exact file TBD in compatibility wave). Default remains OpenSpec.
