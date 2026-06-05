CREATE TABLE IF NOT EXISTS erp_stock_init_import_batch (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    batch_no VARCHAR(64) NOT NULL,
    source_name VARCHAR(255),
    import_mode VARCHAR(32) NOT NULL DEFAULT 'EXCEL_UPLOAD',
    strategy_mode VARCHAR(32) NOT NULL DEFAULT 'NONE',
    total_count INTEGER NOT NULL DEFAULT 0,
    success_count INTEGER NOT NULL DEFAULT 0,
    failed_count INTEGER NOT NULL DEFAULT 0,
    warning_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    summary TEXT,
    count_id BIGINT,
    count_no VARCHAR(64),
    raw_payload JSONB,
    created_by VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_stock_init_import_batch_no
    ON erp_stock_init_import_batch (tenant_id, batch_no)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_stock_init_import_batch_status
    ON erp_stock_init_import_batch (tenant_id, status)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS erp_stock_init_import_item (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    batch_id BIGINT NOT NULL REFERENCES erp_stock_init_import_batch(id),
    row_no INTEGER NOT NULL,
    source_code VARCHAR(128),
    source_name VARCHAR(255),
    matched_product_id BIGINT,
    warehouse_name VARCHAR(128),
    location_name VARCHAR(128),
    counted_qty NUMERIC(18, 4),
    init_unit_cost NUMERIC(18, 4),
    init_total_amount NUMERIC(18, 4),
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

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_stock_init_import_item_batch_row
    ON erp_stock_init_import_item (tenant_id, batch_id, row_no)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_stock_init_import_item_status
    ON erp_stock_init_import_item (tenant_id, status)
    WHERE deleted_at IS NULL;
