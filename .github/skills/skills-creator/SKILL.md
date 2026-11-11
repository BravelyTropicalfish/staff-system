---
name: skills-creator
description: Create or improve a reusable project skill from a learner's workflow, examples, or existing instructions. Use when asked to write a SKILL.md or teach an agent a repeatable task; ordinary execution of that task does not require a new skill.
---

# Skills Creator

Help the learner turn a repeatable task into a focused skill that another agent
can discover and use. Explain choices briefly in the learner's language; keep
repository instructions in English unless the project specifies otherwise.

## Understand the task

Identify the intended trigger, inputs, useful output, and how the learner will
judge success. Reuse examples and repository conventions already supplied. Ask
only about missing information that would change the skill's behavior.

Keep one coherent job per skill. A useful description says what the skill does
and when to use it. Distinguish a nearby task only when that prevents confusion.
Do not turn an example into a universal requirement or add unrelated process.

## Choose the location

In this baseline, author `skills/<name>/SKILL.md`. The refresh script copies this
source to `.qoder/skills/<name>/` and `.github/skills/<name>/` using regular files.
Edit the source and regenerate both host copies together.

Use a lowercase name with digits and single hyphens, at most 64 characters, and
make the folder name match the frontmatter `name`. Check existing local and
bundled skill names before choosing it. Updating an existing skill should
preserve its purpose and the learner's unrelated work.

When working in another repository, follow its existing skill layout instead of
assuming this baseline's refresh script exists. Ask which host to target only
when the repository does not establish it.

## Write the skill

Start with YAML `name` and `description`, followed by concise Markdown
instructions. Include decisions, project-specific commands, output expectations,
and failure handling that the agent cannot reliably infer. Use normal automatic
discovery unless the learner explicitly requests invocation-only behavior.

The following small example shows the format; adapt the task and completion
criteria to the learner's actual workflow:

```markdown
---
name: test-results-summary
description: Summarize supplied test output into a concise report of passed checks, failures, and next debugging steps. Use when a learner asks to explain a test run.
---

# Test Results Summary

Read the supplied test output and identify the command and source version when
available. Report passed, failed, and skipped checks separately. Quote the first
relevant failure and suggest a next debugging step tied to that evidence.

Do not invent execution results. If the output is incomplete, identify what is
missing. Running tests or editing code is a separate action that must be within
the learner's request.
```

Add `references/` only for substantial guidance needed in some situations,
`assets/` for files that belong in the output, and `scripts/` for concrete work
that benefits from reliable repeatable execution. Link resources with relative
paths. Do not add empty scaffold directories or require the author's machine,
private files, global agent configuration, or uninstalled helper tools.

## Check and try it

Review the frontmatter, folder name, resource links, and expected behavior.
Remove unfinished placeholders. Run any new helper script against a small
disposable example and inspect what it actually produced.

Try one realistic request the skill should handle and one nearby request it
should leave alone. For a substantial workflow, also try a missing-input or
failure case. Compare the outcome with the learner's success criteria, then
make focused corrections. Describe an unexecuted scenario as a review, not a
successful run.

In this baseline, run from the repository root:

```sh
node scripts/refresh-agent-assets.mjs
node --test scripts/portability.test.mjs
```

These checks cover packaging and references; they do not prove that an agent
will make the right decisions. Reload skills in the selected host if needed and
try a real request. Report the source path, an example invocation, checks run,
and remaining limitations. Creating a skill does not authorize commits, pushes,
installation outside the project, or publication to other repositories.
