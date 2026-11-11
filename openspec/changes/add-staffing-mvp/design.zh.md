# 设计：add-staffing-mvp

> 本文是 `design.md` 的中文译本，供评审阅读。两者如有冲突，以英文原版 `design.md` 为准。

- **变更 ID：** `add-staffing-mvp`
- **输入：** delta spec（40 条需求 / 90 个场景，rev 11 已通过 G1）、`atdd-design.md`（82 API / 8 E2E /
  10 项单元测试留到 apply）、`proposal.md`（Non-Goals、假设、D1–D8）
- **本文不含：** 生产代码、任务拆解（见 `tasks.md`）、门禁决定

## 1. 技术栈决定（D1，2026-11-11 已定）

产品负责人选定：**前端 React、后端 Java、数据库 H2。** 具体落地：

| 部件 | 选择 | 理由 |
|---|---|---|
| 后端 | Java 21（本机已装 Liberica 21.0.11）、Spring Boot 3.4、Spring Web + Validation + Data JPA + Security | Java 是选定的语言；Boot 是 H2 的常规运行时，事务型服务不必自己造轮子 |
| 构建 | **Maven Wrapper 固定 3.9.9** | 本机全局 Maven 是 **3.0.4（2012 年）**，无法构建 Boot 3——这是阻塞项，不是偏好（见风险 1） |
| 数据库 | H2 2.x —— `dev` 用文件库、测试用内存库，schema 由 `schema.sql` 提供并配 `ddl-auto=validate` | 负责人选定；本机没有 Docker，Postgres 不可用 |
| 前端 | React 19 + TypeScript + Vite + Tailwind + shadcn/ui + React Router，一个 `fetch` 封装，不引状态库 | React 是负责人的选择；其余是渲染既定页面所需的最小集合 |
| API 层 | `/api` 下的 Spring MVC 控制器 + JSON DTO | 让 82 个 API 场景共用一份 HTTP 契约（`atdd-design.md` §1） |
| 测试 | API 用 JUnit 5 + `@SpringBootTest(RANDOM_PORT)` + `TestRestTemplate`，覆盖率用 JaCoCo，前端辅助函数用 Vitest，8 个 E2E 场景用 **Playwright（本机有 Node 24）** | 后端走 Java、浏览器走 Node，两个运行时本机都有 |
| Excel | 服务端用 Apache POI，E2E fixture 脚本引入一个开发依赖 | 工作簿由服务端读取；fixture 生成器是数据构造，不是用例生成器 |

原话「前端用react+React」按 **React + TypeScript** 理解，不引入第二个 React 生态框架。
**待确认的假设：** 如果本意是别的工具链，只影响 `web/`，不动任何 spec、API 或测试意图。

ADR 见 `docs/knowledge/architecture/adr-001-runtime-stack.md`。

**已否决的方案：** Next.js 单体（一个运行时一个构建，但后端变 Node，与负责人选择不符）；
React + Fastify、FastAPI + React（同上，负责人选了 Java 后端）；Docker 里的 Postgres（本机无
Docker）；用服务端模板替代单页应用（会丢掉仓库要求的 shadcn/ui 组件基础）。

## 2. 分层

按 `architecture-baseline` 规则 1–3：UI → 领域 → 数据，单向，不成环。

```text
web/            React 单页应用：页面、组件、fetch 客户端。不含任何业务规则。
api/            Spring 控制器、DTO、校验注解、角色守卫。保持薄。
domain/         服务与规则：门槛引擎、岗位生命周期、确认、导入、项目结束。
data/           JPA 实体、仓储、schema.sql。除约束外不放规则。
```

物理布局：`backend/` 是 Maven 工程（`src/main/java/com/example/staffing/{api,domain,data}`、
`src/main/resources/schema.sql`），`web/` 是 Vite 应用；两者都是本次变更新增。

包根 `com.example.staffing`；每个能力一个服务入口（`CatalogService`、`PersonService`、
`ImportService`、`ProjectService`、`AllocationService`、`PostService`、`MatchingService`、
`ConfirmationService`、`ReleaseService`、`NoticeService`），验收检查不碰内部实现。
`PersonService` 只承担人员池的读侧：人员数据的唯一写入者是导入。

## 3. 数据模型（H2）

