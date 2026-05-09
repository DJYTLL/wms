-- ERP 结算方式/送货方式增加默认标识

ALTER TABLE erp_settlement_method
    ADD COLUMN IF NOT EXISTS is_default BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE erp_delivery_method
    ADD COLUMN IF NOT EXISTS is_default BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN erp_settlement_method.is_default IS '是否默认';
COMMENT ON COLUMN erp_delivery_method.is_default IS '是否默认';

-- 设置默认结算方式（优先 CASH）
UPDATE erp_settlement_method
SET is_default = TRUE
WHERE code = 'CASH'
  AND tenant_id IS NOT NULL;

-- 设置默认送货方式（优先 SELF）
UPDATE erp_delivery_method
SET is_default = TRUE
WHERE code = 'SELF'
  AND tenant_id IS NOT NULL;

-- 若仍无默认，按排序取第一条作为默认
WITH ranked_settlement AS (
    SELECT id, tenant_id,
           ROW_NUMBER() OVER (PARTITION BY tenant_id ORDER BY sort_no, id) AS rn
    FROM erp_settlement_method
)
UPDATE erp_settlement_method m
SET is_default = TRUE
FROM ranked_settlement r
WHERE m.id = r.id
  AND r.rn = 1
  AND NOT EXISTS (
      SELECT 1 FROM erp_settlement_method m2
      WHERE m2.tenant_id = r.tenant_id
        AND m2.is_default = TRUE
  );

WITH ranked_delivery AS (
    SELECT id, tenant_id,
           ROW_NUMBER() OVER (PARTITION BY tenant_id ORDER BY sort_no, id) AS rn
    FROM erp_delivery_method
)
UPDATE erp_delivery_method m
SET is_default = TRUE
FROM ranked_delivery r
WHERE m.id = r.id
  AND r.rn = 1
  AND NOT EXISTS (
      SELECT 1 FROM erp_delivery_method m2
      WHERE m2.tenant_id = r.tenant_id
        AND m2.is_default = TRUE
  );

-- 每个租户仅允许一个默认值
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_settlement_method_default
    ON erp_settlement_method (tenant_id)
    WHERE is_default;

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_delivery_method_default
    ON erp_delivery_method (tenant_id)
    WHERE is_default;
