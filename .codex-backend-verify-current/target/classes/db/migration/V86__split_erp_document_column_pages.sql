CREATE TEMP TABLE tmp_split_column_permission (
    code TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT
) ON COMMIT DROP;

INSERT INTO tmp_split_column_permission (code, name, description)
VALUES
    ('column:erp-purchase-draft:orderNo', 'ERP采购草稿-单号列', 'ERP采购草稿单号列显示'),
    ('column:erp-purchase-draft:supplier', 'ERP采购草稿-供应商列', 'ERP采购草稿供应商列显示'),
    ('column:erp-purchase-draft:status', 'ERP采购草稿-状态列', 'ERP采购草稿状态列显示'),
    ('column:erp-purchase-draft:totalAmount', 'ERP采购草稿-总金额列', 'ERP采购草稿总金额列显示'),
    ('column:erp-purchase-draft:createdAt', 'ERP采购草稿-创建时间列', 'ERP采购草稿创建时间列显示'),
    ('column:erp-purchase-approved:orderNo', 'ERP采购已审核-单号列', 'ERP采购已审核单号列显示'),
    ('column:erp-purchase-approved:supplier', 'ERP采购已审核-供应商列', 'ERP采购已审核供应商列显示'),
    ('column:erp-purchase-approved:status', 'ERP采购已审核-状态列', 'ERP采购已审核状态列显示'),
    ('column:erp-purchase-approved:totalAmount', 'ERP采购已审核-总金额列', 'ERP采购已审核总金额列显示'),
    ('column:erp-purchase-approved:createdAt', 'ERP采购已审核-创建时间列', 'ERP采购已审核创建时间列显示'),
    ('column:erp-purchase-return-draft:orderNo', 'ERP采购退货草稿-单号列', 'ERP采购退货草稿单号列显示'),
    ('column:erp-purchase-return-draft:supplier', 'ERP采购退货草稿-供应商列', 'ERP采购退货草稿供应商列显示'),
    ('column:erp-purchase-return-draft:status', 'ERP采购退货草稿-状态列', 'ERP采购退货草稿状态列显示'),
    ('column:erp-purchase-return-draft:totalAmount', 'ERP采购退货草稿-总金额列', 'ERP采购退货草稿总金额列显示'),
    ('column:erp-purchase-return-draft:createdAt', 'ERP采购退货草稿-创建时间列', 'ERP采购退货草稿创建时间列显示'),
    ('column:erp-purchase-return-approved:orderNo', 'ERP采购退货已审核-单号列', 'ERP采购退货已审核单号列显示'),
    ('column:erp-purchase-return-approved:supplier', 'ERP采购退货已审核-供应商列', 'ERP采购退货已审核供应商列显示'),
    ('column:erp-purchase-return-approved:status', 'ERP采购退货已审核-状态列', 'ERP采购退货已审核状态列显示'),
    ('column:erp-purchase-return-approved:totalAmount', 'ERP采购退货已审核-总金额列', 'ERP采购退货已审核总金额列显示'),
    ('column:erp-purchase-return-approved:createdAt', 'ERP采购退货已审核-创建时间列', 'ERP采购退货已审核创建时间列显示'),
    ('column:erp-sale-draft:orderNo', 'ERP销售草稿-单号列', 'ERP销售草稿单号列显示'),
    ('column:erp-sale-draft:customer', 'ERP销售草稿-客户列', 'ERP销售草稿客户列显示'),
    ('column:erp-sale-draft:totalAmount', 'ERP销售草稿-总金额列', 'ERP销售草稿总金额列显示'),
    ('column:erp-sale-draft:netSaleAmount', 'ERP销售草稿-销售净额列', 'ERP销售草稿销售净额列显示'),
    ('column:erp-sale-draft:netGrossProfit', 'ERP销售草稿-毛利列', 'ERP销售草稿毛利列显示'),
    ('column:erp-sale-draft:receivableStatus', 'ERP销售草稿-收款状态列', 'ERP销售草稿收款状态列显示'),
    ('column:erp-sale-draft:createdAt', 'ERP销售草稿-创建时间列', 'ERP销售草稿创建时间列显示'),
    ('column:erp-sale-approved:orderNo', 'ERP销售已审核-单号列', 'ERP销售已审核单号列显示'),
    ('column:erp-sale-approved:customer', 'ERP销售已审核-客户列', 'ERP销售已审核客户列显示'),
    ('column:erp-sale-approved:status', 'ERP销售已审核-状态列', 'ERP销售已审核状态列显示'),
    ('column:erp-sale-approved:totalAmount', 'ERP销售已审核-总金额列', 'ERP销售已审核总金额列显示'),
    ('column:erp-sale-approved:netSaleAmount', 'ERP销售已审核-销售净额列', 'ERP销售已审核销售净额列显示'),
    ('column:erp-sale-approved:netGrossProfit', 'ERP销售已审核-毛利列', 'ERP销售已审核毛利列显示'),
    ('column:erp-sale-approved:receivableStatus', 'ERP销售已审核-收款状态列', 'ERP销售已审核收款状态列显示'),
    ('column:erp-sale-approved:returnStatus', 'ERP销售已审核-退货状态列', 'ERP销售已审核退货状态列显示'),
    ('column:erp-sale-approved:redFlushTrace', 'ERP销售已审核-红冲追踪列', 'ERP销售已审核红冲追踪列显示'),
    ('column:erp-sale-approved:createdAt', 'ERP销售已审核-创建时间列', 'ERP销售已审核创建时间列显示'),
    ('column:erp-sale-form:profit', 'ERP销售表单-利润列', 'ERP销售表单利润列显示'),
    ('column:erp-sale-form:discountAllocated', 'ERP销售表单-分摊优惠列', 'ERP销售表单分摊优惠列显示'),
    ('column:erp-sale-return-draft:orderNo', 'ERP销售退货草稿-单号列', 'ERP销售退货草稿单号列显示'),
    ('column:erp-sale-return-draft:customer', 'ERP销售退货草稿-客户列', 'ERP销售退货草稿客户列显示'),
    ('column:erp-sale-return-draft:status', 'ERP销售退货草稿-状态列', 'ERP销售退货草稿状态列显示'),
    ('column:erp-sale-return-draft:totalAmount', 'ERP销售退货草稿-总金额列', 'ERP销售退货草稿总金额列显示'),
    ('column:erp-sale-return-draft:refundStatus', 'ERP销售退货草稿-退款状态列', 'ERP销售退货草稿退款状态列显示'),
    ('column:erp-sale-return-draft:createdAt', 'ERP销售退货草稿-创建时间列', 'ERP销售退货草稿创建时间列显示'),
    ('column:erp-sale-return-approved:orderNo', 'ERP销售退货已审核-单号列', 'ERP销售退货已审核单号列显示'),
    ('column:erp-sale-return-approved:customer', 'ERP销售退货已审核-客户列', 'ERP销售退货已审核客户列显示'),
    ('column:erp-sale-return-approved:status', 'ERP销售退货已审核-状态列', 'ERP销售退货已审核状态列显示'),
    ('column:erp-sale-return-approved:totalAmount', 'ERP销售退货已审核-总金额列', 'ERP销售退货已审核总金额列显示'),
    ('column:erp-sale-return-approved:refundStatus', 'ERP销售退货已审核-退款状态列', 'ERP销售退货已审核退款状态列显示'),
    ('column:erp-sale-return-approved:createdAt', 'ERP销售退货已审核-创建时间列', 'ERP销售退货已审核创建时间列显示');

