-- ERP 基础档案模拟数据（用于测试）

-- 分类
INSERT INTO erp_category (tenant_id, code, name, parent_id, level, sort_no, is_enabled, remark, created_at, updated_at)
SELECT t.id,
       'CAT-' || LPAD(g::text, 3, '0'),
       '分类' || g,
       NULL,
       1,
       g,
       TRUE,
       '模拟数据',
       NOW(),
       NOW()
FROM app_tenant t
CROSS JOIN generate_series(1, 5) g
WHERE t.deleted_at IS NULL
ON CONFLICT (tenant_id, code) DO NOTHING;

-- 单位
INSERT INTO erp_unit (tenant_id, code, name, symbol, precision, is_enabled, remark, created_at, updated_at)
SELECT t.id,
       'UNIT-' || LPAD(g::text, 3, '0'),
       '单位' || g,
       CASE g
           WHEN 1 THEN '件'
           WHEN 2 THEN '箱'
           WHEN 3 THEN 'kg'
           WHEN 4 THEN 'm'
           ELSE '个'
       END,
       2,
       TRUE,
       '模拟数据',
       NOW(),
       NOW()
FROM app_tenant t
CROSS JOIN generate_series(1, 5) g
WHERE t.deleted_at IS NULL
ON CONFLICT (tenant_id, code) DO NOTHING;

-- 仓库
INSERT INTO erp_warehouse (tenant_id, code, name, address, manager, phone, is_enabled, remark, created_at, updated_at)
SELECT t.id,
       'WH-' || LPAD(g::text, 3, '0'),
       '仓库' || g,
       '默认仓库地址' || g,
       '管理员' || g,
       '1380000' || LPAD(g::text, 4, '0'),
       TRUE,
       '模拟数据',
       NOW(),
       NOW()
FROM app_tenant t
CROSS JOIN generate_series(1, 3) g
WHERE t.deleted_at IS NULL
ON CONFLICT (tenant_id, code) DO NOTHING;

-- 库位
INSERT INTO erp_location (tenant_id, warehouse_id, code, name, aisle, rack, bin, is_enabled, remark, created_at, updated_at)
SELECT w.tenant_id,
       w.id,
       w.code || '-L' || LPAD(g::text, 2, '0'),
       '库位' || g,
       'A' || g,
       'R' || g,
       'B' || g,
       TRUE,
       '模拟数据',
       NOW(),
       NOW()
FROM erp_warehouse w
JOIN app_tenant t ON t.id = w.tenant_id AND t.deleted_at IS NULL
CROSS JOIN generate_series(1, 5) g
ON CONFLICT (tenant_id, warehouse_id, code) DO NOTHING;

-- 客户
INSERT INTO erp_customer (tenant_id, code, name, short_name, contact, phone, mobile, email, address,
                          tax_no, bank_name, bank_account, invoice_title, payment_terms, credit_limit,
                          contacts, is_enabled, remark, created_at, updated_at)
SELECT t.id,
       'CUS-' || LPAD(g::text, 3, '0'),
       '客户' || g,
       '客户' || g,
       '联系人' || g,
       '021-6000' || LPAD(g::text, 4, '0'),
       '1390000' || LPAD(g::text, 4, '0'),
       'customer' || g || '@example.com',
       '客户地址' || g,
       'TAX' || LPAD(g::text, 6, '0'),
       '中国银行',
       '622200' || LPAD(g::text, 8, '0'),
       '客户' || g,
       '月结',
       100000,
       NULL,
       TRUE,
       '模拟数据',
       NOW(),
       NOW()
FROM app_tenant t
CROSS JOIN generate_series(1, 10) g
WHERE t.deleted_at IS NULL
ON CONFLICT (tenant_id, code) DO NOTHING;

-- 供应商
INSERT INTO erp_supplier (tenant_id, code, name, short_name, contact, phone, mobile, email, address,
                          tax_no, bank_name, bank_account, payment_terms, contacts, is_enabled, remark, created_at, updated_at)
