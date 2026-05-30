CREATE TABLE IF NOT EXISTS erp_product_import_batch (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    batch_no VARCHAR(64) NOT NULL,
    source_name VARCHAR(255),
    import_mode VARCHAR(32) NOT NULL DEFAULT 'EXCEL_UPLOAD',
    total_count INTEGER NOT NULL DEFAULT 0,
    success_count INTEGER NOT NULL DEFAULT 0,
    failed_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    summary TEXT,
    raw_payload JSONB,
    created_by VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_product_import_batch_no
    ON erp_product_import_batch (tenant_id, batch_no)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_product_import_batch_status
    ON erp_product_import_batch (tenant_id, status)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS erp_product_import_item (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    batch_id BIGINT NOT NULL REFERENCES erp_product_import_batch(id),
    row_no INTEGER NOT NULL,
    source_code VARCHAR(128),
    source_name VARCHAR(255),
    matched_product_id BIGINT,
    category_name VARCHAR(128),
    unit_name VARCHAR(64),
    warehouse_name VARCHAR(128),
    supplier_name VARCHAR(255),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    error_field VARCHAR(128),
    error_message TEXT,
    suggestion TEXT,
    warning_message TEXT,
    matched_strategy VARCHAR(64),
    raw_row JSONB NOT NULL,
    normalized_payload JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_product_import_item_batch_row
    ON erp_product_import_item (tenant_id, batch_id, row_no)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_product_import_item_status
    ON erp_product_import_item (tenant_id, status)
    WHERE deleted_at IS NULL;
