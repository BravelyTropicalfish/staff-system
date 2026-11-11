# pii-scrubbing — Scrub PII from logs and model context

## Metadata

| Field | Value |
|---|---|
| **ID** | pii-scrubbing |
| **Category** | security |
| **Priority** | Critical |
| **Activation** | auto |
| **Applies To** | all |
| **Stack** | — |

## Rule

1. Do not log raw PII (names, phones, IDs, payment data) by default.
2. Redact or hash before sending telemetry or model context when policy requires.
3. Document retention for any allowed PII processing path.

## Rationale

Agents and verbose logging easily leak PII; scrubbing must be default-safe.

## Verification

| Check | Blocks |
|---|---|
| Log sampling / scrubber tests | pr-merge / runtime |

## Examples

### Good

- Logs store `user_id` hash only.

### Bad

- Full passport numbers in debug logs.

## Exceptions

- None.
