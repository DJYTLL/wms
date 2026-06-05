# SQL Latency Monitor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增独立的“SQL耗时查询”页面，支持将请求级 SQL 耗时落库，并按请求聚合查看历史 SQL 执行明细。

**Architecture:** 复用现有 `RequestAuditContext`、`RequestTimingFilter` 与 `SlowQueryInterceptor` 链路，在请求生命周期内收集 SQL 明细，请求结束后统一写入“请求聚合表 + SQL 明细表”。前端新增独立系统页面，通过请求分页接口加载列表，再按 `requestId` 拉取 SQL 明细抽屉数据。

**Tech Stack:** Spring Boot 3、MyBatis-Plus、Flyway、Vue 3、Element Plus、现有系统权限体系与路由映射测试脚本。

---

## File Structure

- Create: `D:\project\wms-backend\src\main\resources\db\migration\V133__sql_latency_monitor_tables.sql`
  - 建 `app_sql_request_trace` 与 `app_sql_trace_entry` 两张表及索引。
- Create: `D:\project\wms-backend\src\main\resources\db\migration\V134__seed_sql_latency_monitor_menu_and_permission.sql`
  - 新增页面权限、菜单、角色初始化关联。
- Create: `D:\project\wms-backend\src\main\java\com\example\wms\entity\SqlRequestTrace.java`
  - 请求聚合实体。
- Create: `D:\project\wms-backend\src\main\java\com\example\wms\entity\SqlTraceEntry.java`
  - SQL 明细实体。
- Create: `D:\project\wms-backend\src\main\java\com\example\wms\dto\monitor\SqlRequestTraceRow.java`
  - 请求列表行 DTO。
- Create: `D:\project\wms-backend\src\main\java\com\example\wms\dto\monitor\SqlTraceEntryRow.java`
  - SQL 明细行 DTO。
- Create: `D:\project\wms-backend\src\main\java\com\example\wms\mapper\monitor\SqlRequestTraceMapper.java`
  - 请求分页与按 `requestId` 查询。
- Create: `D:\project\wms-backend\src\main\java\com\example\wms\mapper\monitor\SqlTraceEntryMapper.java`
  - 明细批量插入与明细查询。
- Create: `D:\project\wms-backend\src\main\java\com\example\wms\monitor\RequestSqlTraceContext.java`
  - 单次请求中的 SQL 采集上下文对象。
- Create: `D:\project\wms-backend\src\main\java\com\example\wms\monitor\RequestSqlTraceRecorder.java`
  - 请求结束后统一落库。
- Create: `D:\project\wms-backend\src\main\java\com\example\wms\service\monitor\SqlLatencyMonitorService.java`
  - 请求列表与明细查询服务接口。
- Create: `D:\project\wms-backend\src\main\java\com\example\wms\service\monitor\impl\SqlLatencyMonitorServiceImpl.java`
  - 查询服务实现。
- Create: `D:\project\wms-backend\src\main\java\com\example\wms\controller\SqlLatencyMonitorController.java`
  - 页面查询接口控制器。
- Modify: `D:\project\wms-backend\src\main\java\com\example\wms\audit\RequestAuditContext.java`
  - 扩展 SQL 采集上下文引用与请求结束元数据。
- Modify: `D:\project\wms-backend\src\main\java\com\example\wms\monitor\RequestTimingFilter.java`
  - 请求结束时触发 SQL 采集落库。
- Modify: `D:\project\wms-backend\src\main\java\com\example\wms\monitor\SlowQueryInterceptor.java`
  - 记录 SQL 到请求上下文。
- Modify: `D:\project\wms-backend\src\main\java\com\example\wms\config\PermissionSeedProvider.java`
  - 注册 `sql-latency-monitor:view`。
- Modify: `D:\project\wms-backend\src\main\java\com\example\wms\config\MenuSeedProvider.java`
  - 注册 `sql-latency-monitor` 菜单。
- Modify: `D:\project\wms-backend\src\main\resources\application.properties`
  - 增加 SQL 落库相关开关与参数日志默认值。
- Create: `D:\project\wms-backend\src\test\java\com\example\wms\SqlLatencyMonitorServiceTests.java`
  - 查询服务与明细查询测试。
