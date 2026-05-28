-- 审计日志补充错误字段
ALTER TABLE app_audit_log ADD COLUMN IF NOT EXISTS error_code VARCHAR(32);
ALTER TABLE app_audit_log ADD COLUMN IF NOT EXISTS error_message VARCHAR(500);

CREATE INDEX IF NOT EXISTS idx_audit_log_error_code ON app_audit_log(error_code);
