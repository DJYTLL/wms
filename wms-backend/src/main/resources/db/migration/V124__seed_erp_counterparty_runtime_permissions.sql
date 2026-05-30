CREATE TEMP TABLE tmp_erp_counterparty_runtime_permission (
    code TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT
) ON COMMIT DROP;

INSERT INTO tmp_erp_counterparty_runtime_permission (code, name, description)
VALUES
    ('erp-supplier:import', '导入供应商(ERP)', '导入ERP供应商历史数据'),
    ('column:erp-customer:counterpartySubjectId', 'ERP客户-往来主体列', 'ERP客户往来主体列显示'),
    ('column:erp-counterparty-subject:supplierCount', 'ERP往来主体-供应商数量列', 'ERP往来主体供应商数量列显示'),
    ('column:erp-counterparty-subject:customerCount', 'ERP往来主体-客户数量列', 'ERP往来主体客户数量列显示'),
    ('column:erp-counterparty-subject:bindingStatus', 'ERP往来主体-绑定状态列', 'ERP往来主体绑定状态列显示'),
    ('column:erp-finance-counterparty-subject:lastTransactionAt', 'ERP往来主体汇总-最近往来时间列', 'ERP往来主体汇总最近往来时间列显示');

UPDATE app_permission permission
SET name = seed.name,
    description = seed.description,
    is_enabled = TRUE,
    updated_at = NOW(),
    deleted_at = NULL
FROM tmp_erp_counterparty_runtime_permission seed
WHERE permission.code = seed.code;

INSERT INTO app_permission (code, name, description, is_enabled, created_at, updated_at)
SELECT seed.code, seed.name, seed.description, TRUE, NOW(), NOW()
FROM tmp_erp_counterparty_runtime_permission seed
WHERE NOT EXISTS (
    SELECT 1
    FROM app_permission existing
    WHERE existing.code = seed.code
);

INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at, updated_at)
SELECT DISTINCT role.tenant_id, role.id, permission.id, NOW(), NOW()
FROM app_role role
JOIN app_permission permission
  ON permission.code IN (SELECT code FROM tmp_erp_counterparty_runtime_permission)
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
