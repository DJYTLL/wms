ALTER TABLE erp_supplier
    ADD COLUMN IF NOT EXISTS is_blacklisted BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN erp_supplier.is_blacklisted IS '是否黑名单';

CREATE INDEX IF NOT EXISTS idx_erp_supplier_status_active
    ON erp_supplier (tenant_id, is_enabled, is_blacklisted, deleted_at, updated_at DESC);
