ALTER TABLE erp_settlement_method
    ADD COLUMN IF NOT EXISTS fund_input_mode VARCHAR(20) NOT NULL DEFAULT 'OPTIONAL';

COMMENT ON COLUMN erp_settlement_method.fund_input_mode IS '即时收付款录入模式：HIDDEN不显示，OPTIONAL允许录入，REQUIRED必填';

UPDATE erp_settlement_method
SET fund_input_mode = 'HIDDEN',
    updated_at = NOW()
WHERE deleted_at IS NULL
  AND (
    UPPER(code) IN ('CREDIT', 'ON_ACCOUNT', 'AR', 'AP')
    OR name = '挂账'
  );

UPDATE erp_settlement_method
SET fund_input_mode = 'OPTIONAL',
    updated_at = NOW()
WHERE deleted_at IS NULL
  AND fund_input_mode IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_erp_settlement_method_fund_input_mode') THEN
        ALTER TABLE erp_settlement_method
            ADD CONSTRAINT ck_erp_settlement_method_fund_input_mode
            CHECK (fund_input_mode IN ('HIDDEN', 'OPTIONAL', 'REQUIRED'));
    END IF;
END $$;