UPDATE app_permission permission
SET name = source.name,
    description = source.description,
    is_enabled = TRUE,
    updated_at = NOW(),
    deleted_at = NULL
FROM tmp_split_column_permission source
WHERE permission.code = source.code;

INSERT INTO app_permission (code, name, description, is_enabled, created_at, updated_at)
SELECT source.code, source.name, source.description, TRUE, NOW(), NOW()
FROM tmp_split_column_permission source
WHERE NOT EXISTS (
    SELECT 1
    FROM app_permission existing
    WHERE existing.code = source.code
);

WITH column_mapping AS (
    VALUES
        ('erp-purchase', 'orderNo', 'erp-purchase-draft', 'orderNo'),
        ('erp-purchase', 'supplier', 'erp-purchase-draft', 'supplier'),
        ('erp-purchase', 'status', 'erp-purchase-draft', 'status'),
        ('erp-purchase', 'totalAmount', 'erp-purchase-draft', 'totalAmount'),
        ('erp-purchase', 'createdAt', 'erp-purchase-draft', 'createdAt'),
        ('erp-purchase', 'orderNo', 'erp-purchase-approved', 'orderNo'),
        ('erp-purchase', 'supplier', 'erp-purchase-approved', 'supplier'),
        ('erp-purchase', 'status', 'erp-purchase-approved', 'status'),
        ('erp-purchase', 'totalAmount', 'erp-purchase-approved', 'totalAmount'),
        ('erp-purchase', 'createdAt', 'erp-purchase-approved', 'createdAt'),
        ('erp-purchase-return', 'orderNo', 'erp-purchase-return-draft', 'orderNo'),
        ('erp-purchase-return', 'supplier', 'erp-purchase-return-draft', 'supplier'),
        ('erp-purchase-return', 'status', 'erp-purchase-return-draft', 'status'),
        ('erp-purchase-return', 'totalAmount', 'erp-purchase-return-draft', 'totalAmount'),
        ('erp-purchase-return', 'createdAt', 'erp-purchase-return-draft', 'createdAt'),
        ('erp-purchase-return', 'orderNo', 'erp-purchase-return-approved', 'orderNo'),
        ('erp-purchase-return', 'supplier', 'erp-purchase-return-approved', 'supplier'),
        ('erp-purchase-return', 'status', 'erp-purchase-return-approved', 'status'),
        ('erp-purchase-return', 'totalAmount', 'erp-purchase-return-approved', 'totalAmount'),
        ('erp-purchase-return', 'createdAt', 'erp-purchase-return-approved', 'createdAt'),
        ('erp-sale', 'orderNo', 'erp-sale-draft', 'orderNo'),
        ('erp-sale', 'customer', 'erp-sale-draft', 'customer'),
        ('erp-sale', 'totalAmount', 'erp-sale-draft', 'totalAmount'),
        ('erp-sale', 'netSaleAmount', 'erp-sale-draft', 'netSaleAmount'),
        ('erp-sale', 'netGrossProfit', 'erp-sale-draft', 'netGrossProfit'),
        ('erp-sale', 'receivableStatus', 'erp-sale-draft', 'receivableStatus'),
        ('erp-sale', 'createdAt', 'erp-sale-draft', 'createdAt'),
        ('erp-sale', 'orderNo', 'erp-sale-approved', 'orderNo'),
        ('erp-sale', 'customer', 'erp-sale-approved', 'customer'),
        ('erp-sale', 'status', 'erp-sale-approved', 'status'),
        ('erp-sale', 'totalAmount', 'erp-sale-approved', 'totalAmount'),
        ('erp-sale', 'netSaleAmount', 'erp-sale-approved', 'netSaleAmount'),
        ('erp-sale', 'netGrossProfit', 'erp-sale-approved', 'netGrossProfit'),
        ('erp-sale', 'receivableStatus', 'erp-sale-approved', 'receivableStatus'),
        ('erp-sale', 'returnStatus', 'erp-sale-approved', 'returnStatus'),
        ('erp-sale', 'redFlushTrace', 'erp-sale-approved', 'redFlushTrace'),
        ('erp-sale', 'createdAt', 'erp-sale-approved', 'createdAt'),
        ('erp-sale', 'profit', 'erp-sale-form', 'profit'),
        ('erp-sale', 'discountAllocated', 'erp-sale-form', 'discountAllocated'),
        ('erp-sale-return', 'orderNo', 'erp-sale-return-draft', 'orderNo'),
        ('erp-sale-return', 'customer', 'erp-sale-return-draft', 'customer'),
        ('erp-sale-return', 'status', 'erp-sale-return-draft', 'status'),
        ('erp-sale-return', 'totalAmount', 'erp-sale-return-draft', 'totalAmount'),
        ('erp-sale-return', 'refundStatus', 'erp-sale-return-draft', 'refundStatus'),
        ('erp-sale-return', 'createdAt', 'erp-sale-return-draft', 'createdAt'),
        ('erp-sale-return', 'orderNo', 'erp-sale-return-approved', 'orderNo'),
        ('erp-sale-return', 'customer', 'erp-sale-return-approved', 'customer'),
        ('erp-sale-return', 'status', 'erp-sale-return-approved', 'status'),
        ('erp-sale-return', 'totalAmount', 'erp-sale-return-approved', 'totalAmount'),
        ('erp-sale-return', 'refundStatus', 'erp-sale-return-approved', 'refundStatus'),
        ('erp-sale-return', 'createdAt', 'erp-sale-return-approved', 'createdAt')
), mapped(source_page, source_column, target_page, target_column) AS (
    SELECT * FROM column_mapping
)
INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at, updated_at)
SELECT DISTINCT rp.tenant_id, rp.role_id, target_permission.id, NOW(), NOW()
FROM mapped
JOIN app_permission source_permission
  ON source_permission.code = CONCAT('column:', mapped.source_page, ':', mapped.source_column)
 AND source_permission.deleted_at IS NULL
