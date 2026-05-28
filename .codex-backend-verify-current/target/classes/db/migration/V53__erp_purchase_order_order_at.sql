-- Add order_at for purchase order and backfill old data
ALTER TABLE erp_purchase_order
    ADD COLUMN IF NOT EXISTS order_at TIMESTAMPTZ;

UPDATE erp_purchase_order
SET order_at = created_at
WHERE order_at IS NULL;

COMMENT ON COLUMN erp_purchase_order.order_at IS '单据时间';

CREATE INDEX IF NOT EXISTS idx_erp_purchase_order_order_at
    ON erp_purchase_order (tenant_id, order_at);
