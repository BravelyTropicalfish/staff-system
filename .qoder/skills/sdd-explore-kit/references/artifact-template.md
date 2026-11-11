# Exploration artifact template

Agents fill this shape (or an equivalent) before
`write-exploration.sh --apply`. Header fields are required.

# Exploration — <method>

- method: <elevator-pitch|journey-map|story-map|service-blueprint>
- timestamp: <ISO-8601 or local>
- participants / assumptions: <list or "none stated">
- persist: file | session-only

## Method outcome

<method-specific body>

## Findings

### Confirmed
- …

### Assumption
- …

### Candidate
- …

## Stop / continue

- Decision: continue to Pre-SDD | stop (insufficient info) | stop (not pursuing)
- Notes: …

## Next

- Suggested: `/sdd:pre-spec-workdir` and/or `/sdd:pre-spec-req`
