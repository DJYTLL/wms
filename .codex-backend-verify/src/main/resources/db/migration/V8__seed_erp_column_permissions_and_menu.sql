-- ERP 列权限与库存流水菜单补齐（幂等）
-- 权限种子：ERP 列权限
INSERT INTO app_permission (code, name, description, is_enabled, created_at, updated_at) VALUES
    ('column:erp-product:code', 'ERP商品-编码列', 'ERP商品编码列显示', TRUE, NOW(), NOW()),
    ('column:erp-product:name', 'ERP商品-名称列', 'ERP商品名称列显示', TRUE, NOW(), NOW()),
    ('column:erp-product:category', 'ERP商品-分类列', 'ERP商品分类列显示', TRUE, NOW(), NOW()),
    ('column:erp-product:unit', 'ERP商品-单位列', 'ERP商品单位列显示', TRUE, NOW(), NOW()),
    ('column:erp-product:price', 'ERP商品-价格列', 'ERP商品价格列显示', TRUE, NOW(), NOW()),
    ('column:erp-product:status', 'ERP商品-状态列', 'ERP商品状态列显示', TRUE, NOW(), NOW()),

    ('column:erp-customer:code', 'ERP客户-编码列', 'ERP客户编码列显示', TRUE, NOW(), NOW()),
    ('column:erp-customer:name', 'ERP客户-名称列', 'ERP客户名称列显示', TRUE, NOW(), NOW()),
    ('column:erp-customer:contact', 'ERP客户-联系人列', 'ERP客户联系人列显示', TRUE, NOW(), NOW()),
    ('column:erp-customer:phone', 'ERP客户-电话列', 'ERP客户电话列显示', TRUE, NOW(), NOW()),
    ('column:erp-customer:email', 'ERP客户-邮箱列', 'ERP客户邮箱列显示', TRUE, NOW(), NOW()),
    ('column:erp-customer:status', 'ERP客户-状态列', 'ERP客户状态列显示', TRUE, NOW(), NOW()),

    ('column:erp-supplier:code', 'ERP供应商-编码列', 'ERP供应商编码列显示', TRUE, NOW(), NOW()),
    ('column:erp-supplier:name', 'ERP供应商-名称列', 'ERP供应商名称列显示', TRUE, NOW(), NOW()),
    ('column:erp-supplier:contact', 'ERP供应商-联系人列', 'ERP供应商联系人列显示', TRUE, NOW(), NOW()),
    ('column:erp-supplier:phone', 'ERP供应商-电话列', 'ERP供应商电话列显示', TRUE, NOW(), NOW()),
    ('column:erp-supplier:mobile', 'ERP供应商-手机列', 'ERP供应商手机列显示', TRUE, NOW(), NOW()),
    ('column:erp-supplier:email', 'ERP供应商-邮箱列', 'ERP供应商邮箱列显示', TRUE, NOW(), NOW()),
    ('column:erp-supplier:status', 'ERP供应商-状态列', 'ERP供应商状态列显示', TRUE, NOW(), NOW()),

    ('column:erp-warehouse:code', 'ERP仓库-编码列', 'ERP仓库编码列显示', TRUE, NOW(), NOW()),
    ('column:erp-warehouse:name', 'ERP仓库-名称列', 'ERP仓库名称列显示', TRUE, NOW(), NOW()),
    ('column:erp-warehouse:address', 'ERP仓库-地址列', 'ERP仓库地址列显示', TRUE, NOW(), NOW()),
    ('column:erp-warehouse:manager', 'ERP仓库-负责人列', 'ERP仓库负责人列显示', TRUE, NOW(), NOW()),
    ('column:erp-warehouse:phone', 'ERP仓库-电话列', 'ERP仓库电话列显示', TRUE, NOW(), NOW()),
    ('column:erp-warehouse:status', 'ERP仓库-状态列', 'ERP仓库状态列显示', TRUE, NOW(), NOW()),

    ('column:erp-location:code', 'ERP库位-编码列', 'ERP库位编码列显示', TRUE, NOW(), NOW()),
    ('column:erp-location:name', 'ERP库位-名称列', 'ERP库位名称列显示', TRUE, NOW(), NOW()),
    ('column:erp-location:warehouse', 'ERP库位-仓库列', 'ERP库位仓库列显示', TRUE, NOW(), NOW()),
    ('column:erp-location:aisle', 'ERP库位-巷道列', 'ERP库位巷道列显示', TRUE, NOW(), NOW()),
    ('column:erp-location:rack', 'ERP库位-货架列', 'ERP库位货架列显示', TRUE, NOW(), NOW()),
    ('column:erp-location:bin', 'ERP库位-货位列', 'ERP库位货位列显示', TRUE, NOW(), NOW()),
    ('column:erp-location:status', 'ERP库位-状态列', 'ERP库位状态列显示', TRUE, NOW(), NOW()),

    ('column:erp-category:code', 'ERP分类-编码列', 'ERP分类编码列显示', TRUE, NOW(), NOW()),
    ('column:erp-category:name', 'ERP分类-名称列', 'ERP分类名称列显示', TRUE, NOW(), NOW()),
    ('column:erp-category:parent', 'ERP分类-上级列', 'ERP分类上级列显示', TRUE, NOW(), NOW()),
    ('column:erp-category:level', 'ERP分类-层级列', 'ERP分类层级列显示', TRUE, NOW(), NOW()),
    ('column:erp-category:sort', 'ERP分类-排序列', 'ERP分类排序列显示', TRUE, NOW(), NOW()),
    ('column:erp-category:status', 'ERP分类-状态列', 'ERP分类状态列显示', TRUE, NOW(), NOW()),

    ('column:erp-unit:code', 'ERP单位-编码列', 'ERP单位编码列显示', TRUE, NOW(), NOW()),
    ('column:erp-unit:name', 'ERP单位-名称列', 'ERP单位名称列显示', TRUE, NOW(), NOW()),
    ('column:erp-unit:symbol', 'ERP单位-符号列', 'ERP单位符号列显示', TRUE, NOW(), NOW()),
    ('column:erp-unit:precision', 'ERP单位-精度列', 'ERP单位精度列显示', TRUE, NOW(), NOW()),
    ('column:erp-unit:status', 'ERP单位-状态列', 'ERP单位状态列显示', TRUE, NOW(), NOW()),

    ('column:erp-purchase:orderNo', 'ERP采购-单号列', 'ERP采购单号列显示', TRUE, NOW(), NOW()),
    ('column:erp-purchase:supplier', 'ERP采购-供应商列', 'ERP采购供应商列显示', TRUE, NOW(), NOW()),
    ('column:erp-purchase:status', 'ERP采购-状态列', 'ERP采购状态列显示', TRUE, NOW(), NOW()),
    ('column:erp-purchase:totalAmount', 'ERP采购-总金额列', 'ERP采购总金额列显示', TRUE, NOW(), NOW()),
    ('column:erp-purchase:createdAt', 'ERP采购-创建时间列', 'ERP采购创建时间列显示', TRUE, NOW(), NOW()),

    ('column:erp-sale:orderNo', 'ERP销售-单号列', 'ERP销售单号列显示', TRUE, NOW(), NOW()),
    ('column:erp-sale:customer', 'ERP销售-客户列', 'ERP销售客户列显示', TRUE, NOW(), NOW()),
    ('column:erp-sale:status', 'ERP销售-状态列', 'ERP销售状态列显示', TRUE, NOW(), NOW()),
    ('column:erp-sale:totalAmount', 'ERP销售-总金额列', 'ERP销售总金额列显示', TRUE, NOW(), NOW()),
    ('column:erp-sale:createdAt', 'ERP销售-创建时间列', 'ERP销售创建时间列显示', TRUE, NOW(), NOW()),

    ('column:erp-stock:product', 'ERP库存-商品列', 'ERP库存商品列显示', TRUE, NOW(), NOW()),
    ('column:erp-stock:warehouse', 'ERP库存-仓库列', 'ERP库存仓库列显示', TRUE, NOW(), NOW()),
    ('column:erp-stock:location', 'ERP库存-库位列', 'ERP库存库位列显示', TRUE, NOW(), NOW()),
    ('column:erp-stock:qty', 'ERP库存-数量列', 'ERP库存数量列显示', TRUE, NOW(), NOW()),
    ('column:erp-stock:updatedAt', 'ERP库存-更新时间列', 'ERP库存更新时间列显示', TRUE, NOW(), NOW()),

    ('column:erp-stock-txn:txnNo', 'ERP流水-流水号列', 'ERP流水流水号列显示', TRUE, NOW(), NOW()),
    ('column:erp-stock-txn:bizType', 'ERP流水-业务类型列', 'ERP流水业务类型列显示', TRUE, NOW(), NOW()),
    ('column:erp-stock-txn:product', 'ERP流水-商品列', 'ERP流水商品列显示', TRUE, NOW(), NOW()),
    ('column:erp-stock-txn:warehouse', 'ERP流水-仓库列', 'ERP流水仓库列显示', TRUE, NOW(), NOW()),
    ('column:erp-stock-txn:location', 'ERP流水-库位列', 'ERP流水库位列显示', TRUE, NOW(), NOW()),
    ('column:erp-stock-txn:qtyDelta', 'ERP流水-变更数量列', 'ERP流水变更数量列显示', TRUE, NOW(), NOW()),
    ('column:erp-stock-txn:qtyBefore', 'ERP流水-变更前列', 'ERP流水变更前列显示', TRUE, NOW(), NOW()),
    ('column:erp-stock-txn:qtyAfter', 'ERP流水-变更后列', 'ERP流水变更后列显示', TRUE, NOW(), NOW()),
    ('column:erp-stock-txn:createdAt', 'ERP流水-时间列', 'ERP流水时间列显示', TRUE, NOW(), NOW())
