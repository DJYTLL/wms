-- Split sales return draft/approved permissions and print templates.

INSERT INTO app_permission (code, name, description, is_enabled, created_at, updated_at)
SELECT v.code, v.name, v.description, TRUE, NOW(), NOW()
FROM (
    VALUES
        ('erp-sale-return-draft:view', '查看销售退货草稿(ERP)', '查看ERP销售退货草稿'),
        ('erp-sale-return-draft:add', '新增销售退货草稿(ERP)', '新增ERP销售退货草稿'),
        ('erp-sale-return-draft:edit', '编辑销售退货草稿(ERP)', '编辑ERP销售退货草稿'),
        ('erp-sale-return-draft:delete', '删除销售退货草稿(ERP)', '删除ERP销售退货草稿'),
        ('erp-sale-return-draft:approve', '审核销售退货草稿(ERP)', '审核ERP销售退货草稿'),
        ('erp-sale-return-draft:print', '打印销售退货草稿(ERP)', '打印ERP销售退货草稿'),
        ('erp-sale-return-approved:view', '查看已审核销售退货(ERP)', '查看ERP已审核销售退货'),
        ('erp-sale-return-approved:copy', '复制已审核销售退货(ERP)', '复制ERP已审核销售退货为草稿'),
        ('erp-sale-return-approved:cancel', '作废已审核销售退货(ERP)', '作废ERP已审核销售退货'),
        ('erp-sale-return-approved:redflush', '红冲已审核销售退货(ERP)', '红冲ERP已审核销售退货'),
        ('erp-sale-return-approved:print', '打印已审核销售退货(ERP)', '打印ERP已审核销售退货')
) AS v(code, name, description)
WHERE NOT EXISTS (
    SELECT 1 FROM app_permission p
    WHERE p.code = v.code
      AND p.deleted_at IS NULL
);

UPDATE app_menu
SET permission_code = 'erp-sale-return-draft:view',
    updated_at = NOW()
WHERE code = 'erp-sale-return-draft';

UPDATE app_menu
SET permission_code = 'erp-sale-return-approved:view',
    updated_at = NOW()
WHERE code = 'erp-sale-return-approved';

WITH permission_map(old_code, new_code) AS (
    VALUES
        ('erp-sale-return:view', 'erp-sale-return-draft:view'),
        ('erp-sale-return:view', 'erp-sale-return-approved:view'),
        ('erp-sale-return:view', 'erp-sale-return-draft:print'),
        ('erp-sale-return:view', 'erp-sale-return-approved:print'),
        ('erp-sale-return:add', 'erp-sale-return-draft:add'),
        ('erp-sale-return:edit', 'erp-sale-return-draft:edit'),
        ('erp-sale-return:edit', 'erp-sale-return-draft:delete'),
        ('erp-sale-return:approve', 'erp-sale-return-draft:approve'),
        ('erp-sale-return:cancel', 'erp-sale-return-approved:cancel'),
        ('erp-sale-return:redflush', 'erp-sale-return-approved:redflush')
)
INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at, updated_at)
SELECT rp.tenant_id, rp.role_id, new_perm.id, NOW(), NOW()
FROM app_role_permission rp
JOIN app_permission old_perm ON old_perm.id = rp.permission_id
JOIN permission_map pm ON pm.old_code = old_perm.code
JOIN app_permission new_perm ON new_perm.code = pm.new_code
WHERE rp.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM app_role_permission existing
      WHERE existing.tenant_id = rp.tenant_id
        AND existing.role_id = rp.role_id
        AND existing.permission_id = new_perm.id
        AND existing.deleted_at IS NULL
  );

INSERT INTO erp_print_template (
    tenant_id, code, name, doc_type, header_title, sub_title, footer_note,
    field_config, sort_no, is_default, is_enabled, created_at, updated_at
)
SELECT t.id,
       v.code,
       v.name,
       v.doc_type,
       v.header_title,
       v.sub_title,
       v.footer_note,
       v.field_config::jsonb,
       v.sort_no,
       TRUE,
       TRUE,
       NOW(),
       NOW()
FROM app_tenant t
CROSS JOIN (
    VALUES
        (
            'SALE_RETURN_DRAFT_DEFAULT',
            '销售退货草稿打印模板',
            'SALE_RETURN_DRAFT',
            '销售退货草稿',
            'Sales Return Draft',
            '请核对退货草稿明细',
            '{"headerFields":["orderNo","orderAt","customerName","returnSource","returnType","saleOrderNo","printCount","lastPrintedAt","remark"],"detailColumns":["productCode","productName","warehouse","location","qty","price","amount","taxRate","amountInclTax","remark"],"showTotals":true,"columnWidths":{"productCode":10,"productName":18,"warehouse":12,"location":12,"qty":6,"price":10,"amount":10,"taxRate":8,"amountInclTax":12,"remark":16}}',
            31
        ),
        (
            'SALE_RETURN_APPROVED_DEFAULT',
            '销售退货已审核打印模板',
            'SALE_RETURN_APPROVED',
            '销售退货已审核',
            'Sales Return Approved',
            '请核对退货明细后签字',
            '{"headerFields":["orderNo","orderAt","customerName","returnSource","returnType","saleOrderNo","printCount","lastPrintedAt","remark"],"detailColumns":["productCode","productName","warehouse","location","qty","price","amount","taxRate","amountInclTax","remark"],"showTotals":true,"columnWidths":{"productCode":10,"productName":18,"warehouse":12,"location":12,"qty":6,"price":10,"amount":10,"taxRate":8,"amountInclTax":12,"remark":16}}',
            32
        )
) AS v(code, name, doc_type, header_title, sub_title, footer_note, field_config, sort_no)
WHERE t.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM erp_print_template existing
      WHERE existing.tenant_id = t.id
        AND existing.doc_type = v.doc_type
        AND existing.deleted_at IS NULL
  );
