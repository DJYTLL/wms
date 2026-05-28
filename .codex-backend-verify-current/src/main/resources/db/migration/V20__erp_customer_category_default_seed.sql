-- 客户类别默认值与批量初始化售价

-- 增加默认标识
ALTER TABLE erp_customer_category
    ADD COLUMN IF NOT EXISTS is_default BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN erp_customer_category.is_default IS '是否默认';

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_customer_category_default
    ON erp_customer_category (tenant_id)
    WHERE is_default;

-- 批量初始化客户类别（每租户）
INSERT INTO erp_customer_category (tenant_id, code, name, description, sort_no, is_enabled, is_default, remark, created_at, updated_at)
SELECT t.id,
       v.code,
       v.name,
       v.description,
       v.sort_no,
       TRUE,
       FALSE,
       '系统初始化',
       NOW(),
       NOW()
FROM app_tenant t
CROSS JOIN (
    VALUES
        ('CUST-RETAIL', '零售客户', '默认零售客户类别', 10),
        ('CUST-WHOLE', '批发客户', '批发客户类别', 20),
        ('CUST-VIP', 'VIP客户', 'VIP客户类别', 30)
) AS v(code, name, description, sort_no)
WHERE t.deleted_at IS NULL
ON CONFLICT (tenant_id, code) DO NOTHING;

-- 确保每个租户至少一个默认类别
WITH tenant_no_default AS (
    SELECT t.id AS tenant_id
    FROM app_tenant t
    WHERE t.deleted_at IS NULL
      AND NOT EXISTS (
        SELECT 1 FROM erp_customer_category c
        WHERE c.tenant_id = t.id AND c.is_default = TRUE
      )
),
first_category AS (
    SELECT DISTINCT ON (c.tenant_id) c.id
    FROM erp_customer_category c
    JOIN tenant_no_default t ON t.tenant_id = c.tenant_id
    ORDER BY c.tenant_id, c.sort_no, c.id
)
UPDATE erp_customer_category
SET is_default = TRUE
WHERE id IN (SELECT id FROM first_category);

-- 按客户类别批量初始化商品售价（按商品基础售价打折）
INSERT INTO erp_product_price (tenant_id, product_id, customer_category_id, sale_price, created_at, updated_at)
SELECT p.tenant_id,
       p.id,
       c.id,
       COALESCE(
           CASE c.code
               WHEN 'CUST-RETAIL' THEN p.sale_price
               WHEN 'CUST-WHOLE' THEN p.sale_price * 0.95
               WHEN 'CUST-VIP' THEN p.sale_price * 0.90
               ELSE p.sale_price
           END,
           0
       ),
       NOW(),
       NOW()
FROM erp_product p
JOIN erp_customer_category c
  ON c.tenant_id = p.tenant_id
WHERE c.is_enabled = TRUE
ON CONFLICT (tenant_id, product_id, customer_category_id) DO NOTHING;