JOIN app_role_permission rp
  ON rp.permission_id = source_permission.id
 AND rp.deleted_at IS NULL
JOIN app_permission target_permission
  ON target_permission.code = CONCAT('column:', mapped.target_page, ':', mapped.target_column)
 AND target_permission.deleted_at IS NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM app_role_permission existing
    WHERE existing.tenant_id = rp.tenant_id
      AND existing.role_id = rp.role_id
      AND existing.permission_id = target_permission.id
      AND existing.deleted_at IS NULL
);

INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at, updated_at)
SELECT DISTINCT role.tenant_id, role.id, permission.id, NOW(), NOW()
FROM app_role role
JOIN app_permission permission
  ON permission.code LIKE 'column:erp-%'
 AND permission.code IN (SELECT code FROM tmp_split_column_permission)
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

WITH mapped(source_page, source_column, target_page, target_column) AS (
    VALUES
        ('erp-purchase', 'orderNo', 'erp-purchase-draft', 'orderNo'),
        ('erp-purchase', 'supplier', 'erp-purchase-draft', 'supplier'),
        ('erp-purchase', 'status', 'erp-purchase-draft', 'status'),
        ('erp-purchase', 'totalAmount', 'erp-purchase-draft', 'totalAmount'),
        ('erp-purchase', 'createdAt', 'erp-purchase-draft', 'createdAt'),
        ('erp-purchase', 'orderNo', 'erp-purchase-approved', 'orderNo'),
        ('erp-purchase', 'supplier', 'erp-purchase-approved', 'supplier'),
        ('erp-purchase', 'status', 'erp-purchase-approved', 'status'),
        ('erp-purchase', 'totalAmount', 'erp-purchase-approved', 'totalAmount'),
        ('erp-purchase', 'createdAt', 'erp-purchase-approved', 'createdAt'),
        ('erp-purchase-return', 'orderNo', 'erp-purchase-return-draft', 'orderNo'),
        ('erp-purchase-return', 'supplier', 'erp-purchase-return-draft', 'supplier'),
        ('erp-purchase-return', 'status', 'erp-purchase-return-draft', 'status'),
        ('erp-purchase-return', 'totalAmount', 'erp-purchase-return-draft', 'totalAmount'),
        ('erp-purchase-return', 'createdAt', 'erp-purchase-return-draft', 'createdAt'),
        ('erp-purchase-return', 'orderNo', 'erp-purchase-return-approved', 'orderNo'),
        ('erp-purchase-return', 'supplier', 'erp-purchase-return-approved', 'supplier'),
        ('erp-purchase-return', 'status', 'erp-purchase-return-approved', 'status'),
        ('erp-purchase-return', 'totalAmount', 'erp-purchase-return-approved', 'totalAmount'),
        ('erp-purchase-return', 'createdAt', 'erp-purchase-return-approved', 'createdAt'),
        ('erp-sale', 'orderNo', 'erp-sale-draft', 'orderNo'),
        ('erp-sale', 'customer', 'erp-sale-draft', 'customer'),
        ('erp-sale', 'totalAmount', 'erp-sale-draft', 'totalAmount'),
        ('erp-sale', 'netSaleAmount', 'erp-sale-draft', 'netSaleAmount'),
        ('erp-sale', 'netGrossProfit', 'erp-sale-draft', 'netGrossProfit'),
        ('erp-sale', 'receivableStatus', 'erp-sale-draft', 'receivableStatus'),
        ('erp-sale', 'createdAt', 'erp-sale-draft', 'createdAt'),
        ('erp-sale', 'orderNo', 'erp-sale-approved', 'orderNo'),
        ('erp-sale', 'customer', 'erp-sale-approved', 'customer'),
        ('erp-sale', 'status', 'erp-sale-approved', 'status'),
        ('erp-sale', 'totalAmount', 'erp-sale-approved', 'totalAmount'),
        ('erp-sale', 'netSaleAmount', 'erp-sale-approved', 'netSaleAmount'),
        ('erp-sale', 'netGrossProfit', 'erp-sale-approved', 'netGrossProfit'),
        ('erp-sale', 'receivableStatus', 'erp-sale-approved', 'receivableStatus'),
        ('erp-sale', 'returnStatus', 'erp-sale-approved', 'returnStatus'),
        ('erp-sale', 'redFlushTrace', 'erp-sale-approved', 'redFlushTrace'),
        ('erp-sale', 'createdAt', 'erp-sale-approved', 'createdAt'),
        ('erp-sale', 'profit', 'erp-sale-form', 'profit'),
        ('erp-sale', 'discountAllocated', 'erp-sale-form', 'discountAllocated'),
        ('erp-sale-return', 'orderNo', 'erp-sale-return-draft', 'orderNo'),
        ('erp-sale-return', 'customer', 'erp-sale-return-draft', 'customer'),
        ('erp-sale-return', 'status', 'erp-sale-return-draft', 'status'),
        ('erp-sale-return', 'totalAmount', 'erp-sale-return-draft', 'totalAmount'),
        ('erp-sale-return', 'refundStatus', 'erp-sale-return-draft', 'refundStatus'),
        ('erp-sale-return', 'createdAt', 'erp-sale-return-draft', 'createdAt'),
        ('erp-sale-return', 'orderNo', 'erp-sale-return-approved', 'orderNo'),
        ('erp-sale-return', 'customer', 'erp-sale-return-approved', 'customer'),
        ('erp-sale-return', 'status', 'erp-sale-return-approved', 'status'),
        ('erp-sale-return', 'totalAmount', 'erp-sale-return-approved', 'totalAmount'),
        ('erp-sale-return', 'refundStatus', 'erp-sale-return-approved', 'refundStatus'),
        ('erp-sale-return', 'createdAt', 'erp-sale-return-approved', 'createdAt')
),
tenant_source AS (
    SELECT tenant_id, page_key, string_to_array(COALESCE(visible_columns, ''), ',') AS visible_columns, updated_by, updated_at
    FROM app_tenant_column_setting
    WHERE page_key IN ('erp-purchase', 'erp-purchase-return', 'erp-sale', 'erp-sale-return')
),
tenant_target AS (
    SELECT source.tenant_id,
           mapped.target_page AS page_key,
           string_agg(DISTINCT mapped.target_column, ',' ORDER BY mapped.target_column) AS visible_columns,
           source.updated_by,
           source.updated_at
    FROM tenant_source source
    JOIN mapped
      ON mapped.source_page = source.page_key
    JOIN LATERAL unnest(source.visible_columns) AS visible_column(value)
      ON TRUE
    WHERE btrim(visible_column.value) = mapped.source_column
    GROUP BY source.tenant_id, mapped.target_page, source.updated_by, source.updated_at
)
INSERT INTO app_tenant_column_setting (tenant_id, page_key, visible_columns, updated_by, updated_at)
SELECT target.tenant_id, target.page_key, target.visible_columns, target.updated_by, COALESCE(target.updated_at, NOW())
FROM tenant_target target
WHERE NOT EXISTS (
    SELECT 1
    FROM app_tenant_column_setting existing
    WHERE existing.tenant_id = target.tenant_id
      AND existing.page_key = target.page_key
);

