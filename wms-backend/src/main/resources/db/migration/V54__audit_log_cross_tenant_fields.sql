-- 审计日志补充跨租户代管字段
ALTER TABLE app_audit_log ADD COLUMN IF NOT EXISTS auth_tenant_id BIGINT;
ALTER TABLE app_audit_log ADD COLUMN IF NOT EXISTS auth_tenant_code VARCHAR(100);
ALTER TABLE app_audit_log ADD COLUMN IF NOT EXISTS is_cross_tenant BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_audit_log_auth_tenant_id ON app_audit_log(auth_tenant_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_cross_tenant ON app_audit_log(is_cross_tenant);
