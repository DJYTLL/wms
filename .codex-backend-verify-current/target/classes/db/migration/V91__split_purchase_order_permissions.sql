-- 采购单草稿/已审核权限拆分（幂等）
INSERT INTO app_permission (code, name, description, is_enabled, created_at, updated_at)
SELECT v.code, v.name, v.description, TRUE, NOW(), NOW()
FROM (
    VALUES
        ('erp-purchase-draft:view', '查看采购单草稿(ERP)', '查看ERP采购单草稿'),
        ('erp-purchase-draft:add', '新增采购单草稿(ERP)', '新增ERP采购单草稿'),
        ('erp-purchase-draft:edit', '编辑采购单草稿(ERP)', '编辑ERP采购单草稿'),
        ('erp-purchase-draft:delete', '删除采购单草稿(ERP)', '删除ERP采购单草稿'),
        ('erp-purchase-draft:approve', '审核采购单草稿(ERP)', '审核ERP采购单草稿'),
        ('erp-purchase-draft:print', '打印采购单草稿(ERP)', '打印ERP采购单草稿'),
        ('erp-purchase-approved:view', '查看已审核采购单(ERP)', '查看ERP已审核采购单'),
        ('erp-purchase-approved:copy', '复制已审核采购单(ERP)', '复制ERP已审核采购单为草稿'),
        ('erp-purchase-approved:unapprove', '反审核已审核采购单(ERP)', '反审核ERP已审核采购单'),
        ('erp-purchase-approved:cancel', '作废已审核采购单(ERP)', '作废ERP已审核采购单'),
        ('erp-purchase-approved:print', '打印已审核采购单(ERP)', '打印ERP已审核采购单')
) AS v(code, name, description)
WHERE NOT EXISTS (
    SELECT 1 FROM app_permission p
    WHERE p.code = v.code
      AND p.deleted_at IS NULL
);

UPDATE app_menu
SET permission_code = 'erp-purchase-draft:view',
    updated_at = NOW()
WHERE code = 'erp-purchase-draft';

UPDATE app_menu
SET permission_code = 'erp-purchase-approved:view',
    updated_at = NOW()
WHERE code = 'erp-purchase-approved';

INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at)
SELECT rp.tenant_id, rp.role_id, np.id, NOW()
FROM app_role_permission rp
JOIN app_permission oldp ON oldp.id = rp.permission_id
JOIN app_permission np ON np.code IN (
    'erp-purchase-draft:view',
    'erp-purchase-approved:view',
    'erp-purchase-draft:print',
    'erp-purchase-approved:print'
)
WHERE oldp.code = 'erp-purchase:view'
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
JOIN app_permission np ON np.code = 'erp-purchase-draft:add'
WHERE oldp.code = 'erp-purchase:add'
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
JOIN app_permission np ON np.code IN ('erp-purchase-draft:edit', 'erp-purchase-draft:delete')
WHERE oldp.code = 'erp-purchase:edit'
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
JOIN app_permission np ON np.code = 'erp-purchase-draft:approve'
WHERE oldp.code = 'erp-purchase:approve'
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
JOIN app_permission np ON np.code = 'erp-purchase-approved:unapprove'
WHERE oldp.code = 'erp-purchase:unapprove'
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
JOIN app_permission np ON np.code = 'erp-purchase-approved:cancel'
WHERE oldp.code = 'erp-purchase:cancel'
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
       'PURCHASE_DRAFT_DEFAULT',
       '采购单草稿打印模板',
       'PURCHASE_ORDER_DRAFT',
       '采购单草稿',
       'Purchase Order Draft',
       '请核对无误后签字',
       '{"headerFields":["orderNo","orderAt","supplierName","paymentMethod","paidAmount","discountAmount","remark"],"detailColumns":["productCode","productName","warehouse","location","qty","price","amount","taxRate","amountInclTax","remark"],"showTotals":true}',
       21,
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
        AND existing.code = 'PURCHASE_DRAFT_DEFAULT'
        AND existing.deleted_at IS NULL
  );

INSERT INTO erp_print_template (tenant_id, code, name, doc_type, header_title, sub_title, footer_note, field_config, sort_no, is_default, is_enabled, remark, created_at, updated_at)
SELECT t.id,
       'PURCHASE_APPROVED_DEFAULT',
       '采购单已审核打印模板',
       'PURCHASE_ORDER_APPROVED',
       '采购单',
       'Purchase Order',
       '请核对无误后签字',
       '{"headerFields":["orderNo","orderAt","supplierName","paymentMethod","paidAmount","discountAmount","remark"],"detailColumns":["productCode","productName","warehouse","location","qty","price","amount","taxRate","amountInclTax","remark"],"showTotals":true}',
       22,
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
        AND existing.code = 'PURCHASE_APPROVED_DEFAULT'
        AND existing.deleted_at IS NULL
  );
