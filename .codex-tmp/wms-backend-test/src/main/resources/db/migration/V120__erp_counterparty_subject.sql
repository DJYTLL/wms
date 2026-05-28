CREATE TABLE IF NOT EXISTS erp_counterparty_subject (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    region VARCHAR(128),
    unified_credit_code VARCHAR(64),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    remark VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_by VARCHAR(100),
    delete_reason VARCHAR(500),
    deleted_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS erp_counterparty_subject_link (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    role_type VARCHAR(32) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    remark VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE erp_counterparty_subject IS 'ERP往来主体';
COMMENT ON COLUMN erp_counterparty_subject.id IS '主键ID';
COMMENT ON COLUMN erp_counterparty_subject.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_counterparty_subject.name IS '主体名称';
COMMENT ON COLUMN erp_counterparty_subject.region IS '区域';
COMMENT ON COLUMN erp_counterparty_subject.unified_credit_code IS '统一社会信用代码';
COMMENT ON COLUMN erp_counterparty_subject.enabled IS '是否启用';
COMMENT ON COLUMN erp_counterparty_subject.remark IS '备注';
COMMENT ON COLUMN erp_counterparty_subject.created_at IS '创建时间';
COMMENT ON COLUMN erp_counterparty_subject.updated_at IS '更新时间';
COMMENT ON COLUMN erp_counterparty_subject.deleted_by IS '删除人';
COMMENT ON COLUMN erp_counterparty_subject.delete_reason IS '删除原因';
COMMENT ON COLUMN erp_counterparty_subject.deleted_at IS '删除时间';

COMMENT ON TABLE erp_counterparty_subject_link IS 'ERP往来主体关联';
COMMENT ON COLUMN erp_counterparty_subject_link.id IS '主键ID';
COMMENT ON COLUMN erp_counterparty_subject_link.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_counterparty_subject_link.subject_id IS '往来主体ID';
COMMENT ON COLUMN erp_counterparty_subject_link.target_type IS '关联对象类型';
COMMENT ON COLUMN erp_counterparty_subject_link.target_id IS '关联对象ID';
COMMENT ON COLUMN erp_counterparty_subject_link.role_type IS '业务角色类型';
COMMENT ON COLUMN erp_counterparty_subject_link.is_primary IS '是否主档案';
COMMENT ON COLUMN erp_counterparty_subject_link.remark IS '备注';
COMMENT ON COLUMN erp_counterparty_subject_link.created_at IS '创建时间';
COMMENT ON COLUMN erp_counterparty_subject_link.updated_at IS '更新时间';

CREATE INDEX IF NOT EXISTS idx_erp_counterparty_subject_tenant_deleted
    ON erp_counterparty_subject (tenant_id, deleted_at);

CREATE INDEX IF NOT EXISTS idx_erp_counterparty_subject_link_tenant_subject
    ON erp_counterparty_subject_link (tenant_id, subject_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_counterparty_subject_link_tenant_target_role
    ON erp_counterparty_subject_link (tenant_id, target_type, target_id, role_type);
