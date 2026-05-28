-- 组装/拆分模板
CREATE TABLE erp_assembly_template (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES app_tenant(id),
    order_type VARCHAR(32) NOT NULL,
    name VARCHAR(120) NOT NULL,
    finished_product_id BIGINT NOT NULL REFERENCES erp_product(id),
    finished_qty NUMERIC(18, 4) NOT NULL,
    warehouse_id BIGINT REFERENCES erp_warehouse(id),
    location_id BIGINT REFERENCES erp_location(id),
    labor_cost NUMERIC(18, 4) NOT NULL DEFAULT 0,
    remark TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_by VARCHAR(100),
    delete_reason VARCHAR(255),
    deleted_at TIMESTAMPTZ
);

CREATE TABLE erp_assembly_template_item (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES app_tenant(id),
    template_id BIGINT NOT NULL REFERENCES erp_assembly_template(id) ON DELETE CASCADE,
    line_no INT NOT NULL,
    product_id BIGINT NOT NULL REFERENCES erp_product(id),
    product_code VARCHAR(64),
    product_name VARCHAR(255),
    warehouse_id BIGINT REFERENCES erp_warehouse(id),
    location_id BIGINT REFERENCES erp_location(id),
    qty NUMERIC(18, 4) NOT NULL,
    remark VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_by VARCHAR(100),
    delete_reason VARCHAR(255),
    deleted_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX uk_erp_assembly_template_name_active
    ON erp_assembly_template (tenant_id, order_type, name)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_erp_assembly_template_tenant_type
    ON erp_assembly_template (tenant_id, order_type, updated_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_erp_assembly_template_item_template
    ON erp_assembly_template_item (tenant_id, template_id, line_no)
    WHERE deleted_at IS NULL;

COMMENT ON TABLE erp_assembly_template IS '组装/拆分业务模板';
COMMENT ON COLUMN erp_assembly_template.order_type IS '模板类型：ASSEMBLE / DISASSEMBLE';
COMMENT ON COLUMN erp_assembly_template.name IS '模板名称';
COMMENT ON COLUMN erp_assembly_template.finished_product_id IS '成品商品ID';
COMMENT ON COLUMN erp_assembly_template.finished_qty IS '默认成品数量';
COMMENT ON COLUMN erp_assembly_template.warehouse_id IS '默认仓库ID';
COMMENT ON COLUMN erp_assembly_template.location_id IS '默认库位ID';
COMMENT ON COLUMN erp_assembly_template.labor_cost IS '默认人工成本';
COMMENT ON COLUMN erp_assembly_template.remark IS '模板备注';

COMMENT ON TABLE erp_assembly_template_item IS '组装/拆分模板明细';
COMMENT ON COLUMN erp_assembly_template_item.template_id IS '模板ID';
COMMENT ON COLUMN erp_assembly_template_item.line_no IS '行号';
COMMENT ON COLUMN erp_assembly_template_item.product_id IS '物料商品ID';
COMMENT ON COLUMN erp_assembly_template_item.qty IS '默认数量';
COMMENT ON COLUMN erp_assembly_template_item.remark IS '明细备注';
