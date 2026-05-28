-- 销售退货明细绑定原销售单明细行，用于精确校验可退数量、金额、仓库和成本。

ALTER TABLE erp_sale_return_item
    ADD COLUMN IF NOT EXISTS source_sale_order_item_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_erp_sale_return_item_source_sale_item
    ON erp_sale_return_item (tenant_id, source_sale_order_item_id)
    WHERE source_sale_order_item_id IS NOT NULL;

COMMENT ON COLUMN erp_sale_return_item.source_sale_order_item_id IS '来源销售单明细ID';
