ALTER TABLE erp_assembly_order
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(30) NOT NULL DEFAULT 'MANUAL';

ALTER TABLE erp_assembly_order
    ADD COLUMN IF NOT EXISTS source_sale_order_id BIGINT;

ALTER TABLE erp_assembly_order
    ADD COLUMN IF NOT EXISTS source_sale_order_no VARCHAR(64);

ALTER TABLE erp_assembly_order
    ADD COLUMN IF NOT EXISTS source_sale_order_item_id BIGINT;

ALTER TABLE erp_assembly_order
    ADD COLUMN IF NOT EXISTS customer_id BIGINT;

ALTER TABLE erp_assembly_order
    ADD COLUMN IF NOT EXISTS customer_name VARCHAR(200);

CREATE INDEX IF NOT EXISTS idx_erp_assembly_order_source_sale_order
    ON erp_assembly_order (tenant_id, source_sale_order_id)
    WHERE source_sale_order_id IS NOT NULL AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_assembly_order_source_sale_item
    ON erp_assembly_order (tenant_id, source_sale_order_item_id)
    WHERE source_sale_order_item_id IS NOT NULL AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_assembly_order_customer
    ON erp_assembly_order (tenant_id, customer_id)
    WHERE customer_id IS NOT NULL AND deleted_at IS NULL;

COMMENT ON COLUMN erp_assembly_order.source_type IS '来源类型(MANUAL/SALE_ORDER/STOCK_PREPARE)';
COMMENT ON COLUMN erp_assembly_order.source_sale_order_id IS '来源销售单ID';
COMMENT ON COLUMN erp_assembly_order.source_sale_order_no IS '来源销售单号快照';
COMMENT ON COLUMN erp_assembly_order.source_sale_order_item_id IS '来源销售单明细ID';
COMMENT ON COLUMN erp_assembly_order.customer_id IS '来源客户ID快照';
COMMENT ON COLUMN erp_assembly_order.customer_name IS '来源客户名称快照';
