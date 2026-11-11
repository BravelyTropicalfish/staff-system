---
name: sdd-agent-<kebab-name>
id: agent.sdd-agent-<kebab-name>
description: <one-line when-to-use for host Task/subagent hints>
status: draft
recipe: pN
facade: /sdd:pN
maxTurns: 30
isolation: session
---

# agent.sdd-agent-<kebab-name>

## Goal

One sentence Goal/Capability (not an Activity laundry list).

## When to use / Anti-trigger

- Use when: …
- Do not use when: …

## Preload skills

| Skill id | /sdd | Required |
|---|---|---|
| skill.… | /sdd:… | yes/optional |

## Tool permissions

- Allow: …
- Deny: … (list explicit denials)

## HITL checkpoints

1. …
2. …

## Orchestration notes

Ordered steps; name waits on external `/sdd` or humans.
Include completion checkpoints (agent self-check, not human Gate closure).

## Failure / escalate

- Fail → stop / escalate to role …
- Never: …

## Ready gate

- [ ] Dep skills `ready`
- [ ] Matching recipe facade usable
- [ ] Design §5.1 satisfied
