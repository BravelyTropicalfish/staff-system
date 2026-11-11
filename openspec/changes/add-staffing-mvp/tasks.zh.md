# 任务：add-staffing-mvp

> 本文是 `tasks.md` 的中文译本，供评审阅读。两者如有冲突，以英文原版 `tasks.md` 为准。
> **证据行的场景名保留英文原文**（它们是 spec 里的规范标识，也是 T27 追溯矩阵的键），任务描述与其余说明为中文。
> 勾选状态与英文版逐项一致：`[x]` = 证据已为绿，`[ ]` = 未完成。

- **变更 ID：** `add-staffing-mvp`
- **输入：** `design.md`（技术栈、分层、schema、API、门禁）与 `atdd-design.md`（82 API / 8 E2E /
  10 项单元）建立在 delta spec 之上（40 条需求 / 90 个场景，rev 11 已通过 G1）
- **顺序：** 沿用 proposal 的交付顺序——工具链 → 目录 → 导入 → 人员 → 项目与占用 → 岗位 → 匹配 →
  推荐与确认 → 验收门禁

## 约定

- 一个任务就是一次可评审的变更：先写失败的检查，再写代码（`testing-strategy` 1），最后跑绿。
- 每个任务是一个 `- [ ]` 条目：`[x]` 表示其证据今天已为绿，`[ ]` 表示尚未完成。条目下的两个子项
  承载层、阻塞任务与关闭它的证据，不必再翻别处。
- 任务不得扩大范围：一个场景之外新增检查属于 spec 问题，不是测试问题（`atdd-design.md` §5）。
- 每个任务不改动本能力之外的东西，只删除自己的改动变成死代码的文件（`coding-and-refactoring` 5）。
- E2E 写在拥有该页面的任务里；单字段校验留在 API 层。
- 证据行中斜体的场景是该任务的**主检查**；场景名后的 `(payload)` 表示该场景主层为 E2E，此处的断言是
  配套断言——这样划分仍是 82 API + 8 E2E，且没有场景被计两次。
- 没有任务设计单元测试套件：单元测试与代码同写，见 `design.md` §9。

## 阶段 0 —— 工具链与骨架

- [x] **T01** —— 生成固定 3.9.9 的 Maven Wrapper 与 Java 21 上的 Boot 3.4 骨架；验证 `mvnw -v`
  - 层：`build` · 阻塞于：—
  - 证据：wrapper 与编译器分别报告 3.9.9 / 21；无测试的构建为绿

- [x] **T02** —— 加入 Spring Boot 应用：web、validation、data-jpa、security、H2（`dev` 文件库、`test` 内存库）、`schema.sql`、`ddl-auto=validate`、健康检查路由
  - 层：`API` · 阻塞于：T01
  - 证据：`mvnw verify` 为绿；`GET /api/health` 返回 200

- [x] **T03** —— 加入前端：Vite + React + TS + Tailwind + shadcn/ui + React Router，`/api` 代理到后端
  - 层：`UI` · 阻塞于：—
  - 证据：`npm run build` 为绿；应用渲染出空壳

- [x] **T04** —— 落地 schema（V1）：role、skill、person、person_skill、project、post、post_skill、assignment、allocation、recommendation、notice、import_batch、import_error、app_user；五个角色、六个带技能域的技能、一个协调员与两个 lead
  - 层：`data` · 阻塞于：T02
  - 证据：`schema.sql` 可执行；种子行可见；一人一角色的约束生效

- [x] **T05** —— 认证：会话登录/登出、演示密码哈希、每个路由组上的角色与项目范围守卫
  - 层：`API` · 阻塞于：T04
  - 证据：未认证 401（**会话切片已为绿**）；每种角色或范围越权返回 403 且状态不变——由每个路由组各自证明，因此最后一个路由组（T23）完成时才能勾选

## 阶段 1 —— 目录（`people-pool` 1–4）

- [x] **T06** —— 目录 API：角色与技能的创建、改名、停用；被引用后拒绝改名与删除；技能域按角色可读、可改
  - 层：`API` · 阻塞于：T05
  - 证据：*Seeded roles are the only choices at first use*、*A new role joins the catalog*、*Skill picker offers the seeded set*、*The seeded mapping is readable per role*、*A role added later has an empty domain*、*A domain change reuses stored data untouched*、*Delete and rename are both refused on a referenced skill*、*Deactivation hides an item from new work only*、*Rename is allowed before the first reference*

- [x] **T07** —— 协调员的目录页面：两张表、停用开关、改名被拒时给出原因
  - 层：`UI` · 阻塞于：T06
  - 证据：页面渲染出种子行；被拒的改名显示原因

## 阶段 2 —— 导入（`people-import`）

- [x] **T08** —— 工作簿模型与校验器（POI）：sheet 是否存在、是否有数据行、受控取值、小写归一、工号重复、技能行重复、孤立技能行、一格多角色、真实报表的 `ColumnMap`
  - 层：`unit` · 阻塞于：T04
  - 证据：每条规则一个单元测试；一格多角色只产出一条行错误，绝不拆成两个角色

