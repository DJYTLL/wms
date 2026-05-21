-- 注意：本文件仅作为“表结构参考文档”保留。
-- 真实的数据库演进请使用 Flyway 迁移脚本：
--   src/main/resources/db/migration/V{n}__description.sql
-- 当前项目已配置：spring.sql.init.mode=never

-- 租户表
CREATE TABLE IF NOT EXISTS app_tenant (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

-- 用户表：用于 JWT 登录用户
CREATE TABLE IF NOT EXISTS app_user (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(200),
    email VARCHAR(200),
    phone VARCHAR(50),
    avatar_url VARCHAR(500),
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
    credentials_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    auth_version BIGINT NOT NULL DEFAULT 0,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    remark VARCHAR(500)
);

-- 角色表：角色定义
CREATE TABLE IF NOT EXISTS app_role (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_app_role_tenant_code UNIQUE (tenant_id, code)
);

-- 权限表：具体权限点（全局共享）
CREATE TABLE IF NOT EXISTS app_permission (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(150) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_app_permission_code UNIQUE (code)
);

-- 用户-角色关联表
CREATE TABLE IF NOT EXISTS app_user_role (
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (tenant_id, user_id, role_id)
);

-- 角色-权限关联表
CREATE TABLE IF NOT EXISTS app_role_permission (
    tenant_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (tenant_id, role_id, permission_id)
);

-- 刷新令牌表
CREATE TABLE IF NOT EXISTS app_refresh_token (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    audience_tenant_id BIGINT,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 审计日志表
CREATE TABLE IF NOT EXISTS app_audit_log (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    actor_username VARCHAR(100),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(64),
    detail TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    request_id VARCHAR(64),
    client_ip VARCHAR(64),
    user_agent VARCHAR(400),
    method VARCHAR(16),
    path VARCHAR(300),
    http_status INT,
    error_code VARCHAR(32),
    error_message VARCHAR(500),
    duration_ms BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 系统配置表（租户隔离）
CREATE TABLE IF NOT EXISTS app_system_config (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    config_key VARCHAR(120) NOT NULL,
    config_value TEXT,
    value_type VARCHAR(40) NOT NULL DEFAULT 'string',
    description VARCHAR(500),
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 幂等记录表
CREATE TABLE IF NOT EXISTS app_idempotency (
    idempotency_key VARCHAR(64) PRIMARY KEY,
    method VARCHAR(16) NOT NULL,
    path VARCHAR(200) NOT NULL,
    tenant_id BIGINT,
    username VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL
);

-- 常用索引
CREATE UNIQUE INDEX IF NOT EXISTS uq_app_user_username ON app_user(username);
CREATE INDEX IF NOT EXISTS idx_app_user_username ON app_user(username);
CREATE INDEX IF NOT EXISTS idx_app_user_email ON app_user(email);
CREATE INDEX IF NOT EXISTS idx_app_role_tenant_code ON app_role(tenant_id, code);
CREATE INDEX IF NOT EXISTS idx_app_permission_code ON app_permission(code);
CREATE INDEX IF NOT EXISTS idx_refresh_token_tenant_user_id ON app_refresh_token(tenant_id, user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_tenant_actor ON app_audit_log(tenant_id, actor_username);
CREATE INDEX IF NOT EXISTS idx_audit_log_request_id ON app_audit_log(request_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_system_config_tenant_key ON app_system_config(tenant_id, config_key);
CREATE INDEX IF NOT EXISTS idx_idempotency_expires_at ON app_idempotency(expires_at);

-- 菜单表（全局）
CREATE TABLE IF NOT EXISTS app_menu (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    parent_id BIGINT,
    title VARCHAR(200) NOT NULL,
    i18n_key VARCHAR(100),
    path VARCHAR(200),
    icon TEXT,
    permission_code VARCHAR(150),
    sort INT NOT NULL DEFAULT 0,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 租户菜单映射表
CREATE TABLE IF NOT EXISTS app_tenant_menu (
    tenant_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (tenant_id, menu_id)
);

-- 租户列配置
CREATE TABLE IF NOT EXISTS app_tenant_column_setting (
    tenant_id BIGINT NOT NULL,
    page_key VARCHAR(120) NOT NULL,
    visible_columns TEXT NOT NULL,
    updated_by VARCHAR(100),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (tenant_id, page_key)
);

CREATE INDEX IF NOT EXISTS idx_app_menu_code ON app_menu(code);
CREATE INDEX IF NOT EXISTS idx_app_menu_parent ON app_menu(parent_id);
CREATE INDEX IF NOT EXISTS idx_app_tenant_menu_tenant ON app_tenant_menu(tenant_id);
CREATE INDEX IF NOT EXISTS idx_app_tenant_column_tenant ON app_tenant_column_setting(tenant_id);

CREATE TABLE IF NOT EXISTS app_user_table_setting (
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    page_key VARCHAR(120) NOT NULL,
    config_json JSONB NOT NULL,
    updated_by VARCHAR(100),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (tenant_id, user_id, page_key)
);

CREATE INDEX IF NOT EXISTS idx_app_user_table_setting_tenant_user ON app_user_table_setting(tenant_id, user_id);

-- 现有数据库兼容调整
ALTER TABLE app_tenant ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE app_user DROP CONSTRAINT IF EXISTS uq_app_user_tenant_username;
DROP INDEX IF EXISTS idx_app_user_tenant_username;
ALTER TABLE app_refresh_token ADD COLUMN IF NOT EXISTS audience_tenant_id BIGINT;
ALTER TABLE app_tenant_column_setting ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE app_audit_log ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS';
ALTER TABLE app_audit_log ADD COLUMN IF NOT EXISTS request_id VARCHAR(64);
ALTER TABLE app_audit_log ADD COLUMN IF NOT EXISTS client_ip VARCHAR(64);
ALTER TABLE app_audit_log ADD COLUMN IF NOT EXISTS user_agent VARCHAR(400);
ALTER TABLE app_audit_log ADD COLUMN IF NOT EXISTS method VARCHAR(16);
ALTER TABLE app_audit_log ADD COLUMN IF NOT EXISTS path VARCHAR(300);
ALTER TABLE app_audit_log ADD COLUMN IF NOT EXISTS http_status INT;
ALTER TABLE app_audit_log ADD COLUMN IF NOT EXISTS error_code VARCHAR(32);
ALTER TABLE app_audit_log ADD COLUMN IF NOT EXISTS error_message VARCHAR(500);
ALTER TABLE app_audit_log ADD COLUMN IF NOT EXISTS duration_ms BIGINT;
