CREATE TABLE IF NOT EXISTS app_sql_request_trace (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    request_id VARCHAR(64) NOT NULL,
    request_path VARCHAR(500) NOT NULL,
    request_method VARCHAR(16) NOT NULL,
    response_status INT,
    request_cost_ms BIGINT NOT NULL DEFAULT 0,
    sql_total_cost_ms BIGINT NOT NULL DEFAULT 0,
    sql_count INT NOT NULL DEFAULT 0,
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ NOT NULL,
    username VARCHAR(100),
    user_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_app_sql_request_trace_tenant_request UNIQUE (tenant_id, request_id),
    CONSTRAINT chk_app_sql_request_trace_request_cost_ms_non_negative CHECK (request_cost_ms >= 0),
    CONSTRAINT chk_app_sql_request_trace_sql_total_cost_ms_non_negative CHECK (sql_total_cost_ms >= 0),
    CONSTRAINT chk_app_sql_request_trace_sql_count_non_negative CHECK (sql_count >= 0),
    CONSTRAINT chk_app_sql_request_trace_finished_after_started CHECK (finished_at >= started_at)
);

CREATE TABLE IF NOT EXISTS app_sql_trace_entry (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    request_trace_id BIGINT NOT NULL,
    request_id VARCHAR(64) NOT NULL,
    sequence_no INT NOT NULL,
    mapper_id VARCHAR(300) NOT NULL,
    sql_type VARCHAR(20) NOT NULL,
    cost_ms BIGINT NOT NULL DEFAULT 0,
    sql_text TEXT NOT NULL,
    params_summary TEXT NOT NULL DEFAULT '[disabled]',
    executed_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_app_sql_trace_entry_request_trace
        FOREIGN KEY (request_trace_id) REFERENCES app_sql_request_trace(id),
    CONSTRAINT uq_app_sql_trace_entry_trace_sequence UNIQUE (request_trace_id, sequence_no),
    CONSTRAINT chk_app_sql_trace_entry_sequence_no_positive CHECK (sequence_no > 0),
    CONSTRAINT chk_app_sql_trace_entry_cost_ms_non_negative CHECK (cost_ms >= 0)
);

CREATE INDEX IF NOT EXISTS idx_app_sql_request_trace_tenant_started_id
    ON app_sql_request_trace (tenant_id, started_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_app_sql_request_trace_tenant_request_id
    ON app_sql_request_trace (tenant_id, request_id);

CREATE INDEX IF NOT EXISTS idx_app_sql_request_trace_tenant_path_started
    ON app_sql_request_trace (tenant_id, request_path, started_at DESC);

CREATE INDEX IF NOT EXISTS idx_app_sql_request_trace_tenant_method_started
    ON app_sql_request_trace (tenant_id, request_method, started_at DESC);

CREATE INDEX IF NOT EXISTS idx_app_sql_trace_entry_tenant_trace_sequence
    ON app_sql_trace_entry (tenant_id, request_trace_id, sequence_no);

CREATE INDEX IF NOT EXISTS idx_app_sql_trace_entry_tenant_request_sequence
    ON app_sql_trace_entry (tenant_id, request_id, sequence_no);

CREATE INDEX IF NOT EXISTS idx_app_sql_trace_entry_tenant_mapper_executed
    ON app_sql_trace_entry (tenant_id, mapper_id, executed_at DESC);

CREATE INDEX IF NOT EXISTS idx_app_sql_trace_entry_tenant_cost_executed
    ON app_sql_trace_entry (tenant_id, cost_ms DESC, executed_at DESC);
