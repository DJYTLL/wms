CREATE INDEX IF NOT EXISTS idx_app_role_permission_permission_active_role
    ON app_role_permission (permission_id, role_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_app_menu_permission_code_active
    ON app_menu (permission_code)
    WHERE deleted_at IS NULL;
