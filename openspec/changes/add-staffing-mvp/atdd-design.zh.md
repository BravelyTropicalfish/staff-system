# ATDD 设计：add-staffing-mvp

> 本文是 `atdd-design.md` 的中文译本，供评审阅读。两者如有冲突，以英文原版 `atdd-design.md` 为准。
> **场景名保留英文原文**，理由同 `tasks.zh.md`：它们是规范标识，也是 T27 追溯矩阵的键。

- **变更 ID：** `add-staffing-mvp`
- **门禁上下文：** rev 11 于 2026-11-11 **通过** G1（`g1-decision.md`）；本文是 `/sdd:task-plan` 的 P3 输入
- **被测 spec：** `specs/staffing/{allocation,candidate-matching,people-import,people-pool,
  post-requirement,staffing-confirmation}/spec.md` —— 40 条需求 / 90 个场景
- **适用规则：** `testing-strategy`、`pipeline-gates`、`architecture-baseline`
- **本文范围：** 验收层（API + E2E）、写生产代码之前要先写的检查、门禁对齐、fixtures。不设计单元测试
  套件，也不写任何功能代码。

## 1. 纳入范围的 ATDD 层

两个验收层，外加留到 apply 阶段的单元工作。

| 层 | 证明什么 | 形态 |
|---|---|---|
| **API** | 校验、授权、状态迁移、回写、行不存在 | 一次 HTTP 请求打真实关系库，不经过 UI |
| **E2E** | 跨屏的用户可见行为，以及只有全链路才能证明的「没有被展示」 | 浏览器、种子账号、8 个场景 + 2 条旅程 |
| **Defer UT** | 本节末尾列出的纯逻辑 | apply 阶段经 `/sdd:atdd-apply` 走 TDD |

本文件起草时技术栈尚未确定（proposal 假设 6、开放项 D1）；D1 已在设计阶段关闭（ADR-001）。因此本文件
只固定契约、检查与数据，**不固定框架**——运行栈与 E2E runner 由 `design.md` 记录，这里不另造测试产品。

### 按能力的层分配

| 能力 | 需求 | 场景 | API | E2E | 留到 apply（单元 TDD） |
|---|---|---|---|---|---|
| `allocation` | 5 | 11 | 10 | 1 | 闭区间相交判定；手工与确认来源的规则 |
| `candidate-matching` | 5 | 15 | 15 | 0 | 门槛顺序与首个失败选择；档位次序；姓名排序；兜底判定 |
| `people-import` | 5 | 15 | 15 | 0 | 单元格归一；受控集合判定；批次事务边界 |
| `people-pool` | 8 | 17 | 14 | 3 | 档位次序复用；一人一角色不变式 |
| `post-requirement` | 8 | 14 | 14 | 0 | 由名额推导状态；被锁字段集合 |
| `staffing-confirmation` | 9 | 18 | 14 | 4 | 作废集合（仅相交日期）；回写步骤 |
| **合计** | **40** | **90** | **82** | **8** | — |

每个场景**恰好一个**主层；API 列是 E2E 清单的补集，因此没有场景被丢掉。8 个 E2E 场景中有 7 个另配一条
低成本的 API 配套检查（结束释放的载荷、只读的个人档案、档位名称、占用清单、确认的可见性、搜索排除、通知清单）；
配套检查不新增场景行，也永不成为主层。

### 8 个 E2E 场景

| 能力 | 场景 | 为什么不能放 API |
|---|---|---|
| `allocation` | The end releases everyone and cancels the posts | 证据跨越项目、岗位、占用与后续的一次搜索 |
| `people-pool` | The profile is read-only | 断言的是「没有可点的入口」 |
| `people-pool` | A band is displayed, never a number | 断言用户读到什么 |
| `people-pool` | Profile lists a person's occupancy | 在册与已结束区间同屏出现 |
| `staffing-confirmation` | Confirmation is visible to both roles | 两个账号、两块屏、一个状态 |
| `staffing-confirmation` | The confirmed person leaves other searches | 另一角色的搜索里「不出现」 |
| `staffing-confirmation` | The notice list explains each loss | 断言协调员读到的原因文本 |
| `staffing-confirmation` | Both roles see the same gap numbers | 渲染结果在两种角色之间相等 |

