-- Flyway 迁移模板（请复制到 db/migration 并改名为 V{n}__xxx.sql）
-- 示例：V4__add_indexes.sql

-- 结构变更（推荐 IF NOT EXISTS）
-- ALTER TABLE app_user ADD COLUMN IF NOT EXISTS profile_json TEXT;
-- 新业务表默认要求：
-- 1. 业务删除统一使用 deleted_at TIMESTAMPTZ 逻辑删除
-- 2. 租户隔离表默认提供 tenant_id BIGINT NOT NULL
-- 3. 删除接口需要记录删除原因，审计日志保留删除人（actor）与删除原因（detail）
-- 4. 新业务实体优先复用 entity/base 下的公共基类

-- 逻辑删除字段模板
-- ALTER TABLE app_example ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

-- 索引变更
-- CREATE INDEX IF NOT EXISTS idx_app_user_profile_json ON app_user ((profile_json));
-- 常规租户查询索引模板
-- CREATE INDEX IF NOT EXISTS idx_app_example_tenant_deleted_at ON app_example (tenant_id, deleted_at);
-- 活动态唯一索引模板（避免墓碑数据占用唯一键）
-- CREATE UNIQUE INDEX IF NOT EXISTS uq_app_example_code_active ON app_example (tenant_id, code) WHERE deleted_at IS NULL;

-- 数据回填（注意 WHERE 条件）
-- UPDATE app_user SET profile_json = '{}' WHERE profile_json IS NULL;

-- 技术表例外说明
-- 1. 刷新令牌、会话、幂等过期等技术状态表，可优先使用 revoked_at/expires_at 等状态字段
-- 2. 审计日志、库存流水、打印日志等追加型流水表，通常不提供恢复接口，不走业务逻辑删除
