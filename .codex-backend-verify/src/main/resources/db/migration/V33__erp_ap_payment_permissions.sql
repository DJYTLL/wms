-- ERP应付/付款权限补齐
INSERT INTO app_permission (code, name, description, is_enabled, created_at, updated_at) VALUES
    ('erp-ap:view', '应付管理-查看', '应付管理查看', TRUE, NOW(), NOW()),
    ('erp-payment:view', '付款单-查看', '付款单查看', TRUE, NOW(), NOW()),
    ('erp-payment:add', '付款单-新增/编辑', '付款单新增与编辑', TRUE, NOW(), NOW()),
    ('erp-payment:approve', '付款单-审核', '付款单审核', TRUE, NOW(), NOW()),
    ('erp-payment:red-flush', '付款单-红冲', '付款单红冲', TRUE, NOW(), NOW())
ON CONFLICT (code) DO NOTHING;

-- 给 admin / super_admin 赋权
INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at)
SELECT r.tenant_id, r.id, p.id, NOW()
FROM app_role r
JOIN app_permission p ON p.code IN (
    'erp-ap:view',
    'erp-payment:view',
    'erp-payment:add',
    'erp-payment:approve',
    'erp-payment:red-flush'
)
WHERE r.code IN ('admin', 'super_admin')
ON CONFLICT (tenant_id, role_id, permission_id) DO NOTHING;
