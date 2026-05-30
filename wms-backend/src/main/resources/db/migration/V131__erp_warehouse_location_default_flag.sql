-- 仓库与库位默认标识
ALTER TABLE erp_warehouse
    ADD COLUMN IF NOT EXISTS is_default BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE erp_location
    ADD COLUMN IF NOT EXISTS is_default BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN erp_warehouse.is_default IS '是否默认仓库';
COMMENT ON COLUMN erp_location.is_default IS '是否默认库位';

WITH tenant_no_default AS (
    SELECT DISTINCT w.tenant_id
    FROM erp_warehouse w
    WHERE w.deleted_at IS NULL
      AND NOT EXISTS (
          SELECT 1
          FROM erp_warehouse existing
          WHERE existing.tenant_id = w.tenant_id
            AND existing.deleted_at IS NULL
            AND existing.is_default = TRUE
      )
),
first_warehouse AS (
    SELECT DISTINCT ON (w.tenant_id) w.id
    FROM erp_warehouse w
    JOIN tenant_no_default t ON t.tenant_id = w.tenant_id
    WHERE w.deleted_at IS NULL
      AND w.is_enabled = TRUE
    ORDER BY w.tenant_id, w.id
)
UPDATE erp_warehouse
SET is_default = TRUE
WHERE id IN (SELECT id FROM first_warehouse);

WITH warehouse_no_default_location AS (
    SELECT DISTINCT l.tenant_id, l.warehouse_id
    FROM erp_location l
    WHERE l.deleted_at IS NULL
      AND NOT EXISTS (
          SELECT 1
          FROM erp_location existing
          WHERE existing.tenant_id = l.tenant_id
            AND existing.warehouse_id = l.warehouse_id
            AND existing.deleted_at IS NULL
            AND existing.is_default = TRUE
      )
),
first_location AS (
    SELECT DISTINCT ON (l.tenant_id, l.warehouse_id) l.id
    FROM erp_location l
    JOIN warehouse_no_default_location target
      ON target.tenant_id = l.tenant_id
     AND target.warehouse_id = l.warehouse_id
    WHERE l.deleted_at IS NULL
      AND l.is_enabled = TRUE
    ORDER BY l.tenant_id, l.warehouse_id, l.id
)
UPDATE erp_location
SET is_default = TRUE
WHERE id IN (SELECT id FROM first_location);

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_warehouse_default
    ON erp_warehouse (tenant_id)
    WHERE is_default = TRUE AND deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_location_default
    ON erp_location (tenant_id, warehouse_id)
    WHERE is_default = TRUE AND deleted_at IS NULL;

CREATE TEMP TABLE tmp_erp_warehouse_location_default_column_permission (
    code TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT
) ON COMMIT DROP;

INSERT INTO tmp_erp_warehouse_location_default_column_permission (code, name, description)
VALUES
    ('column:erp-warehouse:default', 'ERP仓库-默认列', 'ERP仓库默认列显示'),
    ('column:erp-location:default', 'ERP库位-默认列', 'ERP库位默认列显示');

UPDATE app_permission permission
SET name = seed.name,
    description = seed.description,
    is_enabled = TRUE,
    updated_at = NOW(),
    deleted_at = NULL
FROM tmp_erp_warehouse_location_default_column_permission seed
WHERE permission.code = seed.code;

INSERT INTO app_permission (code, name, description, is_enabled, created_at, updated_at)
SELECT seed.code, seed.name, seed.description, TRUE, NOW(), NOW()
FROM tmp_erp_warehouse_location_default_column_permission seed
WHERE NOT EXISTS (
    SELECT 1
    FROM app_permission existing
    WHERE existing.code = seed.code
);

INSERT INTO app_role_permission (tenant_id, role_id, permission_id, created_at, updated_at)
SELECT DISTINCT role.tenant_id, role.id, permission.id, NOW(), NOW()
FROM app_role role
JOIN app_permission permission
  ON permission.code IN (SELECT code FROM tmp_erp_warehouse_location_default_column_permission)
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
