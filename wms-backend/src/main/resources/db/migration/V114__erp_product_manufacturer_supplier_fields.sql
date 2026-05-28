-- ERP 商品：厂家信息与来源供应商字段
ALTER TABLE erp_product
    ADD COLUMN IF NOT EXISTS manufacturer_code VARCHAR(100),
    ADD COLUMN IF NOT EXISTS manufacturer_model VARCHAR(200),
    ADD COLUMN IF NOT EXISTS manufacturer_name VARCHAR(200),
    ADD COLUMN IF NOT EXISTS source_supplier_id BIGINT;

COMMENT ON COLUMN erp_product.manufacturer_code IS '厂家编码';
COMMENT ON COLUMN erp_product.manufacturer_model IS '厂家型号';
COMMENT ON COLUMN erp_product.manufacturer_name IS '厂家名称';
COMMENT ON COLUMN erp_product.source_supplier_id IS '来源供应商ID';

CREATE INDEX IF NOT EXISTS idx_erp_product_manufacturer_code
    ON erp_product (tenant_id, manufacturer_code)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_product_source_supplier
    ON erp_product (tenant_id, source_supplier_id)
    WHERE deleted_at IS NULL;

INSERT INTO app_permission (code, name, description, is_enabled, created_at, updated_at)
SELECT v.code, v.name, v.description, TRUE, NOW(), NOW()
FROM (VALUES
    ('column:erp-product:manufacturerCode', 'ERP商品-厂家编码列', 'ERP商品厂家编码列显示'),
    ('column:erp-product:manufacturerModel', 'ERP商品-厂家型号列', 'ERP商品厂家型号列显示'),
    ('column:erp-product:manufacturerName', 'ERP商品-厂家名称列', 'ERP商品厂家名称列显示'),
    ('column:erp-product:sourceSupplier', 'ERP商品-来源供应商列', 'ERP商品来源供应商列显示')
) AS v(code, name, description)
WHERE NOT EXISTS (
    SELECT 1
    FROM app_permission existing
    WHERE existing.code = v.code
      AND existing.deleted_at IS NULL
);

INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at)
SELECT r.tenant_id, r.id, p.id, NOW()
FROM app_role r
JOIN app_permission p ON p.code IN (
    'column:erp-product:manufacturerCode',
    'column:erp-product:manufacturerModel',
    'column:erp-product:manufacturerName',
    'column:erp-product:sourceSupplier'
)
WHERE r.code IN ('admin', 'super_admin')
AND NOT EXISTS (
    SELECT 1
    FROM app_role_permission rp
    WHERE rp.tenant_id = r.tenant_id
      AND rp.role_id = r.id
      AND rp.permission_id = p.id
);
