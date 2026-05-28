-- Remove legacy WMS warehouse/inbound/outbound navigation and permissions.
-- The active product navigation is ERP-only.

DELETE FROM app_tenant_column_setting
WHERE page_key IN ('inbound-management', 'outbound-management');

DELETE FROM app_role_column_setting
WHERE page_key IN ('inbound-management', 'outbound-management');

DELETE FROM app_role_permission
WHERE permission_id IN (
    SELECT id
    FROM app_permission
    WHERE code IN (
        'warehouse:view',
        'warehouse:add',
        'warehouse:edit',
        'warehouse:delete',
        'shelf:view',
        'shelf:add',
        'shelf:edit',
        'shelf:delete',
        'product:view',
        'product:add',
        'product:edit',
        'product:delete',
        'supplier:view',
        'supplier:add',
        'supplier:edit',
        'supplier:delete',
        'category:view',
        'category:add',
        'category:edit',
        'category:delete',
        'unit:view',
        'unit:add',
        'unit:edit',
        'unit:delete',
        'inbound:view',
        'inbound:add',
        'inbound:edit',
        'inbound:delete',
        'outbound:view',
        'column:inbound-management:orderNumber',
        'column:inbound-management:type',
        'column:inbound-management:supplier',
        'column:inbound-management:date',
        'column:inbound-management:status',
        'column:inbound-management:product',
        'column:inbound-management:quantity',
        'column:inbound-management:warehouseLabel',
        'column:inbound-management:shelfLabel'
    )
      AND deleted_at IS NULL
);

DELETE FROM app_permission
WHERE code IN (
    'warehouse:view',
    'warehouse:add',
    'warehouse:edit',
    'warehouse:delete',
    'shelf:view',
    'shelf:add',
    'shelf:edit',
    'shelf:delete',
    'product:view',
    'product:add',
    'product:edit',
    'product:delete',
    'supplier:view',
    'supplier:add',
    'supplier:edit',
    'supplier:delete',
    'category:view',
    'category:add',
    'category:edit',
    'category:delete',
    'unit:view',
    'unit:add',
    'unit:edit',
    'unit:delete',
    'inbound:view',
    'inbound:add',
    'inbound:edit',
    'inbound:delete',
    'outbound:view',
    'column:inbound-management:orderNumber',
    'column:inbound-management:type',
    'column:inbound-management:supplier',
    'column:inbound-management:date',
    'column:inbound-management:status',
    'column:inbound-management:product',
    'column:inbound-management:quantity',
    'column:inbound-management:warehouseLabel',
    'column:inbound-management:shelfLabel'
)
  AND deleted_at IS NULL;

DELETE FROM app_tenant_menu
WHERE menu_id IN (
    SELECT id
    FROM app_menu
    WHERE code IN ('warehouse', 'inbound', 'outbound', 'out-normal', 'out-urgent')
      AND deleted_at IS NULL
);

DELETE FROM app_menu
WHERE code IN ('warehouse', 'inbound', 'outbound', 'out-normal', 'out-urgent')
  AND deleted_at IS NULL;