两条旅程把这些串起来，但不新增场景：（J1）导入 → 项目 → 岗位 → 搜索 → 推荐 → 确认 → 缺口视图；
（J2）项目结束 → 全员释放 → 同一批人出现在后续岗位的搜索里。

### 留到 `/sdd:atdd-apply`（单元级 TDD）

两端闭合的 `[start, end]` 相交 · 三档标尺上的档位次序 · 门槛评估顺序与首个失败门槛 · 姓名排序与工号
并列 · 单元格归一与受控集合判定 · 跨角色兜底判定（职级 +1 且技能域内技能达 `Practitioner` 或以上）·
名额到状态的推导 · 全有或全无的批次边界 · 相交日期的作废集合 · 占用来源规则。这些是 apply 阶段红绿的
建议切入点，不是套件计划。

## 2. 生产代码之前先写什么

顺序固定，与 proposal 的交付顺序一致；每个阶段的验收检查在其功能代码存在之前必须是红的。

1. **种子与 fixtures**（第 4 节）——目录、两个账号、各种 builder。
2. **按阶段的 API 检查**——`people-pool` → `people-import` → `allocation`（项目、区间、项目结束）→
   `post-requirement` → `candidate-matching` → `staffing-confirmation`。每个能力一个路由组、一个服务
   入口，检查不碰内部实现（`architecture-baseline` 规则 5）。
3. **E2E 检查**——8 个场景与 J1/J2，在该阶段的页面存在后写；J1 最后转绿。
4. **单元测试**——apply 阶段与代码同写，永不在此设计。

每条检查都能追溯到 delta spec 里的场景标题；一条检查找不到对应场景，说明场景集有问题，而不是测试有问题。

## 3. 本地与 CI 的门禁期望

与 `pipeline-gates` 对齐（不引入第二套 CI 语义）：

| 检查 | 位置 | 说明 |
|---|---|---|
| lint、类型检查 | 本地停止检查 + CI | 必需 |
| 单元测试 | 本地停止检查 + CI | 与代码同写；覆盖率 100%（`testing-strategy`） |
| 验收测试 —— API | 本地停止检查 + CI | 82 个 API 场景 |
| 验收测试 —— E2E | PR 前本地 + CI | 8 个场景与 2 条旅程；runner 由 `design.md` 选定 |
| 依赖与安全扫描 | CI | 必需 |

停止检查顺序：静态 → 单元 → API 验收 → E2E。前一阶段红即停止后续。

本变更内要补两个缺口，二者都归 `/sdd:task-plan`：

- **尚无 CI workflow。** 仓库的 `.github/` 只有 agent 资产，没有 workflow，因此本次变更要新增一个运行五项
  必需检查的 workflow，且不引入新的 CI 产品。
- **`docs/feedback/verification-commands.md` 仍是占位。** `pipeline-gates` 规则 7 要求真实命令与检查同一
  变更内落地。

## 4. 数据与 fixtures

- **目录种子（固定）：** 五个角色 `PM, BA, DEV, QA, UX`；六个技能 后端开发、前端开发、项目管理、业务分析、
  测试、UI设计，对应 `DEV/DEV/PM/BA/QA/UX` 技能域；三套档位标尺。
- **账号（固定）：** 一个协调员与两个 lead（第二个 lead 只为证明范围隔离），带角色种子，绝不用 SSO。
- **人员：** 只用假名；工号取自 `E1xx` 段，按测试组划分命名空间，使导入批次不会跨测试撞号。永不使用生产
  数据（`testing-strategy` 规则 5）。
- **工作簿由代码从表生成**，不以 `.xlsx` 二进制入库：有效、一行坏行、缺 sheet、空 sheet、工号重复、技能行
  重复、孤立技能行、一格多角色、停用人员重新激活、仅刷新技能。
- **日期是显式字面量**（`2027-01-01` …），边界用例永不依赖系统时钟。人物 `E100` 贯穿 fixture 线：
  2027-01-01 至 03-31、02-01 至 05-31、06-01 至 08-31。
- **builder**：项目、岗位、占用、推荐、通知各一个，只收最少参数、其余取默认，让场景读起来就是它的 GIVEN。
- **隔离与清理：** 每个测试一套全新 schema（或回滚事务）；测试任意顺序均通过且不共享可变状态
  （`testing-strategy` 规则 7）。导入 fixture 随 schema 一起销毁，批次历史不会在测试间泄漏。

