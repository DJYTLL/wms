DROP INDEX IF EXISTS uq_app_user_username_active;

CREATE UNIQUE INDEX IF NOT EXISTS uq_app_user_username_active
    ON app_user (tenant_id, username)
    WHERE deleted_at IS NULL;
