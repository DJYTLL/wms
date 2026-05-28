CREATE TABLE IF NOT EXISTS app_user_table_setting (
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    page_key VARCHAR(120) NOT NULL,
    config_json TEXT NOT NULL,
    updated_by VARCHAR(100),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (tenant_id, user_id, page_key)
);

CREATE INDEX IF NOT EXISTS idx_app_user_table_setting_tenant_user
    ON app_user_table_setting (tenant_id, user_id);
