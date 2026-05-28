ALTER TABLE erp_accounts_receivable
    ADD COLUMN IF NOT EXISTS source_document_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS source_document_id BIGINT,
    ADD COLUMN IF NOT EXISTS source_business_flow VARCHAR(50),
    ADD COLUMN IF NOT EXISTS auto_flow_generated BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS auto_flow_mode VARCHAR(50),
    ADD COLUMN IF NOT EXISTS auto_flow_managed_state VARCHAR(50);

ALTER TABLE erp_accounts_payable
    ADD COLUMN IF NOT EXISTS source_document_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS source_document_id BIGINT,
    ADD COLUMN IF NOT EXISTS source_business_flow VARCHAR(50),
    ADD COLUMN IF NOT EXISTS auto_flow_generated BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS auto_flow_mode VARCHAR(50),
    ADD COLUMN IF NOT EXISTS auto_flow_managed_state VARCHAR(50);

ALTER TABLE erp_receipt
    ADD COLUMN IF NOT EXISTS source_document_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS source_document_id BIGINT,
    ADD COLUMN IF NOT EXISTS source_business_flow VARCHAR(50),
    ADD COLUMN IF NOT EXISTS auto_flow_generated BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS auto_flow_mode VARCHAR(50),
    ADD COLUMN IF NOT EXISTS auto_flow_managed_state VARCHAR(50);

ALTER TABLE erp_payment
    ADD COLUMN IF NOT EXISTS source_document_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS source_document_id BIGINT,
    ADD COLUMN IF NOT EXISTS source_business_flow VARCHAR(50),
    ADD COLUMN IF NOT EXISTS auto_flow_generated BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS auto_flow_mode VARCHAR(50),
    ADD COLUMN IF NOT EXISTS auto_flow_managed_state VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_erp_ar_auto_flow_source
    ON erp_accounts_receivable (tenant_id, source_document_type, source_document_id, source_business_flow)
    WHERE auto_flow_generated = TRUE AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_ap_auto_flow_source
    ON erp_accounts_payable (tenant_id, source_document_type, source_document_id, source_business_flow)
    WHERE auto_flow_generated = TRUE AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_receipt_auto_flow_source
    ON erp_receipt (tenant_id, source_document_type, source_document_id, source_business_flow)
    WHERE auto_flow_generated = TRUE AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_payment_auto_flow_source
    ON erp_payment (tenant_id, source_document_type, source_document_id, source_business_flow)
    WHERE auto_flow_generated = TRUE AND deleted_at IS NULL;

INSERT INTO app_system_config (
    tenant_id,
    config_key,
    config_value,
    value_type,
    description,
    is_public,
    created_at,
    updated_at
)
SELECT
    tenant.id,
    'erp.finance.auto-flow.mode',
    'AR_AP_WITH_APPROVED_PAYMENT',
    'string',
    '审核后财务自动联动模式',
    FALSE,
    NOW(),
    NOW()
FROM app_tenant tenant
WHERE tenant.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM app_system_config config
      WHERE config.tenant_id = tenant.id
        AND config.config_key = 'erp.finance.auto-flow.mode'
  );

COMMENT ON COLUMN erp_accounts_receivable.source_document_type IS '自动联动来源单据类型';
COMMENT ON COLUMN erp_accounts_receivable.source_document_id IS '自动联动来源单据ID';
COMMENT ON COLUMN erp_accounts_receivable.source_business_flow IS '自动联动业务流';
COMMENT ON COLUMN erp_accounts_receivable.auto_flow_generated IS '是否审核联动自动生成';
COMMENT ON COLUMN erp_accounts_receivable.auto_flow_mode IS '自动联动生成时模式';
COMMENT ON COLUMN erp_accounts_receivable.auto_flow_managed_state IS '自动联动托管状态';

COMMENT ON COLUMN erp_accounts_payable.source_document_type IS '自动联动来源单据类型';
COMMENT ON COLUMN erp_accounts_payable.source_document_id IS '自动联动来源单据ID';
COMMENT ON COLUMN erp_accounts_payable.source_business_flow IS '自动联动业务流';
COMMENT ON COLUMN erp_accounts_payable.auto_flow_generated IS '是否审核联动自动生成';
COMMENT ON COLUMN erp_accounts_payable.auto_flow_mode IS '自动联动生成时模式';
COMMENT ON COLUMN erp_accounts_payable.auto_flow_managed_state IS '自动联动托管状态';

COMMENT ON COLUMN erp_receipt.source_document_type IS '自动联动来源单据类型';
COMMENT ON COLUMN erp_receipt.source_document_id IS '自动联动来源单据ID';
COMMENT ON COLUMN erp_receipt.source_business_flow IS '自动联动业务流';
COMMENT ON COLUMN erp_receipt.auto_flow_generated IS '是否审核联动自动生成';
COMMENT ON COLUMN erp_receipt.auto_flow_mode IS '自动联动生成时模式';
COMMENT ON COLUMN erp_receipt.auto_flow_managed_state IS '自动联动托管状态';

COMMENT ON COLUMN erp_payment.source_document_type IS '自动联动来源单据类型';
COMMENT ON COLUMN erp_payment.source_document_id IS '自动联动来源单据ID';
COMMENT ON COLUMN erp_payment.source_business_flow IS '自动联动业务流';
COMMENT ON COLUMN erp_payment.auto_flow_generated IS '是否审核联动自动生成';
COMMENT ON COLUMN erp_payment.auto_flow_mode IS '自动联动生成时模式';
COMMENT ON COLUMN erp_payment.auto_flow_managed_state IS '自动联动托管状态';