SELECT t.id,
       'SUP-' || LPAD(g::text, 3, '0'),
       '供应商' || g,
       '供应商' || g,
       '联系人' || g,
       '021-7000' || LPAD(g::text, 4, '0'),
       '1370000' || LPAD(g::text, 4, '0'),
       'supplier' || g || '@example.com',
       '供应商地址' || g,
       'TAX' || LPAD(g::text, 6, '0'),
       '建设银行',
       '621700' || LPAD(g::text, 8, '0'),
       '现结',
       NULL,
       TRUE,
       '模拟数据',
       NOW(),
       NOW()
FROM app_tenant t
CROSS JOIN generate_series(1, 8) g
WHERE t.deleted_at IS NULL
ON CONFLICT (tenant_id, code) DO NOTHING;

-- 商品
WITH gen AS (
    SELECT t.id AS tenant_id, g
    FROM app_tenant t
    CROSS JOIN generate_series(1, 30) g
    WHERE t.deleted_at IS NULL
)
INSERT INTO erp_product (tenant_id, code, name, short_name, spec, model, category_id, unit_id, barcode, sku,
                         brand, origin, weight, volume, cost_price, sale_price, tax_rate, safety_stock,
                         is_batch, shelf_life_days, is_enabled, ext_attrs, remark, created_at, updated_at)
SELECT gen.tenant_id,
       'PROD-' || LPAD(gen.g::text, 4, '0'),
       '商品' || gen.g,
       '商品' || gen.g,
       '规格' || gen.g,
       '型号' || gen.g,
       c.id,
       u.id,
       '690' || LPAD(gen.g::text, 10, '0'),
       'SKU' || LPAD(gen.g::text, 6, '0'),
       '品牌' || ((gen.g - 1) % 3 + 1),
       '产地' || ((gen.g - 1) % 5 + 1),
       1.2,
       0.8,
       10 + gen.g,
       15 + gen.g,
       0.13,
       20,
       FALSE,
       365,
       TRUE,
       NULL,
       '模拟数据',
       NOW(),
       NOW()
FROM gen
JOIN erp_category c
  ON c.tenant_id = gen.tenant_id
 AND c.code = 'CAT-' || LPAD(((gen.g - 1) % 5 + 1)::text, 3, '0')
JOIN erp_unit u
  ON u.tenant_id = gen.tenant_id
 AND u.code = 'UNIT-' || LPAD(((gen.g - 1) % 5 + 1)::text, 3, '0')
ON CONFLICT (tenant_id, code) DO NOTHING;

-- 结算方式（额外补充）
INSERT INTO erp_settlement_method (tenant_id, code, name, sort_no, is_enabled, remark, created_at, updated_at)
SELECT t.id, 'ALIPAY', '支付宝', 40, TRUE, '模拟数据', NOW(), NOW()
FROM app_tenant t
WHERE t.deleted_at IS NULL
ON CONFLICT (tenant_id, code) DO NOTHING;

INSERT INTO erp_settlement_method (tenant_id, code, name, sort_no, is_enabled, remark, created_at, updated_at)
SELECT t.id, 'WECHAT', '微信支付', 50, TRUE, '模拟数据', NOW(), NOW()
FROM app_tenant t
WHERE t.deleted_at IS NULL
ON CONFLICT (tenant_id, code) DO NOTHING;

-- 送货方式（额外补充）
INSERT INTO erp_delivery_method (tenant_id, code, name, sort_no, is_enabled, remark, created_at, updated_at)
SELECT t.id, 'SAME_DAY', '同城当日达', 40, TRUE, '模拟数据', NOW(), NOW()
FROM app_tenant t
WHERE t.deleted_at IS NULL
ON CONFLICT (tenant_id, code) DO NOTHING;

INSERT INTO erp_delivery_method (tenant_id, code, name, sort_no, is_enabled, remark, created_at, updated_at)
SELECT t.id, 'INSTALL', '送装一体', 50, TRUE, '模拟数据', NOW(), NOW()
FROM app_tenant t
WHERE t.deleted_at IS NULL
ON CONFLICT (tenant_id, code) DO NOTHING;
