CREATE TABLE IF NOT EXISTS erp_product_stock_policy (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT        NOT NULL,
    product_id      BIGINT        NOT NULL,
    warehouse_id    BIGINT        NOT NULL,
    safety_stock    NUMERIC(18,4),
    min_stock       NUMERIC(18,4),
    max_stock       NUMERIC(18,4),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,
    deleted_by      VARCHAR(100),
    delete_reason   VARCHAR(500)
);

COMMENT ON TABLE erp_product_stock_policy IS '商品仓库级库存策略表';
COMMENT ON COLUMN erp_product_stock_policy.id IS '主键';
COMMENT ON COLUMN erp_product_stock_policy.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_product_stock_policy.product_id IS '商品ID';
COMMENT ON COLUMN erp_product_stock_policy.warehouse_id IS '仓库ID';
COMMENT ON COLUMN erp_product_stock_policy.safety_stock IS '安全库存';
COMMENT ON COLUMN erp_product_stock_policy.min_stock IS '最低库存';
COMMENT ON COLUMN erp_product_stock_policy.max_stock IS '最高库存';
COMMENT ON COLUMN erp_product_stock_policy.created_at IS '创建时间';
COMMENT ON COLUMN erp_product_stock_policy.updated_at IS '更新时间';
COMMENT ON COLUMN erp_product_stock_policy.deleted_at IS '删除时间';
COMMENT ON COLUMN erp_product_stock_policy.deleted_by IS '删除人';
COMMENT ON COLUMN erp_product_stock_policy.delete_reason IS '删除原因';

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_product_stock_policy_key
    ON erp_product_stock_policy (tenant_id, product_id, warehouse_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_product_stock_policy_product
    ON erp_product_stock_policy (tenant_id, product_id, deleted_at);

CREATE INDEX IF NOT EXISTS idx_erp_product_stock_policy_warehouse
    ON erp_product_stock_policy (tenant_id, warehouse_id, deleted_at);
