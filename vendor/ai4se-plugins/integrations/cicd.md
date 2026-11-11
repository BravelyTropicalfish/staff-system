# Integration: CI/CD connectors

## Role

Contracts for `/sdd:pipeline-report`, `/sdd:deploy-artifacts`, and related apply/verify permissions.

## Permission model (frozen)

| Action | Skills | Privilege |
|---|---|---|
| Aggregate CI/CD test reports | `/sdd:pipeline-report` | **Read-only** by default |
| Open/update MRs (GitLab) | `/sdd:pr-submit` | Write to git host via **`glab`** (operator/agent); pack scripts under `skills/apply/pr-submit/scripts/` are **local-only hints** |
| Trigger existing CD | `/sdd:deploy-artifacts` | Deploy trigger only — do not invent a second CD product |
| Local build/scan + review notes | `/sdd:local-verify-report` | Local; Fail **blocks** `/sdd:pr-submit` unless human-signed waiver |

## `/sdd:pipeline-report` parameters

- `phase=ci` — remote CI results
- `phase=cd` — post-deploy / CD test results
- `phase=all` — both

## Auth

- Secrets never committed into this pack or customer repos as plaintext.
- Prefer host/OS secret stores or CI OIDC; document per-vendor in later sections.
- For `/sdd:pr-submit`, authenticate with `glab auth login` on the operator machine (token stays in the user/OS store). Pack scripts must **not** run auth or store tokens.

## Vendor status

| Vendor | Status |
|---|---|
| **GitLab CI / GitLab MRs** | **First target** — `/sdd:pr-submit` defaults to `glab` for remote MR steps; report aggregation shape below |
| GitHub Actions | deferred / secondary |
| Jenkins | deferred |
| Azure DevOps | deferred |

## `/sdd:pr-submit` (GitLab)

- Pack helpers (local-only): `preflight-branch.sh`, `list-pr-targets.sh`, `prompt-remote-mr.sh`.
- These scripts do **not** `git push`, `git fetch`, invoke `glab` write APIs, or configure credentials. When remote work is needed they print `REMOTE HINT` commands for the operator/agent.
- Actual MR create/update uses `glab` outside those scripts.
- Skill contract still owns verify Pass / signed-waiver hard-stop; scripts do not waive that gate.

## Interface shape (GitLab CI — draft)

Wave-1 verify authors should assume report aggregation needs:

- `project_id` or `namespace/project` path
- `pipeline_id` or `job_id` reference
- normalized fields: status, conclusion/failure reason, url, failed job names, artifact links if any

Do not expand this file into a full client implementation here.

## GitHub Actions — Wave-1 usage notes

For `/sdd:pipeline-report`:

1. Resolve `owner/repo` from git remote.
2. Locate latest workflow run for the PR head SHA or explicit `workflow_run_id`.
3. Normalize into: status, conclusion, html_url, failed job names, artifact URLs if any.
4. Use read-only tokens (Actions read); never reuse deploy triggers.

Live API clients may live in consumer repos or host MCP; this pack documents the contract, not a mandatory bundled client binary.