| 表 | 关键列 | 说明 |
|---|---|---|
| `role` | `id`、`name`、`active` | 五条种子数据；被引用后禁止改名与删除 |
| `skill` | `id`、`name`、`role_domain_id`、`active` | 技能域只服务兜底 |
| `person` | `employee_no` 主键、`name`、`role_id`、`job_level`、`travel`、`active` | 一人一角色；除导入外无编辑入口 |
| `person_skill` | `(employee_no, skill_id)` 主键、`band` | 报表未提及的技能保留原行 |
| `project` | `id`、`name`、`lead_user_id`、`start_date`、`end_date`、`status` | `OPEN` → `CLOSED`，终态；区间即 spec 的项目区间（起止倒置在入口就拒） |
| `post` | `id`、`project_id`、`title`、`role_id`、`min_job_level`、`headcount`、`start_date`、`end_date`、`travel_required`、`status` | 状态由已确认人数推导：每次读取都重算、每次保存都回写，因此陈旧缓存不会露出 |
| `post_skill` | `(post_id, skill_id)` 主键、`kind` `REQUIRED|BONUS`、`min_band`、`target_band` | |
| `assignment` | `id`、`post_id`、`person_no`、`created_at` | 释放会**删除**指派及其占用（spec：释放即重开名额），因此行永远代表生效指派，不再有 state 列 |
| `allocation` | `id`、`person_no`、`project_id`、`start_date`、`end_date`、`source` `MANUAL|CONFIRMATION`、`assignment_id` | 检查约束：`source='CONFIRMATION'` ⇒ `assignment_id` 非空 |
| `recommendation` | `id`、`post_id`、`person_no`、`state`、`void_reason`、`snapshot_json`、`created_at` | 快照保留门槛与达标技能 |
| `notice` | `id`、`coordinator_id`、`person_no`、`post_id`、`reason`、`created_at`、`dismissed_at` | 每丢一条推荐写一行 |
| `import_batch` | `id`、`filename`、`status` `APPLIED|REJECTED`、`uploaded_by`、`uploaded_at`、`person_rows`、`skill_rows`、`error_count` | 文件不可读时**不产生任何批次行**；规范要求的历史包含上传人与行数，所以它们是列而不是推导值 |
| `import_error` | `id`、`batch_id`、`sheet`、`row_no`、`key_value`、`message` | 错误清单完整，不截断；`key_value` 是该行涉及的员工号 |
| `app_user` | `id`、`username`、`password_hash`、`role` `LEAD|COORDINATOR`、`display_name` | 种子演示账号，假密码加盐哈希；lead 的范围从 `project.lead_user_id` 读出，因此不存重复的范围列 |

日期用 `date` 列（不带时间）。`headcount` 是正整数。规范里说的「冻结」一律由领域服务执行，不靠 UI 隐藏。

## 4. API 契约（每个能力一组路由）

| 能力 | 路由 |
|---|---|
| `people-pool` | `GET/POST /api/roles`、`PATCH /api/roles/{id}`、`DELETE /api/roles/{id}`；`GET/POST /api/skills`、`PATCH /api/skills/{id}`、`DELETE /api/skills/{id}`；两个 `DELETE` 路由只为回答 409「请改用停用」（目录项从不删除）；`GET /api/people`、`GET /api/people/{employeeNo}`（只读、仅协调员；人员数据由导入写入）；`GET /api/bands`（三套刻度——职级、熟练度、出差——岗位表单因此不必复制一份词表） |
| `people-import` | `POST /api/imports`（multipart）、`GET /api/imports`、`GET /api/imports/{id}`（批次 + 错误清单）；历史列表只回计数、`errors` 为空，错误行从详情路由读取——这也正是「拒绝」与「退回」之别：形状不合格回 422 且不建批次，行有错的文件回 201 并留下一个 `REJECTED` 批次 |
| `allocation` | `GET/POST /api/projects`、`GET /api/projects/{id}`（协调员读任意项目、lead 只读自己的——项目页面读的正是它）、`POST /api/projects/{id}/end`；`POST /api/allocations`、`PATCH/DELETE /api/allocations/{id}` |
| `post-requirement` | `GET/POST /api/projects/{id}/posts`、`GET /api/posts/{id}`（岗位自己的 lead 或协调员）、`PATCH /api/posts/{id}`、`POST /api/posts/{id}/cancel`、`GET /api/projects/{id}/gap`（每个已确认座位带自己的指派 id，因为释放命名的是指派而不是人） |
| `candidate-matching` | `GET /api/posts/{id}/candidates`（合适列表、兜底、排除清单） |
| `staffing-confirmation` | `GET /api/posts/{id}/recommendations`（协调员带每行快照，岗位自己的 lead 不带）、`POST /api/posts/{id}/recommendations`、`DELETE /api/recommendations/{id}`、`POST /api/recommendations/{id}/confirm`、`POST /api/recommendations/{id}/reject`、`POST /api/assignments/{id}/release`、`GET /api/notices`、`POST /api/notices/{id}/dismiss` |
| 认证 | `POST /api/session`、`DELETE /api/session`、`GET /api/session` |