## 5. Non-goals

- 本文不含生产功能代码、不含 UI 实现、不含 schema 迁移。
- 不设计单元测试套件；单元工作留到 `/sdd:atdd-apply`。
- 不做 Playwright 的 page object model，不做 Excel 或用例批量生成器——工作簿 fixture 是数据构造器，不是
  用例生成器。
- 不引入新的 CI 产品，不做部署流水线（`proposal` 的 Non-Goals：只做本地运行与测试）。
- 不为让测试好写而新造场景；场景有变化就回到 spec 流程。

## 6. Non-goal 反向验证

proposal 的每个 Non-Goal 至少配一个反向用例——系统必须明显地**不做**那件事。

| # | Non-goal | 反向检查 | 层 |
|---|---|---|---|
| 1 | 不做外部集成或定时投递 | 没有注册任何出站作业或调度器；人员数据只随批次 id 变化 | API |
| 2 | 不手工编辑人员主数据 | 修改人员的角色、职级、技能档位或差旅一律拒绝；没有路由接受这些字段 | API |
| 3 | 不支持兼职 | 占用不带百分比；提交该字段视为未知字段而拒绝，且不改变任何状态 | API |
| 4 | 不自动指派 | 只有一个候选且通过全部门槛的岗位，仍保持 `open` 直到有人确认 | API + E2E（J1） |
| 5 | 不做通知投递 | 不存在邮件或聊天通道；丢单通知只能在应用内读到 | API |
| 6 | 不涉及成本、费率、预算或利用率 | 岗位与占用的载荷拒绝这些字段；没有路由读取它们 | API |
| 7 | 无面试或审批链 | 推荐由 lead 一步确认，没有中间状态 | API |
| 8 | 不做移动端、离线或多租户 | 任何地方都不接受租户参数；唯一范围就是一家公司的数据 | API |
| 9 | 不做目的地差旅 | 差旅只接受 `none < limited < full`；提交目的地字段被拒绝 | API |
| 10 | 不做生产部署流水线 | 新增的 workflow 只跑测试，不含部署作业 | CI 配置评审 |

落地：第 1–9 行在 `NonGoalApiTest` 中执行，第 10 行是对 `.github/workflows/ci.yml` 的审查。第 3、6、9
行依赖「字段被拒绝」，因此应用打开了严格未知字段策略
（`spring.jackson.deserialization.fail-on-unknown-properties=true`）并返回 400 `unreadable body`；
否则字段会被丢弃，Non-Goal 就等于被悄悄支持了。

## 7. NFR 测试（范围：仅安全与幂等）

本轮已与产品负责人确认；其余记入 deferred。

**授权（API）。** 每条路由都按 spec 强制角色范围：lead 不能建项目、不能编辑岗位（403 且状态不变）；lead
打不开别人项目的岗位，也打不开合适列表；协调员不能确认；只有项目自己的 lead 能结束项目；只有协调员能
推荐、撤回与录入手工占用。每项检查同时断言被拒的调用没有改变行数。

**幂等（API）。** 重复上传同一工作簿只新增批次，不改变任何人员或技能状态（不产生重复行、不重排档位）；
同一人同一日期的并发双确认最终只留一条指派与一条占用，失败方被拒并给出原因；重复释放时第二次被拒且名额
状态不变。
`IdempotencyApiTest` 承载这三项检查，各一个测试，每一项都是行数断言。

**Deferred 及理由：**

- *性能*——导入是同步的，量级几千行（proposal 假设 9）；没有使用画像就给出延迟预算等于编数字。
- *可观测性*——MVP 的审计线索就是批次错误清单；结构化日志与指标属于后续变更。
- *韧性、兼容性、可访问性、协议、AI 评估*——本变更没有任何信号：无外部服务、无流式、无模型、只有一个
  浏览器目标。

## 8. 交棒给 `/sdd:task-plan`

- 任务顺序 = 第 2 节（种子 → 按能力写 API → E2E → 单元与代码同写）。
- `/sdd:task-plan` 产出 `design.md`（技术栈、E2E runner、schema）与 `tasks.md`，加上 CI workflow，并填写
  `docs/feedback/verification-commands.md`。
- 进入条件已满足：rev 11 通过 G1。后续 spec 若变更，本文件的检查随之作废，需要新的 G1 记录。
