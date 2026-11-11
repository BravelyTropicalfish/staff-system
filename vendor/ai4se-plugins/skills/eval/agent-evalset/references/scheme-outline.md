# Agent evaluation scheme outline (pack default)

Deliver a **scheme** document for building an Agent evaluation dataset and
release gates. This is a plan — not a golden corpus and not a harness run.

**Authoring rules**

- Insufficient evidence → leave tables empty and list **Gaps**. Never invent
  business facts or numeric thresholds; thresholds are team-owned and must be
  recorded when set.
- Tag statements as Confirmed / Assumption / Gap where useful.
- Store **questions and grading criteria only** in schema design — never agent
  run outputs as sample bodies.

---

## 1. Purpose & scope

- Dual purpose: (1) reliability under change (model, prompt, knowledge, tools,
  permissions, runtime); (2) relative comparison vs a locked baseline.
- In scope for this document: scheme sections below.
- Out of scope for this document: embedding a pack-local eval dataset
  directory; dumping filled golden rows; replacing security pen-tests or load
  tests.

## 2. Capability domains

List capability domains under test. For each domain:

| Field | Notes |
|---|---|
| Domain id / name | Stable identifier |
| Description | One short paragraph |
| Risk tier | `low` \| `mid` \| `high` — if disputed, choose **higher** |
| Primary task type(s) | `classification` \| `RAG` \| `tool-use` \| `open-gen` \| `multi-turn` |
| Object layers | L1 outcome (default must); L2 trace; L3 component — see matrix |
| Safety cross-cut | Always consider prompt-injection, PII, authz, faithfulness |

**Object layers (reference)**

| Layer | Evaluates | Question answered |
|---|---|---|
| L1 Outcome | Final user/downstream result | Task complete? Correct and safe? |
| L2 Trace | Full path: steps, tools, retrieval, efficiency | Process reasonable? |
| L3 Component | Single tool/retrieval/reasoning step | Which step regressed? |

**Task-type overlays (add on top of the matrix)**

| Task type | Extra focus |
|---|---|
| Classification / extraction | L1 usually sufficient |
| RAG | Retrieval quality + faithfulness (report retrieval vs generation separately) |
| Tool-use | Selection, parameters, order, side-effect assertions |
| Open generation | Multi-dimension rubric (no single score without rubric) |
| Multi-turn / agentic | Task completion + trace reasonableness + error recovery |

## 3. Object × risk matrix

Fill each cell: `must` / `sample` / `skip`. Default guidance:

| Object | Low | Mid | High |
|---|---|---|---|
| L1 Outcome | must | must | must |
| L2 Trace | sample/skip | should | must |
| L3 Component | sample/skip | sample/skip | must for critical components |
| Safety (cross-cut) | basic | extended | full + adversarial |

**Critical component** (any one): irreversible / external commitment; security
control point; output that decides later branches.

## 4. Task standard cards

One card per domain or per primary task type (YAML-friendly):

```yaml
task_type:            # classification | RAG | tool-use | open-gen | multi-turn
eval_layers:          # L1 / L2 / L3
primary_metrics:      # names + short definitions (no invented numbers)
rubric_dimensions:    # if Judge/human; dimensions + grade bands
threshold_method:     # absolute floor + relative regression tolerance (intent)
gate_type:            # Hard | Soft
reproducibility:      # fixed seed vs multi-sample aggregate
agreement_target:     # reviewer agreement intent (team-set)
pass_k:               # if high-risk irreversible; k is team-set
```

**Dual-track thresholds:** every release-gate metric needs (1) an absolute floor
and (2) a relative regression tolerance vs baseline. Safety/compliance metrics
need floors. Record owner + rationale when numbers are chosen — do not invent
them in this scheme if unknown.

## 5. Sample schema

Every planned golden sample **must** define these fields (schema only):

| Field | Meaning |
|---|---|
| `id` | Globally unique |
| `input` | User query / input context |
| `expected` | Reference answer **or** rubric reference (at least one) |
| `task_type` | classification / RAG / tool-use / open-gen / multi-turn |
| `risk_level` | low / mid / high |
| `eval_layers` | L1 / L2 / L3 applicable to this sample |
| `grader` | Pointer: rule id / Judge config version / human process |
| `source` | handcrafted / production-feedback / synthetic |
| `sanitization_status` | Required for production-sourced samples |
| `annotation` | Annotator, method, reviewer, date |
| `version_added` | Dataset version when first added |
| `status` | draft / active / quarantined / deprecated |

