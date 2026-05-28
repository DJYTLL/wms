-- 允许同一销售单存在多张应收（用于销售退货负数应收）
DROP INDEX IF EXISTS uk_erp_ar_tenant_sale;

-- 应收单号保持租户内唯一
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_ar_tenant_order_no
    ON erp_accounts_receivable (tenant_id, order_no);