WITH mapped(source_page, source_column, target_page, target_column) AS (
    VALUES
        ('erp-purchase', 'orderNo', 'erp-purchase-draft', 'orderNo'),
        ('erp-purchase', 'supplier', 'erp-purchase-draft', 'supplier'),
        ('erp-purchase', 'status', 'erp-purchase-draft', 'status'),
        ('erp-purchase', 'totalAmount', 'erp-purchase-draft', 'totalAmount'),
        ('erp-purchase', 'createdAt', 'erp-purchase-draft', 'createdAt'),
        ('erp-purchase', 'orderNo', 'erp-purchase-approved', 'orderNo'),
        ('erp-purchase', 'supplier', 'erp-purchase-approved', 'supplier'),
        ('erp-purchase', 'status', 'erp-purchase-approved', 'status'),
        ('erp-purchase', 'totalAmount', 'erp-purchase-approved', 'totalAmount'),
        ('erp-purchase', 'createdAt', 'erp-purchase-approved', 'createdAt'),
        ('erp-purchase-return', 'orderNo', 'erp-purchase-return-draft', 'orderNo'),
        ('erp-purchase-return', 'supplier', 'erp-purchase-return-draft', 'supplier'),
        ('erp-purchase-return', 'status', 'erp-purchase-return-draft', 'status'),
        ('erp-purchase-return', 'totalAmount', 'erp-purchase-return-draft', 'totalAmount'),
        ('erp-purchase-return', 'createdAt', 'erp-purchase-return-draft', 'createdAt'),
        ('erp-purchase-return', 'orderNo', 'erp-purchase-return-approved', 'orderNo'),
        ('erp-purchase-return', 'supplier', 'erp-purchase-return-approved', 'supplier'),
        ('erp-purchase-return', 'status', 'erp-purchase-return-approved', 'status'),
        ('erp-purchase-return', 'totalAmount', 'erp-purchase-return-approved', 'totalAmount'),
        ('erp-purchase-return', 'createdAt', 'erp-purchase-return-approved', 'createdAt'),
        ('erp-sale', 'orderNo', 'erp-sale-draft', 'orderNo'),
        ('erp-sale', 'customer', 'erp-sale-draft', 'customer'),
        ('erp-sale', 'totalAmount', 'erp-sale-draft', 'totalAmount'),
        ('erp-sale', 'netSaleAmount', 'erp-sale-draft', 'netSaleAmount'),
        ('erp-sale', 'netGrossProfit', 'erp-sale-draft', 'netGrossProfit'),
        ('erp-sale', 'receivableStatus', 'erp-sale-draft', 'receivableStatus'),
        ('erp-sale', 'createdAt', 'erp-sale-draft', 'createdAt'),
        ('erp-sale', 'orderNo', 'erp-sale-approved', 'orderNo'),
        ('erp-sale', 'customer', 'erp-sale-approved', 'customer'),
        ('erp-sale', 'status', 'erp-sale-approved', 'status'),
        ('erp-sale', 'totalAmount', 'erp-sale-approved', 'totalAmount'),
        ('erp-sale', 'netSaleAmount', 'erp-sale-approved', 'netSaleAmount'),
        ('erp-sale', 'netGrossProfit', 'erp-sale-approved', 'netGrossProfit'),
        ('erp-sale', 'receivableStatus', 'erp-sale-approved', 'receivableStatus'),
        ('erp-sale', 'returnStatus', 'erp-sale-approved', 'returnStatus'),
        ('erp-sale', 'redFlushTrace', 'erp-sale-approved', 'redFlushTrace'),
        ('erp-sale', 'createdAt', 'erp-sale-approved', 'createdAt'),
        ('erp-sale', 'profit', 'erp-sale-form', 'profit'),
        ('erp-sale', 'discountAllocated', 'erp-sale-form', 'discountAllocated'),
        ('erp-sale-return', 'orderNo', 'erp-sale-return-draft', 'orderNo'),
        ('erp-sale-return', 'customer', 'erp-sale-return-draft', 'customer'),
        ('erp-sale-return', 'status', 'erp-sale-return-draft', 'status'),
        ('erp-sale-return', 'totalAmount', 'erp-sale-return-draft', 'totalAmount'),
        ('erp-sale-return', 'refundStatus', 'erp-sale-return-draft', 'refundStatus'),
        ('erp-sale-return', 'createdAt', 'erp-sale-return-draft', 'createdAt'),
        ('erp-sale-return', 'orderNo', 'erp-sale-return-approved', 'orderNo'),
        ('erp-sale-return', 'customer', 'erp-sale-return-approved', 'customer'),
        ('erp-sale-return', 'status', 'erp-sale-return-approved', 'status'),
        ('erp-sale-return', 'totalAmount', 'erp-sale-return-approved', 'totalAmount'),
        ('erp-sale-return', 'refundStatus', 'erp-sale-return-approved', 'refundStatus'),
        ('erp-sale-return', 'createdAt', 'erp-sale-return-approved', 'createdAt')
),
role_source AS (
    SELECT tenant_id, role_id, page_key, string_to_array(COALESCE(visible_columns, ''), ',') AS visible_columns, updated_by, updated_at
    FROM app_role_column_setting
    WHERE page_key IN ('erp-purchase', 'erp-purchase-return', 'erp-sale', 'erp-sale-return')
),
role_target AS (
    SELECT source.tenant_id,
           source.role_id,
           mapped.target_page AS page_key,
           string_agg(DISTINCT mapped.target_column, ',' ORDER BY mapped.target_column) AS visible_columns,
           source.updated_by,
           source.updated_at
    FROM role_source source
    JOIN mapped
      ON mapped.source_page = source.page_key
    JOIN LATERAL unnest(source.visible_columns) AS visible_column(value)
      ON TRUE
    WHERE btrim(visible_column.value) = mapped.source_column
    GROUP BY source.tenant_id, source.role_id, mapped.target_page, source.updated_by, source.updated_at
)
INSERT INTO app_role_column_setting (tenant_id, role_id, page_key, visible_columns, updated_by, updated_at)
SELECT target.tenant_id, target.role_id, target.page_key, target.visible_columns, target.updated_by, COALESCE(target.updated_at, NOW())
FROM role_target target
WHERE NOT EXISTS (
    SELECT 1
    FROM app_role_column_setting existing
    WHERE existing.tenant_id = target.tenant_id
      AND existing.role_id = target.role_id
      AND existing.page_key = target.page_key
);
