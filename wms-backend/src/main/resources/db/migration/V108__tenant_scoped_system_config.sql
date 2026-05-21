ALTER TABLE app_system_config
    ADD COLUMN IF NOT EXISTS tenant_id BIGINT;

UPDATE app_system_config
SET tenant_id = (SELECT id FROM app_tenant WHERE code = 'default' AND deleted_at IS NULL ORDER BY id LIMIT 1)
WHERE tenant_id IS NULL;

DROP INDEX IF EXISTS uq_system_config_key;

ALTER TABLE app_system_config
    DROP CONSTRAINT IF EXISTS app_system_config_config_key_key;

INSERT INTO app_system_config (
    tenant_id,
    config_key,
    config_value,
    value_type,
    description,
    is_public,
    created_at,
    updated_at
)
SELECT
    tenant.id,
    config.config_key,
    config.config_value,
    config.value_type,
    config.description,
    config.is_public,
    NOW(),
    NOW()
FROM app_tenant tenant
JOIN app_system_config config
    ON config.tenant_id = (SELECT id FROM app_tenant WHERE code = 'default' AND deleted_at IS NULL ORDER BY id LIMIT 1)
WHERE tenant.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM app_system_config existing
      WHERE existing.tenant_id = tenant.id
        AND existing.config_key = config.config_key
  );

ALTER TABLE app_system_config
    ALTER COLUMN tenant_id SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_system_config_tenant_key
    ON app_system_config (tenant_id, config_key);
