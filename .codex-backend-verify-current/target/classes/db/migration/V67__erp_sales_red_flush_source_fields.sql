-- 结构化记录销售链路红冲来源，避免仅依赖备注追溯。
ALTER TABLE erp_sale_order
    ADD COLUMN IF NOT EXISTS red_flush_source_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS red_flush_source_id BIGINT;

ALTER TABLE erp_sale_return
    ADD COLUMN IF NOT EXISTS red_flush_source_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS red_flush_source_id BIGINT;

ALTER TABLE erp_accounts_receivable
    ADD COLUMN IF NOT EXISTS red_flush_source_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS red_flush_source_id BIGINT;

ALTER TABLE erp_receipt
    ADD COLUMN IF NOT EXISTS red_flush_source_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS red_flush_source_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_erp_sale_order_red_flush_source
    ON erp_sale_order (tenant_id, red_flush_source_type, red_flush_source_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_sale_return_red_flush_source
    ON erp_sale_return (tenant_id, red_flush_source_type, red_flush_source_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_ar_red_flush_source
    ON erp_accounts_receivable (tenant_id, red_flush_source_type, red_flush_source_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_receipt_red_flush_source
    ON erp_receipt (tenant_id, red_flush_source_type, red_flush_source_id)
    WHERE deleted_at IS NULL;

UPDATE erp_sale_order
SET red_flush_source_type = 'SALE_ORDER',
    red_flush_source_id = id
WHERE status = 'RED_FLUSHED'
  AND red_flush_source_type IS NULL
  AND deleted_at IS NULL;

UPDATE erp_sale_return
SET red_flush_source_type = 'SALE_RETURN',
    red_flush_source_id = id
WHERE status = 'RED_FLUSHED'
  AND red_flush_source_type IS NULL
  AND deleted_at IS NULL;

COMMENT ON COLUMN erp_sale_order.red_flush_source_type IS '红冲来源类型';
COMMENT ON COLUMN erp_sale_order.red_flush_source_id IS '红冲来源单据ID';
COMMENT ON COLUMN erp_sale_return.red_flush_source_type IS '红冲来源类型';
COMMENT ON COLUMN erp_sale_return.red_flush_source_id IS '红冲来源单据ID';
COMMENT ON COLUMN erp_accounts_receivable.red_flush_source_type IS '红冲来源类型';
COMMENT ON COLUMN erp_accounts_receivable.red_flush_source_id IS '红冲来源单据ID';
COMMENT ON COLUMN erp_receipt.red_flush_source_type IS '红冲来源类型';
COMMENT ON COLUMN erp_receipt.red_flush_source_id IS '红冲来源单据ID';
