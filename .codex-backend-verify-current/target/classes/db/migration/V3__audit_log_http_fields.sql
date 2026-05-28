-- 审计日志补充 HTTP 维度字段
ALTER TABLE app_audit_log ADD COLUMN IF NOT EXISTS method VARCHAR(16);
ALTER TABLE app_audit_log ADD COLUMN IF NOT EXISTS path VARCHAR(300);
ALTER TABLE app_audit_log ADD COLUMN IF NOT EXISTS http_status INT;

CREATE INDEX IF NOT EXISTS idx_audit_log_method ON app_audit_log(method);
CREATE INDEX IF NOT EXISTS idx_audit_log_path ON app_audit_log(path);
