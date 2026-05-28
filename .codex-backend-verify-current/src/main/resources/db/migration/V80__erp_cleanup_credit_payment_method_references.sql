-- 清理误建在付款方式中的“挂账”引用
-- 原则：
-- 1. 没有真实资金流的采购单/采购退货单清空 payment_method_code
-- 2. 已发生真实付款/退款的付款方式改为 TRANSFER（若存在）
-- 3. 供应商默认付款方式若为挂账则清空
-- 4. 将付款方式表中的挂账记录逻辑删除，避免继续被选择

WITH credit_payment_method AS (
    SELECT tenant_id, code
    FROM erp_payment_method
    WHERE deleted_at IS NULL
      AND (
        code = 'CREDIT'
        OR name = '挂账'
      )
),
default_transfer_method AS (
    SELECT DISTINCT ON (tenant_id) tenant_id, code
    FROM erp_payment_method
    WHERE deleted_at IS NULL
      AND is_enabled = TRUE
      AND code = 'TRANSFER'
    ORDER BY tenant_id, id
)
UPDATE erp_supplier s
SET default_payment_method_code = NULL
FROM credit_payment_method c
WHERE s.tenant_id = c.tenant_id
  AND s.default_payment_method_code = c.code;

WITH credit_payment_method AS (
    SELECT tenant_id, code
    FROM erp_payment_method
    WHERE deleted_at IS NULL
      AND (
        code = 'CREDIT'
        OR name = '挂账'
      )
)
UPDATE erp_purchase_order po
SET payment_method_code = NULL
FROM credit_payment_method c
WHERE po.tenant_id = c.tenant_id
  AND po.payment_method_code = c.code
  AND COALESCE(po.paid_amount, 0) = 0;

WITH credit_payment_method AS (
    SELECT tenant_id, code
    FROM erp_payment_method
    WHERE deleted_at IS NULL
      AND (
        code = 'CREDIT'
        OR name = '挂账'
      )
),
default_transfer_method AS (
    SELECT DISTINCT ON (tenant_id) tenant_id, code
    FROM erp_payment_method
    WHERE deleted_at IS NULL
      AND is_enabled = TRUE
      AND code = 'TRANSFER'
    ORDER BY tenant_id, id
)
UPDATE erp_purchase_order po
SET payment_method_code = dt.code
FROM credit_payment_method c
JOIN default_transfer_method dt
  ON dt.tenant_id = c.tenant_id
WHERE po.tenant_id = c.tenant_id
  AND po.payment_method_code = c.code
  AND COALESCE(po.paid_amount, 0) > 0;

WITH credit_payment_method AS (
    SELECT tenant_id, code
    FROM erp_payment_method
    WHERE deleted_at IS NULL
      AND (
        code = 'CREDIT'
        OR name = '挂账'
      )
)
UPDATE erp_purchase_return pr
SET payment_method_code = NULL
FROM credit_payment_method c
WHERE pr.tenant_id = c.tenant_id
  AND pr.payment_method_code = c.code
  AND (
    COALESCE(pr.paid_amount, 0) = 0
    OR COALESCE(pr.refund_action, 'OFFSET_AP') = 'OFFSET_AP'
  );

WITH credit_payment_method AS (
    SELECT tenant_id, code
    FROM erp_payment_method
    WHERE deleted_at IS NULL
      AND (
        code = 'CREDIT'
        OR name = '挂账'
      )
),
default_transfer_method AS (
    SELECT DISTINCT ON (tenant_id) tenant_id, code
    FROM erp_payment_method
    WHERE deleted_at IS NULL
      AND is_enabled = TRUE
      AND code = 'TRANSFER'
    ORDER BY tenant_id, id
)
UPDATE erp_purchase_return pr
SET payment_method_code = dt.code
FROM credit_payment_method c
JOIN default_transfer_method dt
  ON dt.tenant_id = c.tenant_id
WHERE pr.tenant_id = c.tenant_id
  AND pr.payment_method_code = c.code
  AND COALESCE(pr.paid_amount, 0) <> 0
  AND COALESCE(pr.refund_action, 'OFFSET_AP') = 'REFUND';

WITH credit_payment_method AS (
    SELECT tenant_id, code
    FROM erp_payment_method
    WHERE deleted_at IS NULL
      AND (
        code = 'CREDIT'
        OR name = '挂账'
      )
),
default_transfer_method AS (
    SELECT DISTINCT ON (tenant_id) tenant_id, code
    FROM erp_payment_method
    WHERE deleted_at IS NULL
      AND is_enabled = TRUE
      AND code = 'TRANSFER'
    ORDER BY tenant_id, id
)
UPDATE erp_payment p
SET payment_method_code = dt.code
FROM credit_payment_method c
JOIN default_transfer_method dt
  ON dt.tenant_id = c.tenant_id
WHERE p.tenant_id = c.tenant_id
  AND p.payment_method_code = c.code;

UPDATE erp_payment_method pm
SET deleted_at = NOW(),
    deleted_by = 'system',
    delete_reason = '统一模型迁移：挂账属于结算方式，不属于付款方式'
WHERE pm.deleted_at IS NULL
  AND (
    pm.code = 'CREDIT'
    OR pm.name = '挂账'
  );