错误体：`{"code": "...", "message": "...", "details": [...]}`，`code` 使用规范里出现过的值
（`person taken`、`post cancelled`、`release first`、按门槛拒绝）。角色或范围越权返回 403，状态冲突
409，校验失败 422；任何堆栈信息都不得离开服务端（`security-baseline` 6）。

请求体带有任何请求记录未声明的字段时，返回 400 `unreadable body`，而不是反序列化时把该字段丢掉：
静默忽略 `percentage`、费率或出差目的地，等于服务端悄悄支持了一条 Non-Goal（T29）。该策略由
`spring.jackson.deserialization.fail-on-unknown-properties=true` 打开，并由 `ApiExceptionHandler`
把 `HttpMessageNotReadableException` 映射为上文的错误体。

## 5. 门槛引擎与合适列表

顺序固定：**G1** 在职 → **G2** 角色 → **G3** 职级 → **G4** 必要技能 → **G5** 差旅 → **G6** 日期相交。
报出的原因永远是**第一个失败的门槛**；一次评估返回逐门结果，界面才能展示「通过了哪些门」。

`G6` 比较 `[allocation.start, allocation.end]` 与 `[post.start, post.end]`，两端闭合：
`start1 <= end2 && start2 <= end1`。

角色与技能一律按行 id 比较，不按名称：被人员或岗位引用的目录行既不能改名也不能删除，因此 id
才是身份，名称只进入拒绝理由。

合适列表包含岗位角色的**每一个**通过六道门的人，按姓名排序、同名按工号，DTO 里根本没有分数字段。
bonus 技能不足只作为信息携带，永不参与排序。

排除清单同样只覆盖这一个人群：每人带第一个失败门槛，并按门槛计数，六道门全列出以便零值可见。
这就是 G2 从不作为原因出现的原因——其他角色的人根本不是同年角色候选人——也是停用人员计入 G1
而不是被静默丢弃的原因。

兜底仅当该列表为空时开启；要求 **X1**（职级至少高出岗位最低要求一档）与 **X2**（持有岗位角色技能域内
任一技能且达到 `Practitioner` 或以上），并作为独立数组返回、带证据。X1 顶替 G3、X2 顶替 G2 与 G4，
因此岗位的必要技能行从不约束这一层；G1、G5、G6 仍然生效，且先于 X1、X2 判定，停用或已被占用的人
保留硬门槛理由。每行带角色、高出档数与达标的域内技能及其档位，因为推荐快照将原样复制这一行，
且不会给人员挂上第二个角色。技能域为空的角色一个都不放行，不退化为「任意技能」；岗位已在最高档时
必然没有跨角色人选，这时 X1 的理由写明「没有更高的档」。`X1`/`X2` 计数并入 `exclusionCounts`
（兜底未开启时为零），`fallbackReason` 说明该栏为何存在，使空数组也有解释而不是沉默。

## 6. 确认、作废与并发

`POST /api/posts/{id}/recommendations` 只属于协调者，只写一行 `proposed`，带 `snapshot_json` 与
`proposed_by`。先看名额：已取消的岗位回 409 `post cancelled`，已确认人数达到人数的岗位回 409
`post filled`；再看人是否在当前搜索答案里——合适的行，或在该分区打开期间属于跨角色兜底的行。两边都不在的人，
以其被挡下的门槛（`G1`…`G6`、`X1`、`X2`）和排除清单已有的那句理由拒绝，所以两次读取不会互相矛盾；
跨角色分区已关闭时提另一位角色的人回 409 `not a candidate`，同一人在同一岗位再提一次回 409
`already proposed`。快照照抄放行它的那行——跑过并通过的门槛，以及岗位技能行与该人当时的档位——兜底行则改成
携带角色、高出几档与达标领域技能，不再带同年层的技能行。响应从数据库读回这一行而不是回显请求，因此协调者看到
的就是库里存的；人的角色始终不变。

