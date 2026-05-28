-- 审计日志增强字段
ALTER TABLE app_audit_log ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS';
ALTER TABLE app_audit_log ADD COLUMN IF NOT EXISTS request_id VARCHAR(64);
ALTER TABLE app_audit_log ADD COLUMN IF NOT EXISTS client_ip VARCHAR(64);
ALTER TABLE app_audit_log ADD COLUMN IF NOT EXISTS user_agent VARCHAR(400);
ALTER TABLE app_audit_log ADD COLUMN IF NOT EXISTS duration_ms BIGINT;

CREATE INDEX IF NOT EXISTS idx_audit_log_request_id ON app_audit_log(request_id);
