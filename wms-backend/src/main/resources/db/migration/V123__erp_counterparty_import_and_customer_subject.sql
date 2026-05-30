ALTER TABLE erp_customer
    ADD COLUMN IF NOT EXISTS counterparty_subject_id BIGINT;

ALTER TABLE erp_customer
    ADD CONSTRAINT fk_erp_customer_counterparty_subject
        FOREIGN KEY (counterparty_subject_id) REFERENCES erp_counterparty_subject(id);

CREATE INDEX IF NOT EXISTS idx_erp_customer_counterparty_subject
    ON erp_customer (tenant_id, counterparty_subject_id)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS erp_supplier_import_batch (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    batch_no VARCHAR(64) NOT NULL,
    source_name VARCHAR(255),
    import_mode VARCHAR(32) NOT NULL DEFAULT 'PASTE_TABLE',
    total_count INTEGER NOT NULL DEFAULT 0,
    success_count INTEGER NOT NULL DEFAULT 0,
    failed_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    summary TEXT,
    raw_payload JSONB,
    created_by VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_supplier_import_batch_no
    ON erp_supplier_import_batch (tenant_id, batch_no)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_supplier_import_batch_status
    ON erp_supplier_import_batch (tenant_id, status)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS erp_supplier_import_item (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    batch_id BIGINT NOT NULL REFERENCES erp_supplier_import_batch(id),
    row_no INTEGER NOT NULL,
    source_code VARCHAR(128),
    source_name VARCHAR(255),
    matched_supplier_id BIGINT,
    supplier_type_name VARCHAR(128),
    settlement_method_name VARCHAR(128),
    enterprise_match VARCHAR(255),
    price_level VARCHAR(128),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    error_field VARCHAR(128),
    error_message TEXT,
    suggestion TEXT,
    raw_row JSONB NOT NULL,
    normalized_payload JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_supplier_import_item_batch_row
    ON erp_supplier_import_item (tenant_id, batch_id, row_no)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_supplier_import_item_status
    ON erp_supplier_import_item (tenant_id, status)
    WHERE deleted_at IS NULL;
