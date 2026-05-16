-- Backfill ERP column permissions added after the original column-permission seed.
CREATE TEMP TABLE tmp_erp_column_permission (
    code TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT
) ON COMMIT DROP;

INSERT INTO tmp_erp_column_permission (code, name, description)
VALUES
        ('column:erp-supplier:taxNo', 'ERP供应商-税号列', 'ERP供应商税号列显示'),
        ('column:erp-supplier:address', 'ERP供应商-地址列', 'ERP供应商地址列显示'),
        ('column:erp-supplier:bankAccount', 'ERP供应商-银行账号列', 'ERP供应商银行账号列显示'),
        ('column:erp-supplier:recentTransactionAt', 'ERP供应商-最近交易时间列', 'ERP供应商最近交易时间列显示'),
        ('column:erp-supplier:createdAt', 'ERP供应商-创建时间列', 'ERP供应商创建时间列显示'),
        ('column:erp-supplier:updatedAt', 'ERP供应商-更新时间列', 'ERP供应商更新时间列显示'),
        ('column:erp-settlement-method:fundInputMode', 'ERP结算方式-即时收付款列', 'ERP结算方式即时收付款列显示'),
        ('column:erp-purchase-return:orderNo', 'ERP采购退货-单号列', 'ERP采购退货单号列显示'),
        ('column:erp-purchase-return:supplier', 'ERP采购退货-供应商列', 'ERP采购退货供应商列显示'),
        ('column:erp-purchase-return:status', 'ERP采购退货-状态列', 'ERP采购退货状态列显示'),
        ('column:erp-purchase-return:totalAmount', 'ERP采购退货-总金额列', 'ERP采购退货总金额列显示'),
        ('column:erp-purchase-return:createdAt', 'ERP采购退货-创建时间列', 'ERP采购退货创建时间列显示'),
        ('column:erp-sale:netSaleAmount', 'ERP销售-销售净额列', 'ERP销售销售净额列显示'),
        ('column:erp-sale:netGrossProfit', 'ERP销售-毛利列', 'ERP销售毛利列显示'),
        ('column:erp-sale:receivableStatus', 'ERP销售-收款状态列', 'ERP销售收款状态列显示'),
        ('column:erp-sale:returnStatus', 'ERP销售-退货状态列', 'ERP销售退货状态列显示'),
        ('column:erp-sale:redFlushTrace', 'ERP销售-红冲追踪列', 'ERP销售红冲追踪列显示'),
        ('column:erp-sale-return:orderNo', 'ERP销售退货-单号列', 'ERP销售退货单号列显示'),
        ('column:erp-sale-return:customer', 'ERP销售退货-客户列', 'ERP销售退货客户列显示'),
        ('column:erp-sale-return:status', 'ERP销售退货-状态列', 'ERP销售退货状态列显示'),
        ('column:erp-sale-return:totalAmount', 'ERP销售退货-总金额列', 'ERP销售退货总金额列显示'),
        ('column:erp-sale-return:refundStatus', 'ERP销售退货-退款状态列', 'ERP销售退货退款状态列显示'),
        ('column:erp-sale-return:createdAt', 'ERP销售退货-创建时间列', 'ERP销售退货创建时间列显示'),
        ('column:erp-vehicle-brand:code', 'ERP车型品牌-编码列', 'ERP车型品牌编码列显示'),
        ('column:erp-vehicle-brand:name', 'ERP车型品牌-名称列', 'ERP车型品牌名称列显示'),
        ('column:erp-vehicle-brand:enabled', 'ERP车型品牌-状态列', 'ERP车型品牌状态列显示'),
        ('column:erp-vehicle-brand:remark', 'ERP车型品牌-备注列', 'ERP车型品牌备注列显示'),
        ('column:erp-vehicle-series:code', 'ERP车型车系-编码列', 'ERP车型车系编码列显示'),
        ('column:erp-vehicle-series:brand', 'ERP车型车系-品牌列', 'ERP车型车系品牌列显示'),
        ('column:erp-vehicle-series:name', 'ERP车型车系-名称列', 'ERP车型车系名称列显示'),
        ('column:erp-vehicle-series:enabled', 'ERP车型车系-状态列', 'ERP车型车系状态列显示'),
        ('column:erp-vehicle-series:remark', 'ERP车型车系-备注列', 'ERP车型车系备注列显示'),
        ('column:erp-vehicle-model:code', 'ERP车型-编码列', 'ERP车型编码列显示'),
        ('column:erp-vehicle-model:series', 'ERP车型-车系列', 'ERP车型车系列显示'),
        ('column:erp-vehicle-model:name', 'ERP车型-名称列', 'ERP车型名称列显示'),
        ('column:erp-vehicle-model:yearFrom', 'ERP车型-起始年份列', 'ERP车型起始年份列显示'),
        ('column:erp-vehicle-model:yearTo', 'ERP车型-结束年份列', 'ERP车型结束年份列显示'),
        ('column:erp-vehicle-model:displacement', 'ERP车型-排量列', 'ERP车型排量列显示'),
        ('column:erp-vehicle-model:engine', 'ERP车型-发动机列', 'ERP车型发动机列显示'),
        ('column:erp-vehicle-model:enabled', 'ERP车型-状态列', 'ERP车型状态列显示'),
        ('column:erp-vehicle-model:remark', 'ERP车型-备注列', 'ERP车型备注列显示'),
        ('column:erp-product-fitment:product', 'ERP商品适配车型-商品列', 'ERP商品适配车型商品列显示'),
        ('column:erp-product-fitment:vehicleModel', 'ERP商品适配车型-车型列', 'ERP商品适配车型车型列显示'),
        ('column:erp-product-fitment:remark', 'ERP商品适配车型-备注列', 'ERP商品适配车型备注列显示'),
        ('column:erp-ar:customerName', 'ERP应收-客户列', 'ERP应收客户列显示'),
        ('column:erp-ar:orderNo', 'ERP应收-订单号列', 'ERP应收订单号列显示'),
        ('column:erp-ar:status', 'ERP应收-状态列', 'ERP应收状态列显示'),
        ('column:erp-ar:totalAmount', 'ERP应收-总金额列', 'ERP应收总金额列显示'),
        ('column:erp-ar:createdAt', 'ERP应收-创建时间列', 'ERP应收创建时间列显示'),
        ('column:erp-ap:supplierName', 'ERP应付-供应商列', 'ERP应付供应商列显示'),
        ('column:erp-ap:orderNo', 'ERP应付-订单号列', 'ERP应付订单号列显示'),
        ('column:erp-ap:status', 'ERP应付-状态列', 'ERP应付状态列显示'),
        ('column:erp-ap:totalAmount', 'ERP应付-总金额列', 'ERP应付总金额列显示'),
        ('column:erp-ap:paidAmount', 'ERP应付-已付金额列', 'ERP应付已付金额列显示'),
        ('column:erp-ap:discountAmount', 'ERP应付-优惠金额列', 'ERP应付优惠金额列显示'),
        ('column:erp-ap:unpaidAmount', 'ERP应付-未付金额列', 'ERP应付未付金额列显示'),
        ('column:erp-ap:createdAt', 'ERP应付-创建时间列', 'ERP应付创建时间列显示'),
        ('column:erp-receipt:receiptNo', 'ERP收款单-单号列', 'ERP收款单单号列显示'),
        ('column:erp-receipt:customerName', 'ERP收款单-客户列', 'ERP收款单客户列显示'),
        ('column:erp-receipt:status', 'ERP收款单-状态列', 'ERP收款单状态列显示'),
        ('column:erp-receipt:amount', 'ERP收款单-金额列', 'ERP收款单金额列显示'),
        ('column:erp-receipt:discountAmount', 'ERP收款单-优惠金额列', 'ERP收款单优惠金额列显示'),
        ('column:erp-receipt:createdAt', 'ERP收款单-创建时间列', 'ERP收款单创建时间列显示'),
        ('column:erp-payment:paymentNo', 'ERP付款单-单号列', 'ERP付款单单号列显示'),
        ('column:erp-payment:supplierName', 'ERP付款单-供应商列', 'ERP付款单供应商列显示'),
        ('column:erp-payment:status', 'ERP付款单-状态列', 'ERP付款单状态列显示'),
        ('column:erp-payment:amount', 'ERP付款单-金额列', 'ERP付款单金额列显示'),
        ('column:erp-payment:discountAmount', 'ERP付款单-优惠金额列', 'ERP付款单优惠金额列显示'),
        ('column:erp-payment:createdAt', 'ERP付款单-创建时间列', 'ERP付款单创建时间列显示'),
        ('column:erp-finance-customer-debt:customerName', 'ERP客户欠款-客户列', 'ERP客户欠款客户列显示'),
        ('column:erp-finance-customer-debt:totalDebt', 'ERP客户欠款-欠款总额列', 'ERP客户欠款欠款总额列显示'),
        ('column:erp-finance-supplier-debt:supplierName', 'ERP供应商欠款-供应商列', 'ERP供应商欠款供应商列显示'),
        ('column:erp-finance-supplier-debt:totalDebt', 'ERP供应商欠款-欠款总额列', 'ERP供应商欠款欠款总额列显示'),
        ('column:erp-print-template:code', 'ERP打印模板-编码列', 'ERP打印模板编码列显示'),
        ('column:erp-print-template:name', 'ERP打印模板-名称列', 'ERP打印模板名称列显示'),
        ('column:erp-print-template:docType', 'ERP打印模板-单据类型列', 'ERP打印模板单据类型列显示'),
        ('column:erp-print-template:sortNo', 'ERP打印模板-排序列', 'ERP打印模板排序列显示'),
        ('column:erp-print-template:enabled', 'ERP打印模板-状态列', 'ERP打印模板状态列显示'),
        ('column:erp-stock-warning:productCode', 'ERP库存预警-商品编码列', 'ERP库存预警商品编码列显示'),
        ('column:erp-stock-warning:productName', 'ERP库存预警-商品名称列', 'ERP库存预警商品名称列显示'),
        ('column:erp-stock-warning:categoryName', 'ERP库存预警-分类列', 'ERP库存预警分类列显示'),
        ('column:erp-stock-warning:unitName', 'ERP库存预警-单位列', 'ERP库存预警单位列显示'),
        ('column:erp-stock-warning:totalQty', 'ERP库存预警-总库存列', 'ERP库存预警总库存列显示'),
        ('column:erp-stock-warning:minStock', 'ERP库存预警-最低库存列', 'ERP库存预警最低库存列显示'),
        ('column:erp-stock-warning:maxStock', 'ERP库存预警-最高库存列', 'ERP库存预警最高库存列显示'),
        ('column:erp-stock-warning:status', 'ERP库存预警-状态列', 'ERP库存预警状态列显示'),
        ('column:erp-stock-warning:defaultWarehouse', 'ERP库存预警-默认仓库列', 'ERP库存预警默认仓库列显示'),
        ('column:erp-stock-warning:defaultLocation', 'ERP库存预警-默认库位列', 'ERP库存预警默认库位列显示'),
        ('column:erp-stock-count:countNo', 'ERP库存调整-单号列', 'ERP库存调整单号列显示'),
        ('column:erp-stock-count:status', 'ERP库存调整-状态列', 'ERP库存调整状态列显示'),
        ('column:erp-stock-count:countAt', 'ERP库存调整-盘点时间列', 'ERP库存调整盘点时间列显示'),
        ('column:erp-stock-count:adjustmentReason', 'ERP库存调整-调整原因列', 'ERP库存调整调整原因列显示'),
        ('column:erp-stock-count:remark', 'ERP库存调整-备注列', 'ERP库存调整备注列显示'),
        ('column:erp-stock-count:createdAt', 'ERP库存调整-创建时间列', 'ERP库存调整创建时间列显示'),
        ('column:erp-stock-init:countNo', 'ERP初始库存-单号列', 'ERP初始库存单号列显示'),
        ('column:erp-stock-init:status', 'ERP初始库存-状态列', 'ERP初始库存状态列显示'),
        ('column:erp-stock-init:countAt', 'ERP初始库存-盘点时间列', 'ERP初始库存盘点时间列显示'),
        ('column:erp-stock-init:remark', 'ERP初始库存-备注列', 'ERP初始库存备注列显示'),
        ('column:erp-stock-init:createdAt', 'ERP初始库存-创建时间列', 'ERP初始库存创建时间列显示'),
        ('column:erp-stock-transfer:transferNo', 'ERP库存移库-单号列', 'ERP库存移库单号列显示'),
        ('column:erp-stock-transfer:status', 'ERP库存移库-状态列', 'ERP库存移库状态列显示'),
        ('column:erp-stock-transfer:transferAt', 'ERP库存移库-移库时间列', 'ERP库存移库移库时间列显示'),
        ('column:erp-stock-transfer:remark', 'ERP库存移库-备注列', 'ERP库存移库备注列显示'),
        ('column:erp-stock-transfer:printCount', 'ERP库存移库-打印次数列', 'ERP库存移库打印次数列显示'),
        ('column:erp-stock-transfer:lastPrintedAt', 'ERP库存移库-最后打印时间列', 'ERP库存移库最后打印时间列显示'),
        ('column:erp-stock-txn:docNo', 'ERP流水-单据号列', 'ERP流水单据号列显示'),
        ('column:erp-stock-txn:adjustmentReason', 'ERP流水-调整原因列', 'ERP流水调整原因列显示'),
        ('column:erp-stock-txn:operator', 'ERP流水-操作人列', 'ERP流水操作人列显示'),
        ('column:erp-stock-txn:remark', 'ERP流水-备注列', 'ERP流水备注列显示'),
        ('column:erp-assemble-order:orderNo', 'ERP组装单-单号列', 'ERP组装单单号列显示'),
        ('column:erp-assemble-order:orderType', 'ERP组装单-类型列', 'ERP组装单类型列显示'),
        ('column:erp-assemble-order:finishedProduct', 'ERP组装单-成品列', 'ERP组装单成品列显示'),
        ('column:erp-assemble-order:finishedQty', 'ERP组装单-成品数量列', 'ERP组装单成品数量列显示'),
        ('column:erp-assemble-order:totalCost', 'ERP组装单-总成本列', 'ERP组装单总成本列显示'),
        ('column:erp-assemble-order:status', 'ERP组装单-状态列', 'ERP组装单状态列显示'),
        ('column:erp-assemble-order:orderAt', 'ERP组装单-单据时间列', 'ERP组装单单据时间列显示'),
        ('column:erp-disassemble-order:orderNo', 'ERP拆卸单-单号列', 'ERP拆卸单单号列显示'),
        ('column:erp-disassemble-order:orderType', 'ERP拆卸单-类型列', 'ERP拆卸单类型列显示'),
        ('column:erp-disassemble-order:finishedProduct', 'ERP拆卸单-成品列', 'ERP拆卸单成品列显示'),
        ('column:erp-disassemble-order:finishedQty', 'ERP拆卸单-成品数量列', 'ERP拆卸单成品数量列显示'),
        ('column:erp-disassemble-order:totalCost', 'ERP拆卸单-总成本列', 'ERP拆卸单总成本列显示'),
        ('column:erp-disassemble-order:status', 'ERP拆卸单-状态列', 'ERP拆卸单状态列显示'),
        ('column:erp-disassemble-order:orderAt', 'ERP拆卸单-单据时间列', 'ERP拆卸单单据时间列显示');

