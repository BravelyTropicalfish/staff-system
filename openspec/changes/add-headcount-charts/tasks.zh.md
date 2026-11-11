# 任务 —— add-headcount-charts

> 本文是 `tasks.md` 的中文译本，供评审阅读。两者如有冲突，以英文原版为准。
> 任务 ID 与场景标题保留英文原文 —— 它们是证据列与追溯表的键。

- **变更 ID：** `add-headcount-charts`
- **输入：** `design.md`（包结构、路由、组件树）与 `atdd-design.md`（8 API / 4 E2E，另有延后的单元项），覆盖增量规格（6 需求 / 12 场景，G1 在 rev 2 通过）
- **顺序：** `atdd-design.md` §2 —— 夹具 → API 检查 → 区块 → E2E → NFR → 完整验证

## 约定

- 一个任务是一次可评审的改动：先写失败的检查，再写代码，最后转绿。
- 每个任务都是 `- [ ]` 条目；`[x]` 表示证据今天已经绿。
- 斜体的场景是该任务的**主层**检查；尾随 `(payload)` 表示这是主层为 E2E 的场景的 API 伴随断言，于是拆分保持 8 API + 4 E2E，且没有场景被算两次。
- 任务不得添加范围：没有场景的检查是规格问题，不是测试问题（`atdd-design.md` §5）。
- 任务不设计单元测试套件；单元测试随代码书写。

## 阶段 1 —— 报表路由

- [x] **T01** —— `GET /api/headcount` 返回启用池：`total`、`byRole`、`byGrade`；只统计 `active = true` 的人员，停用人员不改变任何数字
  - 层次：`API` · 阻塞于：——
  - 证据：`HeadcountApiTest` —— *Both charts describe one pool*、*An inactive person is in neither chart*；同一答案中总数等于两轴之和

- [x] **T02** —— 轴 = 所有启用目录角色 ∪ 所有仍持有在职人员的角色，外加四个带，两者都按模型顺序；0 值分类保留柱子，停用且无人的角色离开轴
  - 层次：`API` · 阻塞于：T01
  - 证据：`HeadcountApiTest` —— *A category with nobody keeps its bar*、*Role bars follow catalog order*、*Grade bars follow band order*、*A deactivated role keeps its active people counted*

- [x] **T03** —— 每根柱带人数与占总数的比例，一位小数、四舍五入；残差保留不动，人数永不为凑整而调整
  - 层次：`API` · 阻塞于：T01
  - 证据：`HeadcountApiTest` —— *A bar carries its count and its share*（`40` / `35.4`）、*A share rounds to one decimal*（`33.3`、`66.7`、`12.5`）；`HeadcountShareTest` 钉住半值分支与 `100.0` 情形

- [x] **T04** —— 项目经理得到 403 且不含任何数字，走与人员池读取同一个 `Auth.coordinator` 门禁
  - 层次：`API` · 阻塞于：T01
  - 证据：`HeadcountApiTest` —— *A lead sees no report*（payload）：403、共享的 `{code,message}` 响应体、行数不变

- [x] **T05** —— 空池返回 `total: 0`，两个分组都在且为 `count: 0` / `share: 0.0`，不发生除法
  - 层次：`API` · 阻塞于：T01
  - 证据：`HeadcountApiTest` —— *An empty pool says so*（payload）：形状不变，所有计数为零

## 阶段 2 —— 区块

- [x] **T06** —— `HeadcountSection` + `ChartCard` 渲染在人员池两栏之上，且只对协调员；页面在请求报表**之前**先读 `GET /api/session`；柱高成比例，并以文字带出人数与占比
  - 层次：`E2E` · 阻塞于：T01–T05
  - 证据：`headcount.spec.ts` —— *Both charts sit on the pool screen*、*A lead sees no report*（无区块，且网络记录里没有 `/api/headcount` 请求）、*The table and the charts differ on purpose*；`npm test` 新增 `tests/headcount.test.ts` 覆盖读法（`shareText`、`chartTicks`、`barHeight`、`isEmpty`、`sectionTotal`）