`GET /api/posts/{id}/recommendations` 是确认界面读取的那条路由：未决（`proposed`）在前，历史在后，
各自按时间从新到旧。协调员读到的每一行都带推荐理由快照；岗位自己的 lead 读到同样的行但 `snapshot` 为 null，
因为规范只把快照给协调员；其他项目的 lead 回 403，与岗位读取已有的同一条守卫。

每次确认一个事务，锁按同一顺序获取——先岗位行（`SELECT … FOR UPDATE`）、再人员行，都在重检之前，
所以同一人的两次确认排队而不是都读到「尚未占用」，同一岗位的两次确认也不会超员：

1. 按岗位日期重跑 G1 与 G6；
2. 写入 `assignment` + `allocation(source=CONFIRMATION, 日期=岗位日期)`；
3. 剩余名额减一，减到零置 `FILLED`；
4. 将该人所有**岗位日期与新占用相交**的未决推荐置 `voided`，原因 `person taken`，各写一条 `notice`；
5. 日期不相交的推荐保持 `PROPOSED`。

并发双确认的失败方得到 409 `person taken`：赢家的写回把失败方的推荐作废，而被以此理由作废的推荐在再次确认时
也用这句话回答，而不是笼统地说状态已变。测试用两个线程驱动，断言最终只有一条指派与一条占用。
释放与项目结束用同样的事务形状：结束时取消项目下所有岗位、以 `post cancelled` 作废未决推荐、删除该项目
的全部占用并释放指派。

**隔离级别是刻意选的：** 行锁显式加，事务保留默认隔离级别而不是 `SERIALIZABLE`——因为 H2 把
`SERIALIZABLE` 映射成快照隔离，会把失败的确认报成 `40001`（死锁），而不是规范要求的 `person taken`
拒绝。岗位行锁守名额，人员行锁守日历，每次状态变更仍由 `WHERE` 里的 `state = 'proposed'` 守住。

**已接受的 H2 局限：** H2 提供行锁但不是分布式数据库，因此该检查证明的是「行锁下的不变式」，而不是真实
多副本并发下的一致性。这一点记在 `atdd-design.md` §7，也是幂等检查停在 API 层的原因。

## 7. 导入

Apache POI 读取工作簿；校验先跑完两个 sheet 的每一行，才写任何数据。不可读（缺 sheet、无数据行）→ 422
且**不建批次**；可读但有坏行 → 建 `REJECTED` 批次并保存全部错误，人员池保持原状。应用时在一个事务里写
人员、技能与档位，重新激活停用人员，保留未提及技能的档位，且绝不触碰 allocation、assignment、
recommendation 或 project 行。真实 HR 报表的列名映射（D4）集中在 `ColumnMap` 一个类里，后续改名只动一处；工作簿这项决策本身记录在 `docs/knowledge/architecture/adr-002-import-workbook.md`。

## 8. 前端

路由：`/login`、`/imports`、`/people`、`/people/:employeeNo`、`/projects`、`/projects/:projectId`、
`/projects/:projectId/posts/new`、`/posts/:postId`、`/posts/:id/candidates`、`/catalogs`、`/notices`。
登录就是 `/login`：需求把登录定义为「带角色的轻量种子账号」，所以这一屏提交 `POST /api/session` 后回到壳页——它在 G2 之后补上，因为在那之前唯一的入口是手工调用那条路由。外壳来自原型而不是新设计：232px 侧边栏（分组链接按角色过滤）、56px 顶栏（面包屑 + 账号）、无 max-width 的满宽视图，各屏直接用原型自己的类名（`.card`、`.kpi`、`.gate-grid`、`.grid-2`、`.grid-3`）。写本节时并没有要求二者一致，偏差就是这样发生的；T32 把它收口，现在每屏都在外壳内。协调员可见目录、导入与全量人员池；
lead 只看自己的项目、岗位、被推荐的候选与缺口视图。隐藏控件只是便利——**服务端守卫才是权威**
（`security-baseline` 1）。档位一律显示名称，任何地方都不出现数字熟练度。

## 9. 测试与门禁

| 层 | 工具 | 范围 |
|---|---|---|
| 单元 | JUnit 5 | 门槛顺序、档位比较、日期相交、作废集合、状态推导、列映射 |
| API | `@SpringBootTest(RANDOM_PORT)` + `TestRestTemplate`，H2 内存库 | 82 个场景；每个测试类一套全新 schema |
| E2E | Playwright（TypeScript） | 8 个场景 + 旅程 J1/J2 |
| 前端单元 | Vitest | 仅格式化与客户端辅助函数 |