- [x] **T09** —— 导入端点：原子应用、`APPLIED` / `REJECTED` 批次与完整错误清单、不可读时不建批次、重新激活、省略技能保留档位、不触碰排班状态
  - 层：`API` · 阻塞于：T08
  - 证据：*Skills-only refresh is accepted*、*A workbook with one sheet is refused*、*An empty workbook is refused*、*One bad row blocks the whole file*、*The corrected file is a new batch*、*A rejected batch stays readable*、*Lower case is normalised, not rejected*、*A role outside the catalog blocks the file*、*A multi-role cell is a row error, never two roles*、*A duplicated skill row blocks the file*、*An orphan skill row blocks the file*、*A duplicated employee number blocks the file*、*New and existing people in one file*、*An inactive person is reactivated by the report*、*Allocations survive an import*

- [x] **T10** —— 导入页面：上传、批次历史、每个批次的错误清单
  - 层：`UI` · 阻塞于：T09
  - 证据：「不可读」与「被拒」的区别可见；错误指明行号与 sheet

## 阶段 3 —— 人员（`people-pool` 5–8）

- [x] **T11** —— 人员档案 API：人员、唯一角色、带档位的技能、按已结束标记的在册与历史占用；只读；只给档位名称
  - 层：`API` · 阻塞于：T09
  - 证据：*A person holds exactly one role*、*Holding a skill grants no second role*、*A band is overwritten, not duplicated*、*An omitted skill keeps its band*、*Rank ordering drives comparisons*、*A band is displayed, never a number*（payload）、*Profile lists a person's occupancy*（payload）

- [x] **T12** —— 人员列表与档案页面：只读档案、档位名称、含已结束区间的占用
  - 层：`E2E` · 阻塞于：T11
  - 证据：*The profile is read-only*、*A band is displayed, never a number*、*Profile lists a person's occupancy*

## 阶段 4 —— 项目与占用（`allocation`）

- [x] **T13** —— 项目 API：仅协调员可建、列表按 lead 过滤、结束项目事务（取消岗位、以 `post cancelled` 作废未决推荐、删除占用、释放指派、项目关闭且不可重开）
  - 层：`API` · 阻塞于：T05
  - 证据：*A lead cannot create a project*、*The end releases everyone and cancels the posts*（payload）、*Only the project's lead may end it*、*A closed project cannot be reopened*

- [x] **T14** —— 占用 API 与相交规则：手工增删改、确认来源的占用禁止手改、手工重叠区间作为事实存储
  - 层：`API` · 阻塞于：T13
  - 证据：*Inverted dates are refused*、*A hand-recorded allocation is stored*、*A confirmed allocation cannot be removed by hand*、*Two overlapping allocations coexist*、*A boundary day still blocks*（unit）、*A later post stays open*（unit）

## 阶段 5 —— 岗位（`post-requirement`）

- [x] **T15** —— 岗位 API：创建与校验（创建时与改角色时角色须可用、拒绝小数名额、必要与加分技能、缺技能也有效）、状态推导、确认后锁住承载判定的字段、名额可升不可降到已确认数以下、可编辑改动作废未决推荐、已确认时拒绝取消、已取消只读、操作者规则
  - 层：`API` · 阻塞于：T14
  - 证据：*A valid post is created open*、*A fractional headcount is refused*、*A deactivated role does not block the title*、*Required and bonus rows are distinguished*、*A post without skills is valid*、*Filling the last slot*、*An inactive confirmed person keeps the post filled*、*Dates cannot move under a confirmed person*、*The lock spreads to every gate-bearing field*、*Raising the headcount reopens a filled post*、*Lowering below the confirmed count is refused*、*A bonus target change voids only pending recommendations*、*Cancel is refused, then allowed*、*A coordinator cannot edit a post*

- [x] **T16** —— 岗位页面：创建与编辑表单、按岗位与按项目的缺口视图
  - 层：`UI` · 阻塞于：T15
  - 证据：缺口数字与指派一致；被锁字段禁用并给出原因

- [x] **T17** —— 项目页面：项目列表、含岗位与缺口数字的详情、带确认提示的结束操作
  - 层：`E2E` · 阻塞于：T13, T16
  - 证据：*The end releases everyone and cancels the posts*

## 阶段 6 —— 匹配（`candidate-matching`）

- [x] **T18** —— 门槛引擎：固定顺序 G1→G6、只报第一个失败的门槛、`G6` 闭区间相交
  - 层：`unit` · 阻塞于：T14
  - 证据：每个门槛、每个边界日各一条单元测试（边界日阻挡、更晚的岗位不受影响）

- [x] **T19** —— 合适列表 API：列出全部通过者、姓名排序并与工号并列、无分数字段、排除清单带第一个失败门槛、空列表
  - 层：`API` · 阻塞于：T18
  - 证据：*The first failing gate is the reported reason*、*A bonus shortfall never excludes*、*Role equality is by identity*、*Only an intersecting range blocks*、*Everyone who passes is listed, with no score*、*A bonus shortfall is shown, not applied*、*The order comes from the name, not the fit*、*Nothing passes the gates*、*A lead cannot open the suitable list*