- [x] **T07** —— 空池用那句陈述替换两个绘图区，而不是画 0 值柱
  - 层次：`E2E` · 阻塞于：T06
  - 证据：`headcount.spec.ts` —— *An empty pool says so*：两个图表区都读作空态文案且带 `Total 0`，页面上不存在任何柱元素

## 阶段 3 —— NFR 与反向检查

- [x] **T08** —— 新鲜度：报表每次读取即时派生，因此两次读取之间应用的导入会改变人数、占比与轴；旅程 J1 通过页面驱动它
  - 层次：`API` + `E2E` · 阻塞于：T01–T07
  - 证据：`HeadcountApiTest` 围绕一次导入读两次并看到变化；`journeys.spec.ts` 的 J1 导入、打开 `/people`，在未变的表格行旁读到已变的图表；停用人员留在表格、离开图表

- [x] **T09** —— 非目标反向检查：没有筛选参数能改变答案，`/api/headcount` 上没有写动词路由，答案不含任何个人字段，不存在导出或定时推送，也没有新增图表库
  - 层次：`API` + 配置评审 · 阻塞于：T01–T05
  - 证据：`?project=`、`?role=` 返回同一份响应体；`POST` / `PATCH` / `DELETE` 返回 405 且不改状态；答案的键集合被逐项断言（不含 `employeeNo`、`name`、`email`、`skill`、`allocation`）；不存在 `@Scheduled` 方法与返回文件的路线；`web/package.json` 未变（依赖评审）

## 阶段 4 —— 完整验证

- [x] **T10** —— 本地验证：三层按序全绿、覆盖率达 100%，且新的前端测试文件已加入显式 `npm test` 清单而没有挤掉既有条目
  - 层次：`build` · 阻塞于：T01–T09
  - 证据：`cd backend && ./mvnw verify` 全绿，JaCoCo 对 `domain*` / `api*` 行与分支 100%；`cd web && npm test` 全绿（含新文件）；`npx playwright test` 全绿；`npm run build` 与 `npm run lint` 干净

## 追溯

| 场景 | 主任务 | 层次 |
|---|---|---|
| S1 Both charts describe one pool | T01 | API |
| S2 An inactive person is in neither chart | T01 | API |
| S3 A bar carries its count and its share | T03 | API |
| S4 A share rounds to one decimal | T03 | API |
| S5 A category with nobody keeps its bar | T02 | API |
| S6 Role bars follow catalog order | T02 | API |
| S7 Grade bars follow band order | T02 | API |
| S8 Both charts sit on the pool screen | T06 | E2E |
| S9 A lead sees no report | T04 (payload) + T06 | E2E 主层 |
| S10 An empty pool says so | T05 (payload) + T07 | E2E 主层 |
| S11 A deactivated role keeps its active people counted | T02 | API |
| S12 The table and the charts differ on purpose | T06 | E2E |

合计：12 个场景 = 8 API + 4 E2E，每个只计一次。带 payload 伴随的两个 E2E 场景（S9、S10）仍以 E2E 为主层，拆分不会漂移。

## 实施结果（2026-11-11）

十个任务均为 `[x]`。以下为实测，不是声明：

| 层次 | 命令 | 结果 |
|---|---|---|
| 后端单元 + API | `cd backend && ./mvnw verify` | exit 0 —— **316 个测试**，`All coverage checks have been met.`（`domain*` / `api*` 行与分支 100%） |
| 前端读法 | `cd web && npm test` | **60 个测试**，0 失败（新的 `tests/headcount.test.ts` 已在清单内） |
| E2E | `cd web && npx playwright test` | **24 个全过** —— 4 个人数图表检查 + 原有 20 个 |
| 静态 | `npm run build`、`npm run lint` | 构建通过（72 modules），lint 干净 |

`HeadcountApiTest` 15 个用例覆盖 S1–S11、新鲜度与三项反向检查；`HeadcountShareTest` 4 个用例钉住舍入分支。接口不落任何表：schema 未动。
