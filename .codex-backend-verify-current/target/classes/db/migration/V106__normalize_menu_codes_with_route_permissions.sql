-- 统一叶子菜单编码与路由权限资源前缀。
-- 父级分组仍保留业务分组编码，叶子菜单 code 统一使用 permission_code 冒号前的资源名。

UPDATE app_menu
SET code = 'erp-product-fitment',
    i18n_key = 'erp-product-fitment',
    permission_code = 'erp-product-fitment:view',
    updated_at = NOW()
WHERE code = 'erp-vehicle-fitment'
  AND deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM app_menu m
      WHERE m.code = 'erp-product-fitment'
        AND m.deleted_at IS NULL
  );

UPDATE app_menu
SET code = 'erp-finance-customer-debt',
    i18n_key = 'erp-finance-customer-debt',
    permission_code = 'erp-finance-customer-debt:view',
    updated_at = NOW()
WHERE code = 'erp-finance-summary'
  AND deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM app_menu m
      WHERE m.code = 'erp-finance-customer-debt'
        AND m.deleted_at IS NULL
  );

UPDATE app_menu
SET code = normalized.new_code,
    i18n_key = normalized.new_code,
    permission_code = normalized.permission_code,
    updated_at = NOW()
FROM (
    VALUES
        ('users', 'user', 'user:view'),
        ('roles', 'role', 'role:view'),
        ('permissions', 'permission', 'permission:view'),
        ('audit-logs', 'audit', 'audit:view'),
        ('column-permissions', 'column', 'column:role:manage'),
        ('menu-management', 'menu', 'menu:view'),
        ('tenants', 'tenant', 'tenant:view')
) AS normalized(old_code, new_code, permission_code)
WHERE app_menu.code = normalized.old_code
  AND app_menu.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM app_menu m
      WHERE m.code = normalized.new_code
        AND m.deleted_at IS NULL
  );

UPDATE app_menu
SET permission_code = 'system-config:view',
    updated_at = NOW()
WHERE code = 'system-config'
  AND deleted_at IS NULL
  AND permission_code IS DISTINCT FROM 'system-config:view';
