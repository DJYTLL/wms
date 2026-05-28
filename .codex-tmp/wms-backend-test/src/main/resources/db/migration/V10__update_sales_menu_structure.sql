-- 更新销售菜单结构（拆分草稿/已审核/退货）
-- 1) 将 erp-sale 调整为菜单分组
UPDATE app_menu
SET title = '销售管理',
    i18n_key = 'erp-sale',
    path = NULL,
    permission_code = NULL,
    updated_at = NOW()
WHERE code = 'erp-sale';

-- 2) 插入/更新子菜单
INSERT INTO app_menu (code, parent_id, title, i18n_key, path, icon, permission_code, sort, is_enabled, created_at, updated_at)
SELECT 'erp-sale-draft', p.id, '销售单（草稿）', 'erp-sale-draft', '/erp/sale-orders/draft', NULL, 'erp-sale:view', 10, TRUE, NOW(), NOW()
FROM app_menu p
WHERE p.code = 'erp-sale'
ON CONFLICT (code) DO UPDATE
SET parent_id = EXCLUDED.parent_id,
    title = EXCLUDED.title,
    i18n_key = EXCLUDED.i18n_key,
    path = EXCLUDED.path,
    permission_code = EXCLUDED.permission_code,
    sort = EXCLUDED.sort,
    is_enabled = EXCLUDED.is_enabled,
    updated_at = NOW();

INSERT INTO app_menu (code, parent_id, title, i18n_key, path, icon, permission_code, sort, is_enabled, created_at, updated_at)
SELECT 'erp-sale-approved', p.id, '销售单（已审核）', 'erp-sale-approved', '/erp/sale-orders/approved', NULL, 'erp-sale:view', 20, TRUE, NOW(), NOW()
FROM app_menu p
WHERE p.code = 'erp-sale'
ON CONFLICT (code) DO UPDATE
SET parent_id = EXCLUDED.parent_id,
    title = EXCLUDED.title,
    i18n_key = EXCLUDED.i18n_key,
    path = EXCLUDED.path,
    permission_code = EXCLUDED.permission_code,
    sort = EXCLUDED.sort,
    is_enabled = EXCLUDED.is_enabled,
    updated_at = NOW();

INSERT INTO app_menu (code, parent_id, title, i18n_key, path, icon, permission_code, sort, is_enabled, created_at, updated_at)
SELECT 'erp-sale-return-draft', p.id, '销售退货（草稿）', 'erp-sale-return-draft', '/erp/sale-returns/draft', NULL, 'erp-sale-return:view', 30, TRUE, NOW(), NOW()
FROM app_menu p
WHERE p.code = 'erp-sale'
ON CONFLICT (code) DO UPDATE
SET parent_id = EXCLUDED.parent_id,
    title = EXCLUDED.title,
    i18n_key = EXCLUDED.i18n_key,
    path = EXCLUDED.path,
    permission_code = EXCLUDED.permission_code,
    sort = EXCLUDED.sort,
    is_enabled = EXCLUDED.is_enabled,
    updated_at = NOW();

INSERT INTO app_menu (code, parent_id, title, i18n_key, path, icon, permission_code, sort, is_enabled, created_at, updated_at)
SELECT 'erp-sale-return-approved', p.id, '销售退货（已审核）', 'erp-sale-return-approved', '/erp/sale-returns/approved', NULL, 'erp-sale-return:view', 40, TRUE, NOW(), NOW()
FROM app_menu p
WHERE p.code = 'erp-sale'
ON CONFLICT (code) DO UPDATE
SET parent_id = EXCLUDED.parent_id,
    title = EXCLUDED.title,
    i18n_key = EXCLUDED.i18n_key,
    path = EXCLUDED.path,
    permission_code = EXCLUDED.permission_code,
    sort = EXCLUDED.sort,
    is_enabled = EXCLUDED.is_enabled,
    updated_at = NOW();

-- 3) 为所有租户补齐菜单映射
INSERT INTO app_tenant_menu (tenant_id, menu_id, is_enabled, created_at, updated_at)
SELECT t.id, m.id, TRUE, NOW(), NOW()
FROM app_tenant t
JOIN app_menu m ON m.code IN ('erp-sale', 'erp-sale-draft', 'erp-sale-approved', 'erp-sale-return-draft', 'erp-sale-return-approved')
WHERE t.deleted_at IS NULL
ON CONFLICT (tenant_id, menu_id) DO NOTHING;
