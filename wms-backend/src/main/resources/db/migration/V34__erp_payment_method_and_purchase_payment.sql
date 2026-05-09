-- ERP 付款方式 & 采购单付款字段

-- 付款方式表
CREATE TABLE IF NOT EXISTS erp_payment_method (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT        NOT NULL,
    code        VARCHAR(100)  NOT NULL,
    name        VARCHAR(200)  NOT NULL,
    sort_no     INT           NOT NULL DEFAULT 0,
    is_enabled  BOOLEAN       NOT NULL DEFAULT TRUE,
    is_default  BOOLEAN       NOT NULL DEFAULT FALSE,
    remark      VARCHAR(500),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE erp_payment_method IS '付款方式表（ERP进销存）';
COMMENT ON COLUMN erp_payment_method.id IS '主键';
COMMENT ON COLUMN erp_payment_method.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_payment_method.code IS '付款方式编码';
COMMENT ON COLUMN erp_payment_method.name IS '付款方式名称';
COMMENT ON COLUMN erp_payment_method.sort_no IS '排序';
COMMENT ON COLUMN erp_payment_method.is_enabled IS '是否启用';
COMMENT ON COLUMN erp_payment_method.is_default IS '是否默认';
COMMENT ON COLUMN erp_payment_method.remark IS '备注';
COMMENT ON COLUMN erp_payment_method.created_at IS '创建时间';
COMMENT ON COLUMN erp_payment_method.updated_at IS '更新时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_payment_method_code
    ON erp_payment_method (tenant_id, code);

CREATE INDEX IF NOT EXISTS idx_erp_payment_method_name
    ON erp_payment_method (tenant_id, name);

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_payment_method_default
    ON erp_payment_method (tenant_id)
    WHERE is_default;

-- 采购单新增付款字段
ALTER TABLE erp_purchase_order
    ADD COLUMN IF NOT EXISTS payment_method_code VARCHAR(100),
    ADD COLUMN IF NOT EXISTS paid_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(18,2) NOT NULL DEFAULT 0;

COMMENT ON COLUMN erp_purchase_order.payment_method_code IS '付款方式编码';
COMMENT ON COLUMN erp_purchase_order.paid_amount IS '付款金额';
COMMENT ON COLUMN erp_purchase_order.discount_amount IS '优惠金额';

-- 付款单补充付款方式
ALTER TABLE erp_payment
    ADD COLUMN IF NOT EXISTS payment_method_code VARCHAR(100);

COMMENT ON COLUMN erp_payment.payment_method_code IS '付款方式编码';

-- 默认付款方式
INSERT INTO erp_payment_method (tenant_id, code, name, sort_no, is_enabled, is_default, created_at, updated_at)
SELECT t.id, 'CASH', '现金', 10, TRUE, TRUE, NOW(), NOW()
FROM app_tenant t
WHERE t.deleted_at IS NULL
ON CONFLICT (tenant_id, code) DO NOTHING;

INSERT INTO erp_payment_method (tenant_id, code, name, sort_no, is_enabled, is_default, created_at, updated_at)
SELECT t.id, 'TRANSFER', '银行转账', 20, TRUE, FALSE, NOW(), NOW()
FROM app_tenant t
WHERE t.deleted_at IS NULL
ON CONFLICT (tenant_id, code) DO NOTHING;

-- 若无默认则补一个默认
UPDATE erp_payment_method m
SET is_default = TRUE
FROM (
    SELECT tenant_id, MIN(id) AS id
    FROM erp_payment_method
    GROUP BY tenant_id
) pick
WHERE m.tenant_id = pick.tenant_id
  AND m.id = pick.id
  AND NOT EXISTS (
      SELECT 1 FROM erp_payment_method m2
      WHERE m2.tenant_id = m.tenant_id
        AND m2.is_default = TRUE
  );
