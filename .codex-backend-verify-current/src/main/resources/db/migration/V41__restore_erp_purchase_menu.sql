-- Restore purchase management menu group and move draft/approved under it.
INSERT INTO app_menu (code, parent_id, title, i18n_key, path, icon, permission_code, sort, is_enabled, created_at, updated_at)
SELECT 'erp-purchase', p.id, '采购管理', 'erp-purchase', NULL, NULL, NULL, 20, TRUE, NOW(), NOW()
FROM app_menu p
WHERE p.code = 'erp'
ON CONFLICT (code) DO NOTHING;

UPDATE app_menu
SET parent_id = (SELECT id FROM app_menu WHERE code = 'erp-purchase'),
    updated_at = NOW()
WHERE code IN ('erp-purchase-draft', 'erp-purchase-approved');

INSERT INTO app_tenant_menu (tenant_id, menu_id, is_enabled, created_at, updated_at)
SELECT t.id, m.id, TRUE, NOW(), NOW()
FROM app_tenant t
JOIN app_menu m ON m.code = 'erp-purchase'
WHERE t.deleted_at IS NULL
ON CONFLICT (tenant_id, menu_id) DO NOTHING;
