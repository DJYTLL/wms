ALTER TABLE app_tenant DROP CONSTRAINT IF EXISTS app_tenant_code_key;
DROP INDEX IF EXISTS uq_app_user_username;
DROP INDEX IF EXISTS uq_app_role_tenant_code;
DROP INDEX IF EXISTS uq_app_permission_code;
ALTER TABLE app_menu DROP CONSTRAINT IF EXISTS app_menu_code_key;

ALTER TABLE app_role ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE app_permission ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE app_menu ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

ALTER TABLE erp_category ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE erp_customer ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE erp_customer_category ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE erp_delivery_method ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE erp_location ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE erp_payment_method ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE erp_print_template ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE erp_product ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE erp_product_price ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE erp_product_fitment ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE erp_purchase_order_item ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE erp_sale_return ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE erp_sale_return_item ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE erp_assembly_order ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE erp_assembly_order_item ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE erp_settlement_method ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE erp_supplier ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE erp_unit ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE erp_vehicle_brand ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE erp_vehicle_model ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE erp_vehicle_series ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE erp_warehouse ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE erp_payment_payable ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE erp_receipt_receivable ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE erp_stock_count_item ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

CREATE UNIQUE INDEX IF NOT EXISTS uq_app_tenant_code_active
    ON app_tenant (code)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_app_user_username_active
    ON app_user (username)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_app_role_tenant_code_active
    ON app_role (tenant_id, code)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_app_permission_code_active
    ON app_permission (code)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_app_menu_code_active
    ON app_menu (code)
    WHERE deleted_at IS NULL;

DROP INDEX IF EXISTS uk_erp_category_code;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_category_code
    ON erp_category (tenant_id, code)
    WHERE deleted_at IS NULL;

DROP INDEX IF EXISTS uk_erp_customer_code;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_customer_code
    ON erp_customer (tenant_id, code)
    WHERE deleted_at IS NULL;

DROP INDEX IF EXISTS uk_erp_customer_category_code;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_customer_category_code
    ON erp_customer_category (tenant_id, code)
    WHERE deleted_at IS NULL;

DROP INDEX IF EXISTS uk_erp_delivery_method_code;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_delivery_method_code
    ON erp_delivery_method (tenant_id, code)
    WHERE deleted_at IS NULL;

DROP INDEX IF EXISTS uk_erp_location_code;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_location_code
    ON erp_location (tenant_id, warehouse_id, code)
    WHERE deleted_at IS NULL;

DROP INDEX IF EXISTS uk_erp_payment_method_code;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_payment_method_code
    ON erp_payment_method (tenant_id, code)
    WHERE deleted_at IS NULL;

DROP INDEX IF EXISTS uk_erp_print_template_code;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_print_template_code
    ON erp_print_template (tenant_id, code)
    WHERE deleted_at IS NULL;

DROP INDEX IF EXISTS uk_erp_product_code;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_product_code
    ON erp_product (tenant_id, code)
    WHERE deleted_at IS NULL;

DROP INDEX IF EXISTS uk_erp_product_price_key;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_product_price_key
    ON erp_product_price (tenant_id, product_id, customer_category_id)
    WHERE deleted_at IS NULL;

DROP INDEX IF EXISTS uk_erp_product_fitment_key;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_product_fitment_key
    ON erp_product_fitment (tenant_id, product_id, model_id)
    WHERE deleted_at IS NULL;

DROP INDEX IF EXISTS uk_erp_purchase_order_no;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_purchase_order_no
    ON erp_purchase_order (tenant_id, order_no)
    WHERE deleted_at IS NULL;

DROP INDEX IF EXISTS uk_erp_sale_order_no;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_sale_order_no
    ON erp_sale_order (tenant_id, order_no)
    WHERE deleted_at IS NULL;

DROP INDEX IF EXISTS uk_erp_purchase_return_no;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_purchase_return_no
    ON erp_purchase_return (tenant_id, order_no)
    WHERE deleted_at IS NULL;

DROP INDEX IF EXISTS uk_erp_sale_return_no;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_sale_return_no
    ON erp_sale_return (tenant_id, order_no)
    WHERE deleted_at IS NULL;

DROP INDEX IF EXISTS uk_erp_assembly_order_no;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_assembly_order_no
    ON erp_assembly_order (tenant_id, order_no)
    WHERE deleted_at IS NULL;

DROP INDEX IF EXISTS uk_erp_settlement_method_code;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_settlement_method_code
    ON erp_settlement_method (tenant_id, code)
    WHERE deleted_at IS NULL;

DROP INDEX IF EXISTS uk_erp_supplier_code;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_supplier_code
    ON erp_supplier (tenant_id, code)
    WHERE deleted_at IS NULL;

DROP INDEX IF EXISTS uk_erp_unit_code;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_unit_code
    ON erp_unit (tenant_id, code)
    WHERE deleted_at IS NULL;

DROP INDEX IF EXISTS uk_erp_vehicle_brand_code;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_vehicle_brand_code
    ON erp_vehicle_brand (tenant_id, code)
    WHERE deleted_at IS NULL;

DROP INDEX IF EXISTS uk_erp_vehicle_series_code;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_vehicle_series_code
    ON erp_vehicle_series (tenant_id, brand_id, code)
    WHERE deleted_at IS NULL;

DROP INDEX IF EXISTS uk_erp_vehicle_model_code;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_vehicle_model_code
    ON erp_vehicle_model (tenant_id, series_id, code)
    WHERE deleted_at IS NULL;

DROP INDEX IF EXISTS uk_erp_warehouse_code;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_warehouse_code
    ON erp_warehouse (tenant_id, code)
    WHERE deleted_at IS NULL;
