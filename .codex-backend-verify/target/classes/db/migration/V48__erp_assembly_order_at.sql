-- Add order_at for assembly order
ALTER TABLE erp_assembly_order
    ADD COLUMN IF NOT EXISTS order_at TIMESTAMPTZ;

UPDATE erp_assembly_order
SET order_at = created_at
WHERE order_at IS NULL;

COMMENT ON COLUMN erp_assembly_order.order_at IS '单据时间';

CREATE INDEX IF NOT EXISTS idx_erp_assembly_order_order_at
    ON erp_assembly_order (tenant_id, order_at);
