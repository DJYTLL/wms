-- 将组装单/拆分单菜单权限从历史 erp-assembly:* 拆成与菜单 code 一致的页面权限。

INSERT INTO app_permission (code, name, description, is_enabled, created_at, updated_at)
SELECT v.code, v.name, v.description, TRUE, NOW(), NOW()
FROM (
    VALUES
        ('erp-assemble-order:view', '组装单-查看', '查看ERP组装单'),
        ('erp-assemble-order:add', '组装单-新增', '新增ERP组装单'),
        ('erp-assemble-order:edit', '组装单-编辑', '编辑ERP组装单'),
        ('erp-assemble-order:approve', '组装单-审核', '审核ERP组装单'),
        ('erp-assemble-order:delete', '组装单-删除', '删除ERP组装单'),
        ('erp-disassemble-order:view', '拆分单-查看', '查看ERP拆分单'),
        ('erp-disassemble-order:add', '拆分单-新增', '新增ERP拆分单'),
        ('erp-disassemble-order:edit', '拆分单-编辑', '编辑ERP拆分单'),
        ('erp-disassemble-order:approve', '拆分单-审核', '审核ERP拆分单'),
        ('erp-disassemble-order:delete', '拆分单-删除', '删除ERP拆分单')
) AS v(code, name, description)
WHERE NOT EXISTS (
    SELECT 1 FROM app_permission p
    WHERE p.code = v.code
      AND p.deleted_at IS NULL
);

UPDATE app_menu
SET permission_code = 'erp-assemble-order:view',
    updated_at = NOW()
WHERE code = 'erp-assemble-order'
  AND deleted_at IS NULL;

UPDATE app_menu
SET permission_code = 'erp-disassemble-order:view',
    updated_at = NOW()
WHERE code = 'erp-disassemble-order'
  AND deleted_at IS NULL;

WITH mapping(old_code, new_code) AS (
    VALUES
        ('erp-assembly:view', 'erp-assemble-order:view'),
        ('erp-assembly:add', 'erp-assemble-order:add'),
        ('erp-assembly:edit', 'erp-assemble-order:edit'),
        ('erp-assembly:approve', 'erp-assemble-order:approve'),
        ('erp-assembly:delete', 'erp-assemble-order:delete'),
        ('erp-assembly:view', 'erp-disassemble-order:view'),
        ('erp-assembly:add', 'erp-disassemble-order:add'),
        ('erp-assembly:edit', 'erp-disassemble-order:edit'),
        ('erp-assembly:approve', 'erp-disassemble-order:approve'),
        ('erp-assembly:delete', 'erp-disassemble-order:delete')
)
INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at, updated_at)
SELECT DISTINCT rp.tenant_id, rp.role_id, new_permission.id, NOW(), NOW()
FROM app_role_permission rp
JOIN app_permission old_permission
  ON old_permission.id = rp.permission_id
 AND old_permission.deleted_at IS NULL
JOIN mapping
  ON mapping.old_code = old_permission.code
JOIN app_permission new_permission
  ON new_permission.code = mapping.new_code
 AND new_permission.deleted_at IS NULL
WHERE rp.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM app_role_permission existing
      WHERE existing.tenant_id = rp.tenant_id
        AND existing.role_id = rp.role_id
        AND existing.permission_id = new_permission.id
        AND existing.deleted_at IS NULL
  );
