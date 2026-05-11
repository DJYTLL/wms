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
| code | 含义 | 说明 |
| --- | --- | --- |
| 200 | 成功 | 操作成功 |
| 400 | 参数错误 | 参数校验失败或业务校验失败 |
| 401 | 未认证 | 登录失败或 Token 无效 |
| 403 | 无权限 | 权限不足 |
| 404 | 未找到 | 资源不存在 |
| 409 | 冲突 | 重复请求（幂等） |
| 500 | 服务器错误 | 未捕获异常 |

> 说明：业务校验失败返回 400；未授权/无权限由 Spring Security 返回 401/403。

## 数据库表结构

### app_tenant（租户表）
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGSERIAL | 主键 |
| code | VARCHAR(100) | 租户编码（唯一） |
| name | VARCHAR(200) | 租户名称 |
| is_enabled | BOOLEAN | 是否启用 |
| created_at | TIMESTAMPTZ | 创建时间 |
| updated_at | TIMESTAMPTZ | 更新时间 |
| deleted_at | TIMESTAMPTZ | 删除时间（软删除） |

### app_user（用户表）
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGSERIAL | 主键 |
| tenant_id | BIGINT | 租户 ID |
| username | VARCHAR(100) | 用户名（全局唯一） |
| password_hash | VARCHAR(255) | 密码哈希（BCrypt） |
| display_name | VARCHAR(200) | 显示名 |
| email | VARCHAR(200) | 邮箱 |
| phone | VARCHAR(50) | 手机号 |
| avatar_url | VARCHAR(500) | 头像地址 |
| is_enabled | BOOLEAN | 是否启用 |
| account_non_expired | BOOLEAN | 账号是否过期 |
| account_non_locked | BOOLEAN | 账号是否锁定 |
| credentials_non_expired | BOOLEAN | 密码是否过期 |
| auth_version | BIGINT | 权限版本（权限变更时递增） |
| last_login_at | TIMESTAMPTZ | 最近登录时间 |
| created_at | TIMESTAMPTZ | 创建时间 |
| updated_at | TIMESTAMPTZ | 更新时间 |
| deleted_at | TIMESTAMPTZ | 删除时间（软删除） |
| remark | VARCHAR(500) | 备注 |

### app_role（角色表）
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGSERIAL | 主键 |
| tenant_id | BIGINT | 租户 ID |
| code | VARCHAR(100) | 角色编码（租户内唯一） |
| name | VARCHAR(200) | 角色名称 |
| description | VARCHAR(500) | 角色描述 |
| is_enabled | BOOLEAN | 是否启用 |
| created_at | TIMESTAMPTZ | 创建时间 |
| updated_at | TIMESTAMPTZ | 更新时间 |

### app_permission（权限表）
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGSERIAL | 主键 |
| code | VARCHAR(150) | 权限编码（全局唯一） |
| name | VARCHAR(200) | 权限名称 |
| description | VARCHAR(500) | 权限描述 |
| is_enabled | BOOLEAN | 是否启用 |
| created_at | TIMESTAMPTZ | 创建时间 |
| updated_at | TIMESTAMPTZ | 更新时间 |

### app_user_role（用户-角色）
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| tenant_id | BIGINT | 租户 ID |
| user_id | BIGINT | 用户 ID |
| role_id | BIGINT | 角色 ID |
| created_at | TIMESTAMPTZ | 创建时间 |

### app_role_permission（角色-权限）
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| tenant_id | BIGINT | 租户 ID |
| role_id | BIGINT | 角色 ID |
| permission_id | BIGINT | 权限 ID |
| created_at | TIMESTAMPTZ | 创建时间 |

### app_refresh_token（刷新令牌）
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGSERIAL | 主键 |
| tenant_id | BIGINT | 租户 ID |
| user_id | BIGINT | 用户 ID |
| audience_tenant_id | BIGINT | 目标租户 ID（切换租户用） |
| token_hash | VARCHAR(64) | 令牌哈希（SHA-256） |
| expires_at | TIMESTAMPTZ | 过期时间 |
| revoked_at | TIMESTAMPTZ | 撤销时间 |
| created_at | TIMESTAMPTZ | 创建时间 |
| updated_at | TIMESTAMPTZ | 更新时间 |

### app_audit_log（审计日志）
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGSERIAL | 主键 |
| tenant_id | BIGINT | 租户 ID |
| actor_username | VARCHAR(100) | 操作者用户名 |
| action | VARCHAR(100) | 动作标识 |
| entity_type | VARCHAR(100) | 实体类型 |
| entity_id | VARCHAR(64) | 实体 ID |
| detail | TEXT | 详情 |
| status | VARCHAR(20) | 执行结果（SUCCESS/FAIL） |
| request_id | VARCHAR(64) | 请求 ID（X-Request-Id） |
| client_ip | VARCHAR(64) | 客户端 IP |
| user_agent | VARCHAR(400) | 客户端 UA |
| duration_ms | BIGINT | 执行耗时（毫秒） |
| created_at | TIMESTAMPTZ | 创建时间 |

