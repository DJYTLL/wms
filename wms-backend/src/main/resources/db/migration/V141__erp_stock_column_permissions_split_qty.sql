-- 库存台账列权限从旧 qty 列拆分为在库/锁定/可用三列，并迁移租户与角色配置

CREATE TEMP TABLE tmp_erp_stock_column_permission (
    code TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT
) ON COMMIT DROP;

INSERT INTO tmp_erp_stock_column_permission (code, name, description)
VALUES
    ('column:erp-stock:qtyOnHand', 'ERP库存-在库列', 'ERP库存在库列显示'),
    ('column:erp-stock:qtyLocked', 'ERP库存-锁定列', 'ERP库存锁定列显示'),
    ('column:erp-stock:qtyAvailable', 'ERP库存-可用列', 'ERP库存可用列显示');

UPDATE app_permission permission
SET name = column_permission.name,
    description = column_permission.description,
    is_enabled = TRUE,
    updated_at = NOW(),
    deleted_at = NULL
FROM tmp_erp_stock_column_permission column_permission
WHERE permission.code = column_permission.code;

INSERT INTO app_permission (code, name, description, is_enabled, created_at, updated_at)
SELECT column_permission.code, column_permission.name, column_permission.description, TRUE, NOW(), NOW()
FROM tmp_erp_stock_column_permission column_permission
WHERE NOT EXISTS (
    SELECT 1
    FROM app_permission existing
    WHERE existing.code = column_permission.code
);

WITH stock_view_permission AS (
    SELECT id
    FROM app_permission
    WHERE code = 'erp-stock:view'
      AND deleted_at IS NULL
),
new_stock_column_permission AS (
    SELECT id
    FROM app_permission
    WHERE code IN (
        'column:erp-stock:qtyOnHand',
        'column:erp-stock:qtyLocked',
        'column:erp-stock:qtyAvailable'
    )
      AND deleted_at IS NULL
)
INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at, updated_at)
SELECT DISTINCT rp.tenant_id, rp.role_id, new_perm.id, NOW(), NOW()
FROM app_role_permission rp
JOIN stock_view_permission view_perm
  ON view_perm.id = rp.permission_id
JOIN new_stock_column_permission new_perm
  ON TRUE
WHERE rp.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM app_role_permission existing
      WHERE existing.tenant_id = rp.tenant_id
        AND existing.role_id = rp.role_id
        AND existing.permission_id = new_perm.id
        AND existing.deleted_at IS NULL
  );

WITH tenant_rows AS (
    SELECT tenant_id,
           page_key,
           string_agg(
               DISTINCT CASE
                   WHEN column_key = 'qty' THEN mapped.new_key
                   ELSE column_key
               END,
               ',' ORDER BY CASE
                   WHEN column_key = 'qty' THEN mapped.new_key
                   ELSE column_key
               END
           ) AS visible_columns
    FROM (
        SELECT tenant_id, page_key, regexp_split_to_table(COALESCE(visible_columns, ''), ',') AS column_key
        FROM app_tenant_column_setting
        WHERE page_key = 'erp-stock'
    ) source
    LEFT JOIN LATERAL (
        VALUES ('qtyOnHand'), ('qtyLocked'), ('qtyAvailable')
    ) AS mapped(new_key)
      ON source.column_key = 'qty'
    GROUP BY tenant_id, page_key
)
UPDATE app_tenant_column_setting setting
SET visible_columns = tenant_rows.visible_columns,
    updated_at = NOW()
FROM tenant_rows
WHERE setting.tenant_id = tenant_rows.tenant_id
  AND setting.page_key = tenant_rows.page_key;

WITH role_rows AS (
    SELECT tenant_id,
           role_id,
           page_key,
           string_agg(
               DISTINCT CASE
                   WHEN column_key = 'qty' THEN mapped.new_key
                   ELSE column_key
               END,
               ',' ORDER BY CASE
                   WHEN column_key = 'qty' THEN mapped.new_key
                   ELSE column_key
               END
           ) AS visible_columns
    FROM (
        SELECT tenant_id, role_id, page_key, regexp_split_to_table(COALESCE(visible_columns, ''), ',') AS column_key
        FROM app_role_column_setting
        WHERE page_key = 'erp-stock'
    ) source
    LEFT JOIN LATERAL (
        VALUES ('qtyOnHand'), ('qtyLocked'), ('qtyAvailable')
    ) AS mapped(new_key)
      ON source.column_key = 'qty'
    GROUP BY tenant_id, role_id, page_key
)
UPDATE app_role_column_setting setting
SET visible_columns = role_rows.visible_columns,
    updated_at = NOW()
FROM role_rows
WHERE setting.tenant_id = role_rows.tenant_id
  AND setting.role_id = role_rows.role_id
  AND setting.page_key = role_rows.page_key;
