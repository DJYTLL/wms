-- 车型品牌 / 车系 / 车型 / 商品适配关系（ERP进销存）
CREATE TABLE IF NOT EXISTS erp_vehicle_brand (
    id         BIGSERIAL PRIMARY KEY,
    tenant_id  BIGINT       NOT NULL,
    code       VARCHAR(50)  NOT NULL,
    name       VARCHAR(100) NOT NULL,
    is_enabled BOOLEAN      NOT NULL DEFAULT TRUE,
    remark     VARCHAR(500),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE erp_vehicle_brand IS '车型品牌表（ERP进销存）';
COMMENT ON COLUMN erp_vehicle_brand.id IS '主键';
COMMENT ON COLUMN erp_vehicle_brand.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_vehicle_brand.code IS '品牌编码';
COMMENT ON COLUMN erp_vehicle_brand.name IS '品牌名称';
COMMENT ON COLUMN erp_vehicle_brand.is_enabled IS '是否启用';
COMMENT ON COLUMN erp_vehicle_brand.remark IS '备注';
COMMENT ON COLUMN erp_vehicle_brand.created_at IS '创建时间';
COMMENT ON COLUMN erp_vehicle_brand.updated_at IS '更新时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_vehicle_brand_code
    ON erp_vehicle_brand (tenant_id, code);

CREATE INDEX IF NOT EXISTS idx_erp_vehicle_brand_name
    ON erp_vehicle_brand (tenant_id, name);

CREATE TABLE IF NOT EXISTS erp_vehicle_series (
    id         BIGSERIAL PRIMARY KEY,
    tenant_id  BIGINT       NOT NULL,
    brand_id   BIGINT       NOT NULL,
    code       VARCHAR(50)  NOT NULL,
    name       VARCHAR(100) NOT NULL,
    is_enabled BOOLEAN      NOT NULL DEFAULT TRUE,
    remark     VARCHAR(500),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE erp_vehicle_series IS '车型车系表（ERP进销存）';
COMMENT ON COLUMN erp_vehicle_series.id IS '主键';
COMMENT ON COLUMN erp_vehicle_series.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_vehicle_series.brand_id IS '品牌ID';
COMMENT ON COLUMN erp_vehicle_series.code IS '车系编码';
COMMENT ON COLUMN erp_vehicle_series.name IS '车系名称';
COMMENT ON COLUMN erp_vehicle_series.is_enabled IS '是否启用';
COMMENT ON COLUMN erp_vehicle_series.remark IS '备注';
COMMENT ON COLUMN erp_vehicle_series.created_at IS '创建时间';
COMMENT ON COLUMN erp_vehicle_series.updated_at IS '更新时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_vehicle_series_code
    ON erp_vehicle_series (tenant_id, brand_id, code);

CREATE INDEX IF NOT EXISTS idx_erp_vehicle_series_brand
    ON erp_vehicle_series (tenant_id, brand_id);

CREATE TABLE IF NOT EXISTS erp_vehicle_model (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    BIGINT       NOT NULL,
    series_id    BIGINT       NOT NULL,
    code         VARCHAR(50)  NOT NULL,
    name         VARCHAR(120) NOT NULL,
    year_from    INT,
    year_to      INT,
    displacement VARCHAR(50),
    engine       VARCHAR(100),
    is_enabled   BOOLEAN      NOT NULL DEFAULT TRUE,
    remark       VARCHAR(500),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE erp_vehicle_model IS '车型表（ERP进销存）';
COMMENT ON COLUMN erp_vehicle_model.id IS '主键';
COMMENT ON COLUMN erp_vehicle_model.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_vehicle_model.series_id IS '车系ID';
COMMENT ON COLUMN erp_vehicle_model.code IS '车型编码';
COMMENT ON COLUMN erp_vehicle_model.name IS '车型名称';
COMMENT ON COLUMN erp_vehicle_model.year_from IS '起始年款';
COMMENT ON COLUMN erp_vehicle_model.year_to IS '结束年款';
COMMENT ON COLUMN erp_vehicle_model.displacement IS '排量/排气量';
COMMENT ON COLUMN erp_vehicle_model.engine IS '发动机型号';
COMMENT ON COLUMN erp_vehicle_model.is_enabled IS '是否启用';
COMMENT ON COLUMN erp_vehicle_model.remark IS '备注';
COMMENT ON COLUMN erp_vehicle_model.created_at IS '创建时间';
COMMENT ON COLUMN erp_vehicle_model.updated_at IS '更新时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_vehicle_model_code
    ON erp_vehicle_model (tenant_id, series_id, code);

CREATE INDEX IF NOT EXISTS idx_erp_vehicle_model_series
    ON erp_vehicle_model (tenant_id, series_id);

CREATE TABLE IF NOT EXISTS erp_product_fitment (
    id         BIGSERIAL PRIMARY KEY,
    tenant_id  BIGINT      NOT NULL,
    product_id BIGINT      NOT NULL,
    model_id   BIGINT      NOT NULL,
    remark     VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE erp_product_fitment IS '商品适配车型关系表（ERP进销存）';
COMMENT ON COLUMN erp_product_fitment.id IS '主键';
COMMENT ON COLUMN erp_product_fitment.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_product_fitment.product_id IS '商品ID';
COMMENT ON COLUMN erp_product_fitment.model_id IS '车型ID';
COMMENT ON COLUMN erp_product_fitment.remark IS '备注';
COMMENT ON COLUMN erp_product_fitment.created_at IS '创建时间';
COMMENT ON COLUMN erp_product_fitment.updated_at IS '更新时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_product_fitment_key
    ON erp_product_fitment (tenant_id, product_id, model_id);

CREATE INDEX IF NOT EXISTS idx_erp_product_fitment_product
    ON erp_product_fitment (tenant_id, product_id);

CREATE INDEX IF NOT EXISTS idx_erp_product_fitment_model
    ON erp_product_fitment (tenant_id, model_id);
