-- Extend print fields to more ERP documents
ALTER TABLE IF EXISTS erp_sale_return
    ADD COLUMN IF NOT EXISTS print_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_printed_at TIMESTAMPTZ;

ALTER TABLE IF EXISTS erp_purchase_return
    ADD COLUMN IF NOT EXISTS print_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_printed_at TIMESTAMPTZ;

ALTER TABLE IF EXISTS erp_receipt
    ADD COLUMN IF NOT EXISTS print_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_printed_at TIMESTAMPTZ;

ALTER TABLE IF EXISTS erp_payment
    ADD COLUMN IF NOT EXISTS print_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_printed_at TIMESTAMPTZ;

ALTER TABLE IF EXISTS erp_accounts_receivable
    ADD COLUMN IF NOT EXISTS print_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_printed_at TIMESTAMPTZ;

ALTER TABLE IF EXISTS erp_accounts_payable
    ADD COLUMN IF NOT EXISTS print_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_printed_at TIMESTAMPTZ;

ALTER TABLE IF EXISTS erp_stock_count
    ADD COLUMN IF NOT EXISTS print_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_printed_at TIMESTAMPTZ;

COMMENT ON COLUMN erp_sale_return.print_count IS '打印次数';
COMMENT ON COLUMN erp_sale_return.last_printed_at IS '最后打印时间';
COMMENT ON COLUMN erp_purchase_return.print_count IS '打印次数';
COMMENT ON COLUMN erp_purchase_return.last_printed_at IS '最后打印时间';
COMMENT ON COLUMN erp_receipt.print_count IS '打印次数';
COMMENT ON COLUMN erp_receipt.last_printed_at IS '最后打印时间';
COMMENT ON COLUMN erp_payment.print_count IS '打印次数';
COMMENT ON COLUMN erp_payment.last_printed_at IS '最后打印时间';
COMMENT ON COLUMN erp_accounts_receivable.print_count IS '打印次数';
COMMENT ON COLUMN erp_accounts_receivable.last_printed_at IS '最后打印时间';
COMMENT ON COLUMN erp_accounts_payable.print_count IS '打印次数';
COMMENT ON COLUMN erp_accounts_payable.last_printed_at IS '最后打印时间';
COMMENT ON COLUMN erp_stock_count.print_count IS '打印次数';
COMMENT ON COLUMN erp_stock_count.last_printed_at IS '最后打印时间';
