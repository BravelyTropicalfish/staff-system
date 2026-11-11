# AI4SE Baseline

A starting repository for AI-assisted, specification-driven development with
Qoder or GitHub Copilot. It includes the project skeleton, OpenSpec prompts,
AI4SE skills and agents, and Superpowers skills as ordinary repository files.
Cloning does not require access to the author's machine or another private repository.

## Start

1. Clone your class's `ai4se-baseline` repository and open its root in Qoder or
   GitHub Copilot.
2. Install Node.js 20.19 or later and Git. For OpenSpec commands, install the
   matching CLI: `npm install -g @fission-ai/openspec@1.16.1`.
3. Run `node --test scripts/portability.test.mjs` to check the bundled files.
   Run `openspec --version` to check the separately installed CLI.
4. Read [AGENTS.md](AGENTS.md) and the
   [SDD command catalog](vendor/ai4se-plugins/commands/catalog.md).
   Qoder exposes the `sdd-*` skills; in an existing session use `/skills reload`
   and `/agents reload`. Copilot's skills and custom agents are under `.github/`.

The bundled skills are already available after cloning. Do not run the original
plugin installer merely to use this baseline. Shell-based skill helpers require
Bash (for example Git Bash on Windows); their use depends on the selected skill.

## Project content

- [Knowledge base](docs/knowledge/_INDEX.md)
- [Requirement workspace](requirement-workspace/README.md)
- [OpenSpec workspace](openspec/README.md)
- [Harness scripts](scripts/README.md)
- [Bundled dependency versions and licenses](vendor/README.md)

This baseline has no application implementation or CI/CD pipeline. The four
original harness scripts are intentional placeholders and exit with a message
until the project supplies an implementation.

## Create your own skill

Ask your agent: "Use skills-creator to turn my test-result reporting workflow
into a reusable skill." The [skills-creator](skills/skills-creator/SKILL.md)
helps define the trigger, inputs, output, instructions, and a practical trial.
It is included in both Qoder and GitHub Copilot.

Author project skills in `skills/<name>/SKILL.md`, then run:

```sh
node scripts/refresh-agent-assets.mjs
node --test scripts/portability.test.mjs
```

The refresh copies project skills into both hosts and keeps their resource links
portable. Commit the source and generated copies together when ready. It preserves
host-only custom skills with different names.

## Maintain the bundled assets

The AI4SE runtime sources and shared resources are under `vendor/ai4se-plugins/`.
Superpowers sources are under `vendor/superpowers/`. After updating these sources,
run `node scripts/refresh-agent-assets.mjs`, then the portability tests above.
The refresh reuses the AI4SE pack's copy projection and produces relative resource
links for both hosts. Commit the bundled sources and their host projections together.
