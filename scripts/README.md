# Harness scripts

Bootstrap creates **placeholder** shells below. Running a placeholder prints a
customize prompt and exits until the using team replaces the body.

| Script | Role |
|---|---|
| `init.sh` | Environment / “start of day” setup |
| `kb-lint.sh` | KB consistency + compile `docs/knowledge/_INDEX.md` |
| `verify-links.sh` | Bidirectional link checks |
| `archive-change.sh` | Archive helper (e.g. wrap OpenSpec archive + KB checks) |

Do not invent a second Spec authority tree from these scripts. Re-running
`/sdd:bootstrap` will not overwrite non-empty shells (including customized
implementations).
