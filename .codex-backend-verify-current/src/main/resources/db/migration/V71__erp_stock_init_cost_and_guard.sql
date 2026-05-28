-- ERP初始库存成本快照与唯一性保护

ALTER TABLE erp_stock_count_item
    ADD COLUMN IF NOT EXISTS init_unit_cost NUMERIC(18,4),
    ADD COLUMN IF NOT EXISTS init_total_amount NUMERIC(18,4);

COMMENT ON COLUMN erp_stock_count_item.init_unit_cost IS '期初单价';
COMMENT ON COLUMN erp_stock_count_item.init_total_amount IS '期初金额';

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_stock_count_one_approved_init
    ON erp_stock_count (tenant_id)
    WHERE count_type = 'INIT'
      AND status = 'APPROVED'
      AND deleted_at IS NULL;
