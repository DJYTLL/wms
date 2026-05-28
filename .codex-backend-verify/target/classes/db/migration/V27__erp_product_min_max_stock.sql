-- ERP 商品：新增最低库存/最高库存字段
ALTER TABLE erp_product
    ADD COLUMN IF NOT EXISTS min_stock NUMERIC(18,4) NOT NULL DEFAULT 0;

ALTER TABLE erp_product
    ADD COLUMN IF NOT EXISTS max_stock NUMERIC(18,4) NOT NULL DEFAULT 0;

COMMENT ON COLUMN erp_product.min_stock IS '最低库存';
COMMENT ON COLUMN erp_product.max_stock IS '最高库存';
