-- 重新计算应付单未付金额（已付 + 优惠）
UPDATE erp_accounts_payable
SET unpaid_amount = GREATEST(total_amount - (COALESCE(paid_amount, 0) + COALESCE(discount_amount, 0)), 0),
    status = CASE
        WHEN status = 'RED_FLUSHED' THEN status
        WHEN total_amount <= (COALESCE(paid_amount, 0) + COALESCE(discount_amount, 0)) THEN 'SETTLED'
        ELSE 'OPEN'
    END,
    updated_at = NOW();
