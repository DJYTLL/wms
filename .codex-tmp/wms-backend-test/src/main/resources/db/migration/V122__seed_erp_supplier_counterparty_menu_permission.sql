-- Seed supplier-type / counterparty menus, permissions, and column permissions for existing databases.

CREATE TEMP TABLE tmp_erp_supplier_counterparty_permission (
    code TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT
) ON COMMIT DROP;

INSERT INTO tmp_erp_supplier_counterparty_permission (code, name, description)
VALUES
    ('erp-supplier-type:view', '查看供应商类型(ERP)', '查看ERP供应商类型'),
    ('erp-supplier-type:add', '新增供应商类型(ERP)', '新增ERP供应商类型'),
    ('erp-supplier-type:edit', '编辑供应商类型(ERP)', '编辑ERP供应商类型'),
    ('erp-supplier-type:delete', '删除供应商类型(ERP)', '删除ERP供应商类型'),
    ('erp-counterparty-subject:view', '查看往来主体(ERP)', '查看ERP往来主体'),
    ('erp-counterparty-subject:add', '新增往来主体(ERP)', '新增ERP往来主体'),
    ('erp-counterparty-subject:edit', '编辑往来主体(ERP)', '编辑ERP往来主体'),
    ('erp-counterparty-subject:delete', '删除往来主体(ERP)', '删除ERP往来主体'),
    ('column:erp-supplier:supplierTypeId', 'ERP供应商-供应商类型列', 'ERP供应商供应商类型列显示'),
    ('column:erp-supplier:region', 'ERP供应商-区域列', 'ERP供应商区域列显示'),
    ('column:erp-supplier:wechat', 'ERP供应商-微信客服列', 'ERP供应商微信客服列显示'),
    ('column:erp-supplier:purchaser', 'ERP供应商-采购员列', 'ERP供应商采购员列显示'),
    ('column:erp-supplier:businessScope', 'ERP供应商-往来类别列', 'ERP供应商往来类别列显示'),
    ('column:erp-supplier:counterpartySubjectId', 'ERP供应商-往来主体列', 'ERP供应商往来主体列显示'),
    ('column:erp-supplier-type:code', 'ERP供应商类型-编码列', 'ERP供应商类型编码列显示'),
    ('column:erp-supplier-type:name', 'ERP供应商类型-名称列', 'ERP供应商类型名称列显示'),
    ('column:erp-supplier-type:status', 'ERP供应商类型-状态列', 'ERP供应商类型状态列显示'),
    ('column:erp-supplier-type:sort', 'ERP供应商类型-排序列', 'ERP供应商类型排序列显示'),
    ('column:erp-supplier-type:remark', 'ERP供应商类型-备注列', 'ERP供应商类型备注列显示'),
    ('column:erp-supplier-type:createdAt', 'ERP供应商类型-创建时间列', 'ERP供应商类型创建时间列显示'),
    ('column:erp-supplier-type:updatedAt', 'ERP供应商类型-更新时间列', 'ERP供应商类型更新时间列显示'),
    ('column:erp-counterparty-subject:name', 'ERP往来主体-名称列', 'ERP往来主体名称列显示'),
    ('column:erp-counterparty-subject:region', 'ERP往来主体-区域列', 'ERP往来主体区域列显示'),
    ('column:erp-counterparty-subject:unifiedCreditCode', 'ERP往来主体-统一社会信用代码列', 'ERP往来主体统一社会信用代码列显示'),
    ('column:erp-counterparty-subject:status', 'ERP往来主体-状态列', 'ERP往来主体状态列显示'),
    ('column:erp-counterparty-subject:remark', 'ERP往来主体-备注列', 'ERP往来主体备注列显示'),
    ('column:erp-counterparty-subject:createdAt', 'ERP往来主体-创建时间列', 'ERP往来主体创建时间列显示'),
    ('column:erp-counterparty-subject:updatedAt', 'ERP往来主体-更新时间列', 'ERP往来主体更新时间列显示'),
    ('column:erp-finance-counterparty-subject:subjectName', 'ERP往来主体汇总-主体列', 'ERP往来主体汇总主体列显示'),
    ('column:erp-finance-counterparty-subject:customerCount', 'ERP往来主体汇总-客户数量列', 'ERP往来主体汇总客户数量列显示'),
    ('column:erp-finance-counterparty-subject:supplierCount', 'ERP往来主体汇总-供应商数量列', 'ERP往来主体汇总供应商数量列显示'),
    ('column:erp-finance-counterparty-subject:receivableTotal', 'ERP往来主体汇总-应收合计列', 'ERP往来主体汇总应收合计列显示'),
    ('column:erp-finance-counterparty-subject:payableTotal', 'ERP往来主体汇总-应付合计列', 'ERP往来主体汇总应付合计列显示'),
    ('column:erp-finance-counterparty-subject:netAmount', 'ERP往来主体汇总-轧差金额列', 'ERP往来主体汇总轧差金额列显示');

UPDATE app_permission permission
SET name = seed.name,
    description = seed.description,
    is_enabled = TRUE,
    updated_at = NOW(),
    deleted_at = NULL
FROM tmp_erp_supplier_counterparty_permission seed
WHERE permission.code = seed.code;

INSERT INTO app_permission (code, name, description, is_enabled, created_at, updated_at)
SELECT seed.code, seed.name, seed.description, TRUE, NOW(), NOW()
FROM tmp_erp_supplier_counterparty_permission seed
WHERE NOT EXISTS (
    SELECT 1
    FROM app_permission existing
    WHERE existing.code = seed.code
);

CREATE TEMP TABLE tmp_erp_supplier_counterparty_menu (
    code TEXT PRIMARY KEY,
    parent_code TEXT NOT NULL,
    title TEXT NOT NULL,
    i18n_key TEXT,
    path TEXT,
    permission_code TEXT,
    sort INTEGER NOT NULL
) ON COMMIT DROP;

