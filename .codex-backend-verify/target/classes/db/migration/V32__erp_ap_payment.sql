-- ERP 应付与付款单
CREATE TABLE IF NOT EXISTS erp_accounts_payable (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         BIGINT        NOT NULL,
    purchase_order_id BIGINT        NOT NULL,
    order_no          VARCHAR(64)   NOT NULL,
    supplier_id       BIGINT        NOT NULL,
    total_amount      NUMERIC(18,2) NOT NULL DEFAULT 0,
    paid_amount       NUMERIC(18,2) NOT NULL DEFAULT 0,
    unpaid_amount     NUMERIC(18,2) NOT NULL DEFAULT 0,
    status            VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    settlement_method VARCHAR(50),
    remark            VARCHAR(500),
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_ap_tenant_purchase
    ON erp_accounts_payable (tenant_id, purchase_order_id);

CREATE INDEX IF NOT EXISTS idx_erp_ap_supplier
    ON erp_accounts_payable (tenant_id, supplier_id);

CREATE INDEX IF NOT EXISTS idx_erp_ap_status
    ON erp_accounts_payable (tenant_id, status);

COMMENT ON TABLE erp_accounts_payable IS 'ERP应付单';
COMMENT ON COLUMN erp_accounts_payable.id IS '主键';
COMMENT ON COLUMN erp_accounts_payable.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_accounts_payable.purchase_order_id IS '采购单ID';
COMMENT ON COLUMN erp_accounts_payable.order_no IS '采购单号';
COMMENT ON COLUMN erp_accounts_payable.supplier_id IS '供应商ID';
COMMENT ON COLUMN erp_accounts_payable.total_amount IS '应付总金额';
COMMENT ON COLUMN erp_accounts_payable.paid_amount IS '已付金额';
COMMENT ON COLUMN erp_accounts_payable.unpaid_amount IS '未付金额';
COMMENT ON COLUMN erp_accounts_payable.status IS '状态';
COMMENT ON COLUMN erp_accounts_payable.settlement_method IS '结算方式';
COMMENT ON COLUMN erp_accounts_payable.remark IS '备注';
COMMENT ON COLUMN erp_accounts_payable.created_at IS '创建时间';
COMMENT ON COLUMN erp_accounts_payable.updated_at IS '更新时间';

CREATE TABLE IF NOT EXISTS erp_payment (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         BIGINT        NOT NULL,
    payable_id        BIGINT,
    purchase_order_id BIGINT,
    payment_no        VARCHAR(64)   NOT NULL,
    supplier_id       BIGINT        NOT NULL,
    amount            NUMERIC(18,2) NOT NULL DEFAULT 0,
    discount_amount   NUMERIC(18,2) NOT NULL DEFAULT 0,
    settlement_method VARCHAR(50),
    status            VARCHAR(20)   NOT NULL DEFAULT 'APPROVED',
    paid_at           TIMESTAMPTZ,
    remark            VARCHAR(500),
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_payment_tenant_no
    ON erp_payment (tenant_id, payment_no);

CREATE INDEX IF NOT EXISTS idx_erp_payment_purchase
    ON erp_payment (tenant_id, purchase_order_id);

CREATE INDEX IF NOT EXISTS idx_erp_payment_supplier
    ON erp_payment (tenant_id, supplier_id);

COMMENT ON TABLE erp_payment IS 'ERP付款单';
COMMENT ON COLUMN erp_payment.id IS '主键';
COMMENT ON COLUMN erp_payment.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_payment.payable_id IS '应付单ID';
COMMENT ON COLUMN erp_payment.purchase_order_id IS '采购单ID';
COMMENT ON COLUMN erp_payment.payment_no IS '付款单号';
COMMENT ON COLUMN erp_payment.supplier_id IS '供应商ID';
COMMENT ON COLUMN erp_payment.amount IS '付款金额';
COMMENT ON COLUMN erp_payment.discount_amount IS '优惠金额';
COMMENT ON COLUMN erp_payment.settlement_method IS '结算方式';
COMMENT ON COLUMN erp_payment.status IS '状态';
COMMENT ON COLUMN erp_payment.paid_at IS '付款时间';
COMMENT ON COLUMN erp_payment.remark IS '备注';
COMMENT ON COLUMN erp_payment.created_at IS '创建时间';
COMMENT ON COLUMN erp_payment.updated_at IS '更新时间';

CREATE TABLE IF NOT EXISTS erp_payment_payable (
    id                 BIGSERIAL PRIMARY KEY,
    tenant_id          BIGINT        NOT NULL,
    payment_id         BIGINT        NOT NULL,
    payable_id         BIGINT        NOT NULL,
    allocated_amount   NUMERIC(18,2) NOT NULL DEFAULT 0,
    allocated_discount NUMERIC(18,2) NOT NULL DEFAULT 0,
    allocated_total    NUMERIC(18,2) NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_payment_payable
    ON erp_payment_payable (tenant_id, payment_id, payable_id);

CREATE INDEX IF NOT EXISTS idx_erp_payment_payable_payable
    ON erp_payment_payable (tenant_id, payable_id);

COMMENT ON TABLE erp_payment_payable IS 'ERP付款单-应付分摊';
COMMENT ON COLUMN erp_payment_payable.id IS '主键';
COMMENT ON COLUMN erp_payment_payable.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_payment_payable.payment_id IS '付款单ID';
COMMENT ON COLUMN erp_payment_payable.payable_id IS '应付单ID';
COMMENT ON COLUMN erp_payment_payable.allocated_amount IS '分摊付款金额';
COMMENT ON COLUMN erp_payment_payable.allocated_discount IS '分摊优惠金额';
COMMENT ON COLUMN erp_payment_payable.allocated_total IS '分摊合计金额';
COMMENT ON COLUMN erp_payment_payable.created_at IS '创建时间';
