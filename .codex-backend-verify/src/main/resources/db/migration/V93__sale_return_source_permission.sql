-- Add dedicated source-sale-order permission for sale return draft workflow.

INSERT INTO app_permission (code, name, description, is_enabled, created_at, updated_at)
SELECT 'erp-sale-return-draft:source-view',
       '查看销售退货来源销售单(ERP)',
       '查看销售退货可引用的来源销售单',
       TRUE,
       NOW(),
       NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM app_permission
    WHERE code = 'erp-sale-return-draft:source-view'
      AND deleted_at IS NULL
);

WITH permission_map(old_code, new_code) AS (
    VALUES
        ('erp-sale-return-draft:view', 'erp-sale-return-draft:source-view'),
        ('erp-sale-approved:view', 'erp-sale-return-draft:source-view')
)
INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at, updated_at)
SELECT DISTINCT rp.tenant_id, rp.role_id, new_perm.id, NOW(), NOW()
FROM app_role_permission rp
JOIN app_permission old_perm ON old_perm.id = rp.permission_id
    AND old_perm.deleted_at IS NULL
JOIN permission_map pm ON pm.old_code = old_perm.code
JOIN app_permission new_perm ON new_perm.code = pm.new_code
    AND new_perm.deleted_at IS NULL
WHERE rp.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM app_role_permission existing
      WHERE existing.tenant_id = rp.tenant_id
        AND existing.role_id = rp.role_id
        AND existing.permission_id = new_perm.id
        AND existing.deleted_at IS NULL
  );
