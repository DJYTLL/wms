CREATE TABLE IF NOT EXISTS erp_supplier_type (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort INTEGER NOT NULL DEFAULT 0,
    remark VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_by VARCHAR(100),
    delete_reason VARCHAR(500),
    deleted_at TIMESTAMPTZ
);

COMMENT ON TABLE erp_supplier_type IS 'ERP供应商类型';
COMMENT ON COLUMN erp_supplier_type.id IS '主键ID';
COMMENT ON COLUMN erp_supplier_type.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_supplier_type.code IS '类型编码';
COMMENT ON COLUMN erp_supplier_type.name IS '类型名称';
COMMENT ON COLUMN erp_supplier_type.enabled IS '是否启用';
COMMENT ON COLUMN erp_supplier_type.sort IS '排序';
COMMENT ON COLUMN erp_supplier_type.remark IS '备注';
COMMENT ON COLUMN erp_supplier_type.created_at IS '创建时间';
COMMENT ON COLUMN erp_supplier_type.updated_at IS '更新时间';
COMMENT ON COLUMN erp_supplier_type.deleted_by IS '删除人';
COMMENT ON COLUMN erp_supplier_type.delete_reason IS '删除原因';
COMMENT ON COLUMN erp_supplier_type.deleted_at IS '删除时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_supplier_type_tenant_code_deleted
    ON erp_supplier_type (tenant_id, code, deleted_at);

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_supplier_type_tenant_name_deleted
    ON erp_supplier_type (tenant_id, name, deleted_at);

CREATE INDEX IF NOT EXISTS idx_erp_supplier_type_tenant_deleted
    ON erp_supplier_type (tenant_id, deleted_at);
