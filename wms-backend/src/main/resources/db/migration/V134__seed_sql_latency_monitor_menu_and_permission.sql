-- Seed SQL latency monitor permission and menu for existing databases.

INSERT INTO app_permission (code, name, description, is_enabled, created_at, updated_at)
SELECT seed.code, seed.name, seed.description, TRUE, NOW(), NOW()
FROM (
    VALUES
        ('sql-latency-monitor:view', '查看SQL耗时查询', '查看SQL耗时历史记录')
) AS seed(code, name, description)
WHERE NOT EXISTS (
    SELECT 1
    FROM app_permission existing
    WHERE existing.code = seed.code
      AND existing.deleted_at IS NULL
);

INSERT INTO app_menu (code, parent_id, title, i18n_key, path, icon, permission_code, sort, is_enabled, created_at, updated_at)
SELECT
    'sql-latency-monitor',
    parent_menu.id,
    'SQL耗时查询',
    'sql-latency-monitor',
    '/sql-latency-monitor',
    NULL,
    'sql-latency-monitor:view',
    38,
    TRUE,
    NOW(),
    NOW()
FROM app_menu parent_menu
WHERE parent_menu.code = 'system'
  AND parent_menu.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM app_menu existing
      WHERE existing.code = 'sql-latency-monitor'
        AND existing.deleted_at IS NULL
  );

UPDATE app_menu
SET parent_id = parent_menu.id,
    title = 'SQL耗时查询',
    i18n_key = 'sql-latency-monitor',
    path = '/sql-latency-monitor',
    permission_code = 'sql-latency-monitor:view',
    sort = 38,
    is_enabled = TRUE,
    updated_at = NOW()
FROM app_menu parent_menu
WHERE app_menu.code = 'sql-latency-monitor'
  AND parent_menu.code = 'system'
  AND app_menu.deleted_at IS NULL
  AND parent_menu.deleted_at IS NULL;

INSERT INTO app_tenant_menu (tenant_id, menu_id, is_enabled, created_at, updated_at)
SELECT tenant.id, menu.id, TRUE, NOW(), NOW()
FROM app_tenant tenant
JOIN app_menu menu
  ON menu.code = 'sql-latency-monitor'
 AND menu.deleted_at IS NULL
WHERE tenant.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM app_tenant_menu existing
      WHERE existing.tenant_id = tenant.id
        AND existing.menu_id = menu.id
  );

INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at, updated_at)
SELECT DISTINCT role.tenant_id, role.id, permission.id, NOW(), NOW()
FROM app_role role
JOIN app_permission permission
  ON permission.code = 'sql-latency-monitor:view'
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
 AND old_permission.code = 'audit:view'
 AND old_permission.deleted_at IS NULL
JOIN app_permission permission
  ON permission.code = 'sql-latency-monitor:view'
 AND permission.deleted_at IS NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM app_role_permission existing
    WHERE existing.tenant_id = rp.tenant_id
      AND existing.role_id = rp.role_id
      AND existing.permission_id = permission.id
);
