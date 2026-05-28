CREATE OR REPLACE FUNCTION erp_validate_receipt_receivable_sign()
RETURNS trigger AS $$
DECLARE
    ar_total NUMERIC(18, 2);
BEGIN
    SELECT total_amount
      INTO ar_total
      FROM erp_accounts_receivable
     WHERE tenant_id = NEW.tenant_id
       AND id = NEW.receivable_id
       AND deleted_at IS NULL;

    IF ar_total IS NULL THEN
        RAISE EXCEPTION '应收单不存在';
    END IF;

    IF ar_total > 0 AND (
        NEW.allocated_amount < 0
        OR NEW.allocated_discount < 0
        OR NEW.allocated_total <= 0
    ) THEN
        RAISE EXCEPTION '普通应收分摊金额必须大于0';
    END IF;

    IF ar_total < 0 AND (
        NEW.allocated_amount > 0
        OR NEW.allocated_discount > 0
        OR NEW.allocated_total >= 0
    ) THEN
        RAISE EXCEPTION '退货应收分摊金额必须小于0';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_erp_receipt_receivable_sign_guard ON erp_receipt_receivable;

CREATE TRIGGER trg_erp_receipt_receivable_sign_guard
BEFORE INSERT OR UPDATE ON erp_receipt_receivable
FOR EACH ROW
WHEN (NEW.deleted_at IS NULL)
EXECUTE FUNCTION erp_validate_receipt_receivable_sign();
