-- ERP 销售单补充字段与基础档案（结算方式/送货方式）

-- 结算方式表
CREATE TABLE IF NOT EXISTS erp_settlement_method (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT        NOT NULL,
    code        VARCHAR(100)  NOT NULL,
    name        VARCHAR(200)  NOT NULL,
    sort_no     INT           NOT NULL DEFAULT 0,
    is_enabled  BOOLEAN       NOT NULL DEFAULT TRUE,
    remark      VARCHAR(500),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE erp_settlement_method IS '结算方式表（ERP进销存）';
COMMENT ON COLUMN erp_settlement_method.id IS '主键';
COMMENT ON COLUMN erp_settlement_method.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_settlement_method.code IS '结算方式编码';
COMMENT ON COLUMN erp_settlement_method.name IS '结算方式名称';
COMMENT ON COLUMN erp_settlement_method.sort_no IS '排序';
COMMENT ON COLUMN erp_settlement_method.is_enabled IS '是否启用';
COMMENT ON COLUMN erp_settlement_method.remark IS '备注';
COMMENT ON COLUMN erp_settlement_method.created_at IS '创建时间';
COMMENT ON COLUMN erp_settlement_method.updated_at IS '更新时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_settlement_method_code
    ON erp_settlement_method (tenant_id, code);

CREATE INDEX IF NOT EXISTS idx_erp_settlement_method_name
    ON erp_settlement_method (tenant_id, name);

-- 送货方式表
CREATE TABLE IF NOT EXISTS erp_delivery_method (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT        NOT NULL,
    code        VARCHAR(100)  NOT NULL,
    name        VARCHAR(200)  NOT NULL,
    sort_no     INT           NOT NULL DEFAULT 0,
    is_enabled  BOOLEAN       NOT NULL DEFAULT TRUE,
    remark      VARCHAR(500),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE erp_delivery_method IS '送货方式表（ERP进销存）';
COMMENT ON COLUMN erp_delivery_method.id IS '主键';
COMMENT ON COLUMN erp_delivery_method.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_delivery_method.code IS '送货方式编码';
COMMENT ON COLUMN erp_delivery_method.name IS '送货方式名称';
COMMENT ON COLUMN erp_delivery_method.sort_no IS '排序';
COMMENT ON COLUMN erp_delivery_method.is_enabled IS '是否启用';
COMMENT ON COLUMN erp_delivery_method.remark IS '备注';
COMMENT ON COLUMN erp_delivery_method.created_at IS '创建时间';
COMMENT ON COLUMN erp_delivery_method.updated_at IS '更新时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_delivery_method_code
    ON erp_delivery_method (tenant_id, code);

CREATE INDEX IF NOT EXISTS idx_erp_delivery_method_name
    ON erp_delivery_method (tenant_id, name);

-- 销售单补充字段
ALTER TABLE erp_sale_order
    ADD COLUMN IF NOT EXISTS delivery_method_code VARCHAR(100),
    ADD COLUMN IF NOT EXISTS paid_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(18,2) NOT NULL DEFAULT 0;

COMMENT ON COLUMN erp_sale_order.settlement_method IS '结算方式编码';
COMMENT ON COLUMN erp_sale_order.delivery_method_code IS '送货方式编码';
COMMENT ON COLUMN erp_sale_order.paid_amount IS '付款金额';
COMMENT ON COLUMN erp_sale_order.discount_amount IS '优惠金额';

-- 默认结算方式
INSERT INTO erp_settlement_method (tenant_id, code, name, sort_no, is_enabled, created_at, updated_at)
SELECT t.id, 'CASH', '现金', 10, TRUE, NOW(), NOW()
FROM app_tenant t
WHERE t.deleted_at IS NULL
ON CONFLICT (tenant_id, code) DO NOTHING;

INSERT INTO erp_settlement_method (tenant_id, code, name, sort_no, is_enabled, created_at, updated_at)
SELECT t.id, 'TRANSFER', '银行转账', 20, TRUE, NOW(), NOW()
FROM app_tenant t
WHERE t.deleted_at IS NULL
ON CONFLICT (tenant_id, code) DO NOTHING;

INSERT INTO erp_settlement_method (tenant_id, code, name, sort_no, is_enabled, created_at, updated_at)
SELECT t.id, 'CREDIT', '赊账', 30, TRUE, NOW(), NOW()
FROM app_tenant t
WHERE t.deleted_at IS NULL
ON CONFLICT (tenant_id, code) DO NOTHING;

-- 默认送货方式
INSERT INTO erp_delivery_method (tenant_id, code, name, sort_no, is_enabled, created_at, updated_at)
SELECT t.id, 'SELF', '自提', 10, TRUE, NOW(), NOW()
FROM app_tenant t
WHERE t.deleted_at IS NULL
ON CONFLICT (tenant_id, code) DO NOTHING;

INSERT INTO erp_delivery_method (tenant_id, code, name, sort_no, is_enabled, created_at, updated_at)
SELECT t.id, 'EXPRESS', '快递', 20, TRUE, NOW(), NOW()
FROM app_tenant t
WHERE t.deleted_at IS NULL
ON CONFLICT (tenant_id, code) DO NOTHING;

INSERT INTO erp_delivery_method (tenant_id, code, name, sort_no, is_enabled, created_at, updated_at)
SELECT t.id, 'LOGISTICS', '物流', 30, TRUE, NOW(), NOW()
FROM app_tenant t
WHERE t.deleted_at IS NULL
ON CONFLICT (tenant_id, code) DO NOTHING;
