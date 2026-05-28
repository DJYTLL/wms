-- Remove legacy mock-only Basic Information menu entries.
-- Real master-data pages live under the ERP > Basic menu.
DELETE FROM app_tenant_column_setting
WHERE page_key IN (
    'warehouse-management',
    'shelf-management',
    'product-management',
    'supplier-management',
    'category-management',
    'unit-management'
);

DELETE FROM app_role_permission
WHERE permission_id IN (
    SELECT id
    FROM app_permission
    WHERE code IN (
        'column:warehouse-management:name',
        'column:warehouse-management:code',
        'column:warehouse-management:address',
        'column:warehouse-management:status',
        'column:shelf-management:name',
        'column:shelf-management:shelfCode',
        'column:shelf-management:warehouseLabel',
        'column:shelf-management:capacity',
        'column:shelf-management:status',
        'column:product-management:name',
        'column:product-management:sku',
        'column:product-management:price',
        'column:product-management:unit',
        'column:product-management:categoryLabel',
        'column:product-management:warehouseLabel',
        'column:product-management:shelfLabel',
        'column:product-management:status',
        'column:supplier-management:name',
        'column:supplier-management:contactPerson',
        'column:supplier-management:phone',
        'column:supplier-management:email',
        'column:supplier-management:status',
        'column:category-management:name',
        'column:category-management:code',
        'column:category-management:description',
        'column:category-management:status',
        'column:unit-management:name',
        'column:unit-management:symbol',
        'column:unit-management:status'
    )
);

DELETE FROM app_permission
WHERE code IN (
    'column:warehouse-management:name',
    'column:warehouse-management:code',
    'column:warehouse-management:address',
    'column:warehouse-management:status',
    'column:shelf-management:name',
    'column:shelf-management:shelfCode',
    'column:shelf-management:warehouseLabel',
    'column:shelf-management:capacity',
    'column:shelf-management:status',
    'column:product-management:name',
    'column:product-management:sku',
    'column:product-management:price',
    'column:product-management:unit',
    'column:product-management:categoryLabel',
    'column:product-management:warehouseLabel',
    'column:product-management:shelfLabel',
    'column:product-management:status',
    'column:supplier-management:name',
    'column:supplier-management:contactPerson',
    'column:supplier-management:phone',
    'column:supplier-management:email',
    'column:supplier-management:status',
    'column:category-management:name',
    'column:category-management:code',
    'column:category-management:description',
    'column:category-management:status',
    'column:unit-management:name',
    'column:unit-management:symbol',
    'column:unit-management:status'
);

DELETE FROM app_tenant_menu
WHERE menu_id IN (
    SELECT id
    FROM app_menu
    WHERE code IN (
        'basic',
        'warehouse-management',
        'shelf-management',
        'product-management',
        'supplier-management',
        'category-management',
        'unit-management'
    )
);

DELETE FROM app_menu
WHERE code IN (
    'warehouse-management',
    'shelf-management',
    'product-management',
    'supplier-management',
    'category-management',
    'unit-management',
    'basic'
);
