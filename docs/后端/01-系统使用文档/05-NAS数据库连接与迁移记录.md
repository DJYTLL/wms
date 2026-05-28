# NAS 数据库连接与迁移记录

本文档记录 `wms-backend` 当前使用的 NAS PostgreSQL 数据库、迁移结果，以及后续维护时必须注意的边界条件。

## 适用范围

- 后端模块：`D:\project\wms-backend`
- 当前开发环境数据库：NAS 上的 PostgreSQL
- 当前项目使用的数据库名：`wms_backend`

## 当前连接方式

### 开发环境

后端现在默认使用 `dev` profile。

- 公共配置：`wms-backend/src/main/resources/application.properties`
- 开发环境数据库配置：`wms-backend/src/main/resources/application-dev.properties`

当前开发环境连接目标：

- 主机：`duaoyunxuan.synology.me`
- 端口：`5433`
- 数据库：`wms_backend`
- 用户名：`erp`

当前开发环境密码请以 `application-dev.properties` 中的实际值为准。

### 生产环境

生产环境数据库配置不再写死在仓库中。

- 文件：`wms-backend/src/main/resources/application-prod.properties`
- 必须提供的环境变量：
  - `SPRING_DATASOURCE_URL`
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD`

## NAS 容器说明

NAS 上原本已经存在一个与本项目无关的 PostgreSQL 容器，本项目不能复用那个库。

本次确认过的相关容器：

- `babycare-postgres`
  - 现有其他项目容器
  - 容器内端口 `5432/tcp`
  - 不是本项目后端的数据源
- `erp-postgres`
  - 本次迁移使用的目标容器
  - 对外端口映射：`5433 -> 5432`
  - 当前 WMS 使用的数据库：`wms_backend`

## 迁移记录

本地源库：

- 源容器：`wms-backend-pgsql`
- 源数据库：`wms_backend`

远端目标库：

- 目标主机：`duaoyunxuan.synology.me`
- 目标端口：`5433`
- 目标数据库：`wms_backend`

迁移结果：

- 已将本地 Docker PostgreSQL 中的 `wms_backend` 迁移到 NAS 上的 `wms_backend`
- 远端数据库名已与本地项目统一，后续保持使用 `wms_backend`
- 已做抽样行数校验，以下表本地与远端一致：
  - `app_tenant = 5`
  - `app_user = 9`
  - `erp_product = 152`
  - `erp_sale_order = 81`

## 必须记住的边界条件

### 1. 数据库名保持为 `wms_backend`

现在项目配置、远端库名、迁移记录都已经统一到 `wms_backend`。

除非后续有明确的新迁移方案，否则不要再把本项目切回旧的占位库名 `erp`。

### 2. 远端保留了 `postgres` 角色用于兼容导入

迁移过程中，导出的 SQL 包含了对象 owner 指向 `postgres`。

为了不重写整份 dump，远端实例中补了 `postgres` 角色用于兼容导入。

这意味着：

- 应用运行时当前使用的是 `erp`
- 远端实例里还存在一个 `postgres` 角色作为兼容角色

后续不要随意删除这个角色，除非已经重新梳理对象 owner 和导入方案。

### 3. JDBC 导入不等于 `psql` 导入

这次迁移已经验证，普通 JDBC 直接执行 SQL 时，不能原样处理以下内容：

- `\restrict` / `\unrestrict` 这种 `psql` 元命令
- `COPY FROM STDIN` 这种数据导入段

如果后面还要做整库迁移，优先考虑两种方案：

- 用 `pg_dump --inserts --column-inserts` 生成可回放 SQL
- 在网络和 SSH 条件允许时，直接走 `psql` / `pg_restore`

### 4. 外部访问受 IPv6 和 SSH 策略影响

本次操作过程中，NAS 域名解析到了 IPv6 地址。

实际影响是：

- 本机 Docker 容器内的客户端无法稳定直连这个 IPv6 目标
- NAS 的 SSH 端口转发策略也限制了直接隧道恢复

如果以后还要做自动化恢复或远程导库，优先先确认：

- 是否有可用 IPv4
- 是否能在目标主机本机执行 `psql` / `pg_restore`

## 后续排查建议

以后如果怀疑 NAS 数据库连接异常，优先按下面顺序检查：

1. `application-dev.properties` 是否仍然指向 `duaoyunxuan.synology.me:5433/wms_backend`
2. NAS 上 `erp-postgres` 容器是否运行
3. 远端 `wms_backend` 是否还能正常连接
4. 当前运行用户是否仍有访问所需 schema 对象的权限

## 生产部署提醒

仓库当前默认 `spring.profiles.active=dev`。

生产部署前，必须显式切到 `prod`，并注入生产数据库环境变量；不要依赖仓库默认值直接上线。
