# JWT瘦身与授权上下文拆分设计

> 本文档记录当前仓库 `D:\project` 的认证/授权改造决策：正式采用 2 号模式作为目标方案，并将 3 号模式保留为后续架构演进参考。

## 1. 背景

当前仓库的权限种子总数已经达到 524 条，其中 324 条是 `column:*` 列级权限。现有实现将完整 `permissions` 集合写入 JWT 的 `perms` claim，导致：

- 切换租户后 access token 过大，`Authorization` 请求头触发 `431 Request Header Fields Too Large`
- 登录页如果复用旧 token，也会把超大 `Authorization` 一并带到 `/api/login`
- 细粒度权限被固化到 JWT 生命周期内，撤权和权限变更只能依赖 `authVersion` 间接兜底

当前仓库仍是单体后端主导，前端已经依赖登录/刷新响应中的 `authPayload.permissions` 做菜单、按钮、列显示，因此本次设计的目标不是“一步到位重构为统一认证中心”，而是在不打乱现有前端初始化模型的前提下，拆掉 JWT 中不适合携带的大体积授权数据。

## 2. 决策结论

本次正式采用 2 号模式：

- JWT 中不再携带完整 `permissions`
- 登录和刷新接口不再把授权上下文与 access token 绑定为同一来源
- 前端在恢复会话后，单独请求授权上下文接口，例如 `/api/me/authorizations`
- 后端真实鉴权不依赖 JWT 中的 `permissions`，而是按 `userId + tenantId + authVersion` 从服务端加载完整权限

同时保留 3 号模式文档化说明，作为未来多服务、统一认证中心、实时撤权场景的演进参考。

## 3. 为什么选择 2 号模式

### 3.1 相对 1 号模式的价值

1 号模式虽然改动更小，但仍把“前端授权上下文”耦合在 `/login` 和 `/refresh` 响应体中。对当前仓库来说，这会留下几个结构性问题：

- 登录接口承担身份认证和完整前端授权初始化两种职责
- 刷新接口同时承担 token 续期和前端授权刷新，语义偏重
- 后续如果需要在更多入口恢复权限上下文，仍会继续耦合认证接口

2 号模式把“认证令牌恢复”和“前端授权上下文加载”拆开，边界更清晰：

- `/login`、`/refresh` 只负责登录态建立与续期
- `/api/me/authorizations` 负责返回前端展示和交互所需的授权上下文
- 后端鉴权完全按服务端权限集执行，不再依赖前端是否拿到某份 `permissions`

### 3.2 相对 3 号模式的价值

3 号模式更强，但对当前仓库过重。当前没有独立认证中心、API 网关、统一 introspection 服务，也没有拆分成多个独立资源服务。此时直接引入 reference token 或 opaque token 体系，会把复杂度提前引入，但收益并不足以覆盖改造成本。

2 号模式则能在现有单体结构下获得主要收益：

- 彻底消除超大 JWT 问题
- 权限加载边界清晰
- 保留未来继续进化到 3 号模式的空间

## 4. 2 号模式的目标形态

### 4.1 JWT 负责什么

JWT 只保留稳定且低基数的身份与租户摘要，不再携带完整权限集合。建议保留：

- `sub`
- `uid`
- `iss`
- `iat`
- `exp`
- `jti`
- `av`
- `tid`
- `tcode`
- `utid`
- `utcode`
- `roles` 或最小角色摘要

建议移除：

- `perms`
- 冗余的完整 `user` 扩展块中非必要信息

### 4.2 后端鉴权负责什么

后端在每个受保护请求上完成以下流程：

1. 解析并验证 JWT
2. 提取 `uid`、`tid`、`utid`、`av`
3. 校验 `authVersion`
4. 从服务端权限加载器获取完整权限集
5. 组装 `GrantedAuthority`
6. 将角色、权限、租户上下文写入当前请求安全上下文

也就是说：

- JWT 不再是“完整授权清单”
- JWT 只是“可信身份摘要 + 授权版本提示”
- 真正的授权结果以服务端当前权限集为准

### 4.3 前端负责什么