UPDATE app_permission permission
SET name = column_permission.name,
    description = column_permission.description,
    is_enabled = TRUE,
    updated_at = NOW(),
    deleted_at = NULL
FROM tmp_erp_column_permission column_permission
WHERE permission.code = column_permission.code;

INSERT INTO app_permission (code, name, description, is_enabled, created_at, updated_at)
SELECT column_permission.code, column_permission.name, column_permission.description, TRUE, NOW(), NOW()
FROM tmp_erp_column_permission column_permission
WHERE NOT EXISTS (
    SELECT 1
    FROM app_permission existing
    WHERE existing.code = column_permission.code
);

WITH page_permission(page_key, view_code) AS (
    VALUES
        ('erp-supplier', 'erp-supplier:view'),
        ('erp-settlement-method', 'erp-settlement-method:view'),
        ('erp-purchase-return', 'erp-purchase-return:view'),
        ('erp-sale', 'erp-sale:view'),
        ('erp-sale-return', 'erp-sale-return:view'),
        ('erp-vehicle-brand', 'erp-vehicle-brand:view'),
        ('erp-vehicle-series', 'erp-vehicle-series:view'),
        ('erp-vehicle-model', 'erp-vehicle-model:view'),
        ('erp-product-fitment', 'erp-product-fitment:view'),
        ('erp-ar', 'erp-ar:view'),
        ('erp-ap', 'erp-ap:view'),
        ('erp-receipt', 'erp-receipt:view'),
        ('erp-payment', 'erp-payment:view'),
        ('erp-finance-customer-debt', 'erp-finance-customer-debt:view'),
        ('erp-finance-supplier-debt', 'erp-finance-supplier-debt:view'),
        ('erp-print-template', 'erp-print-template:view'),
        ('erp-stock-warning', 'erp-stock-warning:view'),
        ('erp-stock-count', 'erp-stock-count:view'),
        ('erp-stock-init', 'erp-stock-init:view'),
        ('erp-stock-transfer', 'erp-stock-transfer:view'),
        ('erp-stock-txn', 'erp-stock-txn:view'),
        ('erp-assemble-order', 'erp-assemble-order:view'),
        ('erp-disassemble-order', 'erp-disassemble-order:view')
)
INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at, updated_at)
SELECT DISTINCT rp.tenant_id, rp.role_id, column_perm.id, NOW(), NOW()
FROM page_permission mapping
JOIN app_permission view_perm
  ON view_perm.code = mapping.view_code
 AND view_perm.deleted_at IS NULL
JOIN app_role_permission rp
  ON rp.permission_id = view_perm.id
 AND rp.deleted_at IS NULL
JOIN app_permission column_perm
  ON column_perm.code LIKE ('column:' || mapping.page_key || ':%')
 AND column_perm.deleted_at IS NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM app_role_permission existing
    WHERE existing.tenant_id = rp.tenant_id
      AND existing.role_id = rp.role_id
      AND existing.permission_id = column_perm.id
      AND existing.deleted_at IS NULL
);

INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at, updated_at)
SELECT DISTINCT role.tenant_id, role.id, permission.id, NOW(), NOW()
FROM app_role role
JOIN app_permission permission
  ON permission.code LIKE 'column:erp-%'
 AND permission.deleted_at IS NULL
WHERE role.code IN ('admin', 'super_admin')
  AND role.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM app_role_permission existing
      WHERE existing.tenant_id = role.tenant_id
        AND existing.role_id = role.id
        AND existing.permission_id = permission.id
        AND existing.deleted_at IS NULL
  );
