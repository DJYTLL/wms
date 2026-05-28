-- Add dedicated source-document permissions for decoupled cross-document references.

INSERT INTO app_permission (code, name, description, is_enabled, created_at, updated_at)
SELECT v.code, v.name, v.description, TRUE, NOW(), NOW()
FROM (
    VALUES
        ('erp-purchase-return-draft:source-view', '查看采购退货来源采购单(ERP)', '查看采购退货可引用的来源采购单'),
        ('erp-receipt:source-view', '查看收款单来源应收单(ERP)', '查看收款单可引用的来源应收单'),
        ('erp-payment:source-view', '查看付款单来源应付单(ERP)', '查看付款单可引用的来源应付单')
) AS v(code, name, description)
WHERE NOT EXISTS (
    SELECT 1
    FROM app_permission p
    WHERE p.code = v.code
      AND p.deleted_at IS NULL
);

WITH permission_map(old_code, new_code) AS (
    VALUES
        ('erp-purchase-return-draft:view', 'erp-purchase-return-draft:source-view'),
        ('erp-purchase-approved:view', 'erp-purchase-return-draft:source-view'),
        ('erp-receipt:add', 'erp-receipt:source-view'),
        ('erp-ar:view', 'erp-receipt:source-view'),
        ('erp-payment:add', 'erp-payment:source-view'),
        ('erp-ap:view', 'erp-payment:source-view')
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
