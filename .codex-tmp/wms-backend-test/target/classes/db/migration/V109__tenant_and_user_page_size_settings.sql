-- Add tenant-setting permissions/menu and stop exposing default.page.size as public platform config.

INSERT INTO app_permission (code, name, description, is_enabled, created_at, updated_at)
SELECT seed.code, seed.name, seed.description, TRUE, NOW(), NOW()
FROM (
    VALUES
        ('tenant-setting:view', '查看租户设置', '查看租户展示默认配置'),
        ('tenant-setting:edit', '编辑租户设置', '编辑租户展示默认配置')
) AS seed(code, name, description)
WHERE NOT EXISTS (
    SELECT 1
    FROM app_permission existing
    WHERE existing.code = seed.code
      AND existing.deleted_at IS NULL
);

INSERT INTO app_menu (code, parent_id, title, i18n_key, path, icon, permission_code, sort, is_enabled, created_at, updated_at)
SELECT
    'tenant-setting',
    parent.id,
    '租户设置',
    'tenant-setting',
    '/tenant-settings',
    NULL,
    'tenant-setting:view',
    39,
    TRUE,
    NOW(),
    NOW()
FROM app_menu parent
WHERE parent.code = 'system'
  AND parent.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM app_menu existing
      WHERE existing.code = 'tenant-setting'
        AND existing.deleted_at IS NULL
  );

INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at, updated_at)
SELECT DISTINCT role.tenant_id, role.id, permission.id, NOW(), NOW()
FROM app_role role
JOIN app_permission permission
  ON permission.code IN ('tenant-setting:view', 'tenant-setting:edit')
 AND permission.deleted_at IS NULL
WHERE role.code IN ('admin', 'super_admin')
  AND role.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM app_role_permission existing
      WHERE existing.tenant_id = role.tenant_id
        AND existing.role_id = role.id
        AND existing.permission_id = permission.id
  );

UPDATE app_system_config
SET is_public = FALSE,
    updated_at = NOW()
WHERE config_key = 'default.page.size'
  AND is_public = TRUE;
