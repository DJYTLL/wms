ALTER TABLE app_tenant ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE app_tenant ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE app_role ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE app_role ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE app_permission ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE app_permission ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE app_menu ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE app_menu ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE app_tenant_menu ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE app_tenant_menu ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE app_user_role ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE app_user_role ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE app_role_permission ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE app_role_permission ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE app_idempotency ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE app_idempotency ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);

ALTER TABLE erp_category ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE erp_category ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE erp_customer ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE erp_customer ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE erp_customer_category ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE erp_customer_category ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE erp_delivery_method ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE erp_delivery_method ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE erp_location ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE erp_location ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE erp_payment_method ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE erp_payment_method ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE erp_print_template ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE erp_print_template ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE erp_product ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE erp_product ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE erp_product_price ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE erp_product_price ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE erp_product_fitment ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE erp_product_fitment ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE erp_purchase_order_item ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE erp_purchase_order_item ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE erp_sale_return ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE erp_sale_return ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE erp_sale_return_item ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE erp_sale_return_item ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE erp_assembly_order ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE erp_assembly_order ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE erp_assembly_order_item ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE erp_assembly_order_item ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE erp_settlement_method ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE erp_settlement_method ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE erp_supplier ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE erp_supplier ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE erp_unit ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE erp_unit ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE erp_vehicle_brand ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE erp_vehicle_brand ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE erp_vehicle_model ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE erp_vehicle_model ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE erp_vehicle_series ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE erp_vehicle_series ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE erp_warehouse ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE erp_warehouse ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE erp_accounts_receivable ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE erp_accounts_receivable ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE erp_accounts_payable ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE erp_accounts_payable ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE erp_receipt ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE erp_receipt ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE erp_payment ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE erp_payment ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE erp_payment_payable ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE erp_payment_payable ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE erp_receipt_receivable ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE erp_receipt_receivable ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE erp_stock_count ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE erp_stock_count ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);
ALTER TABLE erp_stock_count_item ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE erp_stock_count_item ADD COLUMN IF NOT EXISTS delete_reason VARCHAR(500);

CREATE INDEX IF NOT EXISTS idx_app_user_tenant_deleted_at
    ON app_user (tenant_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_app_role_tenant_deleted_at
    ON app_role (tenant_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_tenant_menu_tenant_deleted_at
    ON app_tenant_menu (tenant_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_user_role_user_deleted_at
    ON app_user_role (tenant_id, user_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_role_permission_permission_deleted_at
    ON app_role_permission (tenant_id, permission_id, deleted_at);

CREATE INDEX IF NOT EXISTS idx_erp_category_tenant_deleted_at
    ON erp_category (tenant_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_erp_customer_tenant_deleted_at
    ON erp_customer (tenant_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_erp_customer_category_tenant_deleted_at
    ON erp_customer_category (tenant_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_erp_delivery_method_tenant_deleted_at
    ON erp_delivery_method (tenant_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_erp_location_tenant_deleted_at
    ON erp_location (tenant_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_erp_payment_method_tenant_deleted_at
    ON erp_payment_method (tenant_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_erp_print_template_tenant_deleted_at
    ON erp_print_template (tenant_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_erp_product_tenant_deleted_at
    ON erp_product (tenant_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_erp_product_price_tenant_deleted_at
    ON erp_product_price (tenant_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_erp_product_fitment_tenant_deleted_at
    ON erp_product_fitment (tenant_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_erp_purchase_order_tenant_deleted_at
    ON erp_purchase_order (tenant_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_erp_purchase_order_item_tenant_deleted_at
    ON erp_purchase_order_item (tenant_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_erp_purchase_return_tenant_deleted_at
    ON erp_purchase_return (tenant_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_erp_purchase_return_item_tenant_deleted_at
    ON erp_purchase_return_item (tenant_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_erp_sale_order_tenant_deleted_at
    ON erp_sale_order (tenant_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_erp_sale_order_item_tenant_deleted_at
    ON erp_sale_order_item (tenant_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_erp_sale_return_tenant_deleted_at
    ON erp_sale_return (tenant_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_erp_sale_return_item_tenant_deleted_at
    ON erp_sale_return_item (tenant_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_erp_assembly_order_tenant_deleted_at
    ON erp_assembly_order (tenant_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_erp_assembly_order_item_tenant_deleted_at
    ON erp_assembly_order_item (tenant_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_erp_settlement_method_tenant_deleted_at
    ON erp_settlement_method (tenant_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_erp_supplier_tenant_deleted_at
    ON erp_supplier (tenant_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_erp_unit_tenant_deleted_at
    ON erp_unit (tenant_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_erp_vehicle_brand_tenant_deleted_at
    ON erp_vehicle_brand (tenant_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_erp_vehicle_model_tenant_deleted_at
    ON erp_vehicle_model (tenant_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_erp_vehicle_series_tenant_deleted_at
    ON erp_vehicle_series (tenant_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_erp_warehouse_tenant_deleted_at
    ON erp_warehouse (tenant_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_erp_receipt_receivable_tenant_deleted_at
    ON erp_receipt_receivable (tenant_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_erp_payment_payable_tenant_deleted_at
    ON erp_payment_payable (tenant_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_erp_stock_count_item_tenant_deleted_at
    ON erp_stock_count_item (tenant_id, deleted_at);

DROP INDEX IF EXISTS uk_erp_settlement_method_default;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_settlement_method_default
    ON erp_settlement_method (tenant_id)
    WHERE is_default = TRUE AND deleted_at IS NULL;

DROP INDEX IF EXISTS uk_erp_delivery_method_default;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_delivery_method_default
    ON erp_delivery_method (tenant_id)
    WHERE is_default = TRUE AND deleted_at IS NULL;

DROP INDEX IF EXISTS uk_erp_customer_category_default;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_customer_category_default
    ON erp_customer_category (tenant_id)
    WHERE is_default = TRUE AND deleted_at IS NULL;

DROP INDEX IF EXISTS uk_erp_payment_method_default;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_payment_method_default
    ON erp_payment_method (tenant_id)
    WHERE is_default = TRUE AND deleted_at IS NULL;

DROP INDEX IF EXISTS uk_erp_print_template_default;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_print_template_default
    ON erp_print_template (tenant_id, doc_type)
    WHERE is_default = TRUE AND deleted_at IS NULL;