### app_system_config（系统配置）
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGSERIAL | 主键 |
| config_key | VARCHAR(120) | 配置键（唯一） |
| config_value | TEXT | 配置值 |
| value_type | VARCHAR(40) | 值类型 |
| description | VARCHAR(500) | 描述 |
| is_public | BOOLEAN | 是否公开 |
| created_at | TIMESTAMPTZ | 创建时间 |
| updated_at | TIMESTAMPTZ | 更新时间 |

### app_idempotency（幂等记录）
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| idempotency_key | VARCHAR(64) | 幂等键（主键） |
| method | VARCHAR(16) | 请求方法 |
| path | VARCHAR(200) | 请求路径 |
| tenant_id | BIGINT | 租户 ID |
| username | VARCHAR(100) | 用户名 |
| created_at | TIMESTAMPTZ | 创建时间 |
| expires_at | TIMESTAMPTZ | 过期时间 |

### app_menu（菜单表）
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGSERIAL | 主键 |
| code | VARCHAR(100) | 菜单编码（唯一） |
| parent_id | BIGINT | 父级菜单 ID |
| title | VARCHAR(200) | 菜单名称 |
| i18n_key | VARCHAR(100) | 多语言 key |
| path | VARCHAR(200) | 路由地址 |
| icon | TEXT | 图标（SVG 可选） |
| permission_code | VARCHAR(150) | 权限编码（可为空） |
| sort | INT | 排序 |
| is_enabled | BOOLEAN | 是否启用 |
| created_at | TIMESTAMPTZ | 创建时间 |
| updated_at | TIMESTAMPTZ | 更新时间 |

### app_tenant_menu（租户菜单）
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| tenant_id | BIGINT | 租户 ID |
| menu_id | BIGINT | 菜单 ID |
| is_enabled | BOOLEAN | 是否启用 |
| created_at | TIMESTAMPTZ | 创建时间 |
| updated_at | TIMESTAMPTZ | 更新时间 |

### app_tenant_column_setting（租户列配置）
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| tenant_id | BIGINT | 租户 ID |
| page_key | VARCHAR(120) | 页面标识 |
| visible_columns | TEXT | 可见列（CSV） |
| updated_by | VARCHAR(100) | 更新人 |
| updated_at | TIMESTAMPTZ | 更新时间 |

## 认证说明
- 登录接口返回 `token` + `refreshToken`。
- 访问业务接口在 Header 中携带: `Authorization: Bearer <token>`。
- 权限变更时会递增 `auth_version`，已登录 Token 在下次请求时失效，前端需调用刷新接口获取新 Token。
- 系统为多租户模式，登录时不需要 `tenantCode`，用户名全局唯一。
- 切换租户使用 `/api/tenants/switch`，返回新 Token。
- 写操作建议携带 `Idempotency-Key`（5 分钟内重复提交返回 409）。
- 所有请求都会返回 `X-Request-Id` 响应头（可在请求头透传同名字段用于链路追踪）。

