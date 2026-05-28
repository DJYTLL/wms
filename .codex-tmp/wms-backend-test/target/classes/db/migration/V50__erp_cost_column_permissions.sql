-- ERP 成本列权限补齐（幂等）
INSERT INTO app_permission (code, name, description, is_enabled, created_at, updated_at) VALUES
    ('column:erp-product:costPrice', 'ERP商品-成本价列', 'ERP商品成本价列显示', TRUE, NOW(), NOW()),
    ('column:erp-stock-txn:unitCost', 'ERP流水-单位成本列', 'ERP流水单位成本列显示', TRUE, NOW(), NOW()),
    ('column:erp-stock-txn:totalCost', 'ERP流水-总成本列', 'ERP流水总成本列显示', TRUE, NOW(), NOW())
ON CONFLICT (code) DO NOTHING;

-- 为 admin / super_admin 补齐新增列权限
INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at)
SELECT r.tenant_id, r.id, p.id, NOW()
FROM app_role r
JOIN app_permission p ON p.code IN (
    'column:erp-product:costPrice',
    'column:erp-stock-txn:unitCost',
    'column:erp-stock-txn:totalCost'
)
WHERE r.code IN ('admin', 'super_admin')
AND NOT EXISTS (
    SELECT 1
    FROM app_role_permission rp
    WHERE rp.tenant_id = r.tenant_id
      AND rp.role_id = r.id
      AND rp.permission_id = p.id
);
