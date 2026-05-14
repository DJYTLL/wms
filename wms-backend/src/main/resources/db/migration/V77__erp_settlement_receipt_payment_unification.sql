-- ERP 结算/收付款模型统一

-- 收款方式表
CREATE TABLE IF NOT EXISTS erp_receipt_method (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT        NOT NULL,
    code        VARCHAR(100)  NOT NULL,
    name        VARCHAR(200)  NOT NULL,
    sort_no     INT           NOT NULL DEFAULT 0,
    is_enabled  BOOLEAN       NOT NULL DEFAULT TRUE,
    is_default  BOOLEAN       NOT NULL DEFAULT FALSE,
    remark      VARCHAR(500),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ,
    deleted_by  VARCHAR(100),
    delete_reason VARCHAR(500)
);

COMMENT ON TABLE erp_receipt_method IS '收款方式表（ERP进销存）';
COMMENT ON COLUMN erp_receipt_method.code IS '收款方式编码';
COMMENT ON COLUMN erp_receipt_method.name IS '收款方式名称';
COMMENT ON COLUMN erp_receipt_method.is_default IS '是否默认';

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_receipt_method_code
    ON erp_receipt_method (tenant_id, code)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_receipt_method_name
    ON erp_receipt_method (tenant_id, name);

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_receipt_method_default
    ON erp_receipt_method (tenant_id)
    WHERE is_default = TRUE AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_receipt_method_tenant_deleted_at
    ON erp_receipt_method (tenant_id, deleted_at);

ALTER TABLE erp_customer
    ADD COLUMN IF NOT EXISTS default_settlement_method_code VARCHAR(100),
    ADD COLUMN IF NOT EXISTS default_receipt_method_code VARCHAR(100);

ALTER TABLE erp_supplier
    ADD COLUMN IF NOT EXISTS default_settlement_method_code VARCHAR(100),
    ADD COLUMN IF NOT EXISTS default_payment_method_code VARCHAR(100);

ALTER TABLE erp_sale_order
    ADD COLUMN IF NOT EXISTS receipt_method_code VARCHAR(100);

ALTER TABLE erp_purchase_order
    ADD COLUMN IF NOT EXISTS settlement_method VARCHAR(100);

ALTER TABLE erp_sale_return
    ADD COLUMN IF NOT EXISTS receipt_method_code VARCHAR(100),
    ADD COLUMN IF NOT EXISTS refund_action VARCHAR(20) NOT NULL DEFAULT 'OFFSET_AR';

ALTER TABLE erp_purchase_return
    ADD COLUMN IF NOT EXISTS payment_method_code VARCHAR(100),
    ADD COLUMN IF NOT EXISTS refund_action VARCHAR(20) NOT NULL DEFAULT 'OFFSET_AP';

ALTER TABLE erp_receipt
    ADD COLUMN IF NOT EXISTS receipt_method_code VARCHAR(100);

COMMENT ON COLUMN erp_customer.default_settlement_method_code IS '默认结算方式编码';
COMMENT ON COLUMN erp_customer.default_receipt_method_code IS '默认收款方式编码';
COMMENT ON COLUMN erp_supplier.default_settlement_method_code IS '默认结算方式编码';
COMMENT ON COLUMN erp_supplier.default_payment_method_code IS '默认付款方式编码';
COMMENT ON COLUMN erp_sale_order.receipt_method_code IS '收款方式编码';
COMMENT ON COLUMN erp_purchase_order.settlement_method IS '结算方式编码';
COMMENT ON COLUMN erp_sale_return.receipt_method_code IS '收款方式编码';
COMMENT ON COLUMN erp_sale_return.refund_action IS '退货处理方式(REFUND/OFFSET_AR)';
COMMENT ON COLUMN erp_purchase_return.payment_method_code IS '付款方式编码';
COMMENT ON COLUMN erp_purchase_return.refund_action IS '退货处理方式(REFUND/OFFSET_AP)';
COMMENT ON COLUMN erp_receipt.receipt_method_code IS '收款方式编码';

-- 历史数据回填
UPDATE erp_customer
SET default_settlement_method_code = COALESCE(NULLIF(default_settlement_method_code, ''), NULLIF(payment_terms, ''))
WHERE default_settlement_method_code IS NULL;

UPDATE erp_supplier
SET default_payment_method_code = COALESCE(NULLIF(default_payment_method_code, ''), NULLIF(payment_terms, ''))
WHERE default_payment_method_code IS NULL;

UPDATE erp_purchase_order po
SET settlement_method = COALESCE(
    NULLIF(po.settlement_method, ''),
    NULLIF(s.default_settlement_method_code, ''),
    (
        SELECT code
        FROM erp_settlement_method sm
        WHERE sm.tenant_id = po.tenant_id
          AND sm.is_default = TRUE
          AND sm.deleted_at IS NULL
        LIMIT 1
    )
)
FROM erp_supplier s
WHERE s.id = po.supplier_id
  AND s.tenant_id = po.tenant_id
  AND po.settlement_method IS NULL;

UPDATE erp_purchase_return
SET refund_action = 'OFFSET_AP'
WHERE refund_action IS NULL OR refund_action = '';

UPDATE erp_sale_return
SET refund_action = 'OFFSET_AR'
WHERE refund_action IS NULL OR refund_action = '';

INSERT INTO erp_receipt_method (tenant_id, code, name, sort_no, is_enabled, is_default, created_at, updated_at)
SELECT t.id, 'CASH', '现金', 10, TRUE, TRUE, NOW(), NOW()
FROM app_tenant t
WHERE t.deleted_at IS NULL
ON CONFLICT DO NOTHING;

INSERT INTO erp_receipt_method (tenant_id, code, name, sort_no, is_enabled, is_default, created_at, updated_at)
SELECT t.id, 'TRANSFER', '银行转账', 20, TRUE, FALSE, NOW(), NOW()
FROM app_tenant t
WHERE t.deleted_at IS NULL
ON CONFLICT DO NOTHING;

UPDATE erp_receipt_method m
SET is_default = TRUE
FROM (
    SELECT tenant_id, MIN(id) AS id
    FROM erp_receipt_method
    WHERE deleted_at IS NULL
    GROUP BY tenant_id
) pick
WHERE m.tenant_id = pick.tenant_id
  AND m.id = pick.id
  AND NOT EXISTS (
      SELECT 1
      FROM erp_receipt_method m2
      WHERE m2.tenant_id = m.tenant_id
        AND m2.is_default = TRUE
        AND m2.deleted_at IS NULL
  );