### JWT 载荷 (Claims) 结构
```json
{
  "user": {
    "username": "admin",
    "role": "admin",
    "avatar": "https://example.com/avatar.png"
  },
  "permissions": [
    "warehouse:view",
    "warehouse:add",
    "product:view",
    "product:edit"
  ],
  "av": 0,
  "tid": 1,
  "tcode": "default",
  "utid": 1,
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
- `utid`: 用户所属租户 ID（用于跨租户切换）。
- `exp`: JWT 过期时间戳（秒）。

## 接口列表

### 1) 登录
- 方法: `POST`
- 路径: `/api/login`
- 是否需要认证: 否

请求参数:
```json
{
  "username": "admin",
  "password": "password"
}
```

参数规则:
- `username`: 必填，非空（全局唯一）
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

### 4.1) 菜单
- 方法: `GET`
- 路径: `/api/menus`
- 是否需要认证: 是

响应参数:
```json
{
  "code": 200,
  "message": "ok",
  "data": [
    {
      "id": 1,
      "key": "dashboard",
      "title": "仪表盘",
      "path": "/",
      "icon": "<svg>...</svg>",
      "children": []
    }
  ]
}
```

### 4.2) 菜单管理（仅超级管理员）
- 权限: `super_admin`

#### 4.2.1 查询全部菜单
- 方法: `GET`
- 路径: `/api/menus/all`

响应参数（示例）:
```json
{
  "code": 200,
  "message": "ok",
  "data": [
    {
      "id": 1,
      "code": "dashboard",
      "parentId": null,
      "title": "仪表盘",
      "i18nKey": "nav.dashboard",
      "path": "/",
      "icon": "<svg>...</svg>",
      "permissionCode": null,
      "sort": 0,
      "enabled": true,
      "children": []
    }
  ]
}
```

#### 4.2.2 新增菜单
- 方法: `POST`
- 路径: `/api/menus`

请求参数:
```json
{
  "code": "basic",
  "parentId": null,
  "title": "基础信息管理",
  "i18nKey": "nav.basic",
  "path": null,
  "icon": "<svg>...</svg>",
  "permissionCode": null,
  "sort": 10,
  "enabled": true
}
```

#### 4.2.3 更新菜单
- 方法: `PUT`
- 路径: `/api/menus/{id}`

请求参数: 同新增菜单

#### 4.2.4 删除菜单
- 方法: `DELETE`
- 路径: `/api/menus/{id}`

说明:
- 有子菜单时不允许删除

### 4.3) 审计日志
- 基础路径: `/api/audit-logs`
- 是否需要认证: 是（需要 `audit:view` 权限）

#### 4.3.1 分页查询审计日志
- 方法: `GET`
- 路径: `/api/audit-logs/page`
- 权限: `audit:view`

请求参数:
- `page`: 页码，从 1 开始，默认 1
- `size`: 每页数量，默认 20
- `tenantId`: 租户 ID（仅 `super_admin` 可跨租户查询，不传则返回当前租户）
- `keyword`: 关键词（操作者/动作/实体/详情）
- `action`: 动作（如 `USER_CREATE`）
- `entityType`: 实体类型（如 `user`）
- `actorUsername`: 操作者用户名
- `startTime`: 开始时间（ISO 8601）
- `endTime`: 结束时间（ISO 8601）

响应参数:
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "total": 1,
    "page": 1,
    "size": 20,
    "items": [
      {
        "id": 1,
        "tenantId": 1,
        "tenantCode": "default",
        "actorUsername": "admin",
        "action": "USER_CREATE",
        "entityType": "user",
        "entityId": "12",
        "detail": "username=tom",
        "status": "SUCCESS",
        "requestId": "9f7b2c1a5e3d4f12a0b9c8d7e6f5a4b3",
        "clientIp": "127.0.0.1",
        "userAgent": "Mozilla/5.0",
        "durationMs": 12,
        "createdAt": "2026-01-18T10:00:00Z"
      }
    ]
  }
}
```

#### 4.3.2 导出审计日志（CSV）
- 方法: `GET`
- 路径: `/api/audit-logs/export`
- 权限: `audit:view`

请求参数（同分页接口）:
- `tenantId`: 租户 ID（仅 `super_admin` 可跨租户导出，不传则导出当前租户）
- `keyword`
- `action`
- `entityType`
- `actorUsername`
- `startTime`
- `endTime`

响应:
- CSV 文件下载（默认文件名 `audit-logs.csv`）。

### 4.4) 系统配置（仅超级管理员）
- 基础路径: `/api/system-configs`

#### 4.4.1 查询全部配置
- 方法: `GET`
- 路径: `/api/system-configs`
- 权限: `system-config:view`

响应参数（示例）:
```json
{
  "code": 200,
  "message": "ok",
  "data": [
    {
      "id": 1,
      "key": "default.page.size",
      "value": "20",
      "valueType": "int",
      "description": "默认分页大小",
      "isPublic": false,
      "createdAt": "2026-01-18T10:00:00Z",
      "updatedAt": "2026-01-18T10:00:00Z"
    }
  ]
}
```

#### 4.4.2 查询配置详情
- 方法: `GET`
- 路径: `/api/system-configs/{key}`
- 权限: `system-config:view`

#### 4.4.3 查询公开配置
- 方法: `GET`
- 路径: `/api/system-configs/public`
- 是否需要认证: 否

响应参数（示例）:
```json
{
  "code": 200,
  "message": "ok",
  "data": [
    {
      "id": 1,
      "key": "default.page.size",
      "value": "20",
      "valueType": "int",
      "description": "默认分页大小",
      "isPublic": true,
      "createdAt": "2026-01-18T10:00:00Z",
      "updatedAt": "2026-01-18T10:00:00Z"
    }
  ]
}
```

#### 4.4.4 新增配置
- 方法: `POST`
- 路径: `/api/system-configs/{key}`
- 权限: `system-config:edit`

请求参数:
```json
{
  "value": "20",
  "valueType": "int",
  "description": "默认分页大小",
  "isPublic": false
}
```

#### 4.4.5 更新配置
- 方法: `PUT`
- 路径: `/api/system-configs/{key}`
- 权限: `system-config:edit`

请求参数: 同新增

