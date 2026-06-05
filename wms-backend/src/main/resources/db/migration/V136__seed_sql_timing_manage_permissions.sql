INSERT INTO app_permission (code, name, description, is_enabled, created_at, updated_at)
SELECT seed.code, seed.name, seed.description, TRUE, NOW(), NOW()
FROM (
    VALUES
        ('system-config:sql-timing:view', '查看SQL采集设置', '查看SQL耗时采集相关系统配置'),
        ('system-config:sql-timing:edit', '编辑SQL采集设置', '编辑SQL耗时采集相关系统配置')
) AS seed(code, name, description)
WHERE NOT EXISTS (
    SELECT 1
    FROM app_permission existing
    WHERE existing.code = seed.code
      AND existing.deleted_at IS NULL
);

INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at, updated_at)
SELECT DISTINCT role.tenant_id, role.id, permission.id, NOW(), NOW()
FROM app_role role
JOIN app_permission permission
  ON permission.code IN ('system-config:sql-timing:view', 'system-config:sql-timing:edit')
 AND permission.deleted_at IS NULL
WHERE role.code = 'super_admin'
  AND role.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM app_role_permission existing
      WHERE existing.tenant_id = role.tenant_id
        AND existing.role_id = role.id
        AND existing.permission_id = permission.id
        AND existing.deleted_at IS NULL
  );

INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at, updated_at)
SELECT DISTINCT role.tenant_id, role.id, permission.id, NOW(), NOW()
FROM app_role role
JOIN app_permission permission
  ON permission.code IN ('system-config:sql-timing:view', 'system-config:sql-timing:edit')
 AND permission.deleted_at IS NULL
WHERE role.code = 'admin'
  AND role.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM app_role_permission existing
      WHERE existing.tenant_id = role.tenant_id
        AND existing.role_id = role.id
        AND existing.permission_id = permission.id
        AND existing.deleted_at IS NULL
  );
