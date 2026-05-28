-- 商品默认仓库与库位字段
ALTER TABLE erp_product
    ADD COLUMN IF NOT EXISTS default_warehouse_id BIGINT,
    ADD COLUMN IF NOT EXISTS default_location_id BIGINT;

COMMENT ON COLUMN erp_product.default_warehouse_id IS '默认仓库ID';
COMMENT ON COLUMN erp_product.default_location_id IS '默认库位ID';

CREATE INDEX IF NOT EXISTS idx_erp_product_default_warehouse
    ON erp_product (tenant_id, default_warehouse_id);

CREATE INDEX IF NOT EXISTS idx_erp_product_default_location
    ON erp_product (tenant_id, default_location_id);
