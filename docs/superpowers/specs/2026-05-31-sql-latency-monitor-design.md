# SQL 耗时查询页面设计

## 背景

当前系统已有“接口耗时查询”页面与请求级耗时统计能力，也已具备基于 MyBatis 的 SQL 耗时日志输出能力，但缺少可查询的历史 SQL 耗时数据与按请求链路排查 SQL 明细的页面能力。

本次目标是在不影响现有“接口耗时查询”页面的前提下，新增独立的“SQL耗时查询”页面，用于排查类似 `/erp/sale-orders/approved/86` 这类接口请求下的 SQL 执行明细，并支持历史查询。

## 目标

1. 新增独立页面“SQL耗时查询”，不复用“接口耗时查询”页面。
2. SQL 耗时记录落库，支持历史查询。
3. 页面查询模型采用“两层结构”：
   - 第一层：按请求聚合查看接口请求
   - 第二层：查看该请求下的 SQL 明细
4. 默认保存完整 SQL 文本。
5. 参数摘要默认关闭或脱敏，不作为首版必看信息。
6. 完整接入现有权限体系、菜单体系与页面映射体系，确保不会落入“未映射页面”。

## 非目标

1. 不改造现有“接口耗时查询”页面为 SQL 页面。
2. 不做实时流式日志页面，首版只做历史查询。
3. 不引入 `p6spy`、`datasource-proxy` 等新依赖，继续复用现有 `SlowQueryInterceptor` 与请求耗时链路。

## 方案比较

### 方案 A：复用 `app_audit_log`

- 优点：少建表。
- 缺点：请求聚合与 SQL 明细语义混杂，查询与页面结构不自然，后续扩展困难。

### 方案 B：新增请求表 + SQL 明细表

- 优点：与页面结构一致，查询语义清晰，最适合“先看请求再看 SQL 明细”的排查方式。
- 缺点：需要新增两张表与对应接口。

### 方案 C：只建 SQL 明细表

- 优点：实现更快。
- 缺点：请求聚合能力弱，列表查询与统计字段都要靠 SQL 聚合现算，页面体验较差。

### 结论

采用方案 B：新增“请求头表 + SQL 明细表”。

## 数据设计

### Flyway 迁移

已扫描目录 `D:\project\wms-backend\src\main\resources\db\migration`，当前最新版本号为 `V132`，不存在版本冲突。

拟新增迁移：

1. `V133__sql_latency_monitor_tables.sql`
2. `V134__seed_sql_latency_monitor_menu_and_permission.sql`

说明：

- `V133` 用于建表与索引。
- `V134` 用于新增菜单、权限与角色初始化映射。
- 只新增迁移，不改历史迁移。

### 表 1：SQL 请求聚合表

建议命名：`app_sql_request_trace`

核心字段：

- `id`
- `tenant_id`
- `request_id`
- `request_path`
- `request_method`
- `response_status`
- `request_cost_ms`
- `sql_total_cost_ms`
- `sql_count`
- `started_at`
- `finished_at`
- `username`
- `user_id`（如当前上下文已有则记录，否则允许为空）
- `created_at`

用途：

- 承载页面第一层请求列表。
- 支持按请求路径、方法、时间范围、状态码、总耗时筛选。

建议索引：

- `(tenant_id, started_at desc, id desc)`
- `(tenant_id, request_id)`
- `(tenant_id, request_path, started_at desc)`
- `(tenant_id, request_method, started_at desc)`

### 表 2：SQL 明细表

建议命名：`app_sql_trace_entry`

核心字段：

- `id`
- `tenant_id`
- `request_trace_id`
- `request_id`
- `sequence_no`
- `mapper_id`
- `sql_type`
- `cost_ms`
- `sql_text`
- `params_summary`
- `executed_at`
- `created_at`

用途：

- 承载页面第二层 SQL 明细。
- 支持按请求展开明细查看。

建议索引：

- `(tenant_id, request_trace_id, sequence_no)`
- `(tenant_id, request_id, sequence_no)`
- `(tenant_id, mapper_id, executed_at desc)`
- `(tenant_id, cost_ms desc, executed_at desc)`

### 数据保留策略

首版先不做自动清理任务，只保证结构可扩展。后续如数据量增长，再补归档或保留天数配置。

## 后端设计

## 请求链路采集

复用现有请求耗时过滤器与 SQL 拦截器，补一层请求上下文。

新增一个轻量上下文对象，用于在单次请求内收集：

- `requestId`
- 请求路径、方法、状态码
- 开始/结束时间
- 本次请求下的 SQL 明细列表
- SQL 总耗时
- SQL 次数

工作方式：

1. 请求进入时，初始化上下文并绑定到当前线程。
2. `SlowQueryInterceptor` 每执行一条 MyBatis SQL，就把明细追加到上下文。
3. 请求结束时，由过滤器统一落库：
   - 先写请求聚合表
   - 再批量写 SQL 明细表

