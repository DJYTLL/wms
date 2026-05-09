-- 确保多应收分摊与优惠金额字段存在（补偿迁移）
ALTER TABLE erp_receipt
    ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(18,2) NOT NULL DEFAULT 0;

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
