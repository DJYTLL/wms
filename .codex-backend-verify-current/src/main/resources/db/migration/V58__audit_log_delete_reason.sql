ALTER TABLE app_audit_log ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);

CREATE INDEX IF NOT EXISTS idx_audit_log_delete_reason ON app_audit_log(delete_reason);

COMMENT ON COLUMN app_audit_log.delete_reason IS '删除原因';