- Create: `D:\project\wms-backend\src\test\java\com\example\wms\RequestSqlTraceRecorderTests.java`
  - 请求结束写入聚合与明细测试。
- Modify: `D:\project\wms-backend\src\test\java\com\example\wms\SlowQueryInterceptorTest.java`
  - 增补“写入请求上下文”的行为测试。
- Create: `D:\project\auto-parts-wms-vue\src\views\system\SqlLatencyMonitorView.vue`
  - 新页面组件。
- Modify: `D:\project\auto-parts-wms-vue\src\router\index.ts`
  - 注册新页面路由与 route meta。
- Modify: `D:\project\auto-parts-wms-vue\src\locales\zh.ts`
  - 增加中文文案。
- Modify: `D:\project\auto-parts-wms-vue\src\locales\en.ts`
  - 增加英文文案。
- Modify: `D:\project\auto-parts-wms-vue\src\utils\i18n.ts`
  - 新页面 `titleKey` 映射。
- Modify: `D:\project\auto-parts-wms-vue\src\views\system\RoleManagement.vue`
  - 补页面/路径/权限前缀映射。
- Modify: `D:\project\auto-parts-wms-vue\src\views\system\PermissionManagement.vue`
  - 补页面映射。
- Modify: `D:\project\auto-parts-wms-vue\scripts\system-permission-mapping.test.mjs`
  - 锁定新页面不会落入未映射页面。
- Create: `D:\project\auto-parts-wms-vue\src\views\system\__tests__\sqlLatencyMonitorView.test.mjs`
  - 页面基础交互测试。

### Task 1: 新增数据库迁移与权限菜单种子

**Files:**
- Create: `D:\project\wms-backend\src\main\resources\db\migration\V133__sql_latency_monitor_tables.sql`
- Create: `D:\project\wms-backend\src\main\resources\db\migration\V134__seed_sql_latency_monitor_menu_and_permission.sql`

- [ ] **Step 1: 写迁移测试前的扫描确认记录**

在实现备注中明确：

```text
当前 migration 最新版本号：V132
拟新增：V133__sql_latency_monitor_tables.sql
拟新增：V134__seed_sql_latency_monitor_menu_and_permission.sql
版本冲突：无
```

- [ ] **Step 2: 编写建表迁移**

在 `V133__sql_latency_monitor_tables.sql` 中创建两张表与索引：

