# /sdd command catalog

Source of truth for slash commands in ai4se-plugins. Adapters MUST project from this file; do not invent divergent names.

## Schema

| Column | Meaning |
|---|---|
| `/sdd` | Slash name (without host-specific prefix quirks) |
| `target` | `skill.<domain>.<name>` or `recipe:pN` |
| `path` | Skill dir or recipe file |
| `impl` | `recipe` \| `wrap-openspec` \| `wrap-superpowers` \| `native` |
| `upstream_ref` | Upstream command or `—` |
| `status` | `stub` \| `draft` \| `ready` |
| `gate` | `true` if human-only decision/gate |
| `auto_invoke` | Always `false` when `gate=true`; else host policy (default false early) |

## Stage facades

| /sdd | target | path | impl | upstream_ref | status | gate | auto_invoke |
|---|---|---|---|---|---|---|---|
| `/sdd:p0` | `recipe:p0` | `recipes/p0.md` | `recipe` | — | `ready` | false | false |
| `/sdd:p1` | `recipe:p1` | `recipes/p1.md` | `recipe` | — | `ready` | false | false |
| `/sdd:p2` | `recipe:p2` | `recipes/p2.md` | `recipe` | — | `ready` | false | false |
| `/sdd:p3` | `recipe:p3` | `recipes/p3.md` | `recipe` | — | `ready` | false | false |
| `/sdd:p4` | `recipe:p4` | `recipes/p4.md` | `recipe` | — | `ready` | false | false |
| `/sdd:p5` | `recipe:p5` | `recipes/p5.md` | `recipe` | — | `ready` | false | false |

## Contract / work-package commands

| /sdd | target | path | impl | upstream_ref | status | gate | auto_invoke |
|---|---|---|---|---|---|---|---|
| `/sdd:bootstrap` | `skill.harness.bootstrap` | `skills/harness/bootstrap/` | `native` | — | `ready` | false | false |
| `/sdd:pipeline-iac` | `skill.harness.pipeline-iac` | `skills/harness/pipeline-iac/` | `native` | — | `ready` | false | false |
| `/sdd:evolve` | `skill.harness.evolve` | `skills/harness/evolve/` | `native` | — | `ready` | false | false |
| `/sdd:rules-govern` | `skill.rules.govern` | `skills/rules/govern/` | `native` | — | `ready` | false | false |
| `/sdd:explore` | `skill.intent.explore-kit` | `skills/intent/explore-kit/` | `wrap-superpowers` | Superpowers brainstorming (etc.) | `ready` | false | false |
| `/sdd:pre-spec-workdir` | `skill.intent.pre-spec-workdir` | `skills/intent/pre-spec-workdir/` | `native` | — | `ready` | false | false |
| `/sdd:pre-spec-req` | `skill.intent.pre-spec-req` | `skills/intent/pre-spec-req/` | `native` | — | `ready` | false | false |
| `/sdd:proposal-split` | `skill.intent.proposal-split` | `skills/intent/proposal-split/` | `wrap-openspec` | `/opsx:propose` (+ continue family) | `ready` | false | false |
| `/sdd:story-split` | `skill.intent.story-split` | `skills/intent/story-split/` | `native` | — | `ready` | false | false |
| `/sdd:impact-analysis` | `skill.intent.impact-analysis` | `skills/intent/impact-analysis/` | `native` | — | `ready` | false | false |
| `/sdd:agent-evalset` | `skill.eval.agent-evalset` | `skills/eval/agent-evalset/` | `native` | — | `ready` | false | false |
| `/sdd:delta-craft` | `skill.spec.delta-craft` | `skills/spec/delta-craft/` | `wrap-openspec` | `/opsx:propose` / continue → specs | `ready` | false | false |
| `/sdd:g1-decision-record` | `skill.spec.g1-decision-record` | `skills/spec/g1-decision-record/` | `native` | — | `ready` | true | false |
| `/sdd:living-merge` | `skill.spec.living-merge` | `skills/spec/living-merge/` | `wrap-openspec` | `/opsx:archive` | `ready` | false | false |
| `/sdd:consistency-lint` | `skill.spec.consistency-lint` | `skills/spec/consistency-lint/` | `native` | — | `ready` | false | false |
| `/sdd:atdd-design` | `skill.design.atdd-design` | `skills/design/atdd-design/` | `native` | — | `ready` | false | false |
| `/sdd:task-plan` | `skill.design.task-plan` | `skills/design/task-plan/` | `wrap-openspec` | OpenSpec `design.md` / `tasks.md` | `ready` | false | false |
| `/sdd:atdd-apply` | `skill.apply.atdd-apply` | `skills/apply/atdd-apply/` | `wrap-openspec` | `/opsx:apply` | `ready` | false | false |
| `/sdd:pr-submit` | `skill.apply.pr-submit` | `skills/apply/pr-submit/` | `native` | GitLab `glab` MR | `ready` | false | false |
| `/sdd:deploy-artifacts` | `skill.apply.deploy-artifacts` | `skills/apply/deploy-artifacts/` | `native` | CD trigger API | `ready` | false | false |
| `/sdd:local-verify-report` | `skill.verify.local-verify-report` | `skills/verify/local-verify-report/` | `native` | — | `ready` | false | false |
| `/sdd:pipeline-report` | `skill.verify.pipeline-report` | `skills/verify/pipeline-report/` | `native` | CI/CD read APIs; `phase=ci\|cd\|all` | `ready` | false | false |
| `/sdd:g2-decision-record` | `skill.verify.g2-decision-record` | `skills/verify/g2-decision-record/` | `native` | — | `ready` | true | false |
| `/sdd:change-summary` | `skill.archive.change-summary` | `skills/archive/change-summary/` | `wrap-openspec` | `/opsx:archive` (summary overlay) | `ready` | false | false |
| `/sdd:g3-decision-record` | `skill.archive.g3-decision-record` | `skills/archive/g3-decision-record/` | `native` | — | `ready` | true | false |
| `/sdd:followups` | `skill.archive.followups` | `skills/archive/followups/` | `native` | — | `ready` | false | false |
| `/sdd:repo-understand` | `skill.common.repo-understand` | `skills/common/repo-understand/` | `native` | — | `ready` | false | false |
| `/sdd:quality-checklist` | `skill.common.quality-checklist` | `skills/common/quality-checklist/` | `native` | — | `ready` | false | false |

## Maintenance

1. Add a row here before adding host adapter entries.
2. Flip `status` to `ready` only when SKILL body meets Inputs/Outputs/Non-goals/Failure/Conventions and isolation checks.
3. Sort new contract rows by domain then name when editing in bulk.
