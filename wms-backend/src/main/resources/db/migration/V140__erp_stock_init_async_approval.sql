ALTER TABLE erp_stock_count
    DROP CONSTRAINT IF EXISTS ck_erp_stock_count_status_async;

ALTER TABLE erp_stock_count
    ADD CONSTRAINT ck_erp_stock_count_status_async
        CHECK (status IN ('DRAFT', 'APPROVING', 'APPROVE_FAILED', 'APPROVED', 'CANCELLED', 'RED_FLUSHED'));

COMMENT ON COLUMN erp_stock_count.status IS '状态（DRAFT/APPROVING/APPROVE_FAILED/APPROVED/CANCELLED/RED_FLUSHED）';
