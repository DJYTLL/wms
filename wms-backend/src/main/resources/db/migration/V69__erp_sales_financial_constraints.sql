DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_erp_sale_order_settlement_amounts') THEN
        ALTER TABLE erp_sale_order
            ADD CONSTRAINT ck_erp_sale_order_settlement_amounts
            CHECK (
                paid_amount >= 0
                AND discount_amount >= 0
                AND paid_amount + discount_amount <= total_amount_incl_tax
            ) NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_erp_sale_return_settlement_amounts') THEN
        ALTER TABLE erp_sale_return
            ADD CONSTRAINT ck_erp_sale_return_settlement_amounts
            CHECK (
                paid_amount >= 0
                AND discount_amount >= 0
                AND paid_amount + discount_amount <= total_amount_incl_tax
            ) NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_erp_receipt_receivable_allocated_total') THEN
        ALTER TABLE erp_receipt_receivable
            ADD CONSTRAINT ck_erp_receipt_receivable_allocated_total
            CHECK (allocated_total = allocated_amount + allocated_discount) NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_erp_sale_order_red_flush_source_pair') THEN
        ALTER TABLE erp_sale_order
            ADD CONSTRAINT ck_erp_sale_order_red_flush_source_pair
            CHECK (
                (red_flush_source_type IS NULL AND red_flush_source_id IS NULL)
                OR (red_flush_source_type IS NOT NULL AND red_flush_source_id IS NOT NULL)
            ) NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_erp_sale_return_red_flush_source_pair') THEN
        ALTER TABLE erp_sale_return
            ADD CONSTRAINT ck_erp_sale_return_red_flush_source_pair
            CHECK (
                (red_flush_source_type IS NULL AND red_flush_source_id IS NULL)
                OR (red_flush_source_type IS NOT NULL AND red_flush_source_id IS NOT NULL)
            ) NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_erp_ar_red_flush_source_pair') THEN
        ALTER TABLE erp_accounts_receivable
            ADD CONSTRAINT ck_erp_ar_red_flush_source_pair
            CHECK (
                (red_flush_source_type IS NULL AND red_flush_source_id IS NULL)
                OR (red_flush_source_type IS NOT NULL AND red_flush_source_id IS NOT NULL)
            ) NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_erp_receipt_red_flush_source_pair') THEN
        ALTER TABLE erp_receipt
            ADD CONSTRAINT ck_erp_receipt_red_flush_source_pair
            CHECK (
                (red_flush_source_type IS NULL AND red_flush_source_id IS NULL)
                OR (red_flush_source_type IS NOT NULL AND red_flush_source_id IS NOT NULL)
            ) NOT VALID;
    END IF;
END $$;
