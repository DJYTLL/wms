- ????????????????????????????????????????

# WMS Backend API 文档

## 基本信息

- 基础地址: http://localhost:8080
- 数据格式: application/json; charset=utf-8
- 认证方式: Bearer Token

## 统一响应结构

```json
{
  "code": 200,
  "message": "ok",
  "data": {}
}
```

### 错误码说明

| code | 含义       | 说明                       |
| ---- | ---------- | -------------------------- |
| 200  | 成功       | 操作成功                   |
| 400  | 参数错误   | 参数校验失败或业务校验失败 |
| 401  | 未认证     | 登录失败或 Token 无效      |
| 403  | 无权限     | 权限不足                   |
| 404  | 未找到     | 资源不存在                 |
| 500  | 服务器错误 | 未捕获异常                 |

> 说明：业务校验失败返回 400；未授权/无权限由 Spring Security 返回 401/403。

## 数据库表结构

### app_tenant（租户表）

| 字段       | 类型         | 说明             |
| ---------- | ------------ | ---------------- |
| id         | BIGSERIAL    | 主键             |
| code       | VARCHAR(100) | 租户编码（唯一） |
| name       | VARCHAR(200) | 租户名称         |
| is_enabled | BOOLEAN      | 是否启用         |
| created_at | TIMESTAMPTZ  | 创建时间         |
| updated_at | TIMESTAMPTZ  | 更新时间         |

### app_user（用户表）

| 字段                    | 类型         | 说明                       |
| ----------------------- | ------------ | -------------------------- |
| id                      | BIGSERIAL    | 主键                       |
| tenant_id               | BIGINT       | 租户 ID                    |
| username                | VARCHAR(100) | 用户名（租户内唯一）       |
| password_hash           | VARCHAR(255) | 密码哈希（BCrypt）         |
| display_name            | VARCHAR(200) | 显示名                     |
| email                   | VARCHAR(200) | 邮箱                       |
| phone                   | VARCHAR(50)  | 手机号                     |
| avatar_url              | VARCHAR(500) | 头像地址                   |
| is_enabled              | BOOLEAN      | 是否启用                   |
| account_non_expired     | BOOLEAN      | 账号是否过期               |
| account_non_locked      | BOOLEAN      | 账号是否锁定               |
| credentials_non_expired | BOOLEAN      | 密码是否过期               |
| auth_version            | BIGINT       | 权限版本（权限变更时递增） |
| last_login_at           | TIMESTAMPTZ  | 最近登录时间               |
| created_at              | TIMESTAMPTZ  | 创建时间                   |
| updated_at              | TIMESTAMPTZ  | 更新时间                   |
| deleted_at              | TIMESTAMPTZ  | 删除时间（软删除）         |
| remark                  | VARCHAR(500) | 备注                       |

### app_role（角色表）

| 字段        | 类型         | 说明                   |
| ----------- | ------------ | ---------------------- |
| id          | BIGSERIAL    | 主键                   |
| tenant_id   | BIGINT       | 租户 ID                |
| code        | VARCHAR(100) | 角色编码（租户内唯一） |
| name        | VARCHAR(200) | 角色名称               |
| description | VARCHAR(500) | 角色描述               |
| is_enabled  | BOOLEAN      | 是否启用               |
| created_at  | TIMESTAMPTZ  | 创建时间               |
| updated_at  | TIMESTAMPTZ  | 更新时间               |

### app_permission（权限表）

| 字段        | 类型         | 说明                   |
| ----------- | ------------ | ---------------------- |
| id          | BIGSERIAL    | 主键                   |
| tenant_id   | BIGINT       | 租户 ID                |
| code        | VARCHAR(150) | 权限编码（租户内唯一） |
| name        | VARCHAR(200) | 权限名称               |
| description | VARCHAR(500) | 权限描述               |
| is_enabled  | BOOLEAN      | 是否启用               |
| created_at  | TIMESTAMPTZ  | 创建时间               |
| updated_at  | TIMESTAMPTZ  | 更新时间               |

### app_user_role（用户-角色）

| 字段       | 类型        | 说明     |
| ---------- | ----------- | -------- |
| tenant_id  | BIGINT      | 租户 ID  |
| user_id    | BIGINT      | 用户 ID  |
| role_id    | BIGINT      | 角色 ID  |
| created_at | TIMESTAMPTZ | 创建时间 |

