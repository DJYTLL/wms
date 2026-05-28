# WMS Backend 开发文档（可脱离代码理解）

本项目是多租户 + 权限 + 菜单的后台体系。目标是：不读代码也能正确新增页面/接口/权限/菜单，并保证登录、菜单、审计日志与权限校验符合现有规则。

## 1. 架构与核心约定

### 1.1 多租户隔离
- 业务数据按 `tenant_id` 隔离，所有查询必须使用 `TenantContext.requireTenantId()`。
- `app_permission` 与 `app_menu` 为全局表，不区分租户。
- 租户菜单通过 `app_tenant_menu` 控制可见/不可见。

### 1.2 权限体系
- 权限码格式：`资源:动作`（如 `warehouse:view`）。
- Spring Security 校验使用 `PERM_` 前缀：
  - Controller 示例：`@PreAuthorize("hasAuthority('PERM_warehouse:view')")`。
  - 角色校验：`@PreAuthorize("hasRole('super_admin')")`。
- JWT 中携带原始权限码（不带 `PERM_` 前缀），供前端 `v-permission` 使用。

### 1.3 菜单体系
- 菜单定义在 `app_menu`，租户可见性在 `app_tenant_menu`。
- 菜单可绑定 `permissionCode`，为空表示“登录即可见”。
- 叶子菜单必须有 `path`，父级菜单建议仅展开不跳转。

### 1.4 登录规则
- `/api/login` 不传 `tenantCode`。
- `username` 全局唯一（数据库唯一索引）。
- 登录流程：
  1) 根据用户名查用户（全局唯一校验）
  2) 校验租户是否启用
  3) 校验密码
  4) 返回 `token + refreshToken`

### 1.5 JWT 与权限版本
JWT Claims 关键字段：
```json
{
  "user": { "username": "admin", "role": "admin", "avatar": null },
  "permissions": ["warehouse:view"],
  "av": 0,
  "tid": 1,
  "tcode": "default",
  "utid": 1,
  "exp": 1735689600
}
```
- `av` = `auth_version`，权限变更后必须递增。
- 请求时若 `token.av != db.auth_version`，该请求视为未认证并返回 401。

### 1.6 刷新令牌（Refresh Token）
- 刷新令牌是随机串，数据库存 SHA-256 哈希。
- `/api/refresh` 会轮换刷新令牌（旧令牌立即失效）。
- 刷新令牌包含 `audience_tenant_id`，用于 `super_admin` 切换租户。

### 1.7 无数据库外键
- 数据库已移除外键，必须在服务层做完整性校验：
  - 删除角色前检查是否仍被用户绑定。
  - 删除权限前检查是否仍被角色绑定。
  - 删除菜单前检查是否存在子菜单。

### 1.8 健康检查
- `GET /api/health` 需要携带 Token，返回 `ok:{username}`。

## 2. 初始化数据（DataInitializer）

启动后会自动执行 `DataInitializer`：
- 创建默认租户（`app.tenant.code` / `app.tenant.name`）。
- 插入权限种子（`PermissionSeedProvider`）。
- 插入菜单种子（`MenuSeedProvider`）。
- 初始化所有租户的 `app_tenant_menu`。
- 创建 `admin` 角色（仅非 `tenant:*` 权限）。
- 创建 `super_admin` 角色（全部权限）。
- 创建默认管理员用户，并绑定 `admin + super_admin`。

新租户创建（`TenantService`）逻辑：
- 自动补齐权限（全局权限不存在则插入）。
- 根据传入菜单或默认全量菜单初始化 `app_tenant_menu`。
- 新增租户接口支持 `menuIds` 作为初始菜单配置。
- 仅创建 `admin` 角色（名称：超级管理员，描述：系统管理员）。
- 创建租户管理员用户：
  - 默认用户名为 `admin@{tenantCode}`（default 租户为 `admin`）。
  - 默认密码来自 `app.admin.password`（默认 `password`）。