- [x] **T20** —— 跨角色兜底层：仅在同年角色列表为空时开启、X1 职级 +1、X2 技能域内技能达 `Practitioner` 或以上、独立数组并带证据、空技能域永不放宽
  - 层：`API` · 阻塞于：T19
  - 证据：*The fallback opens when the post role has nobody*、*One same-role candidate closes the fallback*、*A person at the required band is not above it*、*A skill outside the post role's domain never admits*、*The top band has no cross-role candidate*、*Cross-role evidence travels with the recommendation*

- [x] **T21** —— 候选页面：两个分区、每行的门槛结果与达标技能、带第一个失败门槛的排除清单
  - 层：`UI` · 阻塞于：T20
  - 证据：主列表为空时才出现兜底分区

## 阶段 7 —— 推荐与确认（`staffing-confirmation`）

- [x] **T22** —— 推荐 API：门槛与技能的快照、岗位已填满或已取消时拒绝、携带跨角色证据
  - 层：`API` · 阻塞于：T20
  - 证据：*A recommendation carries its snapshot*、*A filled post accepts no recommendation*、*A cross-role recommendation carries its evidence*、*Two proposals for one person*

- [x] **T23** —— 确认 / 拒绝 / 撤回：在一个事务里先锁岗位行再锁人员行后重检 G1 与 G6、写入指派与占用、名额减一、按日期作废并以 `person taken` 标记、仅 lead 且限本项目、停用人员或迟到占用则拒绝
  - 层：`API` · 阻塞于：T22
  - 证据：*Confirmation writes one allocation for the post's dates*、*A person deactivated after the proposal*、*An allocation recorded after the proposal*、*Another lead's post is out of reach*、*Confirmation is visible to both roles*（payload）、*The confirmed person leaves other searches*（payload）、*The other pending proposal is voided*、*A concurrent double confirmation*、*A later posting succeeds, an overlapping one waits*、*A rejection frees the candidate*、*The coordinator withdraws a proposal*

- [x] **T24** —— 释放与通知：释放删除指派与占用并让名额重新打开；每次作废给协调员写一条通知；通知在被关闭前一直保留
  - 层：`API` · 阻塞于：T23
  - 证据：*Release returns the post to the short list*、*The notice list explains each loss*（payload）、*An entry stays until it is dismissed*

- [x] **T25** —— 确认相关页面：岗位上的被推荐候选、确认与拒绝、释放、通知列表、两种角色的缺口视图
  - 层：`E2E` · 阻塞于：T24
  - 证据：*Confirmation is visible to both roles*、*The confirmed person leaves other searches*、*The notice list explains each loss*、*Both roles see the same gap numbers*

## 阶段 8 —— 验收、NFR 与门禁

- [x] **T26** —— Playwright 搭建：配置、两个账号的 storage state、经 API 造数的 fixture、导入用的工作簿 fixture 生成器
  - 层：`E2E` · 阻塞于：T03, T09
  - 证据：冒烟测试分别以两种角色登录；fixture 每次运行重建数据库

- [x] **T27** —— 旅程 J1（导入 → 项目 → 岗位 → 搜索 → 推荐 → 确认 → 缺口）与 J2（项目结束 → 释放 → 在后续搜索中重新出现）；全量套件为绿并附场景到测试的追溯矩阵
  - 层：`E2E` · 阻塞于：T12, T17, T25
  - 证据：J1 与 J2 通过；90 个场景每一个都能对应到测试 id

- [x] **T28** —— 幂等 NFR：相同文件重复上传只新增批次、不改状态；并发双确认最终只留一条指派；重复释放被拒且状态不变
  - 层：`API` · 阻塞于：T23, T24
  - 证据：三项检查，各自断言行数

- [x] **T29** —— Non-goal 反向检查：`atdd-design.md` §6 的十行
  - 层：`API / config` · 阻塞于：T27
  - 证据：十项检查，每个 Non-Goal 一项

- [x] **T30** —— CI 与验证：`.github/workflows/ci.yml` 含五项必需检查（lint、类型检查、单元、验收、依赖与安全扫描）、JaCoCo 100% 门禁、填写 `docs/feedback/verification-commands.md`、在变更内记录运行说明
  - 层：`CI` · 阻塞于：T27
  - 证据：工作流在 pull request 上运行且不含部署作业；命令文件不再是占位

## 每个任务的完成定义

1. 失败的检查先存在、之后通过；没有跳过或弱化的测试（`testing-strategy` 4）。
2. 被改动的 `domain/` 与 `api/` 代码覆盖率 100%，或该豁免在 pull request 中记录并取得评审同意。
3. 任务写明它关闭了哪些场景；`T27` 的矩阵不得有缺口。
4. 没有死代码、没有注释掉的代码块、没有不相关的改动（`coding-and-refactoring` 5–7）。

