-- ERP应付单补充优惠金额字段并回填
ALTER TABLE erp_accounts_payable
    ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(18,2) NOT NULL DEFAULT 0;

COMMENT ON COLUMN erp_accounts_payable.discount_amount IS '优惠金额';

-- 基于付款分摊回填已付/优惠/未付（优先使用分摊明细）
WITH alloc AS (
    SELECT payable_id,
           COALESCE(SUM(allocated_amount), 0) AS paid_amount,
           COALESCE(SUM(allocated_discount), 0) AS discount_amount
    FROM erp_payment_payable
    GROUP BY payable_id
)
UPDATE erp_accounts_payable ap
SET paid_amount = alloc.paid_amount,
    discount_amount = alloc.discount_amount,
    unpaid_amount = GREATEST(ap.total_amount - (alloc.paid_amount + alloc.discount_amount), 0),
    updated_at = NOW()
FROM alloc
WHERE ap.id = alloc.payable_id;

-- 对没有分摊明细的记录补默认优惠金额
UPDATE erp_accounts_payable
SET discount_amount = COALESCE(discount_amount, 0)
WHERE discount_amount IS NULL;
