# Verification commands

The commands this repository uses as stop checks, in the three layers the harness
asks for (static → runtime → system). Each one is also a CI job in
`.github/workflows/ci.yml`; the five required checks of `pipeline-gates` 2 map
onto them as noted below.

Everything runs from the repository root. On Windows PowerShell use
`.\mvnw.cmd` where the snippets show `./mvnw`; the web commands are the same in
both shells.

## Static

```bash
# lint — ESLint over the web app's TypeScript, TSX and Node tests
cd web && npm ci && npm run lint

# type check — the web project and the Java sources
cd web && npx tsc --noEmit
cd backend && ./mvnw -B -DskipTests compile
```

## Runtime

```bash
# unit tests — JUnit 5 in `backend/src/test`, `node --test` in `web/tests`
cd backend && ./mvnw -B test
cd web && npm test

# unit tests with the coverage gate: JaCoCo fails `verify` when line or branch
# coverage of `domain/` or `api/` drops below 100% (design §9)
cd backend && ./mvnw -B verify
```

## System

```bash
# acceptance tests — the 82 API scenarios run inside `mvnw verify`
# (`@SpringBootTest(RANDOM_PORT)` suites under `backend/src/test/.../api`);
# the 8 E2E scenarios and 2 journeys run in Chromium, with the config starting
# the back end on an in-memory database and the Vite dev server itself
cd web && npx playwright install --with-deps chromium && npm run test:e2e

# dependency and security scan — the registry audit of the web tree fails on
# high or critical findings; GitHub's dependency review covers the npm and
# Maven manifests of a pull request
cd web && npm audit --audit-level=high
```

## The five required checks

| `pipeline-gates` check | Where | Command / evidence |
|---|---|---|
| lint | CI job `lint` | `npm run lint` |
| type check | CI job `type check` | `npx tsc --noEmit`; `./mvnw -B -DskipTests compile` |
| unit tests | CI job `unit tests` | `npm test`; `./mvnw -B test` |
| acceptance tests for the changed behaviour | CI job `acceptance tests` | `./mvnw -B verify` (API scenarios + JaCoCo gate); `npm run test:e2e` |
| dependency and security scan | CI job `dependency and security scan` | `npm audit --audit-level=high`; `dependency-review-action` (`fail-on-severity: high`) |

The workflow runs on every pull request and never deploys. Coverage and
Playwright reports are uploaded with a 30-day retention, matching
`pipeline-gates` 6.
