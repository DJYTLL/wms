# 3号模式参考方案：面向微服务的认证与授权上下文设计

> 本文档是独立的长期参考方案，描述“3 号模式”在未来微服务体系中的推荐落地方式。它不要求当前单体仓库立即实施，目标是为后续拆分认证中心、资源服务、网关与统一授权上下文时提供可复用设计。

## 1. 文档目的

本文档回答一个单独的问题：

在未来微服务体系下，如果不再把完整权限集合塞进 JWT，而是采用“令牌只做会话引用或轻量身份摘要，完整授权上下文由服务端统一托管”的模式，应该如何设计？

本文档关注的是：

- 微服务场景下的认证中心职责
- access token 的形态选择
- 资源服务如何获取完整授权上下文
- 多租户、切租户、撤权、强制下线的处理方式
- 缓存、可用性、审计、性能权衡

本文档不绑定当前 `D:\project` 的实现细节，但会尽量保持与现有业务模型兼容：

- `userId`
- `tenantId`
- `userTenantId`
- `roles`
- `permissions`
- `authVersion`
- `super_admin` 顶层 bypass

## 2. 3 号模式定义

3 号模式的核心原则只有一句话：

**不要把完整授权信息固化在 access token 中，而是让 token 只承担身份引用或轻量身份摘要，完整授权上下文由服务端统一托管并按请求解析。**

它和传统“胖 JWT”模式的区别如下：

### 2.1 胖 JWT 模式

access token 中直接携带：

- 用户信息
- 当前租户
- 角色列表
- 完整权限列表
- 扩展业务字段

资源服务拿到 JWT 后，本地验签即可完成大部分鉴权。

优点：

- 快
- 去中心化
- 本地即可判定

缺点：

- token 容量膨胀
- 撤权不实时
- 多租户切换复杂
- 细粒度权限生命周期与 token 过期时间绑定

### 2.2 3 号模式

access token 中只保留：

- 一个短 token id，或
- 一组最小身份摘要

资源服务无法仅凭 token 自己获得完整权限，而是要去统一授权上下文来源获取：

- 这个 token 属于谁
- 当前会话在哪个租户下
- 用户原始租户是什么
- 拥有哪些角色
- 拥有哪些权限
- 当前授权版本是否有效

这意味着：

- token 小
- 权限可实时失效
- 会话与授权状态由服务端集中控制

## 3. 适用场景

3 号模式更适合以下场景：

1. 已经拆成多个资源服务
2. 多个服务都需要统一登录态和统一撤权
3. 权限集合大且变化频繁
4. 多租户切换需要在多个服务之间保持一致
5. 需要支持强制下线、会话撤销、风险冻结
6. 需要全局审计认证与授权链路

如果系统仍是单体应用，或者只有一个后端服务，3 号模式通常不是第一选择，因为它引入的基础设施复杂度较高。

## 4. 两种实现路线

3 号模式常见有两条路线。

### 4.1 路线 A：opaque/reference token

access token 是一个随机字符串，例如：

```text
at_8f4e7c1b2d...
```

它本身不承载完整用户上下文，只是一个服务端记录的引用键。

资源服务收到请求后，调用认证中心或共享 token 存储查询完整上下文。

这种路线的特点是：

- token 最短
- 完全中心化
- 所有实时授权控制最直接
- 每个受保护请求都需要依赖服务端上下文查询

### 4.2 路线 B：轻量 JWT + introspection/context service

access token 仍是 JWT，但内容极简，只保留：

- `sub`
- `uid`
- `tid`
- `utid`
- `jti`
- `av`
- `iss`
- `aud`
- `exp`

资源服务先本地验签，再视需要调用统一 introspection 或 authorization context 服务，拉取完整授权状态。

这种路线的特点是：

- 比 opaque token 更容易兼容现有 JWT 生态
- 可以保留网关层的基础身份判定能力
- 细授权仍然由中心上下文控制

### 4.3 推荐选择

如果是从单体逐步走向微服务，优先推荐：

**轻量 JWT + 授权上下文服务**

理由：

- 便于从现有 JWT 模式平滑迁移
- 可先在单体内抽象，再逐步外置
- 网关与资源服务对 JWT 验签逻辑改动较小
- 比 opaque token 更容易分阶段演进

如果未来已经形成稳定的统一认证中心和统一会话存储，再进一步收敛为 opaque/reference token 也可以。

## 5. 推荐总体架构

建议将体系拆成四层职责。

### 5.1 Authentication Service

负责：

- 登录认证
- refresh token 轮换
- logout
- 会话创建与撤销
- access token 签发
- token introspection