### 4.5) 租户列配置
- 基础路径: `/api/tenant-columns`

#### 4.5.1 查询租户列配置
- 方法: `GET`
- 路径: `/api/tenant-columns/{pageKey}`
- 是否需要认证: 是

响应参数（示例）:
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "pageKey": "user-management",
    "visibleColumns": ["username", "displayName", "status"],
    "updatedBy": "admin",
    "updatedAt": "2026-01-27T02:54:58.702752Z"
  }
}
```

#### 4.5.2 更新租户列配置
- 方法: `PUT`
- 路径: `/api/tenant-columns/{pageKey}`
- 权限: `column:edit`

请求参数:
```json
{
  "visibleColumns": ["username", "displayName", "status"]
}
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

#### 5.10.1 查询角色下拉选项
- 方法: `GET`
- 路径: `/api/users/role-options`
- 权限: `role:assign:view`

响应参数:
```json
{
  "code": 200,
  "message": "ok",
  "data": [
    { "id": 1, "code": "admin", "name": "超级管理员" }
  ]
}
```

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
- 是否需要认证: 是（查询需 `role:view`，新增/编辑/删除仅超级管理员）

#### 6.1 查询权限列表
- 方法: `GET`
- 路径: `/api/permissions`
- 权限: `role:view`

#### 6.1.1 查询列权限列表
- 方法: `GET`
- 路径: `/api/permissions/columns`
- 权限: `column:role:manage`

响应参数:
```json
{
  "code": 200,
  "message": "ok",
  "data": [
    { "id": 1, "code": "column:user-management:username", "name": "用户管理-用户名列" }
  ]
}
```

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
- 权限: 仅超级管理员

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
- 权限: 仅超级管理员

#### 6.6 删除权限
- 方法: `DELETE`
- 路径: `/api/permissions/{id}`
- 权限: 仅超级管理员

### 7) 角色管理
- 基础路径: `/api/roles`
- 是否需要认证: 是（需要 `role:*` 权限）

#### 7.1 查询角色列表
- 方法: `GET`
- 路径: `/api/roles`
- 权限: `role:view`

#### 7.1.1 查询角色下拉选项
- 方法: `GET`
- 路径: `/api/roles/options`
- 权限: `column:role:manage`

响应参数:
```json
{
  "code": 200,
  "message": "ok",
  "data": [
    { "id": 1, "code": "admin", "name": "超级管理员" }
  ]
}
```

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

#### 8.5 查询角色列权限
- 方法: `GET`
- 路径: `/api/roles/{id}/column-permissions`
- 权限: `column:role:manage`

响应参数:
```json
{
  "code": 200,
  "message": "ok",
  "data": [
    { "id": 100, "code": "column:user-management:username", "name": "用户管理-用户名列" }
  ]
}
```

#### 8.6 设置角色列权限
- 方法: `PUT`
- 路径: `/api/roles/{id}/column-permissions`
- 权限: `column:role:manage`

请求参数:
```json
{
  "permissionIds": [100, 101, 102]
}
```

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
- role:assign:view 用户管理角色下拉选项

- audit:view 查看审计日志
- column:edit 配置列显示
- column:role:manage 角色/租户列权限配置
- system-config:view 查看系统配置
- system-config:edit 编辑系统配置



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
  "adminPassword": "password",
  "menuIds": [1, 2, 3]
}
```

参数规则:
- `code`: 必填，租户编码，唯一
- `name`: 必填，租户名称
- `adminUsername`: 可选，默认 `admin@{tenantCode}`（default 租户为 `admin`）
- `adminPassword`: 可选，默认 `app.admin.password`（未配置时为 `password`）
- `menuIds`: 可选，租户可用菜单 ID 列表

#### 9.4 启用/停用租户
- 方法: `PUT`
- 路径: `/api/tenants/{id}/status`

请求参数:
```json
{
  "enabled": true
}
```

#### 9.5 修改租户名称
- 方法: `PUT`
- 路径: `/api/tenants/{id}`

请求参数:
```json
{
  "name": "租户A-新名称"
}
```

#### 9.6 删除租户（软删除）
- 方法: `DELETE`
- 路径: `/api/tenants/{id}`

说明:
- 默认租户 `default` 不允许删除

#### 9.7 切换租户
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

#### 9.8 查询租户菜单配置
- 方法: `GET`
- 路径: `/api/tenants/{id}/menus`

响应参数:
```json
{
  "code": 200,
  "message": "ok",
  "data": [
    {
      "id": 1,
      "key": "dashboard",
      "title": "仪表盘",
      "path": "/",
      "icon": "<svg>...</svg>",
      "enabled": true,
      "children": []
    }
  ]
}
```

#### 9.9 更新租户菜单配置
- 方法: `PUT`
- 路径: `/api/tenants/{id}/menus`

请求参数:
```json
{
  "menuIds": [1, 2, 3]
}
```

#### 9.10 查询租户列配置（超级管理员）
- 方法: `GET`
- 路径: `/api/tenants/{id}/columns/{pageKey}`
- 权限: `super_admin`

响应参数:
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "pageKey": "user-management",
    "visibleColumns": ["username", "displayName", "status"]
  }
}
```

