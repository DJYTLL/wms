ALTER TABLE app_user_table_setting
    ALTER COLUMN config_json TYPE JSONB USING config_json::jsonb;