典型接口：

- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`
- `POST /auth/introspect`
- `POST /auth/revoke`

### 5.2 Authorization Context Service

负责：

- 返回会话级授权上下文
- 返回用户在当前租户下的角色与权限
- 处理授权版本变化
- 支持按 sessionId、userId、tenantId 查询

典型接口：

- `GET /authorization-context/{sessionId}`
- `GET /authorization-context/by-token/{jti}`
- `GET /authorization-context/me`

### 5.3 API Gateway

负责：

- 验证 token 基本合法性
- 透传或注入 trace 信息
- 可选：缓存 introspection 结果
- 可选：屏蔽明显非法请求

网关不建议承担全部细粒度鉴权逻辑，否则授权模型会过度耦合在网关层。

### 5.4 Resource Services

负责：

- 基于上下文完成真正业务授权
- 维护本服务内的领域权限规则
- 记录审计日志

资源服务应能回答：

- 当前用户是谁
- 当前租户是谁
- 当前会话是否跨租户
- 是否拥有某角色/某权限
- 是否命中特殊 bypass，例如 `super_admin`

## 6. 核心数据模型

推荐在服务端维护统一的 `AuthorizationContext`。

```json
{
  "sessionId": "sess_01J...",
  "tokenId": "jti_01J...",
  "userId": 1001,
  "username": "admin",
  "authTenantId": 1,
  "authTenantCode": "default",
  "tenantId": 3,
  "tenantCode": "tenant-c",
  "crossTenant": true,
  "roles": ["super_admin", "admin"],
  "permissions": ["tenant:switch", "menu:view"],
  "authVersion": 42,
  "issuedAt": "2026-05-24T10:00:00Z",
  "expiresAt": "2026-05-24T12:00:00Z",
  "status": "ACTIVE"
}
```

### 6.1 字段说明

- `sessionId`
  - 服务端会话主键

- `tokenId`
  - 当前 access token 的唯一标识，通常对应 `jti`

- `authTenantId`
  - 用户原始登录租户或认证租户

- `tenantId`
  - 当前业务租户

- `crossTenant`
  - 是否处于跨租户会话

- `roles`
  - 粗粒度角色摘要

- `permissions`
  - 完整细粒度权限集

- `authVersion`
  - 权限/角色变更版本号

- `status`
  - `ACTIVE` / `REVOKED` / `LOCKED` / `EXPIRED`

## 7. token 设计建议

### 7.1 access token

如果采用轻量 JWT，建议只保留：

- `iss`
- `aud`
- `sub`
- `uid`
- `tid`
- `utid`
- `jti`
- `av`
- `iat`
- `exp`

不要放：

- 完整 `permissions`
- 大量业务对象
- 大块 profile
- 冗余菜单信息

### 7.2 refresh token

refresh token 推荐：

- 长随机串
- 服务端持久化哈希
- 绑定用户、会话、认证租户、设备信息
- 每次刷新轮换

### 7.3 session

建议把 session 作为一等公民，而不仅仅依赖 refresh token。

session 记录应支持：

- 手工撤销
- 风险冻结
- 跨租户状态变更
- 多终端并存
- 最后活跃时间

## 8. 请求处理流程

### 8.1 登录流程

1. 用户提交用户名、密码、租户标识
2. Authentication Service 验证身份
3. 生成 session 记录
4. 生成 access token 与 refresh token
5. 生成并缓存 `AuthorizationContext`
6. 返回 access token，写入 refresh token cookie

### 8.2 受保护请求流程

以“轻量 JWT + authorization context service”为例：

1. API Gateway 验签 access token
2. Resource Service 提取 `jti`、`uid`、`tid`、`av`
3. 通过本地缓存或统一 context service 加载 `AuthorizationContext`
4. 检查 session 状态是否可用
5. 检查 `authVersion` 是否匹配
6. 组装业务安全上下文
7. 执行业务授权判断

### 8.3 刷新流程

1. 客户端提交 refresh token
2. Authentication Service 校验 refresh token 是否有效
3. 校验会话状态
4. 轮换 refresh token
5. 签发新的 access token
6. 更新 session 与 context 过期时间

### 8.4 登出流程

1. 客户端请求登出
2. Authentication Service 撤销 refresh token
3. 将 session 状态改为 `REVOKED`
4. 清理相关缓存
5. 后续 access token 即使未过期，也因 session 无效而被拒绝

## 9. 多租户与切租户设计

3 号模式非常适合多租户切换，但必须把“认证租户”和“当前业务租户”分清。

### 9.1 设计原则

- `authTenantId` 表示用户真实登录归属
- `tenantId` 表示当前业务上下文
- 跨租户会话必须被显式标记

### 9.2 切租户推荐流程

1. 用户发起切租户请求
2. Authorization Service 验证是否允许跨租户
3. 更新或重建当前 session 的 `tenantId`
4. 重建当前 `AuthorizationContext`
5. 视策略决定是否签发新 access token

推荐做法：

- 重新签发新的轻量 access token
- `jti` 更新
- 旧 token 立即失效

这样审计边界更清晰，也便于排查问题。

## 10. 撤权与强制下线

3 号模式最有价值的部分之一，就是撤权可以变成“服务端状态变化”，而不是等待旧 JWT 过期。

### 10.1 权限变更

当角色或权限改变时：

1. 更新用户授权版本 `authVersion`
2. 标记相关 session 的 context 失效
3. 清理对应缓存
4. 后续请求重新拉取 context

### 10.2 强制下线

当用户被封禁或管理员强制踢下线时：

1. 将 session 标记为 `REVOKED` 或 `LOCKED`
2. 撤销 refresh token
3. 清理 context 缓存
4. 后续所有 access token 请求立即失败

## 11. 缓存设计

3 号模式不代表每个请求都直接打数据库。标准做法是多层缓存。

### 11.1 建议缓存层级

1. 资源服务本地短缓存
2. Redis 共享缓存
3. 数据库或权威授权服务

### 11.2 推荐缓存 key

- `session:{sessionId}`
- `token:{jti}`
- `authctx:{sessionId}:{authVersion}`

### 11.3 失效策略

以下事件必须触发失效：

- 权限变更
- 角色变更
- 切租户
- 登出
- 强制下线
- refresh token 轮换导致 session token 关系变化

## 12. super_admin 处理原则

即使进入 3 号模式，也应保留业务上清晰的顶层 bypass 规则。

推荐原则：

- `super_admin` 仍是授权上下文中的显式角色
- 资源服务在领域权限判断前保留早期短路
- 不要把 `super_admin` 降级成大量普通 permissions 的组合

这样能避免后续授权策略越来越复杂时丢失最高权限的可审计边界。

## 13. 审计要求

微服务下采用 3 号模式时，审计比单体更重要。

每个请求至少应记录：

- `requestId`
- `sessionId`
- `tokenId`
- `userId`
- `authTenantId`
- `tenantId`
- `crossTenant`
- `resourceService`
- `path`
- `method`
- `authorizationDecision`

这样才能在跨服务情况下追踪：

- 某次请求为什么被放行
- 某次跨租户操作的授权来源是什么
- 某个 token 是否在撤销后仍被错误接受

## 14. 性能与可用性权衡

### 14.1 3 号模式的主要收益

- 小 token
- 实时撤权
- 多服务统一会话控制
- 多租户上下文更可控

### 14.2 3 号模式的主要成本

- 请求链路变长
- 更依赖中心服务或共享缓存
- 系统可用性模型更复杂

### 14.3 关键控制点

要让 3 号模式真正可用，必须做到：

1. introspection/context 查询有缓存
2. context 服务高可用
3. session 与 token 撤销语义一致
4. 网关、认证服务、资源服务之间时钟偏差可控
5. 审计链路完整

## 15. 推荐演进路径

如果未来从当前仓库逐步演进到 3 号模式，推荐分四步走。

### 第一步：先做轻量 JWT

- 删除 JWT 中的 `permissions`
- 保留身份与租户摘要
- 后端改为服务端权限加载

### 第二步：抽象授权上下文服务

- 在单体中先抽出统一 `AuthorizationContextProvider`
- 所有鉴权统一从这里取上下文

### 第三步：外置到共享缓存/独立服务

- 把 context 从单体内部实现迁到 Redis 或独立 auth service

### 第四步：微服务化

- 网关统一验签
- 各资源服务统一获取授权上下文
- 会话与撤权逻辑集中治理

## 16. 对未来微服务方案的最终建议

如果以后你真的要在微服务中使用 3 号模式，我建议采用：

**轻量 JWT + 独立 Authorization Context Service + 多层缓存**

这是最平衡的一条路线，原因是：

- 比胖 JWT 更安全、可控
- 比纯 opaque token 更容易渐进迁移
- 比把全部授权逻辑放在网关更清晰
- 对多租户、切租户、撤权、强制下线支持更自然

一句话总结：

**3 号模式不是“不要 JWT”，而是“不要把 JWT 当作完整授权数据库”；JWT 只承担身份摘要，完整授权状态由服务端统一托管并按请求解析。**
