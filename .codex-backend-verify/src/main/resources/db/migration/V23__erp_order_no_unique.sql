-- ERP单据号唯一约束（按租户隔离）
CREATE UNIQUE INDEX IF NOT EXISTS ux_erp_sale_order_no
ON erp_sale_order (tenant_id, order_no);
COMMENT ON INDEX ux_erp_sale_order_no IS 'ERP销售单号唯一（按租户）';

CREATE UNIQUE INDEX IF NOT EXISTS ux_erp_purchase_order_no
ON erp_purchase_order (tenant_id, order_no);
COMMENT ON INDEX ux_erp_purchase_order_no IS 'ERP采购单号唯一（按租户）';