ON CONFLICT (code) DO NOTHING;

-- ERP 菜单：库存流水
INSERT INTO app_menu (code, parent_id, title, i18n_key, path, icon, permission_code, sort, is_enabled, created_at, updated_at)
SELECT 'erp-stock-txn', p.id, '库存流水', 'erp-stock-txn', '/erp/stock-txns', NULL, 'erp-stock-txn:view', 50, TRUE, NOW(), NOW()
FROM app_menu p
WHERE p.code = 'erp'
ON CONFLICT (code) DO NOTHING;

-- 为所有租户补齐库存流水菜单
INSERT INTO app_tenant_menu (tenant_id, menu_id, is_enabled, created_at, updated_at)
SELECT t.id, m.id, TRUE, NOW(), NOW()
FROM app_tenant t
JOIN app_menu m ON m.code = 'erp-stock-txn'
WHERE t.deleted_at IS NULL
ON CONFLICT (tenant_id, menu_id) DO NOTHING;

-- 为 admin / super_admin 补齐 ERP 列权限
INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at)
SELECT r.tenant_id, r.id, p.id, NOW()
FROM app_role r
JOIN app_permission p ON p.code LIKE 'column:erp-%'
WHERE r.code IN ('admin', 'super_admin')
AND NOT EXISTS (
    SELECT 1
    FROM app_role_permission rp
    WHERE rp.tenant_id = r.tenant_id
      AND rp.role_id = r.id
      AND rp.permission_id = p.id
);
