CREATE TABLE IF NOT EXISTS app_role_column_setting (
    tenant_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    page_key VARCHAR(128) NOT NULL,
    visible_columns TEXT NOT NULL,
    updated_by VARCHAR(64) NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tenant_id, role_id, page_key)
);
