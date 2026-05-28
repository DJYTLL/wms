-- 应付单支持采购退货关联
ALTER TABLE erp_accounts_payable
    ALTER COLUMN purchase_order_id DROP NOT NULL;

ALTER TABLE erp_accounts_payable
    ADD COLUMN IF NOT EXISTS purchase_return_id BIGINT;

DROP INDEX IF EXISTS uk_erp_ap_tenant_purchase;

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_ap_tenant_purchase
    ON erp_accounts_payable (tenant_id, purchase_order_id)
    WHERE purchase_order_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_ap_tenant_purchase_return
    ON erp_accounts_payable (tenant_id, purchase_return_id)
    WHERE purchase_return_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_erp_ap_purchase_return
    ON erp_accounts_payable (tenant_id, purchase_return_id);

COMMENT ON COLUMN erp_accounts_payable.purchase_return_id IS '采购退货单ID';
