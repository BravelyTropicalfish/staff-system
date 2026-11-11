# Change summary — add-staffing-mvp

> P5 overlay, not a Gate decision and not Living Spec authority. The merge ran on 2026-11-11; the
> one open item is a Windows file handle on the emptied source folder, recorded under Residual risks
> and in the G3 condition.

## Merge & archive

- `openspec archive add-staffing-mvp --yes` merged the six delta specs into living specs:
  `openspec/specs/staffing/<capability>/spec.md`, +40 requirements, 0 modified, 0 removed
  (allocation +5, candidate-matching +5, people-import +5, people-pool +8, post-requirement +8,
  staffing-confirmation +9). The merged tree counts 40 requirements / 90 scenarios.
- The change itself now lives at `openspec/changes/archive/2026-11-11-add-staffing-mvp/`; the archive
  copy is complete (30 files, including the Chinese `spec.zh.md` siblings, the prototype, both gate
  records and this summary).
- The Chinese translations were held as `spec.zh.md.hold` for the archive run — OpenSpec treats any
  other `.md` under a capability folder as a stray delta — and restored to `spec.zh.md` inside the
  archived copy afterwards.
- Knowledge base: `scripts/kb-lint.sh` (now a real check, `scripts/kb-lint.mjs`) reports
  `24 entries, every _INDEX.md link resolves`. The repository has no KB writer script, so the KB step
  validates the existing tree rather than importing change prose into it.
- Leftover: an empty `openspec/changes/add-staffing-mvp/prototype` directory is still held by an
  external handle, so the source folder could not be removed completely; `openspec list` therefore
  still shows a task-less `add-staffing-mvp`. Removal is the G3 condition, owner raylen.

## Intent

Match people to project posts on one screen: a lead opens a project, states its posts, the
coordinator reads the gap and proposes people from the pool (same-role, or a cross-role fallback),
and the lead confirms or rejects; a confirmation writes an assignment plus an allocation, a release
or a project end gives the seat back. The change package carries six delta capabilities
(`people-pool`, `people-import`, `allocation`, `post-requirement`, `candidate-matching`,
`staffing-confirmation`), 40 requirements and 90 scenarios, all traced.

## What shipped

- **Backend** — Spring Boot 3.4 on Java 21 with an H2 file database and a 14-table `schema.sql`:
  session and role guards on every route group, the gate engine G1→G6 with no score or ranking, the
  cross-role fallback (X1 one band above, X2 a domain skill at `Practitioner` or above), the
  one-sheet import (person + `skills` cell, whole batch rejected or applied), the project end as one
  transaction, the recommendation snapshot, the decision inbox, notices, and the catalogs with
  reference-guarded rename/deactivate/delete.
- **Frontend** — React 19 + TypeScript + Vite, the prototype's three-group sidebar and flat
  information architecture: `/posts` (three tiles, projects, posts, post detail), `/recommendations`
  (decision inbox + void notices), `/candidates` (chips, suitable / cross-role / excluded), `/people`,
  `/imports`, `/catalogs`, plus the project, post, person and new-post screens. Every user-visible
  string is Chinese; API `code` values stay English.
- **Identity** — `/login` is the only door: a signed-out screen redirects to it and returns after
  sign-in. The servlet cookie stays for API clients; each sign-in also issues a per-tab token in
  `sessionStorage` (`X-Session-Token`) that outranks the cookie, so two tabs can hold two accounts.
- **Rules the owner set after G2** — only the project's own lead opens a project; the coordinator is
  read-only on projects and posts; one workbook, one `people` sheet; an unreferenced catalog item may
  be deleted, a referenced one is refused with `referenced`.
- **Harness** — five CI jobs (lint, type check, unit, acceptance, dependency/security scan), JaCoCo
  coverage gate at `verify`, Playwright acceptance with its own in-memory backend, and the
  three-layer `docs/feedback/verification-commands.md`.

## Verify highlights