前端不再从 `/login` 或 `/refresh` 响应直接假定已经拿到完整授权上下文，而是分两步：

1. 登录成功或刷新成功后，先只更新 token
2. 再请求 `/api/me/authorizations`

该接口返回：

- `user`
- `roles`
- `permissions`
- `tenantId`
- `tenantCode`
- `userTenantId`
- `userTenantCode`
- 可选的菜单版本、授权版本等扩展信息

前端继续使用这份上下文做：

- 菜单过滤
- 按钮显隐
- 列权限判断
- 超级管理员租户切换入口控制

### 4.4 切租户时的行为

切租户后流程应改为：

1. `/api/tenants/switch` 返回新的 access token
2. 前端只更新 token，不再依赖该接口顺带返回完整 `permissions`
3. 前端立刻重新请求 `/api/me/authorizations`
4. 用新租户上下文刷新菜单、列配置、页面权限状态

这样做的好处是：

- access token 不会因不同租户下权限差异而再次膨胀
- 授权上下文来源单一
- 登录、刷新、切租户的前端初始化路径统一

## 5. 关键接口边界

### 5.1 建议保留的认证接口职责

- `POST /api/login`
  - 只负责认证成功、签发 token、写 refreshToken cookie
  - 返回最小登录结果，不承载完整前端授权上下文

- `POST /api/refresh`
  - 只负责续期 token、轮换 refreshToken
  - 返回最小登录结果，不承载完整前端授权上下文

- `POST /api/logout`
  - 只负责登出和 refreshToken 撤销

- `POST /api/tenants/switch`
  - 只负责切换当前租户并签发新 token
  - 不再承担完整授权上下文返回职责

### 5.2 新增授权上下文接口

建议新增：

- `GET /api/me/authorizations`