```sql
CREATE TABLE IF NOT EXISTS app_sql_request_trace (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    request_id VARCHAR(100) NOT NULL,
    request_path VARCHAR(500) NOT NULL,
    request_method VARCHAR(20) NOT NULL,
    response_status INTEGER,
    request_cost_ms BIGINT NOT NULL DEFAULT 0,
    sql_total_cost_ms BIGINT NOT NULL DEFAULT 0,
    sql_count INTEGER NOT NULL DEFAULT 0,
    username VARCHAR(100),
    user_id BIGINT,
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS app_sql_trace_entry (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    request_trace_id BIGINT NOT NULL,
    request_id VARCHAR(100) NOT NULL,
    sequence_no INTEGER NOT NULL,
    mapper_id VARCHAR(500) NOT NULL,
    sql_type VARCHAR(20) NOT NULL,
    cost_ms BIGINT NOT NULL,
    sql_text TEXT NOT NULL,
    params_summary TEXT,
    executed_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

- [ ] **Step 3: 为查询模式补索引**

继续在 `V133__sql_latency_monitor_tables.sql` 中追加：

```sql
CREATE INDEX IF NOT EXISTS idx_sql_request_trace_tenant_started
    ON app_sql_request_trace (tenant_id, started_at DESC, id DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uq_sql_request_trace_tenant_request
    ON app_sql_request_trace (tenant_id, request_id);

CREATE INDEX IF NOT EXISTS idx_sql_request_trace_tenant_path_started
    ON app_sql_request_trace (tenant_id, request_path, started_at DESC);

CREATE INDEX IF NOT EXISTS idx_sql_trace_entry_tenant_trace_seq
    ON app_sql_trace_entry (tenant_id, request_trace_id, sequence_no);

CREATE INDEX IF NOT EXISTS idx_sql_trace_entry_tenant_request_seq
    ON app_sql_trace_entry (tenant_id, request_id, sequence_no);
```

- [ ] **Step 4: 编写菜单权限种子迁移**

在 `V134__seed_sql_latency_monitor_menu_and_permission.sql` 中新增：

```sql
INSERT INTO app_permission (code, name, description, created_at, updated_at)
SELECT 'sql-latency-monitor:view', '查看SQL耗时查询', '查看SQL耗时历史记录'
WHERE NOT EXISTS (
  SELECT 1 FROM app_permission WHERE code = 'sql-latency-monitor:view'
);
```

并参照 `V112__seed_api_latency_monitor_menu_and_permission.sql` 新增菜单 `sql-latency-monitor`，路径 `/sql-latency-monitor`，挂到 system 分组。

- [ ] **Step 5: 运行迁移相关测试或最小校验**

Run:

```bash
mvn -Dtest=ErpUnitSeedMigrationTests test
```

Expected: PASS；若该测试不覆盖新迁移，至少确认编译与 Flyway 资源加载无报错。

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/db/migration/V133__sql_latency_monitor_tables.sql src/main/resources/db/migration/V134__seed_sql_latency_monitor_menu_and_permission.sql
git commit -m "feat: add sql latency monitor migrations"
```

### Task 2: 扩展请求上下文并写 SQL 采集失败测试

**Files:**
- Modify: `D:\project\wms-backend\src\main\java\com\example\wms\audit\RequestAuditContext.java`
- Modify: `D:\project\wms-backend\src\test\java\com\example\wms\SlowQueryInterceptorTest.java`
- Create: `D:\project\wms-backend\src\test\java\com\example\wms\RequestSqlTraceRecorderTests.java`

- [ ] **Step 1: 先写请求上下文记录 SQL 明细的失败测试**

在 `SlowQueryInterceptorTest.java` 中增加测试，断言开启 SQL timing 后会把 SQL 明细写入 `RequestAuditContext` 绑定的请求上下文：

```java
@Test
void enabledTimingAppendsSqlEntryIntoRequestContext() throws Throwable {
    SlowQueryInterceptor interceptor = new SlowQueryInterceptor();
    ReflectionTestUtils.setField(interceptor, "sqlTimingEnabled", true);
    ReflectionTestUtils.setField(interceptor, "sqlTimingLogParams", false);
    ReflectionTestUtils.setField(interceptor, "slowQueryMs", 500L);

    RequestAuditContext context = new RequestAuditContext();
    RequestAuditContext.set(context);
    try {
        interceptor.intercept(queryInvocation("com.example.Mapper.select", Map.of("id", 86), 0L));
        assertThat(context.getSqlTraceEntries()).hasSize(1);
    } finally {
        RequestAuditContext.clear();
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```bash
mvn -Dtest=SlowQueryInterceptorTest test
```

Expected: FAIL，报 `getSqlTraceEntries` 或上下文相关断言失败。

- [ ] **Step 3: 为请求上下文增加 SQL 采集容器**

在 `RequestAuditContext.java` 中增加：

```java
private RequestSqlTraceContext sqlTraceContext;

public RequestSqlTraceContext getSqlTraceContext() {
    return sqlTraceContext;
}

public void setSqlTraceContext(RequestSqlTraceContext sqlTraceContext) {
    this.sqlTraceContext = sqlTraceContext;
}
```

并新建 `RequestSqlTraceContext`，字段至少包含：

```java
private final List<RequestSqlTraceEntry> entries = new ArrayList<>();
private long totalCostMs;
private int sequenceNo;
```

- [ ] **Step 4: 运行测试确认转绿**

Run:

```bash
mvn -Dtest=SlowQueryInterceptorTest test
```

Expected: PASS

- [ ] **Step 5: 为请求结束写库写失败测试**

在 `RequestSqlTraceRecorderTests.java` 中编写用 Mockito 或假 Mapper 的测试，断言：

- 请求结束时写 1 条 `SqlRequestTrace`
- 同一请求的 2 条 SQL 会批量写入 `SqlTraceEntry`
- `sql_total_cost_ms` 为明细耗时和
- `sql_count` 为明细数量

- [ ] **Step 6: 运行测试确认失败**

Run:

```bash
mvn -Dtest=RequestSqlTraceRecorderTests test
```

Expected: FAIL，提示 recorder/mapper/实体未实现。

- [ ] **Step 7: Commit**

```bash
git add src/test/java/com/example/wms/SlowQueryInterceptorTest.java src/test/java/com/example/wms/RequestSqlTraceRecorderTests.java src/main/java/com/example/wms/audit/RequestAuditContext.java
git commit -m "test: add request sql trace context coverage"
```

### Task 3: 实现请求 SQL 落库链路

**Files:**
- Create: `D:\project\wms-backend\src\main\java\com\example\wms\entity\SqlRequestTrace.java`
- Create: `D:\project\wms-backend\src\main\java\com\example\wms\entity\SqlTraceEntry.java`
- Create: `D:\project\wms-backend\src\main\java\com\example\wms\mapper\monitor\SqlRequestTraceMapper.java`
- Create: `D:\project\wms-backend\src\main\java\com\example\wms\mapper\monitor\SqlTraceEntryMapper.java`
- Create: `D:\project\wms-backend\src\main\java\com\example\wms\monitor\RequestSqlTraceRecorder.java`
- Modify: `D:\project\wms-backend\src\main\java\com\example\wms\monitor\RequestTimingFilter.java`
- Modify: `D:\project\wms-backend\src\main\java\com\example\wms\monitor\SlowQueryInterceptor.java`
- Modify: `D:\project\wms-backend\src\main\resources\application.properties`

- [ ] **Step 1: 定义实体与明细数据对象**

为两张表分别创建实体，字段名与 migration 保持一致；为请求内采集明细创建简单值对象：

```java
public record RequestSqlTraceEntry(
    int sequenceNo,
    String mapperId,
    String sqlType,
    long costMs,
    String sqlText,
    String paramsSummary,
    Instant executedAt
) {}
```

- [ ] **Step 2: 实现 Mapper**

`SqlRequestTraceMapper` 提供基础插入与分页查询；
`SqlTraceEntryMapper` 提供批量插入与按 `requestId/requestTraceId` 查询。

批量插入明细可先用 XML-less 的 `<script>` 注解写法，保证最小改动。

- [ ] **Step 3: 实现 `RequestSqlTraceRecorder`**

核心逻辑：

```java
public void record(RequestAuditContext context, int responseStatus, long requestCostMs, Instant finishedAt) {
    if (context == null || context.getSqlTraceContext() == null || context.getSqlTraceContext().entries().isEmpty()) {
        return;
    }
    // 组装 SqlRequestTrace
    // insert request trace
    // 批量 insert entries
}
```

- [ ] **Step 4: 在 `SlowQueryInterceptor` 中把 SQL 追加到上下文**

在现有日志逻辑之后追加：

```java
RequestAuditContext context = RequestAuditContext.get();
if (sqlTimingEnabled && context != null) {
    RequestSqlTraceContext traceContext = context.ensureSqlTraceContext();
    traceContext.append(statement.getId(), sqlType, costMs, sql, params);
}
```

要求：

- 参数摘要在 `sqlTimingLogParams=false` 时写 `[disabled]`
- `sequenceNo` 按请求内自增

- [ ] **Step 5: 在 `RequestTimingFilter` 请求结束时触发落库**

在 `finally` 中写入：

```java
requestSqlTraceRecorder.record(
    RequestAuditContext.get(),
    response.getStatus(),
    costMs,
    Instant.now()
);
```

同时保证 recorder 不抛异常影响主请求，异常只打日志。

- [ ] **Step 6: 配置默认值**

在 `application.properties` 中明确：

```properties
wms.monitor.sql-timing-enabled=false
wms.monitor.sql-timing-log-params=false
wms.monitor.slow-query-ms=500
```

- [ ] **Step 7: 运行 recorder 与 interceptor 测试**

Run:

```bash
mvn test "-Dtest=SlowQueryInterceptorTest,RequestSqlTraceRecorderTests"
```

Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/example/wms/monitor src/main/java/com/example/wms/entity src/main/java/com/example/wms/mapper src/main/resources/application.properties
git commit -m "feat: persist sql trace records"
```

### Task 4: 实现 SQL 耗时查询接口

**Files:**
- Create: `D:\project\wms-backend\src\main\java\com\example\wms\dto\monitor\SqlRequestTraceRow.java`
- Create: `D:\project\wms-backend\src\main\java\com\example\wms\dto\monitor\SqlTraceEntryRow.java`
- Create: `D:\project\wms-backend\src\main\java\com\example\wms\service\monitor\SqlLatencyMonitorService.java`
- Create: `D:\project\wms-backend\src\main\java\com\example\wms\service\monitor\impl\SqlLatencyMonitorServiceImpl.java`
- Create: `D:\project\wms-backend\src\main\java\com\example\wms\controller\SqlLatencyMonitorController.java`
- Create: `D:\project\wms-backend\src\test\java\com\example\wms\SqlLatencyMonitorServiceTests.java`

- [ ] **Step 1: 先写请求分页查询失败测试**

在 `SqlLatencyMonitorServiceTests.java` 中写测试，断言支持按：

- `requestPath`
- `requestMethod`
- `status`
- `minRequestCostMs`
- `minSqlCostMs`
- `startAt/endAt`

做分页筛选，并返回 `sqlCount/sqlTotalCostMs/requestCostMs`。

- [ ] **Step 2: 运行测试确认失败**

Run:

```bash
mvn -Dtest=SqlLatencyMonitorServiceTests test
```

Expected: FAIL，提示 service/controller/mapper 查询方法未实现。

- [ ] **Step 3: 实现 DTO 与查询服务**

请求分页 DTO 至少包含：

```java
public record SqlRequestTraceRow(
    String requestId,
    String requestPath,
    String requestMethod,
    Integer responseStatus,
    Long requestCostMs,
    Long sqlTotalCostMs,
    Integer sqlCount,
    String username,
    Instant startedAt,
    Instant finishedAt
) {}
```

明细 DTO 至少包含：

```java
public record SqlTraceEntryRow(
    Integer sequenceNo,
    String mapperId,
    String sqlType,
    Long costMs,
    String sqlText,
    String paramsSummary,
    Instant executedAt
) {}
```

- [ ] **Step 4: 实现控制器**

新增控制器路径：

```java
@RestController
@RequestMapping("/api/system/sql-latency")
```

接口：

```java
@GetMapping("/requests/page")
@PreAuthorize("hasAuthority('PERM_sql-latency-monitor:view')")

@GetMapping("/requests/{requestId}/entries")
@PreAuthorize("hasAuthority('PERM_sql-latency-monitor:view')")
```

- [ ] **Step 5: 运行服务测试与相关编译测试**

Run:

```bash
mvn test "-Dtest=SqlLatencyMonitorServiceTests,SlowQueryInterceptorTest"
```

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/example/wms/controller src/main/java/com/example/wms/service src/main/java/com/example/wms/dto/monitor src/test/java/com/example/wms/SqlLatencyMonitorServiceTests.java
git commit -m "feat: add sql latency monitor apis"
```

### Task 5: 接入后端权限与菜单种子代码

**Files:**
- Modify: `D:\project\wms-backend\src\main\java\com\example\wms\config\PermissionSeedProvider.java`
- Modify: `D:\project\wms-backend\src\main\java\com\example\wms\config\MenuSeedProvider.java`

- [ ] **Step 1: 补失败测试或字符串锁定**

如果已有权限 seed 相关测试可复用则新增测试；若没有，至少在现有脚本/测试中增加常量断言需求，锁定：

```text
sql-latency-monitor:view
sql-latency-monitor
/sql-latency-monitor
```

- [ ] **Step 2: 在 `PermissionSeedProvider` 中新增权限**

追加：

```java
new PermissionSeed("sql-latency-monitor:view", "查看SQL耗时查询", "查看SQL耗时历史记录")
```

- [ ] **Step 3: 在 `MenuSeedProvider` 中新增菜单**

追加：

```java
new MenuSeed("sql-latency-monitor", "system", "SQL耗时查询", "sql-latency-monitor", "/sql-latency-monitor", null, "sql-latency-monitor:view", 38)
```

- [ ] **Step 4: 运行最小相关测试**

Run:

```bash
mvn -Dtest=AuthPermissionIntegrationTests test
```

Expected: PASS；若该套太重且本地不稳定，至少说明未运行原因并跑编译/已有权限脚本替代。

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/wms/config/PermissionSeedProvider.java src/main/java/com/example/wms/config/MenuSeedProvider.java
git commit -m "feat: seed sql latency monitor permission"
```

### Task 6: 先写前端权限映射与路由失败测试

**Files:**
- Modify: `D:\project\auto-parts-wms-vue\scripts\system-permission-mapping.test.mjs`
- Modify: `D:\project\auto-parts-wms-vue\src\router\index.ts`
- Modify: `D:\project\auto-parts-wms-vue\src\views\system\RoleManagement.vue`
- Modify: `D:\project\auto-parts-wms-vue\src\views\system\PermissionManagement.vue`

- [ ] **Step 1: 在权限映射脚本中先写失败断言**

参照 `api-latency-monitor`，增加：

```js
assertContains(source, "{ prefix: 'sql-latency-monitor:', pageKeys: ['sql-latency-monitor'] }");
assertContains(source, "'sql-latency-monitor': ['sql-latency-monitor']");
assertContains(source, "'/sql-latency-monitor': 'sql-latency-monitor'");
```

以及 `PermissionManagement.vue` 的页面映射断言。

- [ ] **Step 2: 运行脚本确认失败**

Run:

```bash
node auto-parts-wms-vue/scripts/system-permission-mapping.test.mjs
```

Expected: FAIL，提示缺少 `sql-latency-monitor` 映射。

- [ ] **Step 3: 注册路由**

在 `src/router/index.ts` 系统页面区域新增：

```ts
{
  path: 'sql-latency-monitor',
  name: 'sql-latency-monitor',
  component: () => import('../views/system/SqlLatencyMonitorView.vue'),
  meta: { title: 'SQL耗时查询', permission: 'sql-latency-monitor:view', titleKey: 'page.sqlLatencyMonitor' }
}
```

- [ ] **Step 4: 补 `RoleManagement.vue` 映射**

追加：

```ts
{ prefix: 'sql-latency-monitor:', pageKeys: ['sql-latency-monitor'] },
'sql-latency-monitor': ['sql-latency-monitor'],
'/sql-latency-monitor': 'sql-latency-monitor',
'SQL耗时查询': 'sql-latency-monitor',
'sql-latency-monitor': t('page.sqlLatencyMonitor'),
```

- [ ] **Step 5: 补 `PermissionManagement.vue` 映射**

追加：

```ts
'sql-latency-monitor': ['sql-latency-monitor'],
```

- [ ] **Step 6: 运行脚本确认转绿**

Run:

```bash
node auto-parts-wms-vue/scripts/system-permission-mapping.test.mjs
```

Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add auto-parts-wms-vue/scripts/system-permission-mapping.test.mjs auto-parts-wms-vue/src/router/index.ts auto-parts-wms-vue/src/views/system/RoleManagement.vue auto-parts-wms-vue/src/views/system/PermissionManagement.vue
git commit -m "test: lock sql latency monitor permission mappings"
```

### Task 7: 实现前端页面与交互

**Files:**
- Create: `D:\project\auto-parts-wms-vue\src\views\system\SqlLatencyMonitorView.vue`
- Modify: `D:\project\auto-parts-wms-vue\src\locales\zh.ts`
- Modify: `D:\project\auto-parts-wms-vue\src\locales\en.ts`
- Modify: `D:\project\auto-parts-wms-vue\src\utils\i18n.ts`
- Create: `D:\project\auto-parts-wms-vue\src\views\system\__tests__\sqlLatencyMonitorView.test.mjs`

- [ ] **Step 1: 先写页面失败测试**

在 `sqlLatencyMonitorView.test.mjs` 中锁定：

- 页面存在请求筛选区
- 搜索/重置/刷新按钮位于首行右侧操作区
- 点击“查看 SQL 明细”会触发明细接口

可以采用和现有视图测试一致的源代码断言或组件行为测试。

- [ ] **Step 2: 运行测试确认失败**

Run:

```bash
node auto-parts-wms-vue/src/views/system/__tests__/sqlLatencyMonitorView.test.mjs
```

Expected: FAIL，提示页面文件或关键布局不存在。

- [ ] **Step 3: 以 `ApiLatencyMonitorView.vue` 为模板实现新页面**

页面需包含：

- 搜索区：请求路径、请求方法、状态码、最小接口耗时、最小 SQL 总耗时、时间范围
- 第一行右侧操作区：查询、重置、刷新
- 请求列表
- SQL 明细抽屉

请求列表列：

```text
请求时间 / 请求路径 / 请求方法 / 状态码 / 接口总耗时 / SQL总耗时 / SQL条数 / 用户 / 操作
```

明细列：

```text
执行顺序 / mapperId / sqlType / costMs / sqlText / paramsSummary / 执行时间
```

- [ ] **Step 4: 补国际化键值**

在 `zh.ts`、`en.ts` 和 `utils/i18n.ts` 中新增：

```ts
page: {
  sqlLatencyMonitor: 'SQL耗时查询'
}
```

以及筛选项/字段文案。

- [ ] **Step 5: 运行页面测试与权限映射脚本**

Run:

```bash
node auto-parts-wms-vue/src/views/system/__tests__/sqlLatencyMonitorView.test.mjs
node auto-parts-wms-vue/scripts/system-permission-mapping.test.mjs
```

Expected: PASS

- [ ] **Step 6: 如有前端类型检查命令，运行类型检查**

Run:

```bash
npm run test -- sqlLatencyMonitorView
```

或项目现有可用的最小前端校验命令；若命令不存在，记录实际可跑命令。

- [ ] **Step 7: Commit**

```bash
git add auto-parts-wms-vue/src/views/system/SqlLatencyMonitorView.vue auto-parts-wms-vue/src/views/system/__tests__/sqlLatencyMonitorView.test.mjs auto-parts-wms-vue/src/locales/zh.ts auto-parts-wms-vue/src/locales/en.ts auto-parts-wms-vue/src/utils/i18n.ts
git commit -m "feat: add sql latency monitor page"
```

### Task 8: 最终联调与验证

**Files:**
- Modify: 仅修复联调阶段发现的问题

- [ ] **Step 1: 运行后端相关测试**

Run:

```bash
mvn test "-Dtest=SlowQueryInterceptorTest,RequestSqlTraceRecorderTests,SqlLatencyMonitorServiceTests,ErpSaleOrderServiceImplTest"
```

Expected: PASS

- [ ] **Step 2: 运行前端相关脚本/测试**

Run:

```bash
node auto-parts-wms-vue/scripts/system-permission-mapping.test.mjs
node auto-parts-wms-vue/src/views/system/__tests__/sqlLatencyMonitorView.test.mjs
```

Expected: PASS

- [ ] **Step 3: 手动核对页面接入说明**

交付说明必须覆盖：

```text
pageKey: sql-latency-monitor
menu code: sql-latency-monitor
route name/path: sql-latency-monitor / /sql-latency-monitor
permission code/prefix: sql-latency-monitor:view / sql-latency-monitor:
RoleManagement.vue 映射补充项
PermissionManagement.vue 映射补充项
ColumnPermissionManagement.vue 是否需要补列映射及原因
为什么不会再落入“未映射页面”
```

- [ ] **Step 4: 最终 commit**

```bash
git add .
git commit -m "feat: add sql latency monitor feature"
```

## Self-Review

- Spec coverage:
  - 独立页面、历史数据落库、按请求聚合再看 SQL 明细、完整权限接入、测试约束，均已覆盖到具体任务。
- Placeholder scan:
  - 没有保留 TBD/TODO。
- Type consistency:
  - 统一使用 `sql-latency-monitor` 作为 `pageKey/menu code/route name/permission prefix`。

