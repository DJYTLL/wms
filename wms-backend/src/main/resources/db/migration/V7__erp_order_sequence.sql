-- ERP order sequence table for order number generation.
CREATE TABLE IF NOT EXISTS erp_order_sequence (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT      NOT NULL,
    order_type    VARCHAR(40) NOT NULL,
    date_key      VARCHAR(16) NOT NULL,
    current_value BIGINT      NOT NULL DEFAULT 0,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE erp_order_sequence IS 'ERP单据号序列表（按租户/类型/日期）';
COMMENT ON COLUMN erp_order_sequence.id IS '主键';
COMMENT ON COLUMN erp_order_sequence.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_order_sequence.order_type IS '单据类型';
COMMENT ON COLUMN erp_order_sequence.date_key IS '日期键（yyyyMMdd）';
COMMENT ON COLUMN erp_order_sequence.current_value IS '当前序号';
COMMENT ON COLUMN erp_order_sequence.updated_at IS '更新时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_order_sequence_key
    ON erp_order_sequence (tenant_id, order_type, date_key);
