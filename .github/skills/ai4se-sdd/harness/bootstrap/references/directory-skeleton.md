# Default repository skeleton (pack default)

Use this layout when bootstrapping an AI-ready engineering repo. Adjust only when
the checkpoint log records an explicit deviation. Prefer additive create; never
delete user content.

## Review tree (pack default)

Full target shape for a greenfield apply with `spec-backend=openspec` and
`extra-knowledge=docs/knowledge`. Decision-driven omissions are noted below.

```text
<repo-root>/
│
│ ── Instructions ──────────────────────────────────────────
├── AGENTS.md                          # root agent constitution (short)
├── README.md                          # human entry
│
│ ── KB (cross-cutting) ────────────────────────────────────
├── docs/
│   ├── knowledge/
│   │   ├── _SCHEMA.md                 # frontmatter / KB conventions
│   │   ├── _INDEX.md                  # compiled index placeholder (do not hand-author long-term)
│   │   ├── _LOG.md                    # append-only KB timeline stub
│   │   ├── architecture/              # system architecture, C4, topology
│   │   │   └── README.md
│   │   ├── domain/                    # DDD bounded contexts
│   │   │   ├── shared/                # cross-BC vocabulary
│   │   │   │   └── README.md
│   │   │   └── README.md
│   │   ├── rules/                     # business / compliance rules
│   │   │   └── README.md
│   │   ├── interfaces/                # API / events / integration contracts
│   │   │   └── README.md
│   │   ├── operations/                # ops, monitoring, release
│   │   │   └── README.md
│   │   └── external/                  # external-system access notes
│   │       └── README.md
│   ├── feedback/                      # verification / rubric support docs
│   │   ├── README.md
│   │   ├── verification-matrix.md
│   │   ├── verification-commands.md
│   │   └── evaluator-rubric.md
│   └── ai4se-plugins/                 # /sdd:* process checkpoint logs (not Living Spec)
│
│ ── Pre-spec requirements workspace ───────────────────────
├── requirement-workspace/
│   ├── README.md                      # purpose, boundary, handoff to OpenSpec
│   ├── handoff.md                     # map into openspec/changes/<id>/
│   ├── registry.md                    # traceability registry stub
│   ├── intents/                       # raw intent captures
│   ├── requirements/                  # structured requirement entries
│   ├── _templates/                    # requirement / story / checklist templates
│   ├── analysis/                      # in-progress requirement analyses
│   ├── ready/                         # input-ready requirements (+ stories)
│   └── archive/                       # archived after Spec-ready handoff
│
│ ── SDD (OpenSpec) ────────────────────────────────────────
├── openspec/
│   ├── README.md                      # backend note + tree map
│   ├── config.yaml                    # minimal placeholder (extend with OpenSpec install)
│   ├── templates/
│   │   ├── change-spec.md             # change-local spec template stub
│   │   └── main-spec.md               # living capability spec template stub
│   ├── specs/                         # living main specs (source of truth)
│   └── changes/                       # in-flight changes
│       └── archive/                   # archived change packages
│
│ ── Tools ─────────────────────────────────────────────────
└── scripts/
    ├── README.md                      # harness scripts map
    ├── init.sh                        # placeholder — prompts customize on run
    ├── kb-lint.sh                     # placeholder — KB lint + compile _INDEX.md
    ├── verify-links.sh                # placeholder — bidirectional link checks
    └── archive-change.sh              # placeholder — archive helper
```

### Intentionally deferred (not auto-created)

| Item | Why |
|---|---|
| `<sub-module>/AGENTS.md` | Need real module paths from the user; record in checkpoint log, create only when named |
| Per-BC domain folders beyond `domain/shared/` | BC names are project-specific |
| Concrete `scripts/*.sh` **implementations** | Placeholders prompt customize-on-run; real bodies belong to the using team |
| Populated Living Specs / changes | Created by `/sdd:proposal-split` / OpenSpec flows, not bootstrap |
| Full Rules policy text in `AGENTS.md` | `/sdd:rules-govern` |
| `.github/workflows/` or other CI/CD YAML | Out of bootstrap scope; use `/sdd:pipeline-iac` after Rules |

### Decision-driven omissions

| Decision | Effect on tree |
|---|---|
| `spec-backend=defer\|other` | Skip entire `openspec/` |
| `extra-knowledge=docs-only` | Skip `docs/knowledge/**` |
| `extra-knowledge=docs/knowledge` | Create full KB subtree above |
| `agents-md=defer` | Skip root `AGENTS.md` |

## Subsystem map (for reviewers)

| Area | Paths | Role |
|---|---|---|
| Instructions | `AGENTS.md` | Short always-on constitution; deep links out |
| KB | `docs/knowledge/**` | Why / domain semantics (Markdown hub) |
| Pre-spec | `requirement-workspace/**` | Intent → input-ready before OpenSpec change (shared with `/sdd:pre-spec-workdir`) |
| SDD / State | `openspec/**` | What / change packages + living specs |
| Tools | `scripts/` | Harness scripts slot |
| Feedback | `docs/feedback/**` | Verification matrix / commands / rubric stubs |
| Process logs | `docs/ai4se-plugins/` | `/sdd:*` checkpoint logs only |

## Rules carrier

Prefer a short root `AGENTS.md`. Do not dump full `/sdd` procedures into always-on
rules. Sub-module `AGENTS.md` files are layered later when modules exist.
