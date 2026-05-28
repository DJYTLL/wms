CREATE INDEX IF NOT EXISTS idx_erp_sale_order_customer_recent_lookup
    ON erp_sale_order (tenant_id, customer_id, order_at DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_purchase_order_supplier_recent_lookup
    ON erp_purchase_order (tenant_id, supplier_id, order_at DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_sale_return_customer_recent_lookup
    ON erp_sale_return (tenant_id, customer_id, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_purchase_return_supplier_recent_lookup
    ON erp_purchase_return (tenant_id, supplier_id, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_receipt_customer_recent_lookup
    ON erp_receipt (tenant_id, customer_id, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_payment_supplier_recent_lookup
    ON erp_payment (tenant_id, supplier_id, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_accounts_receivable_customer_recent_lookup
    ON erp_accounts_receivable (tenant_id, customer_id, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_accounts_payable_supplier_recent_lookup
    ON erp_accounts_payable (tenant_id, supplier_id, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;
