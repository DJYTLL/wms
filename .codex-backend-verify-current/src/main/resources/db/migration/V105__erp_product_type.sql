ALTER TABLE erp_product
    ADD COLUMN IF NOT EXISTS product_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL';

UPDATE erp_product
SET product_type = 'NORMAL'
WHERE product_type IS NULL OR product_type = '';

COMMENT ON COLUMN erp_product.product_type IS '商品类型(NORMAL普通商品/ASSEMBLY组装商品)';
