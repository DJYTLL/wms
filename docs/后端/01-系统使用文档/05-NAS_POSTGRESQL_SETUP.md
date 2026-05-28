# NAS PostgreSQL Setup

This document records the NAS-hosted PostgreSQL instance now used by `wms-backend` development, plus the migration and operational constraints that matter for future changes.

## Scope

- Backend module: `D:\project\wms-backend`
- Current development datasource target: NAS PostgreSQL on `duaoyunxuan.synology.me`
- Current logical database used by this project: `wms_backend`

## Current Connection Layout

### Development profile

The backend now defaults to the `dev` profile.

- Shared config: `wms-backend/src/main/resources/application.properties`
- Dev datasource config: `wms-backend/src/main/resources/application-dev.properties`

Current dev target:

- Host: `duaoyunxuan.synology.me`
- Port: `5433`
- Database: `wms_backend`
- Username: `erp`

For the exact current password, check `application-dev.properties`.

### Production profile

Production datasource values are not hardcoded in the repo.

- File: `wms-backend/src/main/resources/application-prod.properties`
- Required environment variables:
  - `SPRING_DATASOURCE_URL`
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD`

## NAS Container Notes

The NAS already had an unrelated PostgreSQL container for another project. This project must not reuse that database.

Relevant NAS PostgreSQL containers identified during setup:

- `babycare-postgres`
  - Existing unrelated database container
  - Internal `5432/tcp`
  - Not the datasource for this WMS backend
- `erp-postgres`
  - New target container used for this migration
  - External port mapping: `5433 -> 5432`
  - Current WMS database: `wms_backend`

## Migration Record

Local source database:

- Source container: `wms-backend-pgsql`
- Source database: `wms_backend`

Remote target database:

- Target host: `duaoyunxuan.synology.me`
- Target port: `5433`
- Target database: `wms_backend`

Migration outcome:

- Remote `wms_backend` database was created and populated from the local Dockerized PostgreSQL source.
- The remote database name was aligned with the local project database name and kept as `wms_backend`.
- Sample row-count verification matched between local and remote for these tables:
  - `app_tenant = 5`
  - `app_user = 9`
  - `erp_product = 152`
  - `erp_sale_order = 81`

## Important Compatibility Notes

### 1. Database name must stay `wms_backend`

The project configuration and migration record are now aligned on `wms_backend`.

Do not point this backend to the older placeholder database name `erp` unless there is an explicit future migration plan.

### 2. A `postgres` role exists on the remote instance for dump compatibility

During migration, the exported SQL referenced objects owned by `postgres`.

To keep the import compatible without rewriting the dump, a `postgres` role was created on the remote instance.

This means:

- Application runtime user is currently `erp`
- Remote instance also contains a `postgres` role for compatibility

Do not remove that role casually unless ownership and import strategy are redesigned.

### 3. Standard JDBC import is not equivalent to `psql`

During migration, plain JDBC execution could not directly consume:

- `\restrict` / `\unrestrict` meta commands
- `COPY FROM STDIN` sections

If a future full-database migration is needed again, prefer:

- `pg_dump --inserts --column-inserts` for JDBC-based replay, or
- a true `psql` / `pg_restore` path when network and SSH capabilities permit

### 4. External access behavior matters

The NAS hostname resolved to IPv6 during this work.

Operational impact:

- Direct Docker-based clients on the local machine could not reliably use the NAS hostname over IPv6
- SSH port forwarding was also restricted by the NAS SSH policy

If future tooling needs direct scripted restore access, prefer verifying IPv4 reachability or using native clients on the target host.

## Recommended Operational Checks

When verifying the remote database later, check these points first:

1. `application-dev.properties` still points to `duaoyunxuan.synology.me:5433/wms_backend`
2. NAS container `erp-postgres` is running
3. Remote database `wms_backend` is reachable
4. The runtime user still has access to the required schema objects

## Production Activation Reminder

The repo now defaults to `spring.profiles.active=dev`.

Before a production deployment, explicitly set the active profile to `prod` and provide the datasource environment variables. Do not rely on the repo default in production.
