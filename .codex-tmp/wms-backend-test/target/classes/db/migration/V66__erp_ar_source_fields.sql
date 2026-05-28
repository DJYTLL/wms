-- 结构化记录应收来源，避免销售退货应收依赖 remark 反查。
ALTER TABLE erp_accounts_receivable
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS source_id BIGINT;

UPDATE erp_accounts_receivable
SET source_type = 'SALE_ORDER',
    source_id = sale_order_id
WHERE source_type IS NULL
  AND sale_order_id IS NOT NULL
  AND total_amount >= 0;

CREATE INDEX IF NOT EXISTS idx_erp_ar_source_active
    ON erp_accounts_receivable (tenant_id, source_type, source_id, deleted_at);

COMMENT ON COLUMN erp_accounts_receivable.source_type IS '来源类型(SALE_ORDER/SALE_RETURN)';
COMMENT ON COLUMN erp_accounts_receivable.source_id IS '来源单据ID';
