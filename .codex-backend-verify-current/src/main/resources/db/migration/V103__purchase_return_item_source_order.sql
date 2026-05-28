ALTER TABLE erp_purchase_return_item
    ADD COLUMN IF NOT EXISTS source_purchase_order_item_id BIGINT,
    ADD COLUMN IF NOT EXISTS source_purchase_order_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_erp_purchase_return_item_source_purchase_item
    ON erp_purchase_return_item (tenant_id, source_purchase_order_item_id)
    WHERE source_purchase_order_item_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_erp_purchase_return_item_source_purchase_order
    ON erp_purchase_return_item (tenant_id, source_purchase_order_id)
    WHERE source_purchase_order_id IS NOT NULL;

COMMENT ON COLUMN erp_purchase_return_item.source_purchase_order_item_id IS '来源采购单明细ID';
COMMENT ON COLUMN erp_purchase_return_item.source_purchase_order_id IS '来源采购单ID';
