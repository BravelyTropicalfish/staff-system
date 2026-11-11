# 设计 —— add-headcount-charts

> 本文是 `design.md` 的中文译本，供评审阅读。两者如有冲突，以英文原版为准。

- **变更 ID：** `add-headcount-charts`
- **输入：** 增量规格（1 个能力、6 需求 / 12 场景、G1 在 rev 2 通过）、`atdd-design.md`（8 API / 4 E2E，另有延后的单元项）、`proposal.md`（关键规则、非目标、D1–D4）、`prototype/`（已同意的画面）
- **本文不涉及：** 产品代码、任务拆解（`tasks.md`）、门禁决定

## 1. 技术栈

不变。ADR-001 已固定前端 React 19 + TypeScript + Vite + Tailwind，后端 Spring Boot 3.4 / Java 21 / H2；本变更**不新增任何依赖**，尤其不引入图表库。柱就是带行内高度的标记，原型已经证明两张静态柱状图这样够用。

被否掉的方案：图表库（为两块矩形和一个标签引依赖）；canvas/SVG 渲染器（E2E 里比 DOM 更难断言）；在浏览器里算占比（两处舍入、两个答案 —— 见 §6）。

## 2. 分层

`architecture-baseline` 规则 1–3 成立：UI → domain → data，单向。

```text
web/src/components/HeadcountSection.tsx   区块：两张卡片、空态、协调员门禁
web/src/components/ChartCard.tsx          单张图：标题、总数、轴、柱、标签
web/src/lib/headcount.ts                  载荷类型 + 纯读法（占比文案、刻度、柱高）
api/HeadcountController.java              一个 GET；协调员门禁；别无其他
domain/HeadcountService.java              聚合：轴、人数、占比
domain/HeadcountView.java                 答案：total + byRole + byGrade（record）
data/HeadcountRepository.java             两条只读查询；无 insert / update / delete
```

控制器除了角色不做任何决定；服务拥有轴、人数与舍入；仓储只拥有两条查询。这样规则可以脱离 HTTP 被测，也与其它每一片早已使用的 `Service` + `*View` + 薄控制器形状一致。

## 3. 数据

**无 schema 变更、无迁移、无新表。** 两条查询：

1. **角色轴** —— 所有 `active = true` 的 `role` 行，加上至少仍有一名在职人员持有的角色，按 `role.id` 排序：

   ```sql
   select r.id, r.name, count(p.employee_no)
     from role r
     left join person p on p.role_id = r.id and p.active = true
    where r.active = true or p.employee_no is not null
    group by r.id, r.name
    order by r.id
   ```

2. **等级轴** —— 四个带，所以分组由数据推导、带序来自有序刻度而不是来自行：

   ```sql
   select p.job_level, count(*)
     from person p
    where p.active = true
    group by p.job_level
   ```

   服务遍历 `Bands.JOB_LEVELS` 逐带取人数，于是无人之带是 0 而不是缺失的键。`Bands` 是既有的有序刻度；报表不新增词汇。

**不存储任何东西。** 没有报表表、没有物化视图、没有缓存 —— 答案按请求派生，这正是新鲜度 NFR 按构造成立的原因。

## 4. API

一个路由、一个快照、仅协调员。

```http
GET /api/headcount
```

```json
{
  "total": 113,
  "byRole":  [ { "roleId": 1, "role": "PM", "count": 40, "share": 35.4 }, "…" ],
  "byGrade": [ { "grade": "Junior", "count": 38, "share": 33.6 }, "…" ]
}
```

- **门禁：** `Auth.coordinator(authentication)` —— 与 `/api/people`、`/api/imports` 同一个调用。项目经理得到 403，响应体是共享的 `{code,message}`，不含任何数字。
- **写动词不是路由：** `/api/headcount` 上的 `POST`、`PATCH`、`DELETE` 因无处理器而返回 405，这是 `/api/people` 已有的形状。
- **空池：** `total: 0`，两个分组都在，`count: 0`、`share: 0.0`。形状永不变，客户端只有一种载荷要解析，空态是渲染层的决定（需求 6）。
- **无筛选参数：** `?project=`、`?role=` 之类根本不被读取；该路由没有任何查询参数。这就是「无筛选」非目标的反向检查。

## 5. 前端

`PeopleScreen` 在人员表之上增加一个区块；页面上其它东西不动。