#### 9.11 更新租户列配置（超级管理员）
- 方法: `PUT`
- 路径: `/api/tenants/{id}/columns/{pageKey}`
- 权限: `super_admin`

请求参数:
```json
{
  "visibleColumns": ["username", "displayName", "status"]
}
```



- tenant:view 查看租户列表
- tenant:add 新增租户
- tenant:edit 编辑租户信息
- tenant:delete 删除租户
- tenant:disable 停用/启用租户
- tenant:switch 切换租户

> 说明：`tenant:*` 权限仅超级管理员角色可分配与使用。

## 10) 进销存（审核即入/出库，无出入库单）

本模式下：采购单/销售单从草稿审核后，直接产生库存流水并更新当前库存。

### 10.1 状态机（采购/销售一致）

- `DRAFT`：草稿（不影响库存）
- `APPROVED`：已审核（已影响库存）
- `CANCELLED`：已作废（不可再审核）

允许的流转：
- `DRAFT -> APPROVED`
- `DRAFT -> CANCELLED`
- （可选）`APPROVED -> DRAFT`：反审核，必须写反向库存流水

### 10.2 数据库表结构（核心）

以下为 V5 迁移新增的核心表：

#### erp_purchase_order（采购单头）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGSERIAL | 主键 |
| tenant_id | BIGINT | 租户 ID |
| order_no | VARCHAR(64) | 单号（租户内唯一） |
| status | VARCHAR(20) | 状态（DRAFT/APPROVED/CANCELLED） |
| supplier_id | BIGINT | 供应商 ID |
| total_amount | NUMERIC(18,2) | 总金额 |
| total_amount_excl_tax | NUMERIC(18,2) | 未税总金额 |
| total_tax_amount | NUMERIC(18,2) | 税额合计 |
| total_amount_incl_tax | NUMERIC(18,2) | 含税总金额 |
| version | BIGINT | 乐观锁版本号 |
| approved_by | VARCHAR(100) | 审核人 |
| approved_at | TIMESTAMPTZ | 审核时间 |
| unapproved_by | VARCHAR(100) | 反审核人 |
| unapproved_at | TIMESTAMPTZ | 反审核时间 |
| cancelled_by | VARCHAR(100) | 作废人 |
| cancelled_at | TIMESTAMPTZ | 作废时间 |
| remark | VARCHAR(500) | 备注 |
| created_at | TIMESTAMPTZ | 创建时间 |
| updated_at | TIMESTAMPTZ | 更新时间 |

#### erp_purchase_order_item（采购单明细）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGSERIAL | 主键 |
| tenant_id | BIGINT | 租户 ID |
| order_id | BIGINT | 采购单 ID |
| product_id | BIGINT | 商品 ID |
| product_code | VARCHAR(100) | 商品编码快照 |
| product_name | VARCHAR(200) | 商品名称快照 |
| warehouse_id | BIGINT | 仓库 ID |
| location_id | BIGINT | 库位 ID |
| qty | NUMERIC(18,4) | 数量 |
| price | NUMERIC(18,4) | 单价 |
| price_incl_tax | NUMERIC(18,4) | 含税单价 |
| amount | NUMERIC(18,2) | 金额 |
| amount_incl_tax | NUMERIC(18,2) | 含税金额 |
| tax_rate | NUMERIC(6,4) | 税率 |
| tax_amount | NUMERIC(18,2) | 税额 |
| sort_no | INT | 排序 |
| remark | VARCHAR(500) | 备注 |
| created_at | TIMESTAMPTZ | 创建时间 |
| updated_at | TIMESTAMPTZ | 更新时间 |

#### erp_sale_order（销售单头）

结构与采购单头一致，`supplier_id` 替换为 `customer_id`。

#### erp_sale_order_item（销售单明细）

结构与采购单明细一致（含 `warehouse_id` / `location_id` / `sort_no`）。

