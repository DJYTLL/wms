-- 打印模板与打印日志（ERP进销存）
ALTER TABLE erp_sale_order
    ADD COLUMN IF NOT EXISTS print_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_printed_at TIMESTAMPTZ;

ALTER TABLE erp_purchase_order
    ADD COLUMN IF NOT EXISTS print_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_printed_at TIMESTAMPTZ;

CREATE TABLE IF NOT EXISTS erp_print_template (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    BIGINT       NOT NULL,
    code         VARCHAR(64)  NOT NULL,
    name         VARCHAR(100) NOT NULL,
    doc_type     VARCHAR(30)  NOT NULL,
    header_title VARCHAR(120),
    sub_title    VARCHAR(120),
    footer_note  VARCHAR(500),
    field_config TEXT,
    sort_no      INTEGER      NOT NULL DEFAULT 0,
    is_default   BOOLEAN      NOT NULL DEFAULT FALSE,
    is_enabled   BOOLEAN      NOT NULL DEFAULT TRUE,
    remark       VARCHAR(500),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_print_template_code
    ON erp_print_template (tenant_id, code);

CREATE INDEX IF NOT EXISTS idx_erp_print_template_doc
    ON erp_print_template (tenant_id, doc_type, is_enabled, sort_no);

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_print_template_default
    ON erp_print_template (tenant_id, doc_type)
    WHERE is_default = TRUE;

CREATE TABLE IF NOT EXISTS erp_print_log (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT      NOT NULL,
    doc_type    VARCHAR(30) NOT NULL,
    doc_id      BIGINT      NOT NULL,
    doc_no      VARCHAR(64),
    template_id BIGINT,
    printed_by  VARCHAR(100),
    printed_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    client_ip   VARCHAR(64),
    user_agent  VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_erp_print_log_doc
    ON erp_print_log (tenant_id, doc_type, doc_id);

CREATE INDEX IF NOT EXISTS idx_erp_print_log_time
    ON erp_print_log (tenant_id, printed_at DESC);

COMMENT ON TABLE erp_print_template IS '打印模板配置表（ERP进销存）';
COMMENT ON COLUMN erp_print_template.id IS '主键';
COMMENT ON COLUMN erp_print_template.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_print_template.code IS '模板编码';
COMMENT ON COLUMN erp_print_template.name IS '模板名称';
COMMENT ON COLUMN erp_print_template.doc_type IS '单据类型';
COMMENT ON COLUMN erp_print_template.header_title IS '标题';
COMMENT ON COLUMN erp_print_template.sub_title IS '副标题';
COMMENT ON COLUMN erp_print_template.footer_note IS '页脚备注';
COMMENT ON COLUMN erp_print_template.field_config IS '字段配置(JSON)';
COMMENT ON COLUMN erp_print_template.sort_no IS '排序';
COMMENT ON COLUMN erp_print_template.is_default IS '是否默认';
COMMENT ON COLUMN erp_print_template.is_enabled IS '是否启用';
COMMENT ON COLUMN erp_print_template.remark IS '备注';
COMMENT ON COLUMN erp_print_template.created_at IS '创建时间';
COMMENT ON COLUMN erp_print_template.updated_at IS '更新时间';

COMMENT ON TABLE erp_print_log IS '打印日志表（ERP进销存）';
COMMENT ON COLUMN erp_print_log.id IS '主键';
COMMENT ON COLUMN erp_print_log.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_print_log.doc_type IS '单据类型';
COMMENT ON COLUMN erp_print_log.doc_id IS '单据ID';
COMMENT ON COLUMN erp_print_log.doc_no IS '单据编号';
COMMENT ON COLUMN erp_print_log.template_id IS '模板ID';
COMMENT ON COLUMN erp_print_log.printed_by IS '打印人';
COMMENT ON COLUMN erp_print_log.printed_at IS '打印时间';
COMMENT ON COLUMN erp_print_log.client_ip IS '客户端IP';
COMMENT ON COLUMN erp_print_log.user_agent IS '客户端UA';

COMMENT ON COLUMN erp_sale_order.print_count IS '打印次数';
COMMENT ON COLUMN erp_sale_order.last_printed_at IS '最后打印时间';
COMMENT ON COLUMN erp_purchase_order.print_count IS '打印次数';
COMMENT ON COLUMN erp_purchase_order.last_printed_at IS '最后打印时间';
