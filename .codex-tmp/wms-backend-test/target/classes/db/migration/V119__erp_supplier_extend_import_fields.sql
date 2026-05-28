ALTER TABLE erp_supplier
    ADD COLUMN IF NOT EXISTS supplier_type_id BIGINT,
    ADD COLUMN IF NOT EXISTS region VARCHAR(128),
    ADD COLUMN IF NOT EXISTS wechat VARCHAR(128),
    ADD COLUMN IF NOT EXISTS purchaser VARCHAR(128),
    ADD COLUMN IF NOT EXISTS contact_info VARCHAR(255),
    ADD COLUMN IF NOT EXISTS source_created_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS source_created_by VARCHAR(64),
    ADD COLUMN IF NOT EXISTS business_scope VARCHAR(32) NOT NULL DEFAULT 'SUPPLIER',
    ADD COLUMN IF NOT EXISTS counterparty_subject_id BIGINT;

COMMENT ON COLUMN erp_supplier.supplier_type_id IS '供应商类型ID';
COMMENT ON COLUMN erp_supplier.region IS '区域';
COMMENT ON COLUMN erp_supplier.wechat IS '微信客服';
COMMENT ON COLUMN erp_supplier.purchaser IS '采购员';
COMMENT ON COLUMN erp_supplier.contact_info IS '原始联系方式';
COMMENT ON COLUMN erp_supplier.source_created_at IS '来源创建时间';
COMMENT ON COLUMN erp_supplier.source_created_by IS '来源创建人';
COMMENT ON COLUMN erp_supplier.business_scope IS '往来类别';
COMMENT ON COLUMN erp_supplier.counterparty_subject_id IS '往来主体ID';

CREATE INDEX IF NOT EXISTS idx_erp_supplier_tenant_supplier_type
    ON erp_supplier (tenant_id, supplier_type_id);

CREATE INDEX IF NOT EXISTS idx_erp_supplier_tenant_counterparty_subject
    ON erp_supplier (tenant_id, counterparty_subject_id);

CREATE INDEX IF NOT EXISTS idx_erp_supplier_tenant_business_scope
    ON erp_supplier (tenant_id, business_scope);