INSERT INTO tmp_erp_supplier_counterparty_menu (code, parent_code, title, i18n_key, path, permission_code, sort)
VALUES
    ('erp-supplier-type', 'erp-basic', '供应商类型', 'erp-supplier-type', '/erp/supplier-types', 'erp-supplier-type:view', 35),
    ('erp-counterparty-subject', 'erp-basic', '往来主体管理', 'erp-counterparty-subject', '/erp/counterparty-subjects', 'erp-counterparty-subject:view', 37),
    ('erp-finance-counterparty-subject', 'erp-finance', '往来主体财务汇总', 'erp-finance-counterparty-subject', '/erp/finance/counterparty-subjects', 'erp-finance-summary:view', 15);

INSERT INTO app_menu (code, parent_id, title, i18n_key, path, icon, permission_code, sort, is_enabled, created_at, updated_at)
SELECT
    seed.code,
    parent_menu.id,
    seed.title,
    seed.i18n_key,
    seed.path,
    NULL,
    seed.permission_code,
    seed.sort,
    TRUE,
    NOW(),
    NOW()
FROM tmp_erp_supplier_counterparty_menu seed
JOIN app_menu parent_menu
  ON parent_menu.code = seed.parent_code
 AND parent_menu.deleted_at IS NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM app_menu existing
    WHERE existing.code = seed.code
      AND existing.deleted_at IS NULL
);

UPDATE app_menu menu
SET parent_id = parent_menu.id,
    title = seed.title,
    i18n_key = seed.i18n_key,
    path = seed.path,
    permission_code = seed.permission_code,
    sort = seed.sort,
    is_enabled = TRUE,
    updated_at = NOW(),
    deleted_at = NULL
FROM tmp_erp_supplier_counterparty_menu seed
JOIN app_menu parent_menu
  ON parent_menu.code = seed.parent_code
 AND parent_menu.deleted_at IS NULL
WHERE menu.code = seed.code;

INSERT INTO app_tenant_menu (tenant_id, menu_id, is_enabled, created_at, updated_at)
SELECT tenant.id, menu.id, TRUE, NOW(), NOW()
FROM app_tenant tenant
JOIN app_menu menu
  ON menu.code IN (SELECT code FROM tmp_erp_supplier_counterparty_menu)
 AND menu.deleted_at IS NULL
WHERE tenant.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM app_tenant_menu existing
      WHERE existing.tenant_id = tenant.id
        AND existing.menu_id = menu.id
  );

-- 管理员默认拥有新增功能与列权限
INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at, updated_at)
SELECT DISTINCT role.tenant_id, role.id, permission.id, NOW(), NOW()
FROM app_role role
JOIN app_permission permission
  ON permission.code IN (SELECT code FROM tmp_erp_supplier_counterparty_permission)
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

-- 已有供应商查看角色补充“供应商类型查看”
INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at, updated_at)
SELECT DISTINCT rp.tenant_id, rp.role_id, new_permission.id, NOW(), NOW()
FROM app_role_permission rp
JOIN app_permission old_permission
  ON old_permission.id = rp.permission_id
 AND old_permission.code = 'erp-supplier:view'
 AND old_permission.deleted_at IS NULL
JOIN app_permission new_permission
  ON new_permission.code = 'erp-supplier-type:view'
 AND new_permission.deleted_at IS NULL
WHERE rp.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM app_role_permission existing
      WHERE existing.tenant_id = rp.tenant_id
        AND existing.role_id = rp.role_id
        AND existing.permission_id = new_permission.id
        AND existing.deleted_at IS NULL
  );

-- 已有客户/供应商查看角色补充“往来主体查看”
INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at, updated_at)
SELECT DISTINCT rp.tenant_id, rp.role_id, new_permission.id, NOW(), NOW()
FROM app_role_permission rp
JOIN app_permission old_permission
  ON old_permission.id = rp.permission_id
 AND old_permission.code IN ('erp-customer:view', 'erp-supplier:view')
 AND old_permission.deleted_at IS NULL
JOIN app_permission new_permission
  ON new_permission.code = 'erp-counterparty-subject:view'
 AND new_permission.deleted_at IS NULL
WHERE rp.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM app_role_permission existing
      WHERE existing.tenant_id = rp.tenant_id
        AND existing.role_id = rp.role_id
        AND existing.permission_id = new_permission.id
        AND existing.deleted_at IS NULL
  );

-- 已有财务欠款角色补充“财务汇总查看”，用于显示新汇总菜单
INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at, updated_at)
SELECT DISTINCT rp.tenant_id, rp.role_id, new_permission.id, NOW(), NOW()
FROM app_role_permission rp
JOIN app_permission old_permission
  ON old_permission.id = rp.permission_id
 AND old_permission.code IN ('erp-finance-customer-debt:view', 'erp-finance-supplier-debt:view')
 AND old_permission.deleted_at IS NULL
JOIN app_permission new_permission
  ON new_permission.code = 'erp-finance-summary:view'
 AND new_permission.deleted_at IS NULL
WHERE rp.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM app_role_permission existing
      WHERE existing.tenant_id = rp.tenant_id
        AND existing.role_id = rp.role_id
        AND existing.permission_id = new_permission.id
        AND existing.deleted_at IS NULL
  );

WITH page_permission(page_key, view_code) AS (
    VALUES
        ('erp-supplier', 'erp-supplier:view'),
        ('erp-supplier-type', 'erp-supplier-type:view'),
        ('erp-counterparty-subject', 'erp-counterparty-subject:view'),
        ('erp-finance-counterparty-subject', 'erp-finance-summary:view')
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
