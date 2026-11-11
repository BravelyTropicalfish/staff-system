# Integration: OpenSpec (default Spec backend)

## Role

Default change/spec pipeline backend for `/sdd` wraps. Install independently; this pack does not vendor OpenSpec.

## Install (package-local assumptions)

- Preferred: run this pack’s installer (`node install/bin/ai4se-plugins.mjs install`), which detects the CLI, can run the official global package install, and (when `--openspec-scope project`) runs `openspec init --tools …`.
- Manual: follow current OpenSpec project docs: https://github.com/Fission-AI/OpenSpec
- Customer/pilot repos should expose OpenSpec slash family `/opsx:*` and `openspec/changes/` tree.
- If missing after install/verify fails: stop and ask the user to fix PATH or re-run install — do not recreate propose/apply/archive.

## Delegation table (initial)

| /sdd | Upstream | Notes |
|---|---|---|
| `/sdd:proposal-split` | `/opsx:propose` (+ continue) | Artifacts under `openspec/changes/` |
| `/sdd:delta-craft` | `/opsx:propose` / continue → specs | Pack adds DoV/AC/role review |
| `/sdd:task-plan` | OpenSpec `design.md` / `tasks.md` | ATDD alignment overlay is native |
| `/sdd:atdd-apply` | `/opsx:apply` | Obey P3 ATDD design + Rules |
| `/sdd:living-merge` | `/opsx:archive` | G3 decision record stays native |
| `/sdd:change-summary` | `/opsx:archive` overlay | Summary fields native |

## Authoritative trees

- Living/change specs: OpenSpec `openspec/specs/` + `openspec/changes/`
- Pre-SDD workspace (`/sdd:pre-spec-workdir`) may exist beside that tree; must document handoff into OpenSpec changes — do not invent a second long-term authority tree.

## Wrap authoring pattern

1. State upstream command to invoke first.
2. State pack overlays (DoV, roles, non-goals).
3. Point failures: upstream fail vs overlay fail.

## Forbidden

- Parallel `/sdd` that duplicates `/opsx:propose|apply|archive` without a documented gap.
- Second authoritative change directory competing with OpenSpec.
