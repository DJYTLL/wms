-- 客户类别与按客户类别售价

-- 客户类别表
CREATE TABLE IF NOT EXISTS erp_customer_category (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT        NOT NULL,
    code        VARCHAR(100)  NOT NULL,
    name        VARCHAR(200)  NOT NULL,
    description VARCHAR(500),
    sort_no     INT           NOT NULL DEFAULT 0,
    is_enabled  BOOLEAN       NOT NULL DEFAULT TRUE,
    remark      VARCHAR(500),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE erp_customer_category IS '客户类别表（ERP进销存）';
COMMENT ON COLUMN erp_customer_category.id IS '主键';
COMMENT ON COLUMN erp_customer_category.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_customer_category.code IS '类别编码';
COMMENT ON COLUMN erp_customer_category.name IS '类别名称';
COMMENT ON COLUMN erp_customer_category.description IS '描述';
COMMENT ON COLUMN erp_customer_category.sort_no IS '排序';
COMMENT ON COLUMN erp_customer_category.is_enabled IS '是否启用';
COMMENT ON COLUMN erp_customer_category.remark IS '备注';
COMMENT ON COLUMN erp_customer_category.created_at IS '创建时间';
COMMENT ON COLUMN erp_customer_category.updated_at IS '更新时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_customer_category_code
    ON erp_customer_category (tenant_id, code);

CREATE INDEX IF NOT EXISTS idx_erp_customer_category_name
    ON erp_customer_category (tenant_id, name);

-- 客户表新增类别字段
ALTER TABLE erp_customer
    ADD COLUMN IF NOT EXISTS category_id BIGINT;

COMMENT ON COLUMN erp_customer.category_id IS '客户类别ID';

CREATE INDEX IF NOT EXISTS idx_erp_customer_category
    ON erp_customer (tenant_id, category_id);

-- 按客户类别商品售价
CREATE TABLE IF NOT EXISTS erp_product_price (
    id                    BIGSERIAL PRIMARY KEY,
    tenant_id             BIGINT        NOT NULL,
    product_id            BIGINT        NOT NULL,
    customer_category_id  BIGINT        NOT NULL,
    sale_price            NUMERIC(18,4) NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE erp_product_price IS '商品类别售价表（ERP进销存）';
COMMENT ON COLUMN erp_product_price.id IS '主键';
COMMENT ON COLUMN erp_product_price.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_product_price.product_id IS '商品ID';
COMMENT ON COLUMN erp_product_price.customer_category_id IS '客户类别ID';
COMMENT ON COLUMN erp_product_price.sale_price IS '售价';
COMMENT ON COLUMN erp_product_price.created_at IS '创建时间';
COMMENT ON COLUMN erp_product_price.updated_at IS '更新时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_product_price_key
    ON erp_product_price (tenant_id, product_id, customer_category_id);

CREATE INDEX IF NOT EXISTS idx_erp_product_price_product
    ON erp_product_price (tenant_id, product_id);