返回内容建议为：

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "user": {
      "id": 1,
      "username": "admin",
      "role": "super_admin",
      "avatar": null,
      "roles": ["super_admin", "admin"]
    },
    "permissions": ["tenant:switch", "menu:view"],
    "authVersion": 12,
    "tenantId": 2,
    "tenantCode": "tenant-b",
    "userTenantId": 1,
    "userTenantCode": "default"
  }
}
```

该接口的定位是：

- 给前端提供显示和交互所需的授权上下文
- 不改变后端真实鉴权策略
- 后续如果需要，也可独立扩展为 `/api/me/profile`、`/api/me/menus`、`/api/me/column-authorizations`

## 6. 服务端权限加载策略

### 6.1 推荐实现

推荐使用：

- 一级：进程内短缓存
- 二级：可选 Redis
- 三级：数据库

缓存 key 建议：

- `authz:{tenantId}:{userId}:{authVersion}`

这样当角色、权限、用户授权版本变化时：

- `authVersion` 增加
- 旧缓存自然失效
- 无需复杂的逐条清缓存逻辑

### 6.2 当前仓库的适配原则

保留已有 `super_admin` 无条件 bypass 规则，不因为权限加载链路调整而改变既有治理边界。

也就是说：

- `super_admin` 仍应在前端 `hasPermission()` 中优先短路
- 后端 `@PreAuthorize` 或权限服务中仍保留早期 bypass
- 2 号模式只改变“权限从哪里加载”，不改变“权限规则本身”

## 7. 对列权限的处理原则

当前 324 条 `column:*` 权限不应再进入 JWT，但本次不要求立刻删除它们。

本阶段处理原则：

- 保留现有 `column:*` 权限体系
- 将其从 JWT claim 中移除
- 继续通过授权上下文接口返回给前端
- 后端按服务端权限集完成敏感列相关控制

后续可再做第二阶段治理，把 `column:*` 分为两类：

1. 真正敏感列权限
   - 成本
   - 毛利
   - 联系方式
   - 银行账户
   - 税号
   - 审计敏感字段

2. 仅展示偏好列
   - 更适合做租户/角色/用户级列配置，而不是正式权限码

这属于后续权限模型优化，不属于本次 JWT 瘦身与授权上下文拆分的必做范围。

## 8. 风险与控制点

### 8.1 前端初始化时序风险

如果前端在 token 更新后、授权上下文尚未加载完成前就立即请求菜单或业务接口，可能出现：

- 菜单刷新顺序错乱
- `authStore` 暂时为空导致误判
- 旧权限短暂闪现

控制方式：

- 明确把“token 建立成功”和“authorizations 加载完成”区分为两个阶段
- 在 `authStore` 增加授权上下文初始化完成标记
- 菜单刷新与需要权限的 UI 初始化依赖该标记

### 8.2 刷新接口时序风险

如果 `/refresh` 成功后前端未同步拉取新授权上下文，前端显示可能滞后于后端实际权限。

控制方式：

- `restoreSession()` 成功后总是串行请求 `/api/me/authorizations`
- 刷新成功但授权上下文加载失败时，按登录态不完整处理

### 8.3 切租户时序风险

切租户后必须重新拉取：

- 菜单
- 授权上下文
- 租户相关缓存
- 列配置/表格偏好依赖项

否则会出现“token 已切换，UI 仍停留在旧租户权限”的状态漂移。

## 9. 测试重点

本设计落地后至少应验证以下场景：

1. 普通登录
   - `/login` 返回最小 token 结果
   - `/api/me/authorizations` 返回完整前端授权上下文

2. 刷新续期
   - `/refresh` 返回最小 token 结果
   - 前端随后能恢复完整授权上下文

3. 超级管理员切租户
   - 新 token 体积显著缩小
   - 切租户后菜单和列权限正确刷新

4. 权限变更
   - `authVersion` 变化后旧缓存不再使用
   - 后端鉴权以新权限为准

5. 登录页残留旧 token
   - `/login` 不再附带旧 `Authorization`

6. 431 回归验证
   - `/api/login`
   - `/api/menus`
   - `/api/tenants`
   - `/api/tenants/switch`
   均不再因头部过大触发 431

## 10. 实施建议

推荐按以下顺序推进：

1. 先收缩 JWT claim
2. 再改后端鉴权链路，去掉 JWT 中 `perms` 依赖
3. 新增 `/api/me/authorizations`
4. 前端登录、刷新、切租户流程切换为“两步式初始化”
5. 最后补齐缓存、回归测试和异常处理

这样可以把风险拆开，避免一次性重构登录、刷新、菜单、列权限、租户切换所有链路。

---

## 附录 A：3 号模式说明

3 号模式指的是把当前“JWT 自带大部分授权信息”的模式，升级为“令牌只作为会话引用或轻量身份摘要，完整授权上下文统一由服务端托管”的体系。

### A.1 3 号模式的两种常见实现

#### 方案 A：opaque/reference token

access token 不再是可直接承载大块 claim 的 JWT，而是一个短 token id。资源服务收到请求后，根据 token id 去认证中心或统一会话存储查询完整上下文：

- userId
- tenantId
- authTenantId
- roles
- permissions
- authVersion
- session 状态

#### 方案 B：JWT + introspection/context service

access token 可以仍是 JWT，但只保留很少 claim。资源服务在完成基础验签后，继续调用统一授权上下文接口或 introspection 服务，拉取完整用户授权状态。

### A.2 3 号模式的优势

- token 很小，不会出现大 header 问题
- 权限变更和撤权更实时
- 适合多服务共享统一认证中心
- 更容易实现强制下线、全局撤销、统一审计

### A.3 3 号模式的代价

- 需要独立认证中心或统一 token context 服务
- 请求链路更复杂
- 运行时更依赖中心服务或共享缓存
- 对当前单体仓库来说改造面明显过大

### A.4 3 号模式适用时机

当满足以下条件时，再考虑 3 号模式更合适：

- 后端准备拆分成多个独立服务
- 需要统一登录态和统一撤权
- 需要秒级全局生效的授权变更
- 已有成熟 Redis / 网关 / 认证中心基础设施

### A.5 本仓库当前对 3 号模式的判断

3 号模式不是错误方向，但不是本轮最值得投入的方案。当前仓库更适合先完成 2 号模式，把 JWT 瘦身和授权边界梳理清楚，再视系统演进情况判断是否需要继续升级。