## 本次变更范围之外

部署、容器、Postgres、通知投递、兼职占用、HR/差旅集成、批次回滚与技能删除（D5/D6）、兜底名额上限或
额外审批（D8），以及任何自动评分或排序的表述。

## 对齐自检（Spec ↔ Design ↔ Tasks ↔ ATDD）

| 检查 | 结果 | 证据 |
|---|---|---|
| 每个能力都有设计章节、任务与 ATDD 层 | Pass | `people-pool` §3/§4 + T06–T12；`people-import` §7 + T08–T10；`allocation` §3/§6 + T13、T14、T17；`post-requirement` §3 + T15、T16；`candidate-matching` §5 + T18–T21；`staffing-confirmation` §6 + T22–T25 |
| 场景数与 spec 一致 | Pass | 40 条需求 / 90 个场景，划分为 82 API + 8 E2E；10 项单元留到 apply 阶段 |
| 8 个 E2E 场景都有任务承接 | Pass | T12（3）、T17（1）、T25（4） |
| ATDD Non-goal 都有反向检查 | Pass | 十行全部由 T29 承接 |
| NFR 范围（授权、幂等）都有任务 | Pass | T05 + 每路由守卫；T28 |
| 规则门禁已接通 | Pass | T30 落地五项检查并填写 `verification-commands.md` |
| 设计记录了技术栈决定 | Pass | `design.md` §1 + `adr-001-runtime-stack.md` |
| 没有任务与 ATDD 设计冲突 | Pass | E2E 只覆盖 8 个跨屏场景；不设计单测套件；不做 POM 或用例生成器 |
| 依赖构成可交付的 DAG | Pass | 每个「阻塞于」都指向更早的任务；只有 T01/T03 是起点 |
| 本步骤未写生产代码 | Pass | 只有设计与任务 |

**结论：Pass。** 下一步：`/sdd:quality-checklist`（P3），然后 `/sdd:atdd-apply`。

**结转的开放项：** D4（真实 HR 列名）留在 `ColumnMap` 之后；D5/D6 未变；D7 仍不约束；D8 维持 MVP 默认。
Maven 3.0.4 → wrapper 的决定是唯一的环境前置条件。

## 实施进度（P4）

