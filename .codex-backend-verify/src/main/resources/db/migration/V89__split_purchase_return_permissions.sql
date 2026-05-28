-- 采购退货草稿/已审核权限拆分（幂等）
INSERT INTO app_permission (code, name, description, is_enabled, created_at, updated_at)
SELECT v.code, v.name, v.description, TRUE, NOW(), NOW()
FROM (
    VALUES
        ('erp-purchase-return:cancel', '作废采购退货(ERP)', '作废ERP采购退货'),
        ('erp-purchase-return-draft:view', '查看采购退货草稿(ERP)', '查看ERP采购退货草稿'),
        ('erp-purchase-return-draft:add', '新增采购退货草稿(ERP)', '新增ERP采购退货草稿'),
        ('erp-purchase-return-draft:edit', '编辑采购退货草稿(ERP)', '编辑ERP采购退货草稿'),
        ('erp-purchase-return-draft:delete', '删除采购退货草稿(ERP)', '删除ERP采购退货草稿'),
        ('erp-purchase-return-draft:approve', '审核采购退货草稿(ERP)', '审核ERP采购退货草稿'),
        ('erp-purchase-return-draft:print', '打印采购退货草稿(ERP)', '打印ERP采购退货草稿'),
        ('erp-purchase-return-approved:view', '查看已审核采购退货(ERP)', '查看ERP已审核采购退货'),
        ('erp-purchase-return-approved:copy', '复制已审核采购退货(ERP)', '复制ERP已审核采购退货为草稿'),
        ('erp-purchase-return-approved:cancel', '作废已审核采购退货(ERP)', '作废ERP已审核采购退货'),
        ('erp-purchase-return-approved:print', '打印已审核采购退货(ERP)', '打印ERP已审核采购退货')
) AS v(code, name, description)
WHERE NOT EXISTS (
    SELECT 1 FROM app_permission p
    WHERE p.code = v.code
      AND p.deleted_at IS NULL
);

UPDATE app_menu
SET permission_code = 'erp-purchase-return-draft:view',
    updated_at = NOW()
WHERE code = 'erp-purchase-return-draft';

UPDATE app_menu
SET permission_code = 'erp-purchase-return-approved:view',
    updated_at = NOW()
WHERE code = 'erp-purchase-return-approved';

INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at)
SELECT rp.tenant_id, rp.role_id, np.id, NOW()
FROM app_role_permission rp
JOIN app_permission oldp ON oldp.id = rp.permission_id
JOIN app_permission np ON np.code IN (
    'erp-purchase-return-draft:view',
    'erp-purchase-return-approved:view',
    'erp-purchase-return-draft:print',
    'erp-purchase-return-approved:print'
)
WHERE oldp.code = 'erp-purchase-return:view'
  AND rp.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM app_role_permission existing
      WHERE existing.tenant_id = rp.tenant_id
        AND existing.role_id = rp.role_id
        AND existing.permission_id = np.id
        AND existing.deleted_at IS NULL
  );

INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at)
SELECT rp.tenant_id, rp.role_id, np.id, NOW()
FROM app_role_permission rp
JOIN app_permission oldp ON oldp.id = rp.permission_id
JOIN app_permission np ON np.code = 'erp-purchase-return-draft:add'
WHERE oldp.code = 'erp-purchase-return:add'
  AND rp.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM app_role_permission existing
      WHERE existing.tenant_id = rp.tenant_id
        AND existing.role_id = rp.role_id
        AND existing.permission_id = np.id
        AND existing.deleted_at IS NULL
  );

INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at)
SELECT rp.tenant_id, rp.role_id, np.id, NOW()
FROM app_role_permission rp
JOIN app_permission oldp ON oldp.id = rp.permission_id
JOIN app_permission np ON np.code IN ('erp-purchase-return-draft:edit', 'erp-purchase-return-draft:delete')
WHERE oldp.code = 'erp-purchase-return:edit'
  AND rp.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM app_role_permission existing
      WHERE existing.tenant_id = rp.tenant_id
        AND existing.role_id = rp.role_id
        AND existing.permission_id = np.id
        AND existing.deleted_at IS NULL
  );

INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at)
SELECT rp.tenant_id, rp.role_id, np.id, NOW()
FROM app_role_permission rp
JOIN app_permission oldp ON oldp.id = rp.permission_id
JOIN app_permission np ON np.code = 'erp-purchase-return-draft:approve'
WHERE oldp.code = 'erp-purchase-return:approve'
  AND rp.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM app_role_permission existing
      WHERE existing.tenant_id = rp.tenant_id
        AND existing.role_id = rp.role_id
        AND existing.permission_id = np.id
        AND existing.deleted_at IS NULL
  );

INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at)
SELECT rp.tenant_id, rp.role_id, np.id, NOW()
FROM app_role_permission rp
JOIN app_permission oldp ON oldp.id = rp.permission_id
JOIN app_permission np ON np.code = 'erp-purchase-return-approved:cancel'
WHERE oldp.code = 'erp-purchase-return:cancel'
  AND rp.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM app_role_permission existing
      WHERE existing.tenant_id = rp.tenant_id
        AND existing.role_id = rp.role_id
        AND existing.permission_id = np.id
        AND existing.deleted_at IS NULL
  );

INSERT INTO erp_print_template (tenant_id, code, name, doc_type, header_title, sub_title, footer_note, field_config, sort_no, is_default, is_enabled, remark, created_at, updated_at)
SELECT t.id,
       'PURCHASE_RETURN_DRAFT_DEFAULT',
       '采购退货草稿打印模板',
       'PURCHASE_RETURN_DRAFT',
       '采购退货草稿',
       'Purchase Return Draft',
       '请核对无误后签字',
       '{"headerFields":["orderNo","orderAt","supplierName","returnSource","returnType","purchaseOrderNo","remark"],"detailColumns":["productCode","productName","warehouse","location","qty","price","amount","taxRate","amountInclTax","remark"],"showTotals":true}',
       31,
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
        AND existing.code = 'PURCHASE_RETURN_DRAFT_DEFAULT'
        AND existing.deleted_at IS NULL
  );

INSERT INTO erp_print_template (tenant_id, code, name, doc_type, header_title, sub_title, footer_note, field_config, sort_no, is_default, is_enabled, remark, created_at, updated_at)
SELECT t.id,
       'PURCHASE_RETURN_APPROVED_DEFAULT',
       '采购退货已审核打印模板',
       'PURCHASE_RETURN_APPROVED',
       '采购退货单',
       'Purchase Return',
       '请核对无误后签字',
       '{"headerFields":["orderNo","orderAt","supplierName","returnSource","returnType","purchaseOrderNo","printCount","lastPrintedAt","remark"],"detailColumns":["productCode","productName","warehouse","location","qty","price","amount","taxRate","amountInclTax","remark"],"showTotals":true}',
       32,
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
        AND existing.code = 'PURCHASE_RETURN_APPROVED_DEFAULT'
        AND existing.deleted_at IS NULL
  );
