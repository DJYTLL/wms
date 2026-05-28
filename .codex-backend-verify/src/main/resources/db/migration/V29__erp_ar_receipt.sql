-- ERP 应收与收款单
CREATE TABLE IF NOT EXISTS erp_accounts_receivable (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         BIGINT        NOT NULL,
    sale_order_id     BIGINT        NOT NULL,
    order_no          VARCHAR(64)   NOT NULL,
    customer_id       BIGINT        NOT NULL,
    total_amount      NUMERIC(18,2) NOT NULL DEFAULT 0,
    paid_amount       NUMERIC(18,2) NOT NULL DEFAULT 0,
    unpaid_amount     NUMERIC(18,2) NOT NULL DEFAULT 0,
    status            VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    settlement_method VARCHAR(50),
    remark            VARCHAR(500),
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_ar_tenant_sale
    ON erp_accounts_receivable (tenant_id, sale_order_id);

CREATE INDEX IF NOT EXISTS idx_erp_ar_customer
    ON erp_accounts_receivable (tenant_id, customer_id);

CREATE INDEX IF NOT EXISTS idx_erp_ar_status
    ON erp_accounts_receivable (tenant_id, status);

COMMENT ON TABLE erp_accounts_receivable IS 'ERP应收单';
COMMENT ON COLUMN erp_accounts_receivable.id IS '主键';
COMMENT ON COLUMN erp_accounts_receivable.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_accounts_receivable.sale_order_id IS '销售单ID';
COMMENT ON COLUMN erp_accounts_receivable.order_no IS '销售单号';
COMMENT ON COLUMN erp_accounts_receivable.customer_id IS '客户ID';
COMMENT ON COLUMN erp_accounts_receivable.total_amount IS '应收总金额';
COMMENT ON COLUMN erp_accounts_receivable.paid_amount IS '已收金额';
COMMENT ON COLUMN erp_accounts_receivable.unpaid_amount IS '未收金额';
COMMENT ON COLUMN erp_accounts_receivable.status IS '状态';
COMMENT ON COLUMN erp_accounts_receivable.settlement_method IS '结算方式';
COMMENT ON COLUMN erp_accounts_receivable.remark IS '备注';
COMMENT ON COLUMN erp_accounts_receivable.created_at IS '创建时间';
COMMENT ON COLUMN erp_accounts_receivable.updated_at IS '更新时间';

CREATE TABLE IF NOT EXISTS erp_receipt (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         BIGINT        NOT NULL,
    receivable_id     BIGINT,
    sale_order_id     BIGINT,
    receipt_no        VARCHAR(64)   NOT NULL,
    customer_id       BIGINT        NOT NULL,
    amount            NUMERIC(18,2) NOT NULL DEFAULT 0,
    settlement_method VARCHAR(50),
    status            VARCHAR(20)   NOT NULL DEFAULT 'APPROVED',
    received_at       TIMESTAMPTZ,
    remark            VARCHAR(500),
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_receipt_tenant_no
    ON erp_receipt (tenant_id, receipt_no);

CREATE INDEX IF NOT EXISTS idx_erp_receipt_sale
    ON erp_receipt (tenant_id, sale_order_id);

CREATE INDEX IF NOT EXISTS idx_erp_receipt_customer
    ON erp_receipt (tenant_id, customer_id);

COMMENT ON TABLE erp_receipt IS 'ERP收款单';
COMMENT ON COLUMN erp_receipt.id IS '主键';
COMMENT ON COLUMN erp_receipt.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_receipt.receivable_id IS '应收单ID';
COMMENT ON COLUMN erp_receipt.sale_order_id IS '销售单ID';
COMMENT ON COLUMN erp_receipt.receipt_no IS '收款单号';
COMMENT ON COLUMN erp_receipt.customer_id IS '客户ID';
COMMENT ON COLUMN erp_receipt.amount IS '收款金额';
COMMENT ON COLUMN erp_receipt.settlement_method IS '结算方式';
COMMENT ON COLUMN erp_receipt.status IS '状态';
COMMENT ON COLUMN erp_receipt.received_at IS '收款时间';
COMMENT ON COLUMN erp_receipt.remark IS '备注';
COMMENT ON COLUMN erp_receipt.created_at IS '创建时间';
COMMENT ON COLUMN erp_receipt.updated_at IS '更新时间';
