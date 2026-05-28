-- ERP 销售利润列权限（非含税）
INSERT INTO app_permission (code, name, description, is_enabled, created_at, updated_at)
SELECT 'column:erp-sale:profit', 'ERP销售-利润列', 'ERP销售利润列显示', TRUE, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM app_permission WHERE code = 'column:erp-sale:profit'
);

-- 为 admin / super_admin 补齐利润列权限
INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at)
SELECT r.tenant_id, r.id, p.id, NOW()
FROM app_role r
JOIN app_permission p ON p.code = 'column:erp-sale:profit'
WHERE r.code IN ('admin', 'super_admin')
  AND NOT EXISTS (
    SELECT 1
    FROM app_role_permission rp
    WHERE rp.tenant_id = r.tenant_id
      AND rp.role_id = r.id
      AND rp.permission_id = p.id
  );
