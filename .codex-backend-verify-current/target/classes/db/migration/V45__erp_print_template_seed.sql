-- 打印模板权限与菜单、默认模板种子
INSERT INTO app_permission (code, name, description, is_enabled, created_at, updated_at) VALUES
    ('erp-print-template:view', '打印模板-查看', '打印模板查看', TRUE, NOW(), NOW()),
    ('erp-print-template:add', '打印模板-新增', '打印模板新增', TRUE, NOW(), NOW()),
    ('erp-print-template:edit', '打印模板-编辑', '打印模板编辑', TRUE, NOW(), NOW()),
    ('erp-print-template:delete', '打印模板-删除', '打印模板删除', TRUE, NOW(), NOW())
ON CONFLICT (code) DO NOTHING;

INSERT INTO app_menu (code, parent_id, title, i18n_key, path, icon, permission_code, sort, is_enabled, created_at, updated_at)
SELECT 'erp-print-template', p.id, '打印模板', 'erp-print-template', '/erp/print-templates', NULL, 'erp-print-template:view', 95, TRUE, NOW(), NOW()
FROM app_menu p
WHERE p.code = 'erp-basic'
ON CONFLICT (code) DO UPDATE
SET parent_id = EXCLUDED.parent_id,
    title = EXCLUDED.title,
    i18n_key = EXCLUDED.i18n_key,
    path = EXCLUDED.path,
    permission_code = EXCLUDED.permission_code,
    sort = EXCLUDED.sort,
    is_enabled = EXCLUDED.is_enabled,
    updated_at = NOW();

INSERT INTO app_tenant_menu (tenant_id, menu_id, is_enabled, created_at, updated_at)
SELECT t.id, m.id, TRUE, NOW(), NOW()
FROM app_tenant t
JOIN app_menu m ON m.code = 'erp-print-template'
WHERE t.deleted_at IS NULL
ON CONFLICT (tenant_id, menu_id) DO NOTHING;

-- 为 admin / super_admin 角色补齐权限
INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at)
SELECT r.tenant_id, r.id, p.id, NOW()
FROM app_role r
JOIN app_permission p ON p.code IN (
    'erp-print-template:view',
    'erp-print-template:add',
    'erp-print-template:edit',
    'erp-print-template:delete'
)
WHERE r.code IN ('admin', 'super_admin')
ON CONFLICT (tenant_id, role_id, permission_id) DO NOTHING;

-- 默认模板（按租户）
INSERT INTO erp_print_template (tenant_id, code, name, doc_type, header_title, sub_title, footer_note, field_config, sort_no, is_default, is_enabled, remark, created_at, updated_at)
SELECT t.id,
       'SALE_DEFAULT',
       '销售单默认模板',
       'SALE_ORDER',
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
ON CONFLICT (tenant_id, code) DO NOTHING;

INSERT INTO erp_print_template (tenant_id, code, name, doc_type, header_title, sub_title, footer_note, field_config, sort_no, is_default, is_enabled, remark, created_at, updated_at)
SELECT t.id,
       'PURCHASE_DEFAULT',
       '采购单默认模板',
       'PURCHASE_ORDER',
       '采购单',
       'Purchase Order',
       '请核对无误后签字',
       '{"headerFields":["orderNo","orderAt","supplierName","paymentMethod","paidAmount","discountAmount","remark"],"detailColumns":["productCode","productName","warehouse","location","qty","price","amount","taxRate","amountInclTax","remark"],"showTotals":true}',
       20,
       TRUE,
       TRUE,
       '系统默认模板',
       NOW(),
       NOW()
FROM app_tenant t
WHERE t.deleted_at IS NULL
ON CONFLICT (tenant_id, code) DO NOTHING;
