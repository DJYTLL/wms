CREATE TABLE IF NOT EXISTS erp_stock_transfer (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT        NOT NULL,
    transfer_no   VARCHAR(64)   NOT NULL,
    status        VARCHAR(20)   NOT NULL DEFAULT 'APPROVED',
    transfer_at   TIMESTAMPTZ,
    print_count   INT           NOT NULL DEFAULT 0,
    last_printed_at TIMESTAMPTZ,
    remark        VARCHAR(500),
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted_by    VARCHAR(100),
    delete_reason VARCHAR(500),
    deleted_at    TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_stock_transfer_no
    ON erp_stock_transfer (tenant_id, transfer_no)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_stock_transfer_status
    ON erp_stock_transfer (tenant_id, status)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_stock_transfer_at
    ON erp_stock_transfer (tenant_id, transfer_at DESC)
    WHERE deleted_at IS NULL;

COMMENT ON TABLE erp_stock_transfer IS 'ERP库存移库单';
COMMENT ON COLUMN erp_stock_transfer.id IS '主键';
COMMENT ON COLUMN erp_stock_transfer.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_stock_transfer.transfer_no IS '移库单号';
COMMENT ON COLUMN erp_stock_transfer.status IS '状态';
COMMENT ON COLUMN erp_stock_transfer.transfer_at IS '移库时间';
COMMENT ON COLUMN erp_stock_transfer.print_count IS '打印次数';
COMMENT ON COLUMN erp_stock_transfer.last_printed_at IS '最后打印时间';
COMMENT ON COLUMN erp_stock_transfer.remark IS '备注';
COMMENT ON COLUMN erp_stock_transfer.created_at IS '创建时间';
COMMENT ON COLUMN erp_stock_transfer.updated_at IS '更新时间';
COMMENT ON COLUMN erp_stock_transfer.deleted_by IS '删除人';
COMMENT ON COLUMN erp_stock_transfer.delete_reason IS '删除原因';
COMMENT ON COLUMN erp_stock_transfer.deleted_at IS '删除时间';

CREATE TABLE IF NOT EXISTS erp_stock_transfer_item (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT        NOT NULL,
    transfer_id         BIGINT        NOT NULL,
    line_no             INT           NOT NULL DEFAULT 0,
    product_id          BIGINT        NOT NULL,
    from_warehouse_id   BIGINT        NOT NULL,
    from_location_id    BIGINT,
    to_warehouse_id     BIGINT        NOT NULL,
    to_location_id      BIGINT,
    qty                 NUMERIC(18,4) NOT NULL DEFAULT 0,
    unit_cost           NUMERIC(18,4) NOT NULL DEFAULT 0,
    amount              NUMERIC(18,4) NOT NULL DEFAULT 0,
    remark              VARCHAR(500),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted_by          VARCHAR(100),
    delete_reason       VARCHAR(500),
    deleted_at          TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_erp_stock_transfer_item_transfer
    ON erp_stock_transfer_item (tenant_id, transfer_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_stock_transfer_item_product
    ON erp_stock_transfer_item (tenant_id, product_id)
    WHERE deleted_at IS NULL;

COMMENT ON TABLE erp_stock_transfer_item IS 'ERP库存移库单明细';
COMMENT ON COLUMN erp_stock_transfer_item.id IS '主键';
COMMENT ON COLUMN erp_stock_transfer_item.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_stock_transfer_item.transfer_id IS '移库单ID';
COMMENT ON COLUMN erp_stock_transfer_item.line_no IS '行号';
COMMENT ON COLUMN erp_stock_transfer_item.product_id IS '商品ID';
COMMENT ON COLUMN erp_stock_transfer_item.from_warehouse_id IS '来源仓库ID';
COMMENT ON COLUMN erp_stock_transfer_item.from_location_id IS '来源库位ID';
COMMENT ON COLUMN erp_stock_transfer_item.to_warehouse_id IS '目标仓库ID';
COMMENT ON COLUMN erp_stock_transfer_item.to_location_id IS '目标库位ID';
COMMENT ON COLUMN erp_stock_transfer_item.qty IS '移库数量';
COMMENT ON COLUMN erp_stock_transfer_item.unit_cost IS '单位成本';
COMMENT ON COLUMN erp_stock_transfer_item.amount IS '金额';
COMMENT ON COLUMN erp_stock_transfer_item.remark IS '备注';
COMMENT ON COLUMN erp_stock_transfer_item.created_at IS '创建时间';
COMMENT ON COLUMN erp_stock_transfer_item.updated_at IS '更新时间';
COMMENT ON COLUMN erp_stock_transfer_item.deleted_by IS '删除人';
COMMENT ON COLUMN erp_stock_transfer_item.delete_reason IS '删除原因';
COMMENT ON COLUMN erp_stock_transfer_item.deleted_at IS '删除时间';