**Optional extensions:** `reference_contexts` (RAG), `expected_tool_calls`
(tool-use), `conversation_history` (multi-turn).

**Hard rule:** samples store questions + grading criteria only — **never** a
concrete Agent run output as the sample body.

## 6. Scale & stratification

**Quality pillars (plan how you will satisfy each):**

| Pillar | Intent |
|---|---|
| Business representativeness | Align to production distribution; record known gaps |
| High-risk coverage | Risk-list → case traceability; each known high-risk failure mode ≥ 1 active sample |
| Label trust | High: dual review + SME; mid: single + audit; low: assist + sample audit |
| Versioning | Immutable snapshots + semver + changelog; reports cite the triple |

**Split intent (recommended, non-overlapping):**

| Split | Use | Suggested share |
|---|---|---|
| Eval | Day-to-day scoring | ~70% |
| Calibration | Judge calibration | ~20% |
| Holdout | Blind check (anti-overfit) | ~10% |

Do not mix splits. If Holdout is exposed to the Judge, rebuild it.

**Dataset Card (fill what is known):**

```yaml
dataset_name:
version:
owner:
risk_distribution:        # low / mid / high shares
task_type_distribution:
split_ratio:              # Eval / Calibration / Holdout
quality_self_assessment:  # four pillars
high_risk_scenarios:
known_gaps:
last_review_date:
```

## 7. Judge & baseline strategy

**Grader ladder (cheap first):**

1. Deterministic rules (exact match, regex, JSON schema, state asserts)
2. LLM-as-Judge (only when not programmable) — calibrated
3. Human / SME (high-risk final gate or Judge disagreement band)

**Judge governance essentials (scheme commitments):**

- Calibrate before use; re-calibrate on Prompt/model/rubric change
- Version Prompt, model, params, rubric
- Prefer independence from the agent under test; mitigate position bias
- Require rationale — score-only output is unacceptable for gates
- Agreement below team threshold → do not use that metric as a release gate

**Baseline triple:** comparisons are valid only when
`agent_version × dataset_version × judge_version` are recorded. Baseline
updates need review to avoid silent drift.

**Hard vs Soft gates:** Hard blocks release; Soft monitors. Soft→Hard is fine
for stricter risk; Hard→Soft needs an explicit recorded rationale.

## 8. Lifecycle (slim)

**Sample state machine (intent):**

```text
draft → in_review → active
                      ↓
              quarantined (dispute / suspected bad label)
                      ↓
           active (cleared) or deprecated
```

Suspected bad labels → quarantine first; do not silently edit active items to
make a new agent pass.

**Dual tracks:**

| Track | Trigger | Intent |
|---|---|---|
| Offline | Version / release | Golden regression vs baseline |
| Online | Continuous | Sampled traffic + safety monitors (drift) |

**Production feedback pipeline (optional card — enable or defer):**

```yaml
enabled:              # yes | deferred | no
source_types:         # incident / low-score / user-negative / safety-hit
scrubbing:            # auto + human review (both required if enabled)
filter_criteria:
compliance_level:
annotation_reqs:
ingest_version_policy:
regression_trigger:
```

Do **not** expand full judge-migration playbooks or exception-approval matrices
in this scheme unless the team explicitly asks; note as Gaps / follow-ups.

## 9. Traceability

Link this scheme to:

| Pointer | Example |
|---|---|
| Change / slice id | Pre-SDD or OpenSpec change id |
| Specs / stories | Paths or ids for agent behaviors under test |
| Risk list | High-risk scenarios → planned samples |

**Report minimum-field intent** (for later runs — not executed by this skill):

| Category | Contents |
|---|---|
| Run metadata | run_id, agent/dataset/judge versions, timestamp |
| Scope | layers, metrics, trigger |
| Gates | Hard/Soft values, pass^k mode |
| Results | scores, pass rates, distributions |
| Regression | baseline id, delta, pass/fail |
| Affected | samples marked untrustworthy via layer propagation |
| Failures | P0/P1/P2 counts |
| Trace | story / incident / CI pointers |

## 10. Out of scope / Gaps

**Out of scope for `/sdd:agent-evalset`**

- Creating `ai4se-plugins/eval/` or any pack-local golden dataset tree
- Filling golden sample bodies in this repo pack
- Running the evaluation harness end-to-end
- Writing Gate Pass/Fail decisions

**Gaps**

- List open questions, missing Spec excerpts, unset thresholds, deferred
  lifecycle choices, and owners.
