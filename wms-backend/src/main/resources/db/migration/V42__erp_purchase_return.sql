-- 采购退货单（含草稿/已审核）
CREATE TABLE IF NOT EXISTS erp_purchase_return (
    id                    BIGSERIAL PRIMARY KEY,
    tenant_id             BIGINT        NOT NULL,
    order_no              VARCHAR(64)   NOT NULL,
    status                VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    return_type           VARCHAR(20)   NOT NULL DEFAULT 'RETURN',
    supplier_id           BIGINT,
    purchase_order_id     BIGINT,
    order_at              TIMESTAMPTZ,
    settlement_method     VARCHAR(50),
    paid_amount           NUMERIC(18,2) NOT NULL DEFAULT 0,
    discount_amount       NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_amount          NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_amount_excl_tax NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_tax_amount      NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_amount_incl_tax NUMERIC(18,2) NOT NULL DEFAULT 0,
    version               BIGINT        NOT NULL DEFAULT 0,
    approved_by           VARCHAR(100),
    approved_at           TIMESTAMPTZ,
    remark                VARCHAR(500),
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_purchase_return_no
    ON erp_purchase_return (tenant_id, order_no);

CREATE INDEX IF NOT EXISTS idx_erp_purchase_return_supplier
    ON erp_purchase_return (tenant_id, supplier_id);

CREATE INDEX IF NOT EXISTS idx_erp_purchase_return_status
    ON erp_purchase_return (tenant_id, status);

COMMENT ON TABLE erp_purchase_return IS 'ERP采购退货单';
COMMENT ON COLUMN erp_purchase_return.id IS '主键';
COMMENT ON COLUMN erp_purchase_return.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_purchase_return.order_no IS '退货单号';
COMMENT ON COLUMN erp_purchase_return.status IS '状态';
COMMENT ON COLUMN erp_purchase_return.return_type IS '退货处理方式(RETURN/SCRAP)';
COMMENT ON COLUMN erp_purchase_return.supplier_id IS '供应商ID';
COMMENT ON COLUMN erp_purchase_return.purchase_order_id IS '关联采购单ID';
COMMENT ON COLUMN erp_purchase_return.order_at IS '单据时间';
COMMENT ON COLUMN erp_purchase_return.settlement_method IS '结算方式';
COMMENT ON COLUMN erp_purchase_return.paid_amount IS '已付金额';
COMMENT ON COLUMN erp_purchase_return.discount_amount IS '优惠金额';
COMMENT ON COLUMN erp_purchase_return.total_amount IS '总金额';
COMMENT ON COLUMN erp_purchase_return.total_amount_excl_tax IS '未税总金额';
COMMENT ON COLUMN erp_purchase_return.total_tax_amount IS '税额合计';
COMMENT ON COLUMN erp_purchase_return.total_amount_incl_tax IS '含税总金额';
COMMENT ON COLUMN erp_purchase_return.version IS '乐观锁版本';
COMMENT ON COLUMN erp_purchase_return.approved_by IS '审核人';
COMMENT ON COLUMN erp_purchase_return.approved_at IS '审核时间';
COMMENT ON COLUMN erp_purchase_return.remark IS '备注';
COMMENT ON COLUMN erp_purchase_return.created_at IS '创建时间';
COMMENT ON COLUMN erp_purchase_return.updated_at IS '更新时间';

-- 采购退货明细
CREATE TABLE IF NOT EXISTS erp_purchase_return_item (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT        NOT NULL,
    return_id       BIGINT        NOT NULL,
    product_id      BIGINT        NOT NULL,
    product_code    VARCHAR(100),
    product_name    VARCHAR(200),
    warehouse_id    BIGINT,
    location_id     BIGINT,
    qty             NUMERIC(18,4) NOT NULL DEFAULT 0,
    price           NUMERIC(18,2) NOT NULL DEFAULT 0,
    price_incl_tax  NUMERIC(18,2) NOT NULL DEFAULT 0,
    amount          NUMERIC(18,2) NOT NULL DEFAULT 0,
    amount_incl_tax NUMERIC(18,2) NOT NULL DEFAULT 0,
    tax_rate        NUMERIC(10,4) NOT NULL DEFAULT 0,
    tax_amount      NUMERIC(18,2) NOT NULL DEFAULT 0,
    sort_no         INT           NOT NULL DEFAULT 0,
    remark          VARCHAR(500),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_erp_purchase_return_item_return
    ON erp_purchase_return_item (tenant_id, return_id);

COMMENT ON TABLE erp_purchase_return_item IS 'ERP采购退货明细';
COMMENT ON COLUMN erp_purchase_return_item.id IS '主键';
COMMENT ON COLUMN erp_purchase_return_item.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_purchase_return_item.return_id IS '退货单ID';
COMMENT ON COLUMN erp_purchase_return_item.product_id IS '商品ID';
COMMENT ON COLUMN erp_purchase_return_item.product_code IS '商品编码快照';
COMMENT ON COLUMN erp_purchase_return_item.product_name IS '商品名称快照';
COMMENT ON COLUMN erp_purchase_return_item.warehouse_id IS '仓库ID';
COMMENT ON COLUMN erp_purchase_return_item.location_id IS '库位ID';
COMMENT ON COLUMN erp_purchase_return_item.qty IS '数量';
COMMENT ON COLUMN erp_purchase_return_item.price IS '单价';
COMMENT ON COLUMN erp_purchase_return_item.price_incl_tax IS '含税单价';
COMMENT ON COLUMN erp_purchase_return_item.amount IS '金额';
COMMENT ON COLUMN erp_purchase_return_item.amount_incl_tax IS '含税金额';
COMMENT ON COLUMN erp_purchase_return_item.tax_rate IS '税率';
COMMENT ON COLUMN erp_purchase_return_item.tax_amount IS '税额';
COMMENT ON COLUMN erp_purchase_return_item.sort_no IS '排序';
COMMENT ON COLUMN erp_purchase_return_item.remark IS '备注';
COMMENT ON COLUMN erp_purchase_return_item.created_at IS '创建时间';
COMMENT ON COLUMN erp_purchase_return_item.updated_at IS '更新时间';