- 新租户不会拥有 `super_admin` 角色。

## 3. 表结构与数据范围（必须遵守）

全局表（无 `tenant_id`）：
- `app_permission`
- `app_menu`

租户表（强制 `tenant_id`）：
- `app_tenant`
- `app_user`
- `app_role`
- `app_user_role`
- `app_role_permission`
- `app_refresh_token`
- `app_tenant_menu`
- `app_audit_log`

> 注意：`app_user.username` 为全局唯一索引，跨租户不能重复。

## 4. 鉴权与角色边界

- `super_admin`：跨租户管理（租户管理、菜单管理、权限管理、切换租户）。
- `admin`：租户内管理员，禁止分配 `tenant:*` 权限。
- 服务层会阻止非 `super_admin` 角色绑定 `tenant:*` 权限。

## 5. 菜单逻辑（后端视角）

### 5.1 可见菜单计算
菜单显示需要同时满足：
- 菜单自身启用
- 租户菜单配置启用
- 用户拥有菜单 `permissionCode`（空则跳过）
- 父级菜单链路全部满足上述条件

### 5.2 租户菜单配置
- 更新租户菜单时会自动补齐父级菜单，避免子菜单孤立。
- 若租户无 `app_tenant_menu` 数据，默认全量可见。

### 5.3 菜单 CRUD 规则
- 新增菜单会为所有租户创建默认启用映射。
- 删除菜单前必须确保无子菜单。

## 6. 统一异常与返回格式

所有接口返回：
```json
{ "code": 200, "message": "ok", "data": {} }
```
异常统一处理：
- 参数校验错误：`400` + `字段: 错误`
- 认证失败：`401`
- 无权限：`403`
- 未找到：`404`
- 其他异常：`500`

## 7. 新增页面/模块必做清单（后端）

1) **权限种子**
   - 修改 `PermissionSeedProvider`。
   - 新权限码必须唯一、格式 `资源:动作`。
   - 租户管理相关权限使用 `tenant:*` 前缀。
2) **菜单种子（需要导航时）**
   - 修改 `MenuSeedProvider`。
   - `code` 唯一、`path` 与前端路由一致。
   - `permissionCode` 为空 = 登录可见。
   - `i18nKey` 推荐 `nav.xxx`。
3) **接口层**
   - Controller 加 `@PreAuthorize`（注意 `PERM_` 前缀）。
   - 只有 `super_admin` 可操作租户/权限/菜单。
4) **服务层**
   - 查询必须带 `tenant_id`。
   - 删除/更新必须补业务校验（外键已移除）。
5) **权限版本**
   - 用户角色变更、角色权限变更后递增 `auth_version`。
6) **审计日志**
   - create/update/delete 方法标注 `@AuditLog`。
7) **文档**
   - 更新 `API.md`（接口/参数/权限/返回）。

## 8. 功能模块与权限对照（简表）

系统管理：
- 用户：`user:view/add/edit/delete`
- 角色：`role:view/add/edit/delete`
- 权限：`role:view`（查看），增删改仅 `super_admin`
- 审计日志：`audit:view`
- 列配置：`column:edit`（租户管理员可配置）
- 系统配置：`system-config:view/edit`（仅 `super_admin`）
- 菜单：仅 `super_admin`
- 租户：仅 `super_admin`（含租户菜单配置）
- 租户列配置：`column:edit`（租户管理员可配置）

基础业务：
- 仓库：`warehouse:view/add/edit/delete`
- 货架：`shelf:view/add/edit/delete`
- 商品：`product:view/add/edit/delete`
- 供应商：`supplier:view/add/edit/delete`
- 分类：`category:view/add/edit/delete`
- 单位：`unit:view/add/edit/delete`
- 入库：`inbound:view/add/edit/delete`
- 出库：`outbound:view`

## 9. 审计日志（AOP）