### app_role_permission（角色-权限）

| 字段          | 类型        | 说明     |
| ------------- | ----------- | -------- |
| tenant_id     | BIGINT      | 租户 ID  |
| role_id       | BIGINT      | 角色 ID  |
| permission_id | BIGINT      | 权限 ID  |
| created_at    | TIMESTAMPTZ | 创建时间 |

### app_refresh_token（刷新令牌）

| 字段       | 类型        | 说明                |
| ---------- | ----------- | ------------------- |
| id         | BIGSERIAL   | 主键                |
| tenant_id  | BIGINT      | 租户 ID             |
| user_id    | BIGINT      | 用户 ID             |
| token_hash | VARCHAR(64) | 令牌哈希（SHA-256） |
| expires_at | TIMESTAMPTZ | 过期时间            |
| revoked_at | TIMESTAMPTZ | 撤销时间            |
| created_at | TIMESTAMPTZ | 创建时间            |
| updated_at | TIMESTAMPTZ | 更新时间            |

### app_audit_log（审计日志）

| 字段           | 类型         | 说明         |
| -------------- | ------------ | ------------ |
| id             | BIGSERIAL    | 主键         |
| tenant_id      | BIGINT       | 租户 ID      |
| actor_username | VARCHAR(100) | 操作者用户名 |
| action         | VARCHAR(100) | 动作标识     |
| entity_type    | VARCHAR(100) | 实体类型     |
| entity_id      | VARCHAR(64)  | 实体 ID      |
| detail         | TEXT         | 详情         |
| created_at     | TIMESTAMPTZ  | 创建时间     |

## 认证说明

- 登录接口返回 `token` + `refreshToken`。
- 访问业务接口在 Header 中携带: `Authorization: Bearer <token>`。
- 权限变更时会递增 `auth_version`，已登录 Token 在下次请求时失效，前端需调用刷新接口获取新 Token。
- 系统为多租户模式，登录时必须提供 `tenantCode`，后续请求按 Token 中的租户信息隔离数据。

### JWT 载荷 (Claims) 结构

```json
{
  "user": {
    "username": "admin",
    "role": "admin",
    "avatar": "https://example.com/avatar.png"
  },
  "permissions": ["warehouse:view", "warehouse:add", "product:view", "product:edit"],
  "av": 0,
  "tid": 1,
  "tcode": "default",
  "exp": 1735689600
}
```

字段说明:

- `user.username`: 用户名，用于前端显示。
- `user.role`: 角色标识（例如 `admin`）。
- `user.avatar`: 头像 URL，可为空。
- `permissions`: 权限数组（`资源:操作`）。
- `av`: 权限版本（auth_version）。
- `tid`: 租户 ID。
- `tcode`: 租户编码。
- `exp`: JWT 过期时间戳（秒）。

## 接口列表

### 1) 登录

- 方法: `POST`
- 路径: `/api/login`
- 是否需要认证: 否

请求参数:

```json
{
  "tenantCode": "default",
  "username": "admin",
  "password": "password"
}
```

参数规则:

- `tenantCode`: 必填，非空
- `username`: 必填，非空
- `password`: 必填，非空

