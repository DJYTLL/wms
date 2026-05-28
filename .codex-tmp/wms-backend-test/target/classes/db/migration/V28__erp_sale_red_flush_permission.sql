-- ERP 销售单红冲权限
INSERT INTO app_permission (code, name, description, is_enabled, created_at, updated_at)
SELECT 'erp-sale:redflush', '红冲销售单(ERP)', '红冲ERP销售单', TRUE, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM app_permission WHERE code = 'erp-sale:redflush'
);

-- 为 admin / super_admin 补齐红冲权限
INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at)
SELECT r.tenant_id, r.id, p.id, NOW()
FROM app_role r
JOIN app_permission p ON p.code = 'erp-sale:redflush'
WHERE r.code IN ('admin', 'super_admin')
ON CONFLICT (tenant_id, role_id, permission_id) DO NOTHING;
