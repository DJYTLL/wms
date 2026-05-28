-- 客户表补充送货方式字段（ERP进销存）
ALTER TABLE erp_customer
    ADD COLUMN IF NOT EXISTS delivery_method_code VARCHAR(50);

COMMENT ON COLUMN erp_customer.delivery_method_code IS '送货方式编码';