响应参数:

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "<jwt token>",
    "refreshToken": "<refresh token>"
  }
}
```

### 2) 刷新令牌

- 方法: `POST`
- 路径: `/api/refresh`
- 是否需要认证: 否

请求参数:

```json
{
  "refreshToken": "<refresh token>"
}
```

响应参数:

```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "token": "<new jwt token>",
    "refreshToken": "<new refresh token>"
  }
}
```

### 3) 登出

- 方法: `POST`
- 路径: `/api/logout`
- 是否需要认证: 否

请求参数:

```json
{
  "refreshToken": "<refresh token>"
}
```

响应参数:

```json
{
  "code": 200,
  "message": "ok",
  "data": null
}
```

### 4) 健康检查

- 方法: `GET`
- 路径: `/api/health`
- 是否需要认证: 是

请求头:

```
Authorization: Bearer <token>
```

响应参数:

```
ok:admin
```

### 5) 用户管理

- 基础路径: `/api/users`
- 是否需要认证: 是（需要 `user:*` 权限）

#### 5.1 查询用户列表

- 方法: `GET`
- 路径: `/api/users`
- 权限: `user:view`

#### 5.2 分页查询用户

- 方法: `GET`
- 路径: `/api/users/page`
- 权限: `user:view`

请求参数:

- `page`: 页码，从 1 开始，默认 1
- `size`: 每页数量，默认 20
- `keyword`: 关键词（用户名/显示名/邮箱/手机号）
- `enabled`: 是否启用（true/false）

响应参数:

```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "total": 1,
    "page": 1,
    "size": 20,
    "items": []
  }
}
```

#### 5.3 查询用户详情

- 方法: `GET`
- 路径: `/api/users/{id}`
- 权限: `user:view`

#### 5.4 新增用户

- 方法: `POST`
- 路径: `/api/users`
- 权限: `user:add`

请求参数:

```json
{
  "username": "tom",
  "password": "123456",
  "displayName": "Tom",
  "email": "tom@example.com",
  "phone": "13800000000",
  "avatarUrl": "https://example.com/avatar.png",
  "enabled": true,
  "accountNonExpired": true,
  "accountNonLocked": true,
  "credentialsNonExpired": true
}
```

参数规则:

- `username`: 必填，非空，唯一
- `password`: 必填，非空
- `enabled/accountNonExpired/accountNonLocked/credentialsNonExpired`: 可选，默认 true

#### 5.5 更新用户

- 方法: `PUT`
- 路径: `/api/users/{id}`
- 权限: `user:edit`

#### 5.6 删除用户

- 方法: `DELETE`
- 路径: `/api/users/{id}`
- 权限: `user:delete`

#### 5.7 更新用户状态

- 方法: `PUT`
- 路径: `/api/users/{id}/status`
- 权限: `user:edit`

请求参数:

```json
{
  "enabled": true,
  "accountNonExpired": true,
  "accountNonLocked": true,
  "credentialsNonExpired": true
}
```

#### 5.8 修改用户密码

- 方法: `PUT`
- 路径: `/api/users/{id}/password`
- 权限: `user:edit`

请求参数:

```json
{
  "oldPassword": "oldPass",
  "newPassword": "newPass"
}
```

#### 5.9 重置用户密码

- 方法: `POST`
- 路径: `/api/users/{id}/reset-password`
- 权限: `user:edit`

请求参数:

```json
{
  "newPassword": "newPass"
}
```

#### 5.10 查询用户角色

- 方法: `GET`
- 路径: `/api/users/{id}/roles`
- 权限: `user:view`

#### 5.11 设置用户角色

- 方法: `PUT`
- 路径: `/api/users/{id}/roles`
- 权限: `user:edit`

请求参数:

```json
{
  "roleIds": [1, 2]
}
```

### 6) 权限管理

- 基础路径: `/api/permissions`
- 是否需要认证: 是（权限相关由 `role:*` 控制）

#### 6.1 查询权限列表

- 方法: `GET`
- 路径: `/api/permissions`
- 权限: `role:view`

#### 6.2 分页查询权限

- 方法: `GET`
- 路径: `/api/permissions/page`
- 权限: `role:view`

请求参数:

- `page`: 页码，从 1 开始，默认 1
- `size`: 每页数量，默认 20
- `keyword`: 关键词（编码/名称/描述）
- `enabled`: 是否启用（true/false）

#### 6.3 查询权限详情

- 方法: `GET`
- 路径: `/api/permissions/{id}`
- 权限: `role:view`

#### 6.4 新增权限

- 方法: `POST`
- 路径: `/api/permissions`
- 权限: `role:add`

请求参数:

```json
{
  "code": "inbound:add",
  "name": "新增入库单",
  "description": "新增入库单",
  "enabled": true
}
```

#### 6.5 更新权限

- 方法: `PUT`
- 路径: `/api/permissions/{id}`
- 权限: `role:edit`

#### 6.6 删除权限

- 方法: `DELETE`
- 路径: `/api/permissions/{id}`
- 权限: `role:delete`

### 7) 角色管理

- 基础路径: `/api/roles`
- 是否需要认证: 是（需要 `role:*` 权限）

#### 7.1 查询角色列表

- 方法: `GET`
- 路径: `/api/roles`
- 权限: `role:view`

#### 7.2 分页查询角色

- 方法: `GET`
- 路径: `/api/roles/page`
- 权限: `role:view`

请求参数:

- `page`: 页码，从 1 开始，默认 1
- `size`: 每页数量，默认 20
- `keyword`: 关键词（编码/名称/描述）
- `enabled`: 是否启用（true/false）

#### 7.3 查询角色详情

- 方法: `GET`
- 路径: `/api/roles/{id}`
- 权限: `role:view`

#### 7.4 新增角色

- 方法: `POST`
- 路径: `/api/roles`
- 权限: `role:add`

#### 7.5 更新角色

- 方法: `PUT`
- 路径: `/api/roles/{id}`
- 权限: `role:edit`

#### 7.6 删除角色

- 方法: `DELETE`
- 路径: `/api/roles/{id}`
- 权限: `role:delete`

### 8) 角色权限管理

- 基础路径: `/api/roles/{id}/permissions`
- 是否需要认证: 是（需要 `role:*` 权限）

#### 8.1 查询角色权限

- 方法: `GET`
- 路径: `/api/roles/{id}/permissions`
- 权限: `role:view`

#### 8.2 批量设置角色权限

- 方法: `PUT`
- 路径: `/api/roles/{id}/permissions`
- 权限: `role:edit`

请求参数:

```json
{
  "permissionIds": [1, 2, 3]
}
```

#### 8.3 追加单个权限

- 方法: `POST`
- 路径: `/api/roles/{id}/permissions/{permissionId}`
- 权限: `role:edit`

#### 8.4 移除单个权限

- 方法: `DELETE`
- 路径: `/api/roles/{id}/permissions/{permissionId}`
- 权限: `role:edit`

## 权限清单 (初始化给 admin 角色)

- warehouse:view 查看仓库列表
- warehouse:add 新增仓库
- warehouse:edit 编辑仓库信息
- warehouse:delete 删除仓库

- shelf:view 查看货架列表
- shelf:add 新增货架
- shelf:edit 编辑货架信息
- shelf:delete 删除货架

- product:view 查看商品信息
- product:add 新增商品
- product:edit 编辑商品信息
- product:delete 删除商品

- supplier:view 查看供应商信息
- supplier:add 新增供应商
- supplier:edit 编辑供应商信息
- supplier:delete 删除供应商

- category:view 查看分类信息
- category:add 新增分类
- category:edit 编辑分类信息
- category:delete 删除分类

- unit:view 查看计量单位
- unit:add 新增计量单位
- unit:edit 编辑计量单位
- unit:delete 删除计量单位

- inbound:view 查看入库单
- inbound:add 新增入库单
- inbound:edit 编辑入库单
- inbound:delete 删除入库单

- outbound:view 查看出库单

- user:view 查看用户
- user:add 新增用户
- user:edit 编辑用户
- user:delete 删除用户

- role:view 查看角色
- role:add 新增角色
- role:edit 编辑角色
- role:delete 删除角色

### 9) 租户管理

- 基础路径: `/api/tenants`
- 是否需要认证: 是（仅超级管理员）

#### 9.1 查询租户列表

- 方法: `GET`
- 路径: `/api/tenants`

#### 9.2 查询租户详情

- 方法: `GET`
- 路径: `/api/tenants/{id}`

#### 9.3 新增租户

- 方法: `POST`
- 路径: `/api/tenants`

请求参数:

```json
{
  "code": "tenant-a",
  "name": "租户A",
  "adminUsername": "admin",
  "adminPassword": "password"
}
```

参数规则:

- `code`: 必填，租户编码，唯一
- `name`: 必填，租户名称
- `adminUsername`: 可选，默认使用系统管理员账号
- `adminPassword`: 可选，默认使用系统管理员密码

#### 9.4 启用/停用租户

- 方法: `PUT`
- 路径: `/api/tenants/{id}/status`

请求参数:

```json
{
  "enabled": true
}
```

#### 9.5 切换租户

- 方法: `POST`
- 路径: `/api/tenants/switch`

请求参数:

```json
{
  "tenantCode": "tenant-a"
}
```

响应参数:

```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "token": "<jwt token>",
    "refreshToken": "<refresh token>"
  }
}
```

- tenant:view 查看租户列表
- tenant:add 新增租户
- tenant:edit 编辑租户信息
- tenant:disable 停用/启用租户
- tenant:switch 切换租户
