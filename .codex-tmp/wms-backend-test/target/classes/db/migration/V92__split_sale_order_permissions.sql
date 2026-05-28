-- Split sale order draft/approved permissions, menus, and print templates.

INSERT INTO app_permission (code, name, description, is_enabled, created_at, updated_at)
SELECT v.code, v.name, v.description, TRUE, NOW(), NOW()
FROM (
    VALUES
        ('erp-sale-draft:view', '销售单草稿-查看', '查看销售单草稿'),
        ('erp-sale-draft:add', '销售单草稿-新增', '新增销售单草稿'),
        ('erp-sale-draft:edit', '销售单草稿-编辑', '编辑销售单草稿'),
        ('erp-sale-draft:delete', '销售单草稿-删除', '删除销售单草稿'),
        ('erp-sale-draft:approve', '销售单草稿-审核', '审核销售单草稿'),
        ('erp-sale-draft:print', '销售单草稿-打印', '打印销售单草稿'),
        ('erp-sale-approved:view', '销售单已审核-查看', '查看已审核销售单'),
        ('erp-sale-approved:copy', '销售单已审核-复制', '复制已审核销售单为草稿'),
        ('erp-sale-approved:cancel', '销售单已审核-作废', '作废已审核销售单'),
        ('erp-sale-approved:redflush', '销售单已审核-红冲', '红冲已审核销售单'),
        ('erp-sale-approved:print', '销售单已审核-打印', '打印已审核销售单')
) AS v(code, name, description)
WHERE NOT EXISTS (
    SELECT 1 FROM app_permission p
    WHERE p.code = v.code
      AND p.deleted_at IS NULL
);

UPDATE app_menu
SET permission_code = 'erp-sale-draft:view',
    updated_at = NOW()
WHERE code = 'erp-sale-draft'
  AND deleted_at IS NULL;

UPDATE app_menu
SET permission_code = 'erp-sale-approved:view',
    updated_at = NOW()
WHERE code = 'erp-sale-approved'
  AND deleted_at IS NULL;

WITH mapping(old_code, new_code) AS (
    VALUES
        ('erp-sale:view', 'erp-sale-draft:view'),
        ('erp-sale:view', 'erp-sale-approved:view'),
        ('erp-sale:view', 'erp-sale-draft:print'),
        ('erp-sale:view', 'erp-sale-approved:print'),
        ('erp-sale:add', 'erp-sale-draft:add'),
        ('erp-sale:edit', 'erp-sale-draft:edit'),
        ('erp-sale:edit', 'erp-sale-draft:delete'),
        ('erp-sale:approve', 'erp-sale-draft:approve'),
        ('erp-sale:cancel', 'erp-sale-approved:cancel'),
        ('erp-sale:redflush', 'erp-sale-approved:redflush')
)
INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at)
SELECT rp.tenant_id, rp.role_id, np.id, NOW()
FROM app_role_permission rp
JOIN app_permission op ON op.id = rp.permission_id AND op.deleted_at IS NULL
JOIN mapping m ON m.old_code = op.code
JOIN app_permission np ON np.code = m.new_code AND np.deleted_at IS NULL
WHERE rp.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM app_role_permission existing
      WHERE existing.tenant_id = rp.tenant_id
        AND existing.role_id = rp.role_id
        AND existing.permission_id = np.id
        AND existing.deleted_at IS NULL
  );

INSERT INTO erp_print_template (tenant_id, code, name, doc_type, header_title, sub_title, footer_note, field_config, sort_no, is_default, is_enabled, remark, created_at, updated_at)
SELECT t.id,
       'SALE_DRAFT_DEFAULT',
       '销售单草稿打印模板',
       'SALE_ORDER_DRAFT',
       '销售单草稿',
       'Sales Order Draft',
       '请核对草稿内容',
       '{"headerFields":["orderNo","orderAt","customerName","settlementMethod","deliveryMethod","paidAmount","discountAmount","remark"],"detailColumns":["productCode","productName","warehouse","location","qty","price","amount","taxRate","amountInclTax","remark"],"showTotals":true}',
       10,
       TRUE,
       TRUE,
       '系统默认模板',
       NOW(),
       NOW()
FROM app_tenant t
WHERE t.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM erp_print_template existing
      WHERE existing.tenant_id = t.id
        AND existing.doc_type = 'SALE_ORDER_DRAFT'
        AND existing.deleted_at IS NULL
  );

INSERT INTO erp_print_template (tenant_id, code, name, doc_type, header_title, sub_title, footer_note, field_config, sort_no, is_default, is_enabled, remark, created_at, updated_at)
SELECT t.id,
       'SALE_APPROVED_DEFAULT',
       '销售单已审核打印模板',
       'SALE_ORDER_APPROVED',
       '销售单',
       'Sales Order',
       '感谢您的惠顾',
       '{"headerFields":["orderNo","orderAt","customerName","settlementMethod","deliveryMethod","paidAmount","discountAmount","remark"],"detailColumns":["productCode","productName","warehouse","location","qty","price","amount","taxRate","amountInclTax","remark"],"showTotals":true}',
       10,
       TRUE,
       TRUE,
       '系统默认模板',
       NOW(),
       NOW()
FROM app_tenant t
WHERE t.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM erp_print_template existing
      WHERE existing.tenant_id = t.id
        AND existing.doc_type = 'SALE_ORDER_APPROVED'
        AND existing.deleted_at IS NULL
  );
