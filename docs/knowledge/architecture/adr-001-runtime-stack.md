# ADR-001 — Runtime stack for the Staffing MVP

- **Status:** accepted, 2026-11-11
- **Decision owner:** product owner (chose "React front end, Java back end, H2 database")
- **Change:** `add-staffing-mvp` (`openspec/changes/add-staffing-mvp/design.md` §1, open item D1)

## Context

The change creates the first application in this repository; nothing exists yet. The proposal left
the runtime stack open (assumption 6, D1) and required the choice to be recorded here before
implementation. The machine has Java 21 (Liberica 21.0.11), Node 24.18, npm 11.16 and Python 3.14,
a global Maven 3.0.4 (2012), no Docker and no pnpm. The repository requires shadcn/ui for UI work
and three separated layers (UI → business logic → data) with one service entry point per feature.

## Decision

- **Back end:** Java 21 with Spring Boot 3.4 (Web, Validation, Data JPA, Security), packaged by the
  **Maven Wrapper pinned to 3.9.9**, because the global Maven 3.0.4 cannot build a Boot 3 project.
- **Database:** H2 2.x — file database for `dev`, in-memory for tests, schema from `schema.sql`
  with `ddl-auto=validate`.
- **Front end:** React 19 + TypeScript + Vite + Tailwind + shadcn/ui + React Router, one `fetch`
  wrapper, no state library.
- **Tests:** JUnit 5 with `@SpringBootTest(RANDOM_PORT)` for the API layer, Vitest for front-end
  helpers, Playwright (TypeScript) for the eight E2E scenarios, JaCoCo for coverage.

## Consequences

- Two toolchains run locally (Maven wrapper + npm), which the React front end already forced.
- H2 keeps the MVP to a single process and no container, at the cost of weaker concurrency evidence:
  the double-confirmation check proves the invariant under a row lock, not under a distributed
  database. Accepted in `atdd-design.md` §7.
- Java holds every business rule; the SPA holds none, which keeps the UI thin as the repository
  rules require.
- Playwright on Node rather than in Java keeps the E2E layer close to the browser and reuses the
  Node runtime the front end needs.

## Alternatives considered

- **Next.js single application** — one runtime and one build, but a Node back end, which the owner
  did not choose.
- **React + Fastify**, **FastAPI + React** — same reason: the owner chose Java for the server.
- **Postgres** — needs Docker, which is not installed.
- **Server-rendered templates instead of an SPA** — loses the shadcn/ui component base the
  repository mandates.
- **Gradle** — not installed; the Maven wrapper needs no new global install.

## Verification

- `mvnw -v` reports Maven 3.9.9 and Java 21.
- `mvnw verify` and `npm test` run the same commands recorded in
  `docs/feedback/verification-commands.md`.
- CI runs the five required checks from `docs/knowledge/rules/process/pipeline-gates.md`, with no
  deploy job.