需要记录审计日志的操作：
- 用户：创建/编辑/删除/禁用/改密/重置密码
- 角色：创建/编辑/删除
- 权限：创建/编辑/删除
- 菜单：创建/编辑/删除/租户菜单配置
- 租户：创建/启用/停用/删除/切换

补充说明：
- 审计日志分页与导出支持 `tenantId` 参数，但仅 `super_admin` 可跨租户使用。
- 导出接口：`GET /api/audit-logs/export`，默认最多导出 `wms.audit.export-max` 条。
- 审计日志现在包含增强字段：`status / requestId / clientIp / userAgent / durationMs`。
- 每个请求会返回 `X-Request-Id` 响应头；也可在请求头传入同名字段进行链路追踪。

## 10. 日志与监控

已启用：
- **接口耗时统计**：`RequestTimingFilter` 会记录每个请求耗时，并写入 `X-Response-Time` 响应头。
- **慢请求告警**：超过 `wms.monitor.slow-request-ms` 会以 `WARN` 输出。
- **慢查询告警**：`SlowQueryInterceptor` 记录超过 `wms.monitor.slow-query-ms` 的 SQL。

可调参数（`application.properties`）：
- `wms.monitor.slow-request-ms=1000`
- `wms.monitor.slow-query-ms=500`
- `wms.audit.export-max=5000`
- `wms.idempotency.ttl-seconds=10`

## 11. 数据一致性（幂等）

- 写操作支持 `Idempotency-Key` 请求头，5 分钟内重复提交将返回 409。
- 前端会自动基于 `method + url + body` 生成幂等键，避免重复提交。
- 如需强制幂等，可在业务调用时自行指定 `Idempotency-Key`。

## 12. 数据库迁移（Flyway）

- 已接入 Flyway，迁移脚本位置：`src/main/resources/db/migration`。
- 约定命名：`V{版本号}__{描述}.sql`（如 `V2__audit_log_enhance.sql`）。
- 初始化方式已切换：
  - `spring.sql.init.mode=never`
  - `spring.flyway.baseline-on-migrate=true`
- 建议流程：
  1) 修改表结构时，新增一个 Flyway 迁移脚本（不要直接改线上结构）。
  2) 本地启动应用自动执行迁移。
  3) 再由 `DataInitializer` 负责“种子数据/权限/菜单/默认租户”。

## 13. 租户列配置

- 接口：`GET /api/tenant-columns/{pageKey}` 与 `PUT /api/tenant-columns/{pageKey}`。
- 租户管理员通过 UI 配置列显示（需要 `column:edit`），配置会影响该租户所有用户。
- 前端会将租户配置与本地列偏好取交集，避免越权显示。

## 14. 备份与恢复（PostgreSQL）

推荐使用 `pg_dump/pg_restore`。项目提供脚本：
- 备份：`scripts/pg_backup.ps1`
- 恢复：`scripts/pg_restore.ps1`

示例（PowerShell）：
```powershell
$env:PGPASSWORD="123456"
.\scripts\pg_backup.ps1 -Host localhost -Port 5432 -Database wms_backend -User postgres -Output wms_backup.dump
.\scripts\pg_restore.ps1 -Host localhost -Port 5432 -Database wms_backend -User postgres -Input wms_backup.dump
```

Docker 场景（容器名为 `wms-backend-pgsql`）：
```powershell
docker exec wms-backend-pgsql pg_dump -U postgres -F c wms_backend > wms_backup.dump
docker exec -i wms-backend-pgsql pg_restore -U postgres -d wms_backend -c < wms_backup.dump
```

## 15. 常见问题排查

- 菜单不显示：检查 `app_menu`、`app_tenant_menu` 是否启用 + 是否有权限码。
- 按钮不显示：权限码未写入 `PermissionSeedProvider` 或未绑定给角色。
- 登录失败：用户名不唯一或所属租户被禁用。
- 切换租户无效：必须使用 `/api/tenants/switch` 返回的新 token。
- 新增页面模板：参见 `NEW_PAGE_TEMPLATE.md`。
