-- 销售单增加结算方式字段（ERP进销存）
ALTER TABLE erp_sale_order
    ADD COLUMN IF NOT EXISTS settlement_method VARCHAR(20) NOT NULL DEFAULT 'CASH';

COMMENT ON COLUMN erp_sale_order.settlement_method IS '结算方式(CASH/TRANSFER/CREDIT)';