```text
PeopleScreen
├─ 页头                               （不变）
├─ HeadcountSection                   （新增，仅协调员）
│  ├─ ChartCard  "Headcount by Role"   → 轴刻度、N 根柱、带人数与占比的标签
│  └─ ChartCard  "Headcount by Grade"
├─ .grid-2 → 人员表                   （删除）
└─ 人员表卡片                        （不变；点一行开档案弹窗）
```

- **操作顺序（S9 依赖它）。** 页面先读 `GET /api/session`。只有当角色为 `COORDINATOR` 时才挂载 `HeadcountSection` 并请求 `/api/headcount`。因此项目经理既看不到区块，**也不会**在网络上留下报表请求。先取报表再按 403 隐藏，像素上满足、句子上仍然失败。
- **读法是纯函数**，放在 `lib/headcount.ts` —— `shareText(share)` → `"35.4%"`、`chartTicks(top)` → 轴标签、`barHeight(count, top)` → 像素、`isEmpty(view)` → 是否画空块、`sectionTotal(view)` → 总数行。组件只渲染它们；单元测试不借助浏览器即可覆盖，与 `lib/people.ts` 同风格。
- **客户端绝不重算。** 它收到 `count` 与 `share` 并原样打印。用人数重算占比会引入第二条舍入规则（设计 §6）。
- **0 值柱。** 人数为 0 时渲染 3px 灰色刻度 + 变淡的标签，读作 `0` 与 `0.0%`。高度为 0 的元素不可见，而分类的存在恰恰是 S5 要断言的。
- **样式**照原型：两张卡片构成响应式栅格，绘图区带基线与浅网格，数值在柱上方，名称与占比在下方。原型的黄色「评审状态」条与「图上元素与规格条款」卡片**不构建** —— 它们是评审装置（`atdd-design.md` §6）。

## 6. 决定与取舍

| 决定 | 理由 | 被否掉 |
|---|---|---|
| `share` 为一位小数的 `double`，在服务端算一次 | 它是带既定舍入规则的展示值；`BigDecimal` 会多出一个客户端还得解析的类型 | 在浏览器算（两处舍入会不一致） |
| 轴由 `role` 左连接 `person` 得出，而不是只看人员行 | 启用且无人的角色必须仍有柱，停用且无人的角色必须没有 —— 一次连接表达两者 | 先分组人员行再在服务里补目录（两个顺序来源） |
| 空池仍返回两个计数为 0 的分组 | 载荷形状恒定，客户端没有第二个分支要解析 | 返回 `null` 分组（客户端要防的第二种形状） |
| 无缓存、无快照 | 池子小、查询一次分组就够，缓存是报表唯一可能过期的途径 | 内存缓存（凭空引入规格从未要求的失效规则） |
| 原生 `<div>` 柱，无图表库 | 每张图只有几条静态柱；库会带来依赖和 E2E 读不成文字的渲染面 | Recharts / Chart.js |
| 区块自己读 session | 页面的门禁必须在取数之前决定；`CatalogScreen`、`PostScreen` 已有同样做法 | 靠报表的 403 做门禁（请求已经发出去了） |

## 7. 测试命令（三层）

| 层次 | 命令 | 运行位置 |
|---|---|---|
| 后端单元 + API 验收 | `cd backend && ./mvnw verify` | 本地停止检查、CI `unit` + `acceptance` 作业 |
| 前端读法单元 | `cd web && npm test`（新的 `tests/headcount.test.ts` 加入显式清单） | 本地停止检查、CI `unit` 作业 |
| E2E | `cd web && npx playwright test` | PR 前本地、CI `acceptance` 作业 |

JaCoCo 对 `com.example.staffing.domain*` 与 `com.example.staffing.api*` 保持行与分支 100%；0 值柱、空池与半值舍入是评审一定会看的三个分支，所以它们随测试交付而不是排除。

## 8. 带入实现期的风险

- 新类若不测试，100% 覆盖率门禁会让构建失败（机械约束，不是行为风险）。
- 项目经理的 `/people` 视图只差区块的**缺席**；该屏其它地方不动，因此既有的 E2E 断言理论上无需修改 —— 但 smoke 测试确实断言项目经理看不到人员池链接，新区块不得新增链接。
- `web/tests/board.test.ts` 仍未进入 `npm test`（既有缺陷，已记入 impact brief）。本变更把自己的文件加入该清单，且不得顺手丢掉其它条目。