| 日期 | 任务 | 结果 |
|---|---|---|
| 2026-11-11 | T01 | `mvnw -v` 报 Maven 3.9.9 / Java 21；`mvnw verify` 无测试时为绿 |
| 2026-11-11 | T02 | `HealthApiTest` 1/1；`GET /api/health` 返回 200 `{"status":"UP"}` |
| 2026-11-11 | T03 | `npm run build` 为绿；尚未做浏览器渲染核对（归 T26/T27） |
| 2026-11-11 | T04 | 14 张表由 `schema.sql` 建出；种子 5 角色 / 6 技能含域 / 3 账号；`SchemaSeedTest` 6/6，含「再跑一次种子不改任何计数」 |
| 2026-11-11 | T05（会话切片） | `SessionApiTest` 7/7：登录、读会话、登出、其余路由 401、不泄露内部信息；403 部分留给各自路由组 |
| 2026-11-11 | T06 | `CatalogApiTest` 14/14：种子 5 角色 / 6 技能及其域、新角色域为空、首次引用前可改名、删除恒为 409、停用不动任何已存行、换域不动人员数据、lead 每个写操作 403 而读 200 |
| 2026-11-11 | T08 | `WorkbookParserTest` 8/8 与 `ImportValidatorTest` 10/10：sheet、表头、空文件、空行、一格多角色不拆分、拼写归一、每条行规则都带行号 |
| 2026-11-11 | T09 | `ImportApiTest` 17/17：APPLIED / REJECTED 批次、形状不合格不建批次、原子拒绝、REJECTED 历史可读、重新激活、仅刷新技能、分配不被触碰、lead 403、未知批次 404 |
| 2026-11-11 | T11 | `PeopleApiTest` 9/9 与 `BandsTest` 4/4：每人一个角色且跨域技能不增角色、档位原地覆盖、省略技能保留档位、在册与历史占用且已结束项带标记、载荷只有档位名称、任何写方法均无人应答（405）、lead 两条路由 403、未知工号 404 |
| 2026-11-11 | T13 | `ProjectApiTest` 9/9：lead 建项目被拒、协调员建项目并落库区间、起止倒置 422 且零落库、未知或非 lead 账号 422、列表按所属 lead 收窄、结束时关项目并取消全部岗位且待定推荐作废为 `post cancelled` 并留下通知、占用与指派全清、协调员与另一 lead 均 403、二次结束 409、无重开路由、未知项目 404 |
| 2026-11-11 | T14 | `AllocationApiTest` 9/9 与 `RangesTest` 5/5：手工占用落库且可改可删、起止倒置 422 且零落库、两段重叠占用共存、确认来源的占用改与删均 409 `release first` 且区间未变、lead 三个动词全 403、未知人员或项目 422、已关闭项目 409、未知占用 404；谓词在共享边界日与「岗位结束日落在占用起始日」上都拦，晚于结束日的区间放行 |
| 2026-11-11 | T15 | `PostApiTest` 15/15 与 `PostStatusTest` 4/4：有效岗位以 `open` 落库并带区间、小数名额 422 且零落库、角色停用后改名仍可保存但改成另一停用角色被拒、必要与加分行各带自己的类型与档位且加分行标为 informational、无技能行也有效、缺口视图从 `partially filled` 推到 `filled` 并退出「仍缺人」清单、项目内人员停用后名额仍算已满并打标、承载判定的字段（角色、职级、必要行、起止日期、出差）一律 409 `release first` 并点名人员且零变更、名额上调重开已满岗位、下调到已确认数以下被拒、加分目标变更只作废未决推荐并写 `post changed` 通知、取消先拒后准并转为只读、协调员可读但写全 403、另一 lead 读写均 403 |
| 2026-11-11 | T18 | `GatesTest` 18/18：枚举按 spec 顺序走 G1→G6、一次评估返回逐门槛结果且全部通过者被接纳、G1–G5 各以自家理由拒绝（停用；另一角色；职级低于要求而等于要求时放行；必要技能缺失或低于档位且逐条必要行都要求、无必要行的岗位不问技能；出差意愿低于要求而同等档位放行）、角色相等比的是行 id 而不是名字、走到第一个失败即停（职级与技能同时不足时报职级）、G6 在「占用结束于岗位起始日」与「占用起始于岗位结束日」两处都拦，而岗位落在占用之前或之后均放行 |
| 2026-11-11 | T19 | `CandidateApiTest` 10/10：`GET /api/posts/{id}/candidates` 仅协调员可开、未知岗位 404，角色停用后合适列表不变且另一角色两侧都不出现，首个失败门槛即报告原因并计入对应计数，加分不足者被列出且带已持档位与目标值而全部计数为零，只有与岗位区间相交的占用才排除、更晚的岗位仍列出该人，三名通过者带角色、职级、差旅与六道门且载荷无分数与名次字段，姓名先于拟合度（名字更晚者即使技能盈余更大也靠后）且同名按工号，全员差旅不足时合适列表为空而 G5 计数为二，停用人员计入 G1 |
| 2026-11-11 | T20 | `CrossRoleApiTest` 6/6：仅在同角色列表为空时开启并给出 `no QA person passes the gates`；X1 拒绝与要求同档的人、岗位已在最高档时写明没有更高档；X2 拒绝另一技能域的最高档；一名同角色合格者即关闭兜底且 X 计数归零；每行带角色、职级、高出档数与达标技能，库里的角色仍为 `DEV`；`mvnw verify` 152 条绿 |
| 2026-11-11 | T21 | `npm test` 5/5（页面自身的读数，`node --test`）且 `npm run build` 为绿：兜底分区只在主列表为空时出现、即使载荷里仍带行也隐藏，兜底为空时仍显示理由，门槛条带全部八个代码且未跑过的计零，档位按名称显示并带加分目标，档差读作 `+1 band` / `+2 bands`；页面在 `/posts/:id/candidates`；尚未做浏览器渲染核对 |
| 2026-11-11 | T22 | `RecommendationApiTest` 9/9：合适的人被存为 `proposed`，快照带六道门与该人当时的岗位技能档位，且响应回显的正是库里 `snapshot_json` 的内容；已填满岗位回 409 `post filled`、已取消回 409 `post cancelled`，两者都不落库；由兜底放行的 DEV 快照里是 `G1, G5, G6, X1, X2` 加角色、高出一档与 `Practitioner` 的测试技能，而库里角色仍为 `DEV`；`E100` 被提到两个项目的岗位后留两行 `proposed` 且无指派无占用；同一人同一岗位再提回 409 `already proposed`，列表后被停用的人回带 `G1` 理由的 409，兜底关闭时提另一位角色的人回 409 `not a candidate`，未知人员 404，lead 调该协调员路由 403；`mvnw verify` 161 条绿 |
| 2026-11-11 | T23 | `ConfirmationApiTest` 11/11：一次确认写一条指派与一条覆盖岗位日期、`source=CONFIRMATION` 的占用，推荐变为 `confirmed` 且快照未被改写，回的是 `partially filled` 与 `remaining 1`，两个角色在岗位下看到同一位已选人员；被确认的人在日期相交的搜索里以占用理由被排除，日期不相交的岗位仍列出他；相交日期上的另一条提案变为 `voided` / `person taken` 并写出一条通告，不相交的保持 `proposed`；提案后被停用的人回 409 `G1`，迟到占用回 409 `person taken`，两者都让其仍为 `proposed` 且无指派；别项目 lead 与协调员都回 403；两个线程在同一人身上并发确认时只得一条指派、一条占用、一条 `confirmed` 与一条 `voided`，失败方回 409 `person taken`；把手工占用删掉后同一次确认可成功；拒绝会存下备注并释放此人，协调员只能撤回自己的提案；`mvnw verify` 172 条绿 |
| 2026-11-11 | T24 | `ReleaseApiTest` 4/4 与 `NoticeApiTest` 3/3：释放删除指派与其占用，回 `open` 与 `remaining 1`，岗位在 gap 里重新 `stillShort`，此人又能通过别项目上日期不相交岗位的 G6；两席中释放一席后为 `partially filled`，另一位仍在岗位下；别项目 lead 回 403，本项目 lead 与协调员都能释放；第二次释放回 404 且不改变任何东西。通告列表按三种理由各列一条，每条都有岗位、人员、理由与时间，最新的在前；把一条改成九十天前仍以原时间在列，关闭后从列表消失、再次关闭回 `already dismissed`、未知 id 回 404，lead 读与关闭都被 403 拒绝；`mvnw verify` 179 条绿 |
| 2026-11-11 | T16 | `npm test` 12/12（两个页面自身的读数，`node --test`）且 `npm run build` 为绿：创建表单把目录自己的角色、职级与出差经 `POST /api/projects/{id}/posts` 送出并落到保存后的岗位，编辑表单从 `GET /api/posts/{id}` 起笔并原样 PATCH 回去，空标题送出为空、人数送出为数字，确认存在时六个承载门槛的字段被冻结且原因按人数点名，人数以已确认数为下限，缺口面板按服务器给的三个数写成 `x of y chosen · z still needed`，每个座位点名人员、日期与停用标记；`GET /api/posts/{id}` 对本项目 lead 与协调员开放、对别项目 lead 403，`GET /api/bands` 两个角色可读（`PostApiTest` 17/17、`CatalogApiTest` 15/15）；`mvnw verify` 182 条绿；尚未做浏览器渲染核对 |
| 2026-11-11 | T25 | `npm test` 18/18（`candidates` 5、`post` 7、`confirmation` 6）且 `npm run build` 为绿：岗位页把未决提案与历史分开且保持服务器顺序，被作废的行点名理由，lead 得到确认与拒绝、协调员得到撤回、已关闭的行不给任何动词，快照读成它跑过的门槛加上背后的证据（同年层技能行，或跨角色的高几档与达标技能），通告行点名岗位、人员、理由与分钟，座位按自己的指派 id 释放；`GET /api/posts/{id}/recommendations` 让协调员拿到带快照的全部提案、让岗位自己的 lead 拿到同样的行但 `snapshot` 为 null、让别项目 lead 得到 403、未知岗位 404（`RecommendationApiTest` 11/11）；协调员的 `/notices` 页面列出每一次损失并可关闭；`mvnw verify` 184 条绿；尚未做浏览器渲染核对 |
| 2026-11-11 | T05（关闭） | 十一个路由组套件里的 31 处 `HttpStatus.FORBIDDEN` 断言全绿（目录 3、导入 2、人员 2、项目 3、占用 3、岗位 9、候选人 1、推荐 2、确认 3、释放 1、通告 2），每个守卫组都自证了 403 且状态未变；加上 `SessionApiTest` 7/7 承担 401 一半，这一项就此关闭 |
| 2026-11-11 | T07 | `npm test` 22/22（`candidates` 5、`post` 7、`confirmation` 6、`catalog` 4）且 `npm run build` 为绿：`/catalogs` 把 `GET /api/roles` 与 `GET /api/skills` 读成两张表，角色以技能名称写出自己的域、为空时写 `no domain skills`，切换按钮给的是当前不在的状态，改名只送出 trim 过的 `name`，所以被拒时行保持原样，409 原样打印 `code: message`（被引用项为 `referenced`），lead 看到同样的行但控件禁用、协调员可用；外壳改为链接 `/catalogs` 与 `/notices` 而不再承诺它们；角色来自 `GET /api/session`；没有删除按钮，因为删除路由返回空体且恒为 409 `delete refused`；尚未做浏览器渲染核对 |
| 2026-11-11 | T10 | `npm test` 28/28（`candidates` 5、`post` 7、`confirmation` 6、`catalog` 4、`imports` 6）且 `npm run build` 为绿：`/imports` 只给协调员一个 `.xlsx` 选择框与一个 Upload 按钮，拒绝读作 `refused · code: message · no batch was recorded`（这句只属于拒绝），退回的批次读作 `rejected · nothing was written · n rows to fix`，历史逐条列出文件名、状态、上传人、分钟、批次号与计数，每一条被拒的行都印成 `sheet row n · key — reason`；历史只回计数，所以每个 `REJECTED` 批次再经 `GET /api/imports/{id}` 读取自己的行；`sendForm` 是唯一 fetch 封装的 multipart 成员；尚未做浏览器渲染核对 |
| 2026-11-11 | T12 | `npm test` 33/33（`candidates` 5、`post` 7、`confirmation` 6、`catalog` 4、`imports` 6、`people` 5）且 `npm run build` 为绿：`/people` 按工号列出人员池，带唯一角色、职级、出差刻度与导入的启用状态，姓名链到各自的档案；`/people/{employeeNo}` 渲染主数据、每条技能写成 `skill · band · domain`（只写名称不写编号，测试以禁止数字出现来钉住）以及每条占用写成 `project · start → end · confirmed seat|hand record`，已过去的区间仍列出、只标 `· ended`；页面不画任何输入框与按钮，因为人员数据只由导入写入、表单字段将是一个服务器没有的写操作，姓名下的说明正是这句话；403、404 与未登录各说自己的话；尚未做浏览器渲染核对 |
| 2026-11-11 | T28 | `IdempotencyApiTest` 3/3，每项检查都以行数断言：同一份工作簿应用两次留下两个 `APPLIED` 批次且 id 不同，`person` 与 `person_skill` 各仍只有一行、人员行读回与之前完全相同、档位仍为 `Expert`；两个线程在同一人日期相交的两个岗位上确认后只留一条指派、一条 `CONFIRMATION` 占用、一条 `confirmed`、一条 `voided` 与一条 `person taken` 通告，失败方回 409 `person taken`；释放回 `open` 与 `remaining 2`，同一 id 再释放回 404 且指派与占用都是零、岗位仍 `open`、提案仍 `confirmed`；`mvnw verify` 187 条绿 |
| 2026-11-11 | T26 | 以开发依赖装上 Playwright 1.65 与 `exceljs`；`web/playwright.config.ts` 自己拉起两个服务——后端用 `mvnw spring-boot:run` 并配 `cwd: ../backend` 与 `--spring.datasource.url=jdbc:h2:mem:e2e`（schema 与种子在每次运行时重建，什么都不会活过一次运行），前端是 5173 上的 Vite 开发服务器、`/api` 代理指向后端；一个 setup 项目分别以 coordinator、lead.one、lead.two 登录并为每个账号写一份 `storageState`（任务写的是两个账号；第二个 lead 存在，是因为必须有测试证明项目不归他），e2e 项目复用它们——cookie 不区分端口，8080 的会话在 5173 同样有效；`e2e/support/workbook.ts` 用 `exceljs` 写出两张表，列名取 `WorkbookParser` 所读、词取 `Bands` 所认，`fixtures.ts` 只经公开路由造数（`POST /api/imports` 201、`POST /api/projects` 201、`POST /api/projects/{id}/posts` 201、`POST /api/posts/{id}/recommendations` 201、`POST /api/recommendations/{id}/confirm` 200），所以 fixture 造不出服务器会拒绝的东西，`smoke.spec.ts` 从 `GET /api/session` 读回每个账号自己的角色并以 lead.one 打开 `/` 看到壳页的链接；`npx playwright test` 5/5（3 setup + 2 spec，21.9s），`npm test` 仍 39/39，且在 `tests` 与 `e2e` 进入 `tsc --noEmit` 后 `npm run build` 仍为绿；尚无旅程与场景 spec（归 T27） |
| 2026-11-11 | T17 | `npm test` 39/39（`candidates` 5、`post` 7、`confirmation` 6、`catalog` 4、`imports` 6、`people` 5、`projects` 6）且 `npm run build` 为绿：`/projects` 列出服务器按调用者范围给出的项目，每个名字打开自己的页面；`/projects/{id}` 读 `GET /api/projects/{id}` 连同岗位与缺口行，把缺口加成一句（`2 posts · 3 of 5 seats chosen · 2 seats still needed`，含单数与 `filled`），并逐条列出岗位各自的 `gapLabel`（已取消的写破折号）；结束只给项目自己的 lead 且只在项目处于 open 时，且问两次——按钮先给出后果 `Ending Atlas cancels its 2 posts and releases 3 confirmed people. It cannot be undone.`，再由 `End it` 发 `POST /api/projects/{id}/end`，页面重载为 `closed · every post is cancelled and every allocation of this project is gone` 且每个岗位都是 `cancelled`；`GET /api/projects/{id}` 让协调员读任意项目、让自己 lead 读到自己的、让别项目 lead 得 403，未知 id 404（`ProjectApiTest` 11/11）；`mvnw verify` 189 条绿；尚未做浏览器渲染核对 |
| 2026-11-11 | T27 | `npx playwright test` 15/15（3 setup + 2 旅程 + 8 场景 + 2 冒烟，38.8s）：`scenarios.spec.ts` 里的八条是 ATDD 设计点名、只有浏览器能证的检查——结束后页面读作 `closed · every post is cancelled and every allocation of this project is gone`、每个岗位都是 `cancelled`，协调员打开被释放者的档案读到 `No allocation is recorded for this person.`；档案页不渲染任何 `input, textarea, select, button`，技能段里没有一位数字；占用清单把过去的区间标为 `· ended` 与当前区间并列；确认在 lead.one 与协调员两侧都读作 `E2E Person <tag> (E2E-<tag>) · confirmed`，缺口两侧都是 `1 of 2 chosen · 1 still needed`；被确认者在后续岗位的 `Suitable people` 中消失、在 `Excluded people` 中以 `G6` 与 `intersects the post` 出现；`/notices` 带着落选岗位的标题与 `person taken`。`journeys.spec.ts` 跑 J1（在 `/imports` 导入 1 人 → 经 API 造项目与岗位 → 该人读作合适 → 经 API 推荐 → lead 在 `/posts/{id}` 确认到 `1 of 1 chosen · filled` → `/projects/{id}` 上同一个缺口）与 J2（lead 在自己的页面上两次确认后结束第一个项目 → 协调员在后续搜索里又看到这位被释放的人）。`openspec/changes/add-staffing-mvp/traceability.md` 把每个场景都映射到测试：90 行 = 82 API + 8 E2E，按能力 11/15/15/17/14/18，105 个不同的 API id 与 8 个 E2E 标题已用脚本对源码逐个核验。旅程暴露了一个产品缺口，如实记录而不拿 API 断言掩盖：没有任何页面发布推荐，J1 因此用 `POST /api/posts/{id}/recommendations` 造这一步 |
| 2026-11-11 | T29 | `NonGoalApiTest` 9/9，每个 Non-Goal 行一条测试（`atdd-design.md` §6）。1：`com.example.staffing` 下没有任何 `@EnableScheduling` 类，也没有任何 `@Scheduled` 方法（直接以 `SimpleMetadataReaderFactory` 读 class 文件，不加载类），且 `PATCH`/`POST`/`DELETE /api/people/E100` 均回 405、`job_level` 仍为 `Senior`。2：手工改档位回 405、`person_skill.band` 仍为 `Novice`。3： `POST /api/allocations` 带 `percentage: 50` 回 400 `unreadable body` 且占用计数仍为 0。4：有合适人选时岗位仍为 `open` / `confirmed 0` / `remaining 1`，指派与占用都是零。5：类路径上没有 `JavaMailSender` 与 `SimpMessagingTemplate`，`POST /api/notices/1/send` 回 404。6：`hourlyRate` 回 400，且岗位视图里没有任何匹配 rate/cost/budget/utilisation 的键。7：`PATCH /api/recommendations/{id}` 回 405 且该行仍为 `proposed`，一次 `POST .../confirm` 直接变 `confirmed`——不存在中间态。8：`/api/people?tenant=acme` 与 `/api/people` 回同一份 body。9：`travelDestination` 回 400，而视图读作 `travel: none` 且不带 destination 键。第 10 行是审查：`.github/workflows/ci.yml` 只有五项检查、没有部署作业。披露：为了让「以未知字段拒绝」真的成立而不是静默丢弃，`spring.jackson.deserialization.fail-on-unknown-properties=true` 同时加进主 `application.properties` **与**测试用的那一份（测试文件在测试类路径上遮住了主文件），并由 `ApiExceptionHandler` 把 `HttpMessageNotReadableException` 映射为 400 `unreadable body`；`mvnw verify` 280 条，行 1489/1489、分支 537/537 |
| 2026-11-11 | T30 | `.github/workflows/ci.yml` 在 `pull_request` 与推送 `main` 时触发，带 `permissions: contents: read` 与五个作业：`lint`（`npm ci` + `npm run lint`）、`type-check`（`npx tsc --noEmit` + `./mvnw -B -DskipTests compile`）、`unit`（`npm test` + `./mvnw -B test`）、`acceptance`（`./mvnw -B verify` 加 Playwright chromium 运行，并保留 JaCoCo 与 Playwright 报告 30 天）与 `scan`（`npm audit --audit-level=high` + pull request 上的 `actions/dependency-review-action@v4`）；没有部署作业、也不引用任何 secret。仓库原先没有 linter，所以引入 ESLint 9 与 `typescript-eslint`（`web/eslint.config.js`，`npm run lint` 0 问题），而不是摆一个恒过的检查；`web/playwright.config.ts` 改为在 Windows 用 `mvnw.cmd`、其他平台用 `./mvnw`，好让验收作业的 web server 在 Linux 上起得来。JaCoCo 的 `check` 在 `verify` 阶段以一条 `PACKAGE` 规则覆盖 `com.example.staffing.domain*` 与 `com.example.staffing.api*`，LINE 与 BRANCH 都要求 `COVEREDRATIO 1.0`——用「临时把 minimum 调到 `1.000001` 让构建失败、再恢复 `1.0` 打印 "All coverage checks have been met."」的方式当场证明门禁真的生效（必须用点号包名：斜杠路径匹配不到任何包，门禁会空过）。`docs/feedback/verification-commands.md` 不再是占位：静态、运行时、系统三层命令，外加把五项必需检查映射到各自 CI 作业的表。`npm test` 39/39、`npm run build` 为绿、`npm audit --audit-level=high` 退出码 0（9 个 moderate）、`mvnw verify` 280 条；换成按平台的 `MAVEN` 之后在 Windows 上重跑仍为 15/15（40.1s），`npm run lint` 0 问题 |