#### erp_stock_txn（库存流水）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGSERIAL | 主键 |
| tenant_id | BIGINT | 租户 ID |
| txn_no | VARCHAR(100) | 流水号（租户内唯一） |
| biz_type | VARCHAR(50) | 业务类型 |
| biz_id | BIGINT | 业务单据 ID |
| biz_item_id | BIGINT | 业务明细 ID |
| product_id | BIGINT | 商品 ID |
| warehouse_id | BIGINT | 仓库 ID |
| location_id | BIGINT | 库位 ID |
| qty_delta | NUMERIC(18,4) | 变更数量（正负） |
| qty_before | NUMERIC(18,4) | 变更前数量 |
| qty_after | NUMERIC(18,4) | 变更后数量 |
| operator | VARCHAR(100) | 操作者 |
| operator_id | BIGINT | 操作者 ID |
| remark | VARCHAR(500) | 备注 |
| created_at | TIMESTAMPTZ | 创建时间 |

`biz_type` 建议取值：
- `PURCHASE_APPROVE_IN`
- `SALE_APPROVE_OUT`
- `APPROVE_UNDO`
- `ADJUST`

#### erp_stock_balance（当前库存）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGSERIAL | 主键 |
| tenant_id | BIGINT | 租户 ID |
| product_id | BIGINT | 商品 ID |
| warehouse_id | BIGINT | 仓库 ID |
| location_id | BIGINT | 库位 ID |
| qty_on_hand | NUMERIC(18,4) | 当前库存 |
| updated_by | VARCHAR(100) | 更新人 |
| updated_at | TIMESTAMPTZ | 更新时间 |

#### erp_product（商品）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGSERIAL | 主键 |
| tenant_id | BIGINT | 租户 ID |
| code | VARCHAR(100) | 商品编码 |
| name | VARCHAR(200) | 商品名称 |
| short_name | VARCHAR(100) | 商品简称 |
| spec | VARCHAR(200) | 规格型号 |
| model | VARCHAR(200) | 型号 |
| category_id | BIGINT | 分类 ID |
| unit_id | BIGINT | 单位 ID |
| barcode | VARCHAR(100) | 条码 |
| sku | VARCHAR(100) | SKU |
| brand | VARCHAR(100) | 品牌 |
| origin | VARCHAR(200) | 产地 |
| weight | NUMERIC(18,4) | 重量 |
| volume | NUMERIC(18,4) | 体积 |
| cost_price | NUMERIC(18,4) | 成本价 |
| sale_price | NUMERIC(18,4) | 销售价 |
| tax_rate | NUMERIC(6,4) | 默认税率 |
| safety_stock | NUMERIC(18,4) | 安全库存 |
| is_batch | BOOLEAN | 是否批次管理 |
| shelf_life_days | INT | 保质期(天) |
| is_enabled | BOOLEAN | 是否启用 |
| ext_attrs | JSONB | 扩展属性 |
| remark | VARCHAR(500) | 备注 |
| created_at | TIMESTAMPTZ | 创建时间 |
| updated_at | TIMESTAMPTZ | 更新时间 |

#### erp_customer（客户）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGSERIAL | 主键 |
| tenant_id | BIGINT | 租户 ID |
| code | VARCHAR(100) | 客户编码 |
| name | VARCHAR(200) | 客户名称 |
| short_name | VARCHAR(100) | 客户简称 |
| contact | VARCHAR(100) | 联系人 |
| phone | VARCHAR(50) | 联系电话 |
| mobile | VARCHAR(50) | 联系手机 |
| email | VARCHAR(200) | 邮箱 |
| address | VARCHAR(500) | 地址 |
| tax_no | VARCHAR(100) | 税号 |
| bank_name | VARCHAR(200) | 开户行 |
| bank_account | VARCHAR(200) | 银行账号 |
| invoice_title | VARCHAR(200) | 发票抬头 |
| payment_terms | VARCHAR(100) | 结算方式 |
| credit_limit | NUMERIC(18,2) | 授信额度 |
| contacts | JSONB | 联系人列表 |
| is_enabled | BOOLEAN | 是否启用 |
| remark | VARCHAR(500) | 备注 |
| created_at | TIMESTAMPTZ | 创建时间 |
| updated_at | TIMESTAMPTZ | 更新时间 |

#### erp_supplier（供应商）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGSERIAL | 主键 |
| tenant_id | BIGINT | 租户 ID |
| code | VARCHAR(100) | 供应商编码 |
| name | VARCHAR(200) | 供应商名称 |
| short_name | VARCHAR(100) | 供应商简称 |
| contact | VARCHAR(100) | 联系人 |
| phone | VARCHAR(50) | 联系电话 |
| mobile | VARCHAR(50) | 联系手机 |
| email | VARCHAR(200) | 邮箱 |
| address | VARCHAR(500) | 地址 |
| tax_no | VARCHAR(100) | 税号 |
| bank_name | VARCHAR(200) | 开户行 |
| bank_account | VARCHAR(200) | 银行账号 |
| payment_terms | VARCHAR(100) | 结算方式 |
| contacts | JSONB | 联系人列表 |
| is_enabled | BOOLEAN | 是否启用 |
| remark | VARCHAR(500) | 备注 |
| created_at | TIMESTAMPTZ | 创建时间 |
| updated_at | TIMESTAMPTZ | 更新时间 |