## SQL 采集行为

保留现在的日志行为，同时追加数据库写入能力。

默认配置：

- `wms.monitor.sql-timing-enabled=false`
- `wms.monitor.sql-timing-log-params=false`
- `wms.monitor.slow-query-ms=500`

规则：

1. 开关关闭时：
   - 不做数据库写入
   - 保留当前慢 SQL 日志行为
2. 开关开启时：
   - 每条 SQL 写入请求上下文
   - 请求结束后统一落库
3. 参数摘要默认关闭：
   - `params_summary` 存储 `[disabled]`
4. SQL 文本默认保存完整归一化内容

## 查询接口

### 1. 请求分页列表

`GET /api/system/sql-latency/requests/page`

参数建议：

- `page`
- `size`
- `requestPath`
- `requestMethod`
- `status`
- `minRequestCostMs`
- `minSqlCostMs`
- `startAt`
- `endAt`

返回：

- 请求聚合分页列表
- 每行包含请求总耗时、SQL 总耗时、SQL 条数等摘要字段

### 2. 请求 SQL 明细

`GET /api/system/sql-latency/requests/{requestId}/entries`

返回：

- 指定请求下的 SQL 明细列表
- 按 `sequence_no` 升序

### 3. 配置读取接口（可选）

`GET /api/system/sql-latency/config`

若页面需要展示当前采集是否开启，可补此接口；若首版不需要可暂缓。

## 权限与菜单接入

本任务属于“页面接入权限体系任务”。

统一命名建议：

- `pageKey`: `sql-latency-monitor`
- 菜单 `code`: `sql-latency-monitor`
- 路由 `name`: `sql-latency-monitor`
- 路由 `path`: `/sql-latency-monitor`
- 权限前缀：`sql-latency-monitor:`
- 首版查看权限：`sql-latency-monitor:view`

需完成内容：

1. 新增前端页面组件。
2. 新增前端路由，并补 `meta.title`、`meta.permission`、`meta.titleKey`。
3. 新增后端权限 seed 与菜单 seed。
4. 补 `RoleManagement.vue` 映射。
5. 补 `PermissionManagement.vue` 映射。
6. 若无列权限需求，`ColumnPermissionManagement.vue` 可不新增列定义，但要确认不会误入未映射。
7. 补测试，锁定该页面不会落入“未映射页面”。

## 前端页面设计

### 页面定位

新增独立页面：`SQL耗时查询`

不修改现有 `ApiLatencyMonitorView.vue` 的职责边界。

### 首屏接口

页面打开到首屏展示，计划触发的接口：

1. `GET /api/system/sql-latency/requests/page`

用户点击某条请求的“查看 SQL 明细”后，再触发：

2. `GET /api/system/sql-latency/requests/{requestId}/entries`

### 布局

遵循现有 ERP / 系统列表布局习惯：

- 顶部搜索卡片
- 第一行右侧固定操作区
- 下方请求列表
- 明细采用抽屉或下半区展开，推荐抽屉

### 搜索区

首版筛选项建议：

- 请求路径
- 请求方法
- 状态码
- 最小接口耗时
- 最小 SQL 总耗时
- 时间范围

操作区按钮：

- 查询
- 重置
- 刷新

### 请求列表字段

- 请求时间
- 请求路径
- 请求方法
- 状态码
- 接口总耗时
- SQL 总耗时
- SQL 条数
- 用户
- 操作：查看 SQL 明细

### SQL 明细字段

- 执行顺序
- `mapperId`
- `sqlType`
- `costMs`
- `sqlText`
- `paramsSummary`
- 执行时间

## 测试策略

### 后端

1. 新增 SQL 请求/明细持久化单测或集成测试。
2. 补接口分页与明细查询测试。
3. 保留已完成的 `SlowQueryInterceptor` 单测。

### 前端

1. 补路由/权限映射测试：
   - 锁定 `sql-latency-monitor` 不会进入“未映射页面”
2. 补页面基础交互测试：
   - 首屏请求列表加载
   - 点击查看 SQL 明细后加载明细
3. 若搜索区布局有专门测试体系，补操作区固定在首行右侧的回归测试。

## 风险与处理

### 风险 1：请求上下文与异步线程不一致

首版默认只保证同步请求链路。若未来存在异步数据库调用，再单独扩展上下文传递方案。

### 风险 2：写库本身带来额外开销

通过总开关控制采集开启范围；默认关闭，仅在需要时打开。

### 风险 3：SQL 文本与参数内容较敏感

默认只保存完整 SQL，参数摘要默认关闭；后续如需更严格控制，可进一步脱敏。

## 实施顺序

1. 新增数据库迁移：建表、索引、菜单权限种子
2. 扩展请求上下文与 SQL 采集链路
3. 新增后端查询接口
4. 新增前端页面、路由、权限映射
5. 补测试并跑验证

