ALTER TABLE app_user_role ADD COLUMN IF NOT EXISTS id BIGSERIAL;
ALTER TABLE app_user_role ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
ALTER TABLE app_user_role ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE app_user_role DROP CONSTRAINT IF EXISTS app_user_role_pkey;
ALTER TABLE app_user_role ADD CONSTRAINT app_user_role_pkey PRIMARY KEY (id);
DROP INDEX IF EXISTS ux_app_user_role_tur;
CREATE UNIQUE INDEX IF NOT EXISTS ux_app_user_role_tur_active
    ON app_user_role (tenant_id, user_id, role_id)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_app_user_role_role_active
    ON app_user_role (tenant_id, role_id, deleted_at);

ALTER TABLE app_role_permission ADD COLUMN IF NOT EXISTS id BIGSERIAL;
ALTER TABLE app_role_permission ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
ALTER TABLE app_role_permission ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE app_role_permission DROP CONSTRAINT IF EXISTS app_role_permission_pkey;
ALTER TABLE app_role_permission ADD CONSTRAINT app_role_permission_pkey PRIMARY KEY (id);
DROP INDEX IF EXISTS ux_app_role_permission_trp;
CREATE UNIQUE INDEX IF NOT EXISTS ux_app_role_permission_trp_active
    ON app_role_permission (tenant_id, role_id, permission_id)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_app_role_permission_role_active
    ON app_role_permission (tenant_id, role_id, deleted_at);

ALTER TABLE app_tenant_menu ADD COLUMN IF NOT EXISTS id BIGSERIAL;
ALTER TABLE app_tenant_menu ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE app_tenant_menu DROP CONSTRAINT IF EXISTS app_tenant_menu_pkey;
ALTER TABLE app_tenant_menu ADD CONSTRAINT app_tenant_menu_pkey PRIMARY KEY (id);
DROP INDEX IF EXISTS ux_app_tenant_menu_tm;
CREATE UNIQUE INDEX IF NOT EXISTS ux_app_tenant_menu_tm_active
    ON app_tenant_menu (tenant_id, menu_id)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_app_tenant_menu_tenant_active
    ON app_tenant_menu (tenant_id, deleted_at);

ALTER TABLE app_idempotency ADD COLUMN IF NOT EXISTS id BIGSERIAL;
ALTER TABLE app_idempotency ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE app_idempotency DROP CONSTRAINT IF EXISTS app_idempotency_pkey;
ALTER TABLE app_idempotency ADD CONSTRAINT app_idempotency_pkey PRIMARY KEY (id);
DROP INDEX IF EXISTS idx_idempotency_expires_at;
CREATE UNIQUE INDEX IF NOT EXISTS uq_app_idempotency_key_active
    ON app_idempotency (idempotency_key)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_idempotency_key_expiry_active
    ON app_idempotency (idempotency_key, expires_at, deleted_at);

ALTER TABLE erp_accounts_receivable ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
DROP INDEX IF EXISTS uk_erp_ar_tenant_order_no;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_ar_tenant_order_no
    ON erp_accounts_receivable (tenant_id, order_no)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_erp_ar_sale_active
    ON erp_accounts_receivable (tenant_id, sale_order_id, deleted_at);

ALTER TABLE erp_receipt ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
DROP INDEX IF EXISTS uk_erp_receipt_tenant_no;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_receipt_tenant_no
    ON erp_receipt (tenant_id, receipt_no)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_erp_receipt_sale_active
    ON erp_receipt (tenant_id, sale_order_id, deleted_at);

ALTER TABLE erp_accounts_payable ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
DROP INDEX IF EXISTS uk_erp_ap_tenant_purchase;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_ap_tenant_purchase
    ON erp_accounts_payable (tenant_id, purchase_order_id)
    WHERE purchase_order_id IS NOT NULL
      AND deleted_at IS NULL;
DROP INDEX IF EXISTS uk_erp_ap_tenant_purchase_return;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_ap_tenant_purchase_return
    ON erp_accounts_payable (tenant_id, purchase_return_id)
    WHERE purchase_return_id IS NOT NULL
      AND deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_erp_ap_purchase_active
    ON erp_accounts_payable (tenant_id, purchase_order_id, deleted_at);

ALTER TABLE erp_payment ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
DROP INDEX IF EXISTS uk_erp_payment_tenant_no;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_payment_tenant_no
    ON erp_payment (tenant_id, payment_no)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_erp_payment_purchase_active
    ON erp_payment (tenant_id, purchase_order_id, deleted_at);

DROP INDEX IF EXISTS uk_erp_payment_payable;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_payment_payable
    ON erp_payment_payable (tenant_id, payment_id, payable_id)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_erp_payment_payable_payment_active
    ON erp_payment_payable (tenant_id, payment_id, deleted_at);

DROP INDEX IF EXISTS uk_erp_receipt_receivable;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_receipt_receivable
    ON erp_receipt_receivable (tenant_id, receipt_id, receivable_id)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_erp_receipt_receivable_receipt_active
    ON erp_receipt_receivable (tenant_id, receipt_id, deleted_at);

ALTER TABLE erp_stock_count ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
DROP INDEX IF EXISTS uk_erp_stock_count_no;
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_stock_count_no
    ON erp_stock_count (tenant_id, count_no)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_erp_stock_count_status_active
    ON erp_stock_count (tenant_id, count_type, status, deleted_at, created_at DESC);
