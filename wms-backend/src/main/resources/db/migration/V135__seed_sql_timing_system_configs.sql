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
    t.id,
    'wms.monitor.sql-timing-enabled',
    'false',
    'bool',
    'SQL耗时采集开关',
    FALSE,
    NOW(),
    NOW()
FROM app_tenant t
WHERE NOT EXISTS (
    SELECT 1
    FROM app_system_config c
    WHERE c.tenant_id = t.id
      AND c.config_key = 'wms.monitor.sql-timing-enabled'
);

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
    t.id,
    'wms.monitor.sql-timing-log-params',
    'false',
    'bool',
    'SQL耗时参数摘要开关',
    FALSE,
    NOW(),
    NOW()
FROM app_tenant t
WHERE NOT EXISTS (
    SELECT 1
    FROM app_system_config c
    WHERE c.tenant_id = t.id
      AND c.config_key = 'wms.monitor.sql-timing-log-params'
);
