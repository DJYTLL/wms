CREATE INDEX IF NOT EXISTS idx_erp_sale_order_source_page
    ON erp_sale_order (tenant_id, status, customer_id, order_at DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_purchase_order_source_page
    ON erp_purchase_order (tenant_id, status, supplier_id, order_at DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_sale_order_item_order_active
    ON erp_sale_order_item (tenant_id, order_id, id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_purchase_order_item_order_active
    ON erp_purchase_order_item (tenant_id, order_id, id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_sale_return_source_order_status
    ON erp_sale_return (tenant_id, sale_order_id, status, updated_at DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_purchase_return_source_order_status
    ON erp_purchase_return (tenant_id, purchase_order_id, status, updated_at DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_accounts_receivable_source_page
    ON erp_accounts_receivable (tenant_id, customer_id, status, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_accounts_payable_source_page
    ON erp_accounts_payable (tenant_id, supplier_id, status, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_stock_txn_biz_lookup
    ON erp_stock_txn (tenant_id, biz_type, biz_id);
