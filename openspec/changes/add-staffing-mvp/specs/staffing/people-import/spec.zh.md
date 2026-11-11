# staffing/people-import —— 增量规格（中文译本）

> 本文是 `spec.md` 的中文译本，供评审阅读。两者如有冲突，以英文原版 `spec.md` 为准。
> 结构关键字（`ADDED Requirements` 段、Traces 行、GIVEN / WHEN / THEN / AND 标记）以及角色、技能、
> 档位、门槛与场景编号保留原样，便于与英文版逐项对照。

范围：工作簿上传 —— 人员主数据的唯一入口。一次上传即一个工作单元。

## ADDED Requirements

### Requirement: 一个工作簿，两张表

系统每次上传应只接受一个 `.xlsx` 工作簿：`people` 表携带 `employee_no`、`name`、`email`、`role`、`job_level`、`travel_willingness`、`active`，`skills` 表携带 `employee_no`、`skill`、`proficiency`。两张表都必须存在。`people` 表可以为空，以便只刷新技能；若两张表中都没有任何数据行，该文件应被拒绝。

**Traces:** proposal 步骤 2；REQ-001 §5.1；S32。

#### Scenario: 仅刷新技能的上传被接受

- **GIVEN** 一个工作簿，`people` 表为空，`skills` 各行指向已存在的员工与目录技能
- **WHEN** 协调员上传它
- **THEN** 该批次为 `applied`，所列档位被更新，且不创建任何人员

#### Scenario: 只有一张表的工作簿被拒绝

- **GIVEN** 一个只有 `people` 表的工作簿
- **WHEN** 协调员上传它
- **THEN** 上传被拒绝，且不记录任何批次

#### Scenario: 空工作簿被拒绝

- **GIVEN** 一个两张表都只有表头的工作簿
- **WHEN** 协调员上传它
- **THEN** 上传被拒绝，理由指明文件为空，且不记录任何批次

### Requirement: 导入是原子的

系统应在写入任何内容之前校验两张表的每一行。若有至少一行未通过校验，系统不得写入任何内容 —— 不创建人员、不改变档位、不翻转标记 —— 应将批次记为 `rejected`，并逐条列出每个出错行，含表名、行号、键值与理由。所有行均通过校验的批次应记为 `applied`。

**Traces:** proposal 步骤 2 与关键规则 "An import is all-or-nothing"；REQ-001 §5.1；S12、S14、S29、S31。

#### Scenario: 一行出错使整份文件失败

- **GIVEN** 一个含五行 `people` 数据的工作簿，其中一行 `job_level` 为 `Mid`，另有一行 `skills` 指向技能 `Kotlin`
- **WHEN** 协调员上传它
- **THEN** 批次为 `rejected`
- **AND** 五名人员保持与上传前完全一致的数据
- **AND** 错误清单中每个出错行各有一条记录，含表名、行号、键与理由

#### Scenario: 修正后的文件是新批次

- **GIVEN** 上述被拒绝的批次
- **WHEN** 协调员修正两行后重新上传该文件
- **THEN** 新批次为 `applied`，每一行都被写入
- **AND** 先前的批次保留自己的状态与错误清单

#### Scenario: 被拒绝的批次仍然可读

- **GIVEN** 存在一个被拒绝的批次
- **WHEN** 协调员打开批次历史
- **THEN** 该批次展示文件名、上传人、时间戳、行数、状态及其错误清单，全部只读

### Requirement: 行级校验规则

系统应在写入任何内容之前对每一行应用这些规则。`people` 行中的 `employee_no`、`name`、`role`、`job_level`、`travel_willingness`、`active` 应非空，`email` 可以为空。`employee_no` 在文件内应唯一。`role` 应恰好是一条启用中的目录角色 —— 一个单元格里写两个角色、或写成以分隔符连接的角色清单，属于行错误，且绝不拆成两个角色；`skill` 应为目录技能；`job_level` 应为四档之一；`proficiency` 应为五档之一；`travel_willingness` 应为 `none` / `limited` / `full` 之一；`active` 应为 `active` / `inactive` 之一。同一工号与同一技能出现第二条 `skills` 行属于错误；`skills` 行的工号既不在系统内、也不在该文件 `people` 表中，同样属于错误。单元格应做去空白处理，ASCII 取值应按大小写不敏感匹配，存储值采用目录中的写法。

