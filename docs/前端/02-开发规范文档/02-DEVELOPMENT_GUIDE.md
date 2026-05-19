# WMS Frontend 开发文档（可脱离代码理解）

本项目为多租户 + 权限驱动的前端。目标是：不读代码也能新增页面/路由/按钮权限，并保证菜单与权限逻辑一致。

## 1. 认证与 Token 机制

### 1.1 登录与 Token 存储
- 登录接口：`POST /api/login`（仅 `username + password`）。
- 成功后返回 `token + refreshToken`。
- Token 存储在 `localStorage`：
  - `token`：访问令牌
  - `refreshToken`：刷新令牌

### 1.2 请求封装
- 统一使用 `src/utils/request.ts`。
- 自动注入 `Authorization: Bearer <token>`。
- 401 自动刷新：
  - `POST /api/refresh`。
  - 刷新成功后重试原请求。
  - 刷新失败则清空 token 并跳转 `/login`。
- 业务错误（`code != 200`）会抛错，页面必须通过 `useApiError()` 处理。

### 1.3 Token 事件
- `auth:tokens-updated`：token 更新时触发（刷新/切换租户）。
- `auth:tokens-cleared`：token 清空时触发。
- `authStore` 会监听并更新用户/权限/租户信息。

## 2. 权限体系（前端）

### 2.1 路由守卫
- 路由配置在 `src/router/index.ts`。
- `meta.permission`：权限校验（如 `user:view`）。
- `meta.role`：角色校验，仅 `super_admin` 页面需要。
- 无权限会被重定向到 `/`。

### 2.2 按钮权限
- 使用 `v-permission="'xxx:action'"` 控制按钮显示。
- 权限列表来自 JWT `permissions` 字段。

## 3. 菜单体系（前端）

### 3.1 菜单来源
- 登录后从 `/api/menus` 获取菜单树。
- 数据结构：`{ id, key, title, path, icon, children }`。
- `key` 对应后端 `i18n_key`。

### 3.2 菜单展示规则
- 有子级的菜单仅展开，不跳转。
- 叶子菜单必须有 `path`，才能路由跳转。
- 菜单权限由后端过滤，前端不重复判断。

### 3.3 菜单缓存
- `menuStore` 会按 `tenantCode` 缓存菜单。
- 触发刷新：`window.dispatchEvent(new Event('menu:refresh'))`。
- `auth:tokens-updated` 会自动触发刷新。

## 4. i18n 规范

新增文案统一使用命名空间：
- `nav.*` 菜单导航文本
- `page.*` 页面标题
- `action.*` 按钮/动作
- `field.*` 字段/列名
- `status.*` 状态
- `menu.*` 菜单管理字段
- `table.*` 表格通用
- `message.*` 提示信息
- `placeholder.*` 输入占位
- `filter.*` 筛选
- `sort.*` 排序
- `orderType.*` 业务枚举

旧菜单 key 兼容映射在 `src/utils/i18n.ts`，新增菜单建议直接使用 `nav.xxx`。

## 5. UI 与交互规范

### 5.1 表格页规范
统一使用：
- 容器类：`page-shell`
- 表格卡片：`table-card`
- 分页区：`table-pagination`
- 空状态：`table.empty`

结构示例：
```html
<div class="page-shell">
  <div class="page-header">...</div>
  <div class="table-card">
    <div class="table-body">
      <el-table ... />
    </div>
    <div class="table-pagination">
      <el-pagination ... />
    </div>
  </div>
</div>
```

### 5.2 错误提示
- 禁止在页面内直接 `ElMessage`。
- 统一使用 `useApiError()`：
  - `notifySuccess()`
  - `notifyWarning()`
  - `notifyError(error)`

## 6. 新增页面开发清单（前端）

1) **确认后端权限/菜单**
   - 权限需写入 `PermissionSeedProvider`。
   - 菜单需写入 `MenuSeedProvider`。
2) **新增路由**
   - `src/router/index.ts` 增加路由。
   - `meta.permission` 与后端权限码保持一致。
   - 仅超级管理员页面加 `meta.role = 'super_admin'`。
3) **新增页面组件**
   - 使用统一表格结构与样式。
   - API 请求使用 `request`。
   - 错误提示使用 `useApiError()`。
4) **按钮权限控制**
   - 所有新增/编辑/删除按钮必须加 `v-permission`。
5) **i18n 补齐**
   - `nav.*`（菜单文本）
   - `page.*`（页面标题）
   - `field.*` / `action.*` / `message.*`

## 7. 页面与接口对照（常用）

### 登录
- `POST /api/login`
- 登录成功后写入 token，并触发 `auth:tokens-updated`。

### 用户管理
- 权限：`user:view/add/edit/delete`
- 列表接口：`GET /api/users/page`
- 角色关联：`PUT /api/users/{id}/roles`

### 角色管理
- 权限：`role:view/add/edit/delete`
- 列表接口：`GET /api/roles/page`
- 权限配置：`PUT /api/roles/{id}/permissions`

### 权限管理（仅 super_admin）
- 权限：`role:view`（查看），增删改仅 `super_admin`
- 列表接口：`GET /api/permissions/page`

### 审计日志
- 权限：`audit:view`
- 列表接口：`GET /api/audit-logs/page`
- 筛选：动作/实体类型下拉选择，`super_admin` 可选择租户
- 导出：`GET /api/audit-logs/export`（CSV）

### 系统配置（仅 super_admin）
- 权限：`system-config:view/edit`
- 列表接口：`GET /api/system-configs`
- 更新接口：`PUT /api/system-configs/{key}`

### 租户列配置
- 接口：`GET /api/tenant-columns/{pageKey}`、`PUT /api/tenant-columns/{pageKey}`
- 租户管理员可统一控制该租户用户的列显示

### 菜单管理（仅 super_admin）
- `GET /api/menus/all`
- `POST /api/menus`
- `PUT /api/menus/{id}`
- `DELETE /api/menus/{id}`
- 操作完成后触发 `menu:refresh`

### 租户管理（仅 super_admin）
- `GET /api/tenants`
- `POST /api/tenants`（支持选择菜单）
- `PUT /api/tenants/{id}/menus`
- `POST /api/tenants/switch`

### 基础数据与业务
权限码示例：
- 仓库：`warehouse:view/add/edit/delete`
- 货架：`shelf:view/add/edit/delete`
- 商品：`product:view/add/edit/delete`
- 供应商：`supplier:view/add/edit/delete`
- 分类：`category:view/add/edit/delete`
- 单位：`unit:view/add/edit/delete`
- 入库：`inbound:view/add/edit/delete`
- 出库：`outbound:view`

## 8. 常见问题排查

- 菜单不更新：菜单修改或租户切换后触发 `menu:refresh`。
- 按钮不显示：确认权限码在权限表并已绑定角色。
- 401 跳转登录：refreshToken 失效或被撤销。
- 页面可见但接口 403：检查 `meta.permission` 与后端权限码是否一致。

## 9. 新增页面模板

可直接复用的模板在 `NEW_PAGE_TEMPLATE.md`。
