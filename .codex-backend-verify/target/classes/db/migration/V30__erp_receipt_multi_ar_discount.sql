-- ERP 收款单多应收关联与优惠金额
ALTER TABLE erp_receipt
    ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(18,2) NOT NULL DEFAULT 0;

COMMENT ON COLUMN erp_receipt.discount_amount IS '优惠金额';

CREATE TABLE IF NOT EXISTS erp_receipt_receivable (
    id                 BIGSERIAL PRIMARY KEY,
    tenant_id          BIGINT        NOT NULL,
    receipt_id         BIGINT        NOT NULL,
    receivable_id      BIGINT        NOT NULL,
    allocated_amount   NUMERIC(18,2) NOT NULL DEFAULT 0,
    allocated_discount NUMERIC(18,2) NOT NULL DEFAULT 0,
    allocated_total    NUMERIC(18,2) NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_receipt_receivable
    ON erp_receipt_receivable (tenant_id, receipt_id, receivable_id);

CREATE INDEX IF NOT EXISTS idx_erp_receipt_receivable_receivable
    ON erp_receipt_receivable (tenant_id, receivable_id);

COMMENT ON TABLE erp_receipt_receivable IS 'ERP收款单-应收分摊';
COMMENT ON COLUMN erp_receipt_receivable.id IS '主键';
COMMENT ON COLUMN erp_receipt_receivable.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_receipt_receivable.receipt_id IS '收款单ID';
COMMENT ON COLUMN erp_receipt_receivable.receivable_id IS '应收单ID';
COMMENT ON COLUMN erp_receipt_receivable.allocated_amount IS '分摊收款金额';
COMMENT ON COLUMN erp_receipt_receivable.allocated_discount IS '分摊优惠金额';
COMMENT ON COLUMN erp_receipt_receivable.allocated_total IS '分摊合计金额';
COMMENT ON COLUMN erp_receipt_receivable.created_at IS '创建时间';