**Traces:** proposal 步骤 2；REQ-001 §5.1；S12、S14、S17、S29、S30、S31。

#### Scenario: 小写被归一化，而不是被拒绝

- **GIVEN** 一条 `people` 行，`job_level` = `senior`、`active` = `Active`；一条 `skills` 行，`proficiency` = `practitioner`
- **WHEN** 协调员上传该文件
- **THEN** 批次为 `applied`，档案中存储 `Senior`、`active` 与 `Practitioner`

#### Scenario: 目录之外的角色使整份文件失败

- **GIVEN** 一条 `people` 行，`role` = `Product`
- **WHEN** 协调员上传该文件
- **THEN** 批次为 `rejected`，理由中列出可接受的角色

#### Scenario: 多角色单元格是行错误，绝不变成两个角色

- **GIVEN** 一条 `people` 行，其 `role` 单元格内容为 `DEV, QA`；另一条内容为 `DEV / QA`
- **WHEN** 协调员上传该文件
- **THEN** 批次为 `rejected`，每一行在 `role` 列上各有一条行错误
- **AND** 不会创建或存储任何持有两个角色的人员，因为该单元格从不被拆分

#### Scenario: 重复的技能行使整份文件失败

- **GIVEN** 同一工号与技能 测试 的两条 `skills` 行
- **WHEN** 协调员上传该文件
- **THEN** 批次为 `rejected`，两个行号都出现在错误清单中

#### Scenario: 孤儿的技能行使整份文件失败

- **GIVEN** 一条 `skills` 行指向工号 `E999`，该工号在系统与该文件的 `people` 表中都不存在
- **WHEN** 协调员上传该文件
- **THEN** 批次为 `rejected`，该行的理由为 `person not found`

#### Scenario: 重复的工号使整份文件失败

- **GIVEN** 两条 `people` 行携带相同工号
- **WHEN** 协调员上传该文件
- **THEN** 批次为 `rejected`，理由指明该重复工号

### Requirement: 成功的批次写入人员与技能数据

当所有行都通过校验时，系统应为每个未知工号创建人员，覆盖文件中携带字段的已存值，并为每条 `skills` 行插入或覆盖熟练度档位。系统不得删除人员、技能档位或任何其他记录，并应把该批次以 `applied` 及其行数存储。

**Traces:** proposal 步骤 2；REQ-001 §5.1；S13、S15、S16。

#### Scenario: 同一文件中的新人与老人

- **GIVEN** 系统已有 `E100`，文件中 `E100` 带有新的职级，另有一个系统不认识的 `E200`
- **WHEN** 协调员上传该文件
- **THEN** `E200` 按文件中的角色、职级、差旅意愿与启用标记被创建，`E100` 带上新的职级
- **AND** 批次为 `applied`

#### Scenario: 报表把停用人员重新启用

- **GIVEN** `E100` 已存为停用，而文件中 `E100` 的 `active` = `active`
- **WHEN** 协调员上传该文件
- **THEN** 档案把 `E100` 显示为启用

### Requirement: 导入从不触碰人员配置状态

导入不得创建、修改或删除占用、岗位、推荐或指派。下一次候选人搜索应直接使用导入后的数据。

**Traces:** proposal 步骤 2；REQ-001 §5.1 副作用、§9；S11、S16。

#### Scenario: 占用在导入后存活

- **GIVEN** `E100` 在 `Atlas` 上有一条占用，文件上报了新的差旅意愿
- **WHEN** 协调员上传该文件
- **THEN** 该占用不变，新的差旅意愿展示在档案上