#### erp_warehouse（仓库）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGSERIAL | 主键 |
| tenant_id | BIGINT | 租户 ID |
| code | VARCHAR(100) | 仓库编码 |
| name | VARCHAR(200) | 仓库名称 |
| address | VARCHAR(500) | 地址 |
| manager | VARCHAR(100) | 负责人 |
| phone | VARCHAR(50) | 联系电话 |
| is_enabled | BOOLEAN | 是否启用 |
| remark | VARCHAR(500) | 备注 |
| created_at | TIMESTAMPTZ | 创建时间 |
| updated_at | TIMESTAMPTZ | 更新时间 |

#### erp_location（库位）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGSERIAL | 主键 |
| tenant_id | BIGINT | 租户 ID |
| warehouse_id | BIGINT | 仓库 ID |
| code | VARCHAR(100) | 库位编码 |
| name | VARCHAR(200) | 库位名称 |
| aisle | VARCHAR(50) | 巷道 |
| rack | VARCHAR(50) | 货架 |
| bin | VARCHAR(50) | 货位 |
| is_enabled | BOOLEAN | 是否启用 |
| remark | VARCHAR(500) | 备注 |
| created_at | TIMESTAMPTZ | 创建时间 |
| updated_at | TIMESTAMPTZ | 更新时间 |

#### erp_category（分类）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGSERIAL | 主键 |
| tenant_id | BIGINT | 租户 ID |
| code | VARCHAR(100) | 分类编码 |
| name | VARCHAR(200) | 分类名称 |
| parent_id | BIGINT | 父级分类 ID |
| level | INT | 层级 |
| sort_no | INT | 排序 |
| is_enabled | BOOLEAN | 是否启用 |
| remark | VARCHAR(500) | 备注 |
| created_at | TIMESTAMPTZ | 创建时间 |
| updated_at | TIMESTAMPTZ | 更新时间 |

#### erp_unit（单位）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGSERIAL | 主键 |
| tenant_id | BIGINT | 租户 ID |
| code | VARCHAR(100) | 单位编码 |
| name | VARCHAR(200) | 单位名称 |
| symbol | VARCHAR(50) | 单位符号 |
| precision | INT | 小数精度 |
| is_enabled | BOOLEAN | 是否启用 |
| remark | VARCHAR(500) | 备注 |
| created_at | TIMESTAMPTZ | 创建时间 |
| updated_at | TIMESTAMPTZ | 更新时间 |

### 10.3 ERP 接口（已实现）

#### 10.3.1 基础资料（商品 / 客户 / 供应商 / 仓库 / 库位 / 分类 / 单位）

统一规则：
- `list`：`GET /api/erp/{resource}`  
- `page`：`GET /api/erp/{resource}/page`  
- `get`：`GET /api/erp/{resource}/{id}`  
- `create`：`POST /api/erp/{resource}`  
- `update`：`PUT /api/erp/{resource}/{id}`  
- `delete`：`DELETE /api/erp/{resource}/{id}`

资源与权限对照：
- 商品：`products`（`PERM_erp-product:view/add/edit/delete`）
- 客户：`customers`（`PERM_erp-customer:view/add/edit/delete`）
- 供应商：`suppliers`（`PERM_erp-supplier:view/add/edit/delete`）
- 仓库：`warehouses`（`PERM_erp-warehouse:view/add/edit/delete`）
- 库位：`locations`（`PERM_erp-location:view/add/edit/delete`）
- 分类：`categories`（`PERM_erp-category:view/add/edit/delete`）
- 单位：`units`（`PERM_erp-unit:view/add/edit/delete`）

分页公共参数：
- `page`：页码（默认 1）
- `size`：每页数量（默认 20）
- `keyword`：关键词（编码/名称/联系人等）
- `enabled`：启用状态（true/false，可选）
- `categoryId`：仅商品支持
- `warehouseId`：仅库位支持

