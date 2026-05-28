-- 销售单增加单据时间字段
ALTER TABLE erp_sale_order
    ADD COLUMN IF NOT EXISTS order_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

COMMENT ON COLUMN erp_sale_order.order_at IS '单据时间';
