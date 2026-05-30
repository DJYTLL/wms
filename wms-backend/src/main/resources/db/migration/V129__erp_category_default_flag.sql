-- 商品分类默认标识
ALTER TABLE erp_category
    ADD COLUMN IF NOT EXISTS is_default BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN erp_category.is_default IS '是否默认';

WITH tenant_no_default AS (
    SELECT DISTINCT c.tenant_id
    FROM erp_category c
    WHERE c.deleted_at IS NULL
      AND NOT EXISTS (
          SELECT 1
          FROM erp_category existing
          WHERE existing.tenant_id = c.tenant_id
            AND existing.deleted_at IS NULL
            AND existing.is_default = TRUE
      )
),
first_category AS (
    SELECT DISTINCT ON (c.tenant_id) c.id
    FROM erp_category c
    JOIN tenant_no_default t ON t.tenant_id = c.tenant_id
    WHERE c.deleted_at IS NULL
      AND c.is_enabled = TRUE
    ORDER BY c.tenant_id, c.sort_no NULLS LAST, c.id
)
UPDATE erp_category
SET is_default = TRUE
WHERE id IN (SELECT id FROM first_category);

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_category_default
    ON erp_category (tenant_id)
    WHERE is_default = TRUE AND deleted_at IS NULL;

CREATE TEMP TABLE tmp_erp_category_default_column_permission (
    code TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT
) ON COMMIT DROP;

INSERT INTO tmp_erp_category_default_column_permission (code, name, description)
VALUES ('column:erp-category:default', 'ERP分类-默认列', 'ERP分类默认列显示');

UPDATE app_permission permission
SET name = seed.name,
    description = seed.description,
    is_enabled = TRUE,
    updated_at = NOW(),
    deleted_at = NULL
FROM tmp_erp_category_default_column_permission seed
WHERE permission.code = seed.code;

INSERT INTO app_permission (code, name, description, is_enabled, created_at, updated_at)
SELECT seed.code, seed.name, seed.description, TRUE, NOW(), NOW()
FROM tmp_erp_category_default_column_permission seed
WHERE NOT EXISTS (
    SELECT 1
    FROM app_permission existing
    WHERE existing.code = seed.code
);

INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at, updated_at)
SELECT DISTINCT role.tenant_id, role.id, permission.id, NOW(), NOW()
FROM app_role role
JOIN app_permission permission
  ON permission.code IN (SELECT code FROM tmp_erp_category_default_column_permission)
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