基础资料响应示例（以商品为例）：
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "total": 1,
    "page": 1,
    "size": 20,
    "items": [
      {
        "id": 1,
        "tenantId": 1,
        "code": "P001",
        "name": "螺丝",
        "categoryId": 10,
        "unitId": 3,
        "isEnabled": true,
        "createdAt": "2026-01-28T10:00:00Z",
        "updatedAt": "2026-01-28T10:00:00Z"
      }
    ]
  }
}
```

#### 10.3.2 采购单（审核即入库）

- 基础路径：`/api/erp/purchase-orders`
- 权限：`PERM_erp-purchase:*`

1) 新增草稿单  
- 方法：`POST`  
- 路径：`/api/erp/purchase-orders`

请求示例：
```json
{
  "orderNo": "PO20260128001",
  "supplierId": 1001,
  "items": [
    {
      "productId": 1,
      "warehouseId": 2,
      "locationId": 3,
      "qty": 10,
      "price": 2.5,
      "taxRate": 0.13,
      "sortNo": 1,
      "remark": "首批采购"
    }
  ],
  "remark": "采购草稿"
}
```
说明：
- `orderNo` 可留空，后端将自动生成单号

2) 分页查询  
- 方法：`GET`  
- 路径：`/api/erp/purchase-orders/page`  
- 参数：`page/size/keyword/status/supplierId/startAt/endAt`

3) 详情  
- 方法：`GET`  
- 路径：`/api/erp/purchase-orders/{id}`

4) 编辑草稿  
- 方法：`PUT`  
- 路径：`/api/erp/purchase-orders/{id}`  
- 规则：仅 `DRAFT`

5) 作废  
- 方法：`POST`  
- 路径：`/api/erp/purchase-orders/{id}/cancel`  
- 规则：`DRAFT` 或 `APPROVED`  
- 行为：若已审核则回滚库存并写流水

6) 审核（直接入库）  
- 方法：`POST`  
- 路径：`/api/erp/purchase-orders/{id}/approve`  
- 规则：仅 `DRAFT`  
- 行为：写库存流水 + 更新当前库存

7) 反审核  
- 方法：`POST`  
- 路径：`/api/erp/purchase-orders/{id}/unapprove`  
- 规则：仅 `APPROVED`  
- 行为：写反向流水 + 回滚库存

#### 10.3.3 销售单（审核即出库）

- 基础路径：`/api/erp/sale-orders`
- 权限：`PERM_erp-sale:*`
- 接口与采购单对称：
  - `POST /api/erp/sale-orders`
  - `GET /api/erp/sale-orders/page`（参数：`page/size/keyword/status/customerId/startAt/endAt`）
  - `GET /api/erp/sale-orders/{id}`
  - `PUT /api/erp/sale-orders/{id}`（仅 DRAFT）
  - `POST /api/erp/sale-orders/{id}/cancel`（DRAFT 或 APPROVED）
  - `POST /api/erp/sale-orders/{id}/approve`（关键）

说明：
- `orderNo` 可留空，后端将自动生成单号

销售审核硬规则：
- 审核前必须校验库存充足（库存不足直接报错）

#### 10.3.4 库存查询（台账 + 当前库存）

- 基础路径：`/api/erp/stock`

1) 当前库存分页  
- 方法：`GET`  
- 路径：`/api/erp/stock/balances/page`  
- 权限：`PERM_erp-stock:view`  
- 参数：`page/size/productId/warehouseId/locationId`

2) 库存流水分页  
- 方法：`GET`  
- 路径：`/api/erp/stock/txns/page`  
- 权限：`PERM_erp-stock-txn:view`  
- 参数：`page/size/bizType/bizId/productId`

对应菜单：
- 库存台账：`/erp/stocks`（`erp-stock:view`）
- 库存流水：`/erp/stock-txns`（`erp-stock-txn:view`）

#### 10.3.5 单号生成配置（SystemConfig）

- `erp.order.no.purchase.prefix`：采购单号前缀（默认 PO）
- `erp.order.no.sale.prefix`：销售单号前缀（默认 SO）
- `erp.order.no.date-format`：日期格式（默认 yyyyMMdd）
- `erp.order.no.seq-length`：序列长度（默认 4）

### 10.4 ERP 权限点（已落库）

基础资料：
- `erp-product:view` / `erp-product:add` / `erp-product:edit` / `erp-product:delete`
- `erp-customer:view` / `erp-customer:add` / `erp-customer:edit` / `erp-customer:delete`
- `erp-supplier:view` / `erp-supplier:add` / `erp-supplier:edit` / `erp-supplier:delete`
- `erp-warehouse:view` / `erp-warehouse:add` / `erp-warehouse:edit` / `erp-warehouse:delete`
- `erp-location:view` / `erp-location:add` / `erp-location:edit` / `erp-location:delete`
- `erp-category:view` / `erp-category:add` / `erp-category:edit` / `erp-category:delete`
- `erp-unit:view` / `erp-unit:add` / `erp-unit:edit` / `erp-unit:delete`

单据：
- `erp-purchase:view` / `erp-purchase:add` / `erp-purchase:edit` / `erp-purchase:cancel` / `erp-purchase:approve` / `erp-purchase:unapprove`
- `erp-sale:view` / `erp-sale:add` / `erp-sale:edit` / `erp-sale:cancel` / `erp-sale:approve` / `erp-sale:redflush`

库存：
- `erp-stock:view`
- `erp-stock-txn:view`
