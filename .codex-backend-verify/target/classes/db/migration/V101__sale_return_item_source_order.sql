ALTER TABLE erp_sale_return_item
    ADD COLUMN IF NOT EXISTS source_sale_order_id BIGINT;

UPDATE erp_sale_return_item item
SET source_sale_order_id = sale_return.sale_order_id
FROM erp_sale_return sale_return
WHERE item.return_id = sale_return.id
  AND item.tenant_id = sale_return.tenant_id
  AND item.source_sale_order_id IS NULL
  AND item.source_sale_order_item_id IS NOT NULL
  AND sale_return.sale_order_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_erp_sale_return_item_source_sale_order
    ON erp_sale_return_item (tenant_id, source_sale_order_id)
    WHERE source_sale_order_id IS NOT NULL;

COMMENT ON COLUMN erp_sale_return_item.source_sale_order_id IS '来源销售单ID';
