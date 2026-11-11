# Bundled dependencies

| Component | Pinned source | Included content |
|---|---|---|
| AI4SE plugins | 1.0.1 | Skills, agents, catalog, recipes, shared resources, helpers, and copy-projection libraries |
| Superpowers | `22f0b463e7a5e579904dd6211a520321035526f7` | Skills and their resources from [obra/superpowers](https://github.com/obra/superpowers/tree/22f0b463e7a5e579904dd6211a520321035526f7) |
| OpenSpec | 1.16.1 | Existing Qoder commands and Copilot prompts/skills; install the CLI separately |

Superpowers is distributed under its [MIT license](superpowers/LICENSE).
The OpenSpec assets retain the [MIT license](OPENSPEC-LICENSE).

The AI4SE runtime is bundled with this baseline for course use. Maintainer-only
installation entry points, editor metadata, local installation records, and
author checkpoint logs are not part of this runtime subset.

`scripts/refresh-agent-assets.mjs` at the repository root uses the bundled AI4SE
projection libraries in copy mode. It keeps script paths and shared references
inside this repository and copies Superpowers to both supported host directories.
The host assets are committed so learners need no separate plugin checkout.