- Latest local verify: `docs/ai4se-plugins/20261111-220510-local-verify-report.md` — **Overall Pass**
  (E3 Pending, no waiver). Eight whitelist commands exit 0.
- Backend: `mvnw -B test` 297 tests; `mvnw -B verify` 297 tests with
  `All coverage checks have been met.` (JaCoCo `PACKAGE` rules at 100% line and branch over
  `domain*`/`api*`).
- Web: `npm test` 54/54; `npm run lint` 0 problems; `npx tsc --noEmit` clean; `npm run build` green;
  `npm audit --audit-level=high` 0 high/critical (9 moderate, dev toolchain).
- Acceptance: `npx playwright test` 20/20 (3 setup + 2 journeys + 8 scenarios + 2 smoke + 3 login +
  J1/J2/J3), including the signed-out redirect and a two-tab, two-account run.
- Evidence checklist: 40 requirements / 90 scenarios, English and Chinese copies identical in count,
  90/90 scenarios mapped in `traceability.md` (`missing=0` by script).
- Gates: G1 signed for rev 8 / rev 11 / rev 12, and rev 13 signed 2026-11-11 (raylen IO / kyrie
  SA); G2 signed 2026-11-11 (Pass) with addenda 1–8.

## Residual risks

- **G1 rev 13 is now signed** (raylen IO, kyrie SA, 2026-11-11) for the one-sheet import, catalog
  deletion, lead-only project creation and coordinator read-only rule; G2 remains Pass with
  addenda 1–8.
- **Session tokens have no expiry**; the store is in-process memory, so a sign-in adds an entry until
  a restart clears it. Accepted for the single-process MVP; a later change should add a TTL.
- **E1 has no CI run for this commit**: the branch has not been pushed since T31–T43.
- **CSRF stays disabled** while the cookie remains a credential (pre-existing, unchanged).
- **9 moderate npm advisories** in the dev toolchain (no high or critical).
- **The tree mixes owner-side dev tooling** (`dev/H2ConsoleConfig.java`, application properties,
  `pom.xml`) with the change; it is not part of T31–T43.
- **The emptied source folder is gone**: the handle that held `prototype` was released when the owner
  closed that terminal, the leftover `openspec/changes/add-staffing-mvp/` was removed, and
  `openspec list` shows only `add-headcount-charts`. G3 is **Pass**.

## Links

- Specs: `openspec/changes/add-staffing-mvp/specs/staffing/*/spec.md` (+ `spec.zh.md`)
- Design: `openspec/changes/add-staffing-mvp/design.md` (+ `.zh.md`)
- Tasks: `openspec/changes/add-staffing-mvp/tasks.md` (+ `.zh.md`, T01–T43)
- Traceability: `openspec/changes/add-staffing-mvp/traceability.md`
- Gates: `openspec/changes/add-staffing-mvp/g1-decision.md` (rev 8/11/12 signed, rev 13 awaiting),
  `openspec/changes/add-staffing-mvp/g2-decision.md` (Pass + addenda 1–8)
- Verify: `docs/ai4se-plugins/20261111-220510-local-verify-report.md`,
  `docs/ai4se-plugins/20261111-220510-local-verify-report-checkpoints.md`,
  `docs/ai4se-plugins/20261111-220510-local-verify-run.log`
- Earlier verify / pipeline evidence: `docs/ai4se-plugins/20261111-164232-local-verify-report.md`,
  `docs/ai4se-plugins/20261111-191403-pipeline-report.md`
- Pull request: https://github.com/BravelyTropicalfish/staff-system/pull/1 (open; its head predates
  T31–T43)
- Archive / KB: archived at `openspec/changes/archive/2026-11-11-add-staffing-mvp/`; living specs at
  `openspec/specs/staffing/*/spec.md`; KB `scripts/kb-lint.sh` exit 0 (24 entries)
- Pipeline: `docs/ai4se-plugins/20261111-231243-pipeline-report.md` (run #12 on `a11bffd`, five jobs
  green, 2m28s; the three failed runs before it failed only in the browser installation)
