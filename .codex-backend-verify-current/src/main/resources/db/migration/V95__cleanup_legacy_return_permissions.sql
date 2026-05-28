-- 清理旧版采购/销售退货权限，统一收敛到 draft/approved 新权限模型。

UPDATE app_menu
SET permission_code = 'erp-purchase-return-draft:view',
    updated_at = NOW()
WHERE code = 'erp-purchase-return-draft'
  AND permission_code <> 'erp-purchase-return-draft:view';

UPDATE app_menu
SET permission_code = 'erp-purchase-return-approved:view',
    updated_at = NOW()
WHERE code = 'erp-purchase-return-approved'
  AND permission_code <> 'erp-purchase-return-approved:view';

UPDATE app_menu
SET permission_code = 'erp-sale-return-draft:view',
    updated_at = NOW()
WHERE code = 'erp-sale-return-draft'
  AND permission_code <> 'erp-sale-return-draft:view';

UPDATE app_menu
SET permission_code = 'erp-sale-return-approved:view',
    updated_at = NOW()
WHERE code = 'erp-sale-return-approved'
  AND permission_code <> 'erp-sale-return-approved:view';

WITH permission_map(old_code, new_code) AS (
    VALUES
        ('erp-purchase-return:view', 'erp-purchase-return-draft:view'),
        ('erp-purchase-return:view', 'erp-purchase-return-approved:view'),
        ('erp-purchase-return:view', 'erp-purchase-return-draft:print'),
        ('erp-purchase-return:view', 'erp-purchase-return-approved:print'),
        ('erp-purchase-return:add', 'erp-purchase-return-draft:add'),
        ('erp-purchase-return:edit', 'erp-purchase-return-draft:edit'),
        ('erp-purchase-return:edit', 'erp-purchase-return-draft:delete'),
        ('erp-purchase-return:approve', 'erp-purchase-return-draft:approve'),
        ('erp-purchase-return:cancel', 'erp-purchase-return-approved:cancel'),
        ('erp-sale-return:view', 'erp-sale-return-draft:view'),
        ('erp-sale-return:view', 'erp-sale-return-approved:view'),
        ('erp-sale-return:view', 'erp-sale-return-draft:print'),
        ('erp-sale-return:view', 'erp-sale-return-approved:print'),
        ('erp-sale-return:add', 'erp-sale-return-draft:add'),
        ('erp-sale-return:edit', 'erp-sale-return-draft:edit'),
        ('erp-sale-return:edit', 'erp-sale-return-draft:delete'),
        ('erp-sale-return:approve', 'erp-sale-return-draft:approve'),
        ('erp-sale-return:cancel', 'erp-sale-return-approved:cancel'),
        ('erp-sale-return:redflush', 'erp-sale-return-approved:redflush')
)
INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at, updated_at)
SELECT rp.tenant_id, rp.role_id, new_perm.id, NOW(), NOW()
FROM app_role_permission rp
JOIN app_permission old_perm ON old_perm.id = rp.permission_id
JOIN permission_map pm ON pm.old_code = old_perm.code
JOIN app_permission new_perm ON new_perm.code = pm.new_code
WHERE rp.deleted_at IS NULL
  AND old_perm.deleted_at IS NULL
  AND new_perm.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM app_role_permission existing
      WHERE existing.tenant_id = rp.tenant_id
        AND existing.role_id = rp.role_id
        AND existing.permission_id = new_perm.id
        AND existing.deleted_at IS NULL
  );

DELETE FROM app_role_permission
WHERE permission_id IN (
    SELECT id
    FROM app_permission
    WHERE code IN (
        'erp-purchase-return:view',
        'erp-purchase-return:add',
        'erp-purchase-return:edit',
        'erp-purchase-return:approve',
        'erp-purchase-return:cancel',
        'erp-sale-return:view',
        'erp-sale-return:add',
        'erp-sale-return:edit',
        'erp-sale-return:approve',
        'erp-sale-return:cancel',
        'erp-sale-return:redflush'
    )
      AND deleted_at IS NULL
);

DELETE FROM app_permission
WHERE code IN (
    'erp-purchase-return:view',
    'erp-purchase-return:add',
    'erp-purchase-return:edit',
    'erp-purchase-return:approve',
    'erp-purchase-return:cancel',
    'erp-sale-return:view',
    'erp-sale-return:add',
    'erp-sale-return:edit',
    'erp-sale-return:approve',
    'erp-sale-return:cancel',
    'erp-sale-return:redflush'
)
  AND deleted_at IS NULL;
