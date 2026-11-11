# security-baseline — Mandatory security checks

## Metadata

| Field | Value |
|---|---|
| **ID** | security-baseline |
| **Category** | security |
| **Priority** | Critical |
| **Activation** | always |
| **Applies To** | all |
| **Stack** | — |

## Rule

1. Before any commit that touches production paths, confirm the checklist below.
2. Secret handling follows `@rule:no-hardcoded-secrets` (env / secret manager; rotate on exposure).
3. If a security issue is found: **STOP**, fix CRITICAL items before continuing, rotate exposed secrets, and scan for the same class of defect elsewhere.

### Mandatory checklist

- [ ] No hardcoded secrets (API keys, passwords, tokens)
- [ ] User inputs validated at trust boundaries
- [ ] Injection prevented (parameterized queries / safe APIs — no string-concat SQL)
- [ ] XSS prevented (encode/sanitize untrusted HTML)
- [ ] CSRF protection enabled where session cookies authenticate state-changing requests
- [ ] Authentication and authorization verified for the change
- [ ] Rate limiting on exposed endpoints (or documented exception)
- [ ] Error messages do not leak secrets, stack internals, or PII

### Response protocol

1. Stop feature work on the affected path
2. Triage severity (CRITICAL blocks merge)
3. Fix CRITICAL before continuing
4. Rotate any exposed secrets
5. Search the codebase for the same pattern

## Rationale

A short, always-on baseline catches the most common agent/codegen security misses without replacing deeper security rules (prompt injection, PII scrubbing).

## Verification

| Check | Blocks |
|---|---|
| Secret scan + checklist affirmed in PR for security-sensitive diffs | pre-commit / pr-merge |

## Examples

### Good

- New endpoint uses parameterized queries, validates input, and pulls credentials from the secret store.

### Bad

- Handler builds SQL with string concat and returns raw exception text to clients.

## Exceptions

- Non-production spikes in isolated sandboxes may defer rate-limit wiring when explicitly labeled and not mergeable to protected branches.
