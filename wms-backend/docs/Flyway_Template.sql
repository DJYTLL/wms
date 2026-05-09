-- Flyway 迁移模板（请复制到 db/migration 并改名为 V{n}__xxx.sql）
-- 示例：V4__add_indexes.sql

-- 结构变更（推荐 IF NOT EXISTS）
-- ALTER TABLE app_user ADD COLUMN IF NOT EXISTS profile_json TEXT;

-- 索引变更
-- CREATE INDEX IF NOT EXISTS idx_app_user_profile_json ON app_user ((profile_json));

-- 数据回填（注意 WHERE 条件）
-- UPDATE app_user SET profile_json = '{}' WHERE profile_json IS NULL;
