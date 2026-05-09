-- 商品组装/拆解单
CREATE TABLE IF NOT EXISTS erp_assembly_order (
    id                 BIGSERIAL PRIMARY KEY,
    tenant_id          BIGINT        NOT NULL,
    order_no           VARCHAR(64)   NOT NULL,
    order_type         VARCHAR(20)   NOT NULL DEFAULT 'ASSEMBLE',
    status             VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    finished_product_id BIGINT       NOT NULL,
    finished_qty       NUMERIC(18,4) NOT NULL DEFAULT 0,
    warehouse_id       BIGINT,
    location_id        BIGINT,
    labor_cost         NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_cost         NUMERIC(18,2) NOT NULL DEFAULT 0,
    unit_cost          NUMERIC(18,4) NOT NULL DEFAULT 0,
    remark             VARCHAR(500),
    approved_by        VARCHAR(100),
    approved_at        TIMESTAMPTZ,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_assembly_order_no
    ON erp_assembly_order (tenant_id, order_no);

CREATE INDEX IF NOT EXISTS idx_erp_assembly_order_status
    ON erp_assembly_order (tenant_id, status);

CREATE INDEX IF NOT EXISTS idx_erp_assembly_order_product
    ON erp_assembly_order (tenant_id, finished_product_id);

COMMENT ON TABLE erp_assembly_order IS 'ERP商品组装/拆解单';
COMMENT ON COLUMN erp_assembly_order.id IS '主键';
COMMENT ON COLUMN erp_assembly_order.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_assembly_order.order_no IS '单号';
COMMENT ON COLUMN erp_assembly_order.order_type IS '类型(ASSEMBLE/DISASSEMBLE)';
COMMENT ON COLUMN erp_assembly_order.status IS '状态';
COMMENT ON COLUMN erp_assembly_order.finished_product_id IS '成品商品ID';
COMMENT ON COLUMN erp_assembly_order.finished_qty IS '成品数量';
COMMENT ON COLUMN erp_assembly_order.warehouse_id IS '成品仓库';
COMMENT ON COLUMN erp_assembly_order.location_id IS '成品库位';
COMMENT ON COLUMN erp_assembly_order.labor_cost IS '工时费';
COMMENT ON COLUMN erp_assembly_order.total_cost IS '总成本';
COMMENT ON COLUMN erp_assembly_order.unit_cost IS '单位成本';
COMMENT ON COLUMN erp_assembly_order.remark IS '备注';
COMMENT ON COLUMN erp_assembly_order.approved_by IS '审核人';
COMMENT ON COLUMN erp_assembly_order.approved_at IS '审核时间';
COMMENT ON COLUMN erp_assembly_order.created_at IS '创建时间';
COMMENT ON COLUMN erp_assembly_order.updated_at IS '更新时间';

CREATE TABLE IF NOT EXISTS erp_assembly_order_item (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    BIGINT        NOT NULL,
    order_id     BIGINT        NOT NULL,
    line_no      INT           NOT NULL DEFAULT 0,
    product_id   BIGINT        NOT NULL,
    product_code VARCHAR(100),
    product_name VARCHAR(200),
    warehouse_id BIGINT,
    location_id  BIGINT,
    qty          NUMERIC(18,4) NOT NULL DEFAULT 0,
    unit_cost    NUMERIC(18,4) NOT NULL DEFAULT 0,
    amount       NUMERIC(18,2) NOT NULL DEFAULT 0,
    remark       VARCHAR(500),
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_erp_assembly_order_item_order
    ON erp_assembly_order_item (tenant_id, order_id);

COMMENT ON TABLE erp_assembly_order_item IS 'ERP商品组装/拆解明细';
COMMENT ON COLUMN erp_assembly_order_item.id IS '主键';
COMMENT ON COLUMN erp_assembly_order_item.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_assembly_order_item.order_id IS '组装单ID';
COMMENT ON COLUMN erp_assembly_order_item.line_no IS '行号';
COMMENT ON COLUMN erp_assembly_order_item.product_id IS '商品ID';
COMMENT ON COLUMN erp_assembly_order_item.product_code IS '商品编码快照';
COMMENT ON COLUMN erp_assembly_order_item.product_name IS '商品名称快照';
COMMENT ON COLUMN erp_assembly_order_item.warehouse_id IS '仓库ID';
COMMENT ON COLUMN erp_assembly_order_item.location_id IS '库位ID';
COMMENT ON COLUMN erp_assembly_order_item.qty IS '数量';
COMMENT ON COLUMN erp_assembly_order_item.unit_cost IS '单位成本';
COMMENT ON COLUMN erp_assembly_order_item.amount IS '金额';
COMMENT ON COLUMN erp_assembly_order_item.remark IS '备注';
COMMENT ON COLUMN erp_assembly_order_item.created_at IS '创建时间';
COMMENT ON COLUMN erp_assembly_order_item.updated_at IS '更新时间';
