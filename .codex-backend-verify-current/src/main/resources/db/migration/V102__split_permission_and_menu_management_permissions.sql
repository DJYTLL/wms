-- Give permission management and menu management their own functional permissions.

INSERT INTO app_permission (code, name, description, is_enabled, created_at, updated_at)
SELECT seed.code, seed.name, seed.description, TRUE, NOW(), NOW()
FROM (
    VALUES
        ('permission:view', '查看权限', '查看权限定义列表'),
        ('permission:add', '新增权限', '新增权限定义'),
        ('permission:edit', '编辑权限', '编辑权限定义'),
        ('permission:delete', '删除权限', '删除权限定义'),
        ('menu:view', '查看菜单', '查看菜单定义列表'),
        ('menu:add', '新增菜单', '新增菜单定义'),
        ('menu:edit', '编辑菜单', '编辑菜单定义'),
        ('menu:delete', '删除菜单', '删除菜单定义')
) AS seed(code, name, description)
WHERE NOT EXISTS (
    SELECT 1
    FROM app_permission existing
    WHERE existing.code = seed.code
      AND existing.deleted_at IS NULL
);

UPDATE app_menu
SET permission_code = 'permission:view',
    updated_at = NOW()
WHERE code = 'permissions'
  AND deleted_at IS NULL
  AND permission_code IS DISTINCT FROM 'permission:view';

UPDATE app_menu
SET permission_code = 'menu:view',
    updated_at = NOW()
WHERE code = 'menu-management'
  AND deleted_at IS NULL
  AND permission_code IS DISTINCT FROM 'menu:view';

INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at, updated_at)
SELECT DISTINCT role.tenant_id, role.id, permission.id, NOW(), NOW()
FROM app_role role
JOIN app_permission permission
  ON permission.code IN (
      'permission:view',
      'permission:add',
      'permission:edit',
      'permission:delete',
      'menu:view',
      'menu:add',
      'menu:edit',
      'menu:delete'
  )
 AND permission.deleted_at IS NULL
WHERE role.code = 'super_admin'
  AND role.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM app_role_permission existing
      WHERE existing.tenant_id = role.tenant_id
        AND existing.role_id = role.id
        AND existing.permission_id = permission.id
  );

INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at, updated_at)
SELECT DISTINCT rp.tenant_id, rp.role_id, permission.id, NOW(), NOW()
FROM app_role_permission rp
JOIN app_permission old_permission
  ON old_permission.id = rp.permission_id
 AND old_permission.code = 'role:view'
 AND old_permission.deleted_at IS NULL
JOIN app_permission permission
  ON permission.code = 'permission:view'
 AND permission.deleted_at IS NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM app_role_permission existing
    WHERE existing.tenant_id = rp.tenant_id
      AND existing.role_id = rp.role_id
      AND existing.permission_id = permission.id
);

INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at, updated_at)
SELECT DISTINCT rp.tenant_id, rp.role_id, permission.id, NOW(), NOW()
FROM app_role_permission rp
JOIN app_permission old_permission
  ON old_permission.id = rp.permission_id
 AND old_permission.code = 'tenant:view'
 AND old_permission.deleted_at IS NULL
JOIN app_permission permission
  ON permission.code = 'menu:view'
 AND permission.deleted_at IS NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM app_role_permission existing
    WHERE existing.tenant_id = rp.tenant_id
      AND existing.role_id = rp.role_id
      AND existing.permission_id = permission.id
);