T21 无法安装 Vitest：本机访问不到包仓库，因此页面的纯读数改由 Node 自带测试器（`node --test`，
TypeScript 靠类型擦除直接跑），不新增任何依赖。这些函数是纯 TS，日后换回 Vitest 只是换命令而非改写；
Vitest 仍是这一层记录在案的工具。

T26 以开发依赖加入 Playwright 1.65 与 `exceljs`。`web/playwright.config.ts` 自己拉起两个服务：后端用
`mvnw spring-boot:run` 并配 `cwd: ../backend` 与 `--spring.datasource.url=jdbc:h2:mem:e2e`，因此每次运行
都从种子 schema 起步；前端是 5173 上的 Vite 开发服务器，其 `/api` 代理指向后端。一个 setup 项目分别以
coordinator、lead.one、lead.two 登录——任务写的是两个账号，测试需要第二个 lead 才能证明项目不归他——
并为每个账号存一份 `storageState`；cookie 不区分端口，所以在 8080 取得的会话对 5173 的应用同样有效。
工作簿 fixture 由 `exceljs` 生成，列名取 `WorkbookParser` 所读、词表取 `Bands` 所认，因此导入 fixture
一旦与解析器漂移就会当场失败。

覆盖率：`domain/` 与 `api/` 的 JaCoCo 行与分支覆盖率 100%；确实无法测试的行需在 PR 写明理由并取得评审
同意（`testing-strategy` 6）。门禁是 `verify` 阶段的一个 `check` execution，以一条 `PACKAGE` 规则覆盖
`com.example.staffing.domain*` 与 `com.example.staffing.api*`，LINE 与 BRANCH 均要求 `COVEREDRATIO 1.0`，
所以覆盖率一旦回退是构建失败，而不是评审时才被注意。两处细节是当场验证而非假定的：规则必须用点号包名
（斜杠路径匹配不到任何包，空规则会恒过）；比例真的在执行，靠把 minimum 临时调到 `1.000001` 让 `verify`
失败来看见。当前一次运行在 280 条测试上读到行 1489/1489、分支 537/537。

CI 跑 `pipeline-gates` 要求的五项检查；`.github/workflows/ci.yml` 由本次变更创建，
`docs/feedback/verification-commands.md` 在同一变更内填好（规则 7）。工作流不含部署作业。有两条环境事实是第一次运行后才确认、而不是假定出来的：`web/package-lock.json` 必须解析到 `registry.npmjs.org`，因为 runner 访问不到开发机所用的内网 Artifactory（npm 会把公网主机换回配置的镜像源，所以本地安装照旧可用）；`dependency-review-action` 在仓库非公开时必须有 GitHub Advanced Security——本仓库是公开的，所以这项审查在每个 pull request 上真的会跑，私有库那次失败正是这道门禁不能用 `continue-on-error` 抹平的理由。仓库原先没有
linter，因此 lint 检查引入的是 ESLint 9（配 `typescript-eslint`），而不是一条恒过的命令；
`web/playwright.config.ts` 在 Windows 用 `mvnw.cmd`、其余平台用 `./mvnw`，好让验收作业能在 Linux 上拉起后端。

`atdd-design.md` §6 的十项反向检查住在 `NonGoalApiTest`（第 1–9 行）与对工作流的人工审查（第 10 行）。
第 4 行既按 API 读数断言——有合格候选人的岗位仍为 `open` 且指派数为零——也由 J1 覆盖，因为页面证明与
库内状态是两件不同的事。

## 10. 风险与未决项

1. **工具链：** 全局 Maven 3.0.4 无法构建 Boot 3。Wrapper 固定 3.9.9；若离线无法生成 wrapper，则安装
   Maven 3.9.x 属于前置条件，而不是代码改动。
2. **D4**（真实 HR 列名）仍开放；由 `ColumnMap` 隔离。
3. **D5/D6**（批次回滚、技能删除）仍不做：批次是只读历史，没有删除通道。
4. **D7**（岗位日期是否须落在项目日期内）仍不约束；不校验包含关系。
5. **D8**（兜底下限名额或额外审批）维持 MVP 默认；确认时的重检是唯一把关。
6. **POI** 是新依赖：加入时按 `security-baseline` 7 做漏洞评审。
