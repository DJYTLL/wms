ALTER TABLE erp_stock_txn
    ADD COLUMN IF NOT EXISTS unit_cost NUMERIC(18,4) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_cost NUMERIC(18,4) NOT NULL DEFAULT 0;

COMMENT ON COLUMN erp_stock_txn.unit_cost IS '单位成本';
COMMENT ON COLUMN erp_stock_txn.total_cost IS '成本金额';
