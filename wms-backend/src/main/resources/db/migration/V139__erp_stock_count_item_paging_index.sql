CREATE INDEX IF NOT EXISTS idx_erp_stock_count_item_count_line
    ON erp_stock_count_item (tenant_id, count_id, line_no)
    WHERE deleted_at IS NULL;
