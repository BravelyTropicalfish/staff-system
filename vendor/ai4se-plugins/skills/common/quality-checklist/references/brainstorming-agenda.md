# quality-checklist brainstorming agenda

Use Superpowers brainstorming: **one question at a time**. Record each answer in
the checkpoint log before probe / evaluate.

| id | topic | Typical choices |
|---|---|---|
| C1 | `stage-selection` | list below (`all` or one/more of `p0`–`p5`) |
| C2 | `focus` | `full-stage` / one or more check ids from the selected stage checklist(s) |

Report and checkpoint persist are **always on**.

Skip only when the user already answered in this session; mark
`skipped-preanswered` in the log.

**Root:** default to cwd; do not add a root confirmation checkpoint unless the
user names a different path.

---

## C1 — stage-selection (present as a list)

When stage is omitted or unclear, ask once and show **this list** (multi-select
allowed). Prefer numbered or bullet options so the user can reply with ids.

| Option | English | 中文 |
|--------|---------|------|
| `all` | All stages (`p0`–`p5`) | 全部阶段 |
| `p0` | Project baseline DoV | 项目基线 |
| `p1` | Intent & proposal DoV | 意图与提案 |
| `p2` | Spec align DoV (G1) | 规格对齐（G1） |
| `p3` | Design & decompose DoV | 设计与分解 |
| `p4` | Apply & verify DoV (G2) | 实施与验证（G2） |
| `p5` | Archive & evolve DoV (G3) | 归档与演进（G3） |

**Recording rules:**

- User picks `all`, says “all / 全部 / none / default”, or leaves stage omitted →
  record `all` and evaluate every stage.
- User picks one or more of `p0`…`p5` → record those stage ids (comma-separated
  OK). Do **not** expand to `all` unless they chose `all`.

---

## C2 — focus (explain each check id)

After C1 is recorded, ask focus **once**. Always include `full-stage` as the
default. For the **selected** stage(s) only, list each check id with English
meaning **and** 中文 (use the tables below; for `all`, group by stage).

Typical choices: `full-stage` (default) **or** one/more check ids
(e.g. `P0-1`, `P1-3`).

### Always offer

| Option | English | 中文 |
|--------|---------|------|
| `full-stage` | Evaluate all required checks for the selected stage(s) | 对所选阶段做完整 DoV 检查（默认） |

### p0 — Project baseline / 项目基线

| id | English | 中文 |
|----|---------|------|
| `P0-1` | Harness / baseline skeleton present or explicitly deferred | 基座/基线骨架已就绪，或已明确延期 |
| `P0-2` | Rules carrier covers branch / test / pipeline / env / IaC / NFR (or lists gaps) | Rules 载体覆盖分支/测试/流水线/环境/IaC/NFR（或已列缺口） |
| `P0-3` | Catalog `/sdd:p0` sequence skills are resolvable | 目录中 `/sdd:p0` 序列技能可解析 |
| `P0-4` | No pack isolation violations in edited pack files (else Skipped) | 已编辑的 pack 文件无隔离违规（未改 pack 则跳过） |
| `P0-5` | Pipeline/IaC skeleton when required by Rules (optional; no evidence → Skipped) | Rules 要求时具备流水线/IaC 骨架（可选；无证据 → 跳过） |

### p1 — Intent & proposal / 意图与提案

| id | English | 中文 |
|----|---------|------|
| `P1-1` | Pre-SDD workdir / analysis slots present for the change intent | 变更意图对应的 Pre-SDD 工作区/分析槽位已存在 |
| `P1-2` | Requirements / Pre-SDD req artifacts exist | 需求 / Pre-SDD 需求产物已存在 |
| `P1-3` | Impact analysis brief exists (or explicit N/A with rationale) | 影响分析简报已存在（或有明确 N/A 及理由） |
| `P1-4` | OpenSpec proposal and/or story list exists | OpenSpec 提案和/或故事列表已存在 |
| `P1-5` | Explore kit run (optional; no evidence → Skipped) | Explore kit 已跑（可选；无证据 → 跳过） |
| `P1-6` | Agent evalset (optional; no evidence → Skipped) | Agent 评测方案（可选；无证据 → 跳过） |

### p2 — Spec align / 规格对齐

| id | English | 中文 |
|----|---------|------|
| `P2-1` | Delta Spec artifacts exist for the change | 变更的 Delta Spec 产物已存在 |
| `P2-2` | AC/DoV review table exists | AC/DoV 评审表已存在 |
| `P2-3` | G1 decision record when G1 is claimed complete | 宣称完成 G1 时存在 G1 决策记录 |
| `P2-4` | Consistency lint (optional; no evidence → Skipped) | 一致性检查（可选；无证据 → 跳过） |

### p3 — Design & decompose / 设计与分解

| id | English | 中文 |
|----|---------|------|
| `P3-1` | ATDD design artifact exists | ATDD 设计产物已存在 |
| `P3-2` | Design / tasks artifacts exist | 设计 / 任务产物已存在 |
| `P3-3` | Alignment self-check vs Delta Spec (no silent scope creep) | 相对 Delta Spec 的对齐自检（无静默扩范围） |
| `P3-4` | Task list is actionable (checkboxes / clear owners) | 任务列表可执行（勾选框/清晰负责人） |

### p4 — Apply & verify / 实施与验证

| id | English | 中文 |
|----|---------|------|
| `P4-1` | Local verify report exists | 本地验证报告已存在 |
| `P4-2` | Local Overall is Pass, or Fail with complete human-signed Waiver | 本地 Overall 为 Pass，或 Fail 且有完整人工签署 Waiver |
| `P4-3` | Pipeline report for required `phase=ci` (when past PR) | 过 PR 后具备所需 `phase=ci` 流水线报告 |
| `P4-4` | Pipeline `phase=cd` when claiming post-deploy verify | 宣称部署后验证时具备 `phase=cd` 报告 |
| `P4-5` | G2 decision record when G2 is claimed complete | 宣称完成 G2 时存在 G2 决策记录 |
| `P4-6` | Apply vs verify separation respected (no self-approved Gate) | 遵守 Apply/Verify 分离（无自批 Gate） |

### p5 — Archive & evolve / 归档与演进

| id | English | 中文 |
|----|---------|------|
| `P5-1` | Living-merge / archive evidence when merge claimed | 宣称合并时具备 Living-merge/归档证据 |
| `P5-2` | Change summary exists | 变更总结已存在 |
| `P5-3` | G3 decision record when G3 is claimed complete | 宣称完成 G3 时存在 G3 决策记录 |
| `P5-4` | Consistency lint (optional; no evidence → Skipped) | 一致性检查（可选；无证据 → 跳过） |
| `P5-5` | Evolve (optional; no evidence → Skipped) | 演进建议（可选；无证据 → 跳过） |
| `P5-6` | Followups (optional; no evidence → Skipped) | 后续跟进（可选；无证据 → 跳过） |
