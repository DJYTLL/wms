-- Inventory core tables for draft -> approve direct stock changes.

-- Purchase order header.
CREATE TABLE IF NOT EXISTS erp_purchase_order (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT        NOT NULL,
    order_no        VARCHAR(64)   NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    supplier_id     BIGINT,
    total_amount    NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_amount_excl_tax NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_tax_amount      NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_amount_incl_tax NUMERIC(18,2) NOT NULL DEFAULT 0,
    version         BIGINT        NOT NULL DEFAULT 0,
    approved_by     VARCHAR(100),
    approved_at     TIMESTAMPTZ,
    unapproved_by   VARCHAR(100),
    unapproved_at   TIMESTAMPTZ,
    cancelled_by    VARCHAR(100),
    cancelled_at    TIMESTAMPTZ,
    remark          VARCHAR(500),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE erp_purchase_order IS '采购单头表（ERP进销存）';
COMMENT ON COLUMN erp_purchase_order.id IS '主键';
COMMENT ON COLUMN erp_purchase_order.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_purchase_order.order_no IS '单号';
COMMENT ON COLUMN erp_purchase_order.status IS '状态';
COMMENT ON COLUMN erp_purchase_order.supplier_id IS '供应商ID';
COMMENT ON COLUMN erp_purchase_order.total_amount IS '总金额';
COMMENT ON COLUMN erp_purchase_order.total_amount_excl_tax IS '未税总金额';
COMMENT ON COLUMN erp_purchase_order.total_tax_amount IS '税额合计';
COMMENT ON COLUMN erp_purchase_order.total_amount_incl_tax IS '含税总金额';
COMMENT ON COLUMN erp_purchase_order.version IS '乐观锁版本';
COMMENT ON COLUMN erp_purchase_order.approved_by IS '审核人';
COMMENT ON COLUMN erp_purchase_order.approved_at IS '审核时间';
COMMENT ON COLUMN erp_purchase_order.unapproved_by IS '反审核人';
COMMENT ON COLUMN erp_purchase_order.unapproved_at IS '反审核时间';
COMMENT ON COLUMN erp_purchase_order.cancelled_by IS '作废人';
COMMENT ON COLUMN erp_purchase_order.cancelled_at IS '作废时间';
COMMENT ON COLUMN erp_purchase_order.remark IS '备注';
COMMENT ON COLUMN erp_purchase_order.created_at IS '创建时间';
COMMENT ON COLUMN erp_purchase_order.updated_at IS '更新时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_purchase_order_no
    ON erp_purchase_order (tenant_id, order_no);

CREATE INDEX IF NOT EXISTS idx_erp_purchase_order_status
    ON erp_purchase_order (tenant_id, status, updated_at DESC);

-- Purchase order items.
CREATE TABLE IF NOT EXISTS erp_purchase_order_item (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT        NOT NULL,
    order_id        BIGINT        NOT NULL,
    product_id      BIGINT        NOT NULL,
    product_code    VARCHAR(100),
    product_name    VARCHAR(200),
    warehouse_id    BIGINT,
    location_id     BIGINT,
    qty             NUMERIC(18,4) NOT NULL,
    price           NUMERIC(18,4) NOT NULL DEFAULT 0,
    price_incl_tax  NUMERIC(18,4) NOT NULL DEFAULT 0,
    amount          NUMERIC(18,2) NOT NULL DEFAULT 0,
    amount_incl_tax NUMERIC(18,2) NOT NULL DEFAULT 0,
    tax_rate        NUMERIC(6,4)  NOT NULL DEFAULT 0,
    tax_amount      NUMERIC(18,2) NOT NULL DEFAULT 0,
    sort_no         INT           NOT NULL DEFAULT 0,
    remark          VARCHAR(500),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE erp_purchase_order_item IS '采购单明细表（ERP进销存）';
COMMENT ON COLUMN erp_purchase_order_item.id IS '主键';
COMMENT ON COLUMN erp_purchase_order_item.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_purchase_order_item.order_id IS '采购单ID';
COMMENT ON COLUMN erp_purchase_order_item.product_id IS '商品ID';
COMMENT ON COLUMN erp_purchase_order_item.product_code IS '商品编码快照';
COMMENT ON COLUMN erp_purchase_order_item.product_name IS '商品名称快照';
COMMENT ON COLUMN erp_purchase_order_item.warehouse_id IS '仓库ID';
COMMENT ON COLUMN erp_purchase_order_item.location_id IS '库位ID';
COMMENT ON COLUMN erp_purchase_order_item.qty IS '数量';
COMMENT ON COLUMN erp_purchase_order_item.price IS '单价';
COMMENT ON COLUMN erp_purchase_order_item.price_incl_tax IS '含税单价';
COMMENT ON COLUMN erp_purchase_order_item.amount IS '金额';
COMMENT ON COLUMN erp_purchase_order_item.amount_incl_tax IS '含税金额';
COMMENT ON COLUMN erp_purchase_order_item.tax_rate IS '税率';
COMMENT ON COLUMN erp_purchase_order_item.tax_amount IS '税额';
COMMENT ON COLUMN erp_purchase_order_item.sort_no IS '排序';
COMMENT ON COLUMN erp_purchase_order_item.remark IS '备注';
COMMENT ON COLUMN erp_purchase_order_item.created_at IS '创建时间';
COMMENT ON COLUMN erp_purchase_order_item.updated_at IS '更新时间';

CREATE INDEX IF NOT EXISTS idx_erp_purchase_item_order
    ON erp_purchase_order_item (tenant_id, order_id);

CREATE INDEX IF NOT EXISTS idx_erp_purchase_item_product
    ON erp_purchase_order_item (tenant_id, product_id);

CREATE INDEX IF NOT EXISTS idx_erp_purchase_item_sort
    ON erp_purchase_order_item (tenant_id, order_id, sort_no);

-- Sale order header.
CREATE TABLE IF NOT EXISTS erp_sale_order (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT        NOT NULL,
    order_no        VARCHAR(64)   NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    customer_id     BIGINT,
    total_amount    NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_amount_excl_tax NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_tax_amount      NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_amount_incl_tax NUMERIC(18,2) NOT NULL DEFAULT 0,
    version         BIGINT        NOT NULL DEFAULT 0,
    approved_by     VARCHAR(100),
    approved_at     TIMESTAMPTZ,
    unapproved_by   VARCHAR(100),
    unapproved_at   TIMESTAMPTZ,
    cancelled_by    VARCHAR(100),
    cancelled_at    TIMESTAMPTZ,
    remark          VARCHAR(500),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE erp_sale_order IS '销售单头表（ERP进销存）';
COMMENT ON COLUMN erp_sale_order.id IS '主键';
COMMENT ON COLUMN erp_sale_order.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_sale_order.order_no IS '单号';
COMMENT ON COLUMN erp_sale_order.status IS '状态';
COMMENT ON COLUMN erp_sale_order.customer_id IS '客户ID';
COMMENT ON COLUMN erp_sale_order.total_amount IS '总金额';
COMMENT ON COLUMN erp_sale_order.total_amount_excl_tax IS '未税总金额';
COMMENT ON COLUMN erp_sale_order.total_tax_amount IS '税额合计';
COMMENT ON COLUMN erp_sale_order.total_amount_incl_tax IS '含税总金额';
COMMENT ON COLUMN erp_sale_order.version IS '乐观锁版本';
COMMENT ON COLUMN erp_sale_order.approved_by IS '审核人';
COMMENT ON COLUMN erp_sale_order.approved_at IS '审核时间';
COMMENT ON COLUMN erp_sale_order.unapproved_by IS '反审核人';
COMMENT ON COLUMN erp_sale_order.unapproved_at IS '反审核时间';
COMMENT ON COLUMN erp_sale_order.cancelled_by IS '作废人';
COMMENT ON COLUMN erp_sale_order.cancelled_at IS '作废时间';
COMMENT ON COLUMN erp_sale_order.remark IS '备注';
COMMENT ON COLUMN erp_sale_order.created_at IS '创建时间';
COMMENT ON COLUMN erp_sale_order.updated_at IS '更新时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_sale_order_no
    ON erp_sale_order (tenant_id, order_no);

CREATE INDEX IF NOT EXISTS idx_erp_sale_order_status
    ON erp_sale_order (tenant_id, status, updated_at DESC);

-- Sale order items.
CREATE TABLE IF NOT EXISTS erp_sale_order_item (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT        NOT NULL,
    order_id        BIGINT        NOT NULL,
    product_id      BIGINT        NOT NULL,
    product_code    VARCHAR(100),
    product_name    VARCHAR(200),
    warehouse_id    BIGINT,
    location_id     BIGINT,
    qty             NUMERIC(18,4) NOT NULL,
    price           NUMERIC(18,4) NOT NULL DEFAULT 0,
    price_incl_tax  NUMERIC(18,4) NOT NULL DEFAULT 0,
    amount          NUMERIC(18,2) NOT NULL DEFAULT 0,
    amount_incl_tax NUMERIC(18,2) NOT NULL DEFAULT 0,
    tax_rate        NUMERIC(6,4)  NOT NULL DEFAULT 0,
    tax_amount      NUMERIC(18,2) NOT NULL DEFAULT 0,
    sort_no         INT           NOT NULL DEFAULT 0,
    remark          VARCHAR(500),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE erp_sale_order_item IS '销售单明细表（ERP进销存）';
COMMENT ON COLUMN erp_sale_order_item.id IS '主键';
COMMENT ON COLUMN erp_sale_order_item.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_sale_order_item.order_id IS '销售单ID';
COMMENT ON COLUMN erp_sale_order_item.product_id IS '商品ID';
COMMENT ON COLUMN erp_sale_order_item.product_code IS '商品编码快照';
COMMENT ON COLUMN erp_sale_order_item.product_name IS '商品名称快照';
COMMENT ON COLUMN erp_sale_order_item.warehouse_id IS '仓库ID';
COMMENT ON COLUMN erp_sale_order_item.location_id IS '库位ID';
COMMENT ON COLUMN erp_sale_order_item.qty IS '数量';
COMMENT ON COLUMN erp_sale_order_item.price IS '单价';
COMMENT ON COLUMN erp_sale_order_item.price_incl_tax IS '含税单价';
COMMENT ON COLUMN erp_sale_order_item.amount IS '金额';
COMMENT ON COLUMN erp_sale_order_item.amount_incl_tax IS '含税金额';
COMMENT ON COLUMN erp_sale_order_item.tax_rate IS '税率';
COMMENT ON COLUMN erp_sale_order_item.tax_amount IS '税额';
COMMENT ON COLUMN erp_sale_order_item.sort_no IS '排序';
COMMENT ON COLUMN erp_sale_order_item.remark IS '备注';
COMMENT ON COLUMN erp_sale_order_item.created_at IS '创建时间';
COMMENT ON COLUMN erp_sale_order_item.updated_at IS '更新时间';

CREATE INDEX IF NOT EXISTS idx_erp_sale_item_order
    ON erp_sale_order_item (tenant_id, order_id);

CREATE INDEX IF NOT EXISTS idx_erp_sale_item_product
    ON erp_sale_order_item (tenant_id, product_id);

CREATE INDEX IF NOT EXISTS idx_erp_sale_item_sort
    ON erp_sale_order_item (tenant_id, order_id, sort_no);

-- Stock transaction ledger.
CREATE TABLE IF NOT EXISTS erp_stock_txn (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT        NOT NULL,
    txn_no          VARCHAR(100)  NOT NULL,
    biz_type        VARCHAR(50)   NOT NULL,
    biz_id          BIGINT,
    biz_item_id     BIGINT,
    product_id      BIGINT        NOT NULL,
    warehouse_id    BIGINT,
    location_id     BIGINT,
    qty_delta       NUMERIC(18,4) NOT NULL,
    qty_before      NUMERIC(18,4),
    qty_after       NUMERIC(18,4),
    operator        VARCHAR(100),
    operator_id     BIGINT,
    remark          VARCHAR(500),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE erp_stock_txn IS '库存流水表（ERP进销存）';
COMMENT ON COLUMN erp_stock_txn.id IS '主键';
COMMENT ON COLUMN erp_stock_txn.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_stock_txn.txn_no IS '流水号';
COMMENT ON COLUMN erp_stock_txn.biz_type IS '业务类型';
COMMENT ON COLUMN erp_stock_txn.biz_id IS '业务单据ID';
COMMENT ON COLUMN erp_stock_txn.biz_item_id IS '业务明细ID';
COMMENT ON COLUMN erp_stock_txn.product_id IS '商品ID';
COMMENT ON COLUMN erp_stock_txn.warehouse_id IS '仓库ID';
COMMENT ON COLUMN erp_stock_txn.location_id IS '库位ID';
COMMENT ON COLUMN erp_stock_txn.qty_delta IS '变更数量';
COMMENT ON COLUMN erp_stock_txn.qty_before IS '变更前数量';
COMMENT ON COLUMN erp_stock_txn.qty_after IS '变更后数量';
COMMENT ON COLUMN erp_stock_txn.operator IS '操作者';
COMMENT ON COLUMN erp_stock_txn.operator_id IS '操作者ID';
COMMENT ON COLUMN erp_stock_txn.remark IS '备注';
COMMENT ON COLUMN erp_stock_txn.created_at IS '创建时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_stock_txn_no
    ON erp_stock_txn (tenant_id, txn_no);

CREATE INDEX IF NOT EXISTS idx_erp_stock_txn_biz
    ON erp_stock_txn (tenant_id, biz_type, biz_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_erp_stock_txn_product
    ON erp_stock_txn (tenant_id, product_id, warehouse_id, location_id, created_at DESC);

-- Stock balance snapshot.
CREATE TABLE IF NOT EXISTS erp_stock_balance (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT        NOT NULL,
    product_id      BIGINT        NOT NULL,
    warehouse_id    BIGINT,
    location_id     BIGINT,
    qty_on_hand     NUMERIC(18,4) NOT NULL DEFAULT 0,
    updated_by      VARCHAR(100),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE erp_stock_balance IS '当前库存表（ERP进销存）';
COMMENT ON COLUMN erp_stock_balance.id IS '主键';
COMMENT ON COLUMN erp_stock_balance.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_stock_balance.product_id IS '商品ID';
COMMENT ON COLUMN erp_stock_balance.warehouse_id IS '仓库ID';
COMMENT ON COLUMN erp_stock_balance.location_id IS '库位ID';
COMMENT ON COLUMN erp_stock_balance.qty_on_hand IS '当前库存';
COMMENT ON COLUMN erp_stock_balance.updated_by IS '更新人';
COMMENT ON COLUMN erp_stock_balance.updated_at IS '更新时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_stock_balance_key
    ON erp_stock_balance (tenant_id, product_id, warehouse_id, location_id);

CREATE INDEX IF NOT EXISTS idx_erp_stock_balance_product
    ON erp_stock_balance (tenant_id, product_id, updated_at DESC);

-- Master data: product.
CREATE TABLE IF NOT EXISTS erp_product (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT        NOT NULL,
    code            VARCHAR(100)  NOT NULL,
    name            VARCHAR(200)  NOT NULL,
    short_name      VARCHAR(100),
    spec            VARCHAR(200),
    model           VARCHAR(200),
    category_id     BIGINT,
    unit_id         BIGINT,
    barcode         VARCHAR(100),
    sku             VARCHAR(100),
    brand           VARCHAR(100),
    origin          VARCHAR(200),
    weight          NUMERIC(18,4),
    volume          NUMERIC(18,4),
    cost_price      NUMERIC(18,4) NOT NULL DEFAULT 0,
    sale_price      NUMERIC(18,4) NOT NULL DEFAULT 0,
    tax_rate        NUMERIC(6,4)  NOT NULL DEFAULT 0,
    safety_stock    NUMERIC(18,4) NOT NULL DEFAULT 0,
    is_batch        BOOLEAN       NOT NULL DEFAULT FALSE,
    shelf_life_days INT,
    is_enabled      BOOLEAN       NOT NULL DEFAULT TRUE,
    ext_attrs       JSONB,
    remark          VARCHAR(500),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE erp_product IS '商品表（ERP进销存）';
COMMENT ON COLUMN erp_product.id IS '主键';
COMMENT ON COLUMN erp_product.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_product.code IS '商品编码';
COMMENT ON COLUMN erp_product.name IS '商品名称';
COMMENT ON COLUMN erp_product.short_name IS '商品简称';
COMMENT ON COLUMN erp_product.spec IS '规格型号';
COMMENT ON COLUMN erp_product.model IS '型号';
COMMENT ON COLUMN erp_product.category_id IS '分类ID';
COMMENT ON COLUMN erp_product.unit_id IS '单位ID';
COMMENT ON COLUMN erp_product.barcode IS '条码';
COMMENT ON COLUMN erp_product.sku IS 'SKU';
COMMENT ON COLUMN erp_product.brand IS '品牌';
COMMENT ON COLUMN erp_product.origin IS '产地';
COMMENT ON COLUMN erp_product.weight IS '重量';
COMMENT ON COLUMN erp_product.volume IS '体积';
COMMENT ON COLUMN erp_product.cost_price IS '成本价';
COMMENT ON COLUMN erp_product.sale_price IS '销售价';
COMMENT ON COLUMN erp_product.tax_rate IS '默认税率';
COMMENT ON COLUMN erp_product.safety_stock IS '安全库存';
COMMENT ON COLUMN erp_product.is_batch IS '是否批次管理';
COMMENT ON COLUMN erp_product.shelf_life_days IS '保质期(天)';
COMMENT ON COLUMN erp_product.is_enabled IS '是否启用';
COMMENT ON COLUMN erp_product.ext_attrs IS '扩展属性(JSON)';
COMMENT ON COLUMN erp_product.remark IS '备注';
COMMENT ON COLUMN erp_product.created_at IS '创建时间';
COMMENT ON COLUMN erp_product.updated_at IS '更新时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_product_code
    ON erp_product (tenant_id, code);

CREATE INDEX IF NOT EXISTS idx_erp_product_name
    ON erp_product (tenant_id, name);

-- Master data: customer.
CREATE TABLE IF NOT EXISTS erp_customer (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT        NOT NULL,
    code            VARCHAR(100)  NOT NULL,
    name            VARCHAR(200)  NOT NULL,
    short_name      VARCHAR(100),
    contact         VARCHAR(100),
    phone           VARCHAR(50),
    mobile          VARCHAR(50),
    email           VARCHAR(200),
    address         VARCHAR(500),
    tax_no          VARCHAR(100),
    bank_name       VARCHAR(200),
    bank_account    VARCHAR(200),
    invoice_title   VARCHAR(200),
    payment_terms   VARCHAR(100),
    credit_limit    NUMERIC(18,2) NOT NULL DEFAULT 0,
    contacts        JSONB,
    is_enabled      BOOLEAN       NOT NULL DEFAULT TRUE,
    remark          VARCHAR(500),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE erp_customer IS '客户表（ERP进销存）';
COMMENT ON COLUMN erp_customer.id IS '主键';
COMMENT ON COLUMN erp_customer.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_customer.code IS '客户编码';
COMMENT ON COLUMN erp_customer.name IS '客户名称';
COMMENT ON COLUMN erp_customer.short_name IS '客户简称';
COMMENT ON COLUMN erp_customer.contact IS '联系人';
COMMENT ON COLUMN erp_customer.phone IS '联系电话';
COMMENT ON COLUMN erp_customer.mobile IS '联系手机';
COMMENT ON COLUMN erp_customer.email IS '邮箱';
COMMENT ON COLUMN erp_customer.address IS '地址';
COMMENT ON COLUMN erp_customer.tax_no IS '税号';
COMMENT ON COLUMN erp_customer.bank_name IS '开户行';
COMMENT ON COLUMN erp_customer.bank_account IS '银行账号';
COMMENT ON COLUMN erp_customer.invoice_title IS '发票抬头';
COMMENT ON COLUMN erp_customer.payment_terms IS '结算方式';
COMMENT ON COLUMN erp_customer.credit_limit IS '授信额度';
COMMENT ON COLUMN erp_customer.contacts IS '联系人列表(JSON)';
COMMENT ON COLUMN erp_customer.is_enabled IS '是否启用';
COMMENT ON COLUMN erp_customer.remark IS '备注';
COMMENT ON COLUMN erp_customer.created_at IS '创建时间';
COMMENT ON COLUMN erp_customer.updated_at IS '更新时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_customer_code
    ON erp_customer (tenant_id, code);

CREATE INDEX IF NOT EXISTS idx_erp_customer_name
    ON erp_customer (tenant_id, name);

-- Master data: supplier.
CREATE TABLE IF NOT EXISTS erp_supplier (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT        NOT NULL,
    code            VARCHAR(100)  NOT NULL,
    name            VARCHAR(200)  NOT NULL,
    short_name      VARCHAR(100),
    contact         VARCHAR(100),
    phone           VARCHAR(50),
    mobile          VARCHAR(50),
    email           VARCHAR(200),
    address         VARCHAR(500),
    tax_no          VARCHAR(100),
    bank_name       VARCHAR(200),
    bank_account    VARCHAR(200),
    payment_terms   VARCHAR(100),
    contacts        JSONB,
    is_enabled      BOOLEAN       NOT NULL DEFAULT TRUE,
    remark          VARCHAR(500),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE erp_supplier IS '供应商表（ERP进销存）';
COMMENT ON COLUMN erp_supplier.id IS '主键';
COMMENT ON COLUMN erp_supplier.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_supplier.code IS '供应商编码';
COMMENT ON COLUMN erp_supplier.name IS '供应商名称';
COMMENT ON COLUMN erp_supplier.short_name IS '供应商简称';
COMMENT ON COLUMN erp_supplier.contact IS '联系人';
COMMENT ON COLUMN erp_supplier.phone IS '联系电话';
COMMENT ON COLUMN erp_supplier.mobile IS '联系手机';
COMMENT ON COLUMN erp_supplier.email IS '邮箱';
COMMENT ON COLUMN erp_supplier.address IS '地址';
COMMENT ON COLUMN erp_supplier.tax_no IS '税号';
COMMENT ON COLUMN erp_supplier.bank_name IS '开户行';
COMMENT ON COLUMN erp_supplier.bank_account IS '银行账号';
COMMENT ON COLUMN erp_supplier.payment_terms IS '结算方式';
COMMENT ON COLUMN erp_supplier.contacts IS '联系人列表(JSON)';
COMMENT ON COLUMN erp_supplier.is_enabled IS '是否启用';
COMMENT ON COLUMN erp_supplier.remark IS '备注';
COMMENT ON COLUMN erp_supplier.created_at IS '创建时间';
COMMENT ON COLUMN erp_supplier.updated_at IS '更新时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_supplier_code
    ON erp_supplier (tenant_id, code);

CREATE INDEX IF NOT EXISTS idx_erp_supplier_name
    ON erp_supplier (tenant_id, name);

-- Master data: warehouse.
CREATE TABLE IF NOT EXISTS erp_warehouse (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT        NOT NULL,
    code            VARCHAR(100)  NOT NULL,
    name            VARCHAR(200)  NOT NULL,
    address         VARCHAR(500),
    manager         VARCHAR(100),
    phone           VARCHAR(50),
    is_enabled      BOOLEAN       NOT NULL DEFAULT TRUE,
    remark          VARCHAR(500),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE erp_warehouse IS '仓库表（ERP进销存）';
COMMENT ON COLUMN erp_warehouse.id IS '主键';
COMMENT ON COLUMN erp_warehouse.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_warehouse.code IS '仓库编码';
COMMENT ON COLUMN erp_warehouse.name IS '仓库名称';
COMMENT ON COLUMN erp_warehouse.address IS '地址';
COMMENT ON COLUMN erp_warehouse.manager IS '负责人';
COMMENT ON COLUMN erp_warehouse.phone IS '联系电话';
COMMENT ON COLUMN erp_warehouse.is_enabled IS '是否启用';
COMMENT ON COLUMN erp_warehouse.remark IS '备注';
COMMENT ON COLUMN erp_warehouse.created_at IS '创建时间';
COMMENT ON COLUMN erp_warehouse.updated_at IS '更新时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_warehouse_code
    ON erp_warehouse (tenant_id, code);

CREATE INDEX IF NOT EXISTS idx_erp_warehouse_name
    ON erp_warehouse (tenant_id, name);

-- Master data: location/bin.
CREATE TABLE IF NOT EXISTS erp_location (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT        NOT NULL,
    warehouse_id    BIGINT        NOT NULL,
    code            VARCHAR(100)  NOT NULL,
    name            VARCHAR(200),
    aisle           VARCHAR(50),
    rack            VARCHAR(50),
    bin             VARCHAR(50),
    is_enabled      BOOLEAN       NOT NULL DEFAULT TRUE,
    remark          VARCHAR(500),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE erp_location IS '库位表（ERP进销存）';
COMMENT ON COLUMN erp_location.id IS '主键';
COMMENT ON COLUMN erp_location.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_location.warehouse_id IS '仓库ID';
COMMENT ON COLUMN erp_location.code IS '库位编码';
COMMENT ON COLUMN erp_location.name IS '库位名称';
COMMENT ON COLUMN erp_location.aisle IS '巷道';
COMMENT ON COLUMN erp_location.rack IS '货架';
COMMENT ON COLUMN erp_location.bin IS '货位';
COMMENT ON COLUMN erp_location.is_enabled IS '是否启用';
COMMENT ON COLUMN erp_location.remark IS '备注';
COMMENT ON COLUMN erp_location.created_at IS '创建时间';
COMMENT ON COLUMN erp_location.updated_at IS '更新时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_location_code
    ON erp_location (tenant_id, warehouse_id, code);

CREATE INDEX IF NOT EXISTS idx_erp_location_warehouse
    ON erp_location (tenant_id, warehouse_id);

-- Master data: category.
CREATE TABLE IF NOT EXISTS erp_category (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT        NOT NULL,
    code            VARCHAR(100)  NOT NULL,
    name            VARCHAR(200)  NOT NULL,
    parent_id       BIGINT,
    level           INT           NOT NULL DEFAULT 1,
    sort_no         INT           NOT NULL DEFAULT 0,
    is_enabled      BOOLEAN       NOT NULL DEFAULT TRUE,
    remark          VARCHAR(500),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE erp_category IS '商品分类表（ERP进销存）';
COMMENT ON COLUMN erp_category.id IS '主键';
COMMENT ON COLUMN erp_category.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_category.code IS '分类编码';
COMMENT ON COLUMN erp_category.name IS '分类名称';
COMMENT ON COLUMN erp_category.parent_id IS '父级分类ID';
COMMENT ON COLUMN erp_category.level IS '层级';
COMMENT ON COLUMN erp_category.sort_no IS '排序';
COMMENT ON COLUMN erp_category.is_enabled IS '是否启用';
COMMENT ON COLUMN erp_category.remark IS '备注';
COMMENT ON COLUMN erp_category.created_at IS '创建时间';
COMMENT ON COLUMN erp_category.updated_at IS '更新时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_category_code
    ON erp_category (tenant_id, code);

CREATE INDEX IF NOT EXISTS idx_erp_category_parent
    ON erp_category (tenant_id, parent_id);

-- Master data: unit.
CREATE TABLE IF NOT EXISTS erp_unit (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT        NOT NULL,
    code            VARCHAR(100)  NOT NULL,
    name            VARCHAR(200)  NOT NULL,
    symbol          VARCHAR(50),
    precision       INT           NOT NULL DEFAULT 0,
    is_enabled      BOOLEAN       NOT NULL DEFAULT TRUE,
    remark          VARCHAR(500),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE erp_unit IS '计量单位表（ERP进销存）';
COMMENT ON COLUMN erp_unit.id IS '主键';
COMMENT ON COLUMN erp_unit.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_unit.code IS '单位编码';
COMMENT ON COLUMN erp_unit.name IS '单位名称';
COMMENT ON COLUMN erp_unit.symbol IS '单位符号';
COMMENT ON COLUMN erp_unit.precision IS '小数精度';
COMMENT ON COLUMN erp_unit.is_enabled IS '是否启用';
COMMENT ON COLUMN erp_unit.remark IS '备注';
COMMENT ON COLUMN erp_unit.created_at IS '创建时间';
COMMENT ON COLUMN erp_unit.updated_at IS '更新时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_unit_code
    ON erp_unit (tenant_id, code);
