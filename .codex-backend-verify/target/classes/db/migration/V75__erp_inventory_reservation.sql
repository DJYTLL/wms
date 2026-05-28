ALTER TABLE erp_stock_balance
    ADD COLUMN IF NOT EXISTS qty_reserved NUMERIC(18,4) NOT NULL DEFAULT 0;

COMMENT ON COLUMN erp_stock_balance.qty_reserved IS '锁定库存';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_erp_stock_balance_qty_reserved_non_negative'
    ) THEN
        ALTER TABLE erp_stock_balance
            ADD CONSTRAINT ck_erp_stock_balance_qty_reserved_non_negative
            CHECK (qty_reserved >= 0);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_erp_stock_balance_qty_reserved_not_exceed_on_hand'
    ) THEN
        ALTER TABLE erp_stock_balance
            ADD CONSTRAINT ck_erp_stock_balance_qty_reserved_not_exceed_on_hand
            CHECK (qty_reserved <= qty_on_hand);
    END IF;
END $$;

ALTER TABLE erp_sale_order
    ADD COLUMN IF NOT EXISTS inventory_reserved BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN erp_sale_order.inventory_reserved IS '草稿是否已占用库存';

ALTER TABLE erp_purchase_return
    ADD COLUMN IF NOT EXISTS inventory_reserved BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN erp_purchase_return.inventory_reserved IS '草稿是否已占用库存';

ALTER TABLE erp_assembly_order
    ADD COLUMN IF NOT EXISTS inventory_reserved BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN erp_assembly_order.inventory_reserved IS '草稿是否已占用库存';
