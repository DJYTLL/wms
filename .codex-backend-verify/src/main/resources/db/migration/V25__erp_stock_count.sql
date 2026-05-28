-- ERP库存盘点与初始库存

-- 库存盘点单表
CREATE TABLE IF NOT EXISTS erp_stock_count (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    BIGINT      NOT NULL,
    count_no     VARCHAR(64) NOT NULL,
    count_type   VARCHAR(20) NOT NULL DEFAULT 'COUNT',
    status       VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    warehouse_id BIGINT,
    location_id  BIGINT,
    count_at     TIMESTAMPTZ,
    remark       VARCHAR(500),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    approved_by  VARCHAR(100),
    approved_at  TIMESTAMPTZ,
    cancelled_by VARCHAR(100),
    cancelled_at TIMESTAMPTZ
);

COMMENT ON TABLE erp_stock_count IS '库存盘点单表（ERP进销存）';
COMMENT ON COLUMN erp_stock_count.id IS '主键';
COMMENT ON COLUMN erp_stock_count.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_stock_count.count_no IS '盘点单号';
COMMENT ON COLUMN erp_stock_count.count_type IS '盘点类型（COUNT/INIT）';
COMMENT ON COLUMN erp_stock_count.status IS '状态（DRAFT/APPROVED/CANCELLED）';
COMMENT ON COLUMN erp_stock_count.warehouse_id IS '仓库ID';
COMMENT ON COLUMN erp_stock_count.location_id IS '库位ID';
COMMENT ON COLUMN erp_stock_count.count_at IS '盘点时间';
COMMENT ON COLUMN erp_stock_count.remark IS '备注';
COMMENT ON COLUMN erp_stock_count.created_at IS '创建时间';
COMMENT ON COLUMN erp_stock_count.updated_at IS '更新时间';
COMMENT ON COLUMN erp_stock_count.approved_by IS '审核人';
COMMENT ON COLUMN erp_stock_count.approved_at IS '审核时间';
COMMENT ON COLUMN erp_stock_count.cancelled_by IS '作废人';
COMMENT ON COLUMN erp_stock_count.cancelled_at IS '作废时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_stock_count_no
    ON erp_stock_count (tenant_id, count_no);
CREATE INDEX IF NOT EXISTS idx_erp_stock_count_status
    ON erp_stock_count (tenant_id, count_type, status, created_at DESC);

-- 库存盘点单明细表
CREATE TABLE IF NOT EXISTS erp_stock_count_item (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT        NOT NULL,
    count_id    BIGINT        NOT NULL,
    line_no     INTEGER       NOT NULL DEFAULT 1,
    product_id  BIGINT        NOT NULL,
    warehouse_id BIGINT,
    location_id BIGINT,
    system_qty  NUMERIC(18,4) NOT NULL DEFAULT 0,
    counted_qty NUMERIC(18,4) NOT NULL DEFAULT 0,
    diff_qty    NUMERIC(18,4) NOT NULL DEFAULT 0,
    remark      VARCHAR(500),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE erp_stock_count_item IS '库存盘点单明细表（ERP进销存）';
COMMENT ON COLUMN erp_stock_count_item.id IS '主键';
COMMENT ON COLUMN erp_stock_count_item.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_stock_count_item.count_id IS '盘点单ID';
COMMENT ON COLUMN erp_stock_count_item.line_no IS '明细排序';
COMMENT ON COLUMN erp_stock_count_item.product_id IS '商品ID';
COMMENT ON COLUMN erp_stock_count_item.warehouse_id IS '仓库ID';
COMMENT ON COLUMN erp_stock_count_item.location_id IS '库位ID';
COMMENT ON COLUMN erp_stock_count_item.system_qty IS '系统数量';
COMMENT ON COLUMN erp_stock_count_item.counted_qty IS '盘点数量';
COMMENT ON COLUMN erp_stock_count_item.diff_qty IS '差异数量';
COMMENT ON COLUMN erp_stock_count_item.remark IS '备注';
COMMENT ON COLUMN erp_stock_count_item.created_at IS '创建时间';
COMMENT ON COLUMN erp_stock_count_item.updated_at IS '更新时间';

CREATE INDEX IF NOT EXISTS idx_erp_stock_count_item_count
    ON erp_stock_count_item (tenant_id, count_id);
CREATE INDEX IF NOT EXISTS idx_erp_stock_count_item_product
    ON erp_stock_count_item (tenant_id, product_id, warehouse_id, location_id);
