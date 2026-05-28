ALTER TABLE erp_stock_count
    ADD COLUMN IF NOT EXISTS adjustment_reason VARCHAR(32);

COMMENT ON COLUMN erp_stock_count.adjustment_reason IS '库存调整原因（PROFIT/LOSS/CORRECTION/MIGRATION/OTHER）';

UPDATE erp_stock_count
SET adjustment_reason = COALESCE(NULLIF(adjustment_reason, ''), 'OTHER')
WHERE count_type = 'COUNT'
  AND adjustment_reason IS NULL;

ALTER TABLE erp_stock_count
    DROP CONSTRAINT IF EXISTS ck_erp_stock_count_adjustment_reason;

ALTER TABLE erp_stock_count
    ADD CONSTRAINT ck_erp_stock_count_adjustment_reason
    CHECK (
        (count_type = 'COUNT' AND adjustment_reason IN ('PROFIT', 'LOSS', 'CORRECTION', 'MIGRATION', 'OTHER'))
        OR (count_type <> 'COUNT')
    );

CREATE OR REPLACE FUNCTION erp_stock_count_item_scope_guard()
RETURNS TRIGGER AS $$
DECLARE
    parent_count_type VARCHAR(20);
    location_warehouse_id BIGINT;
BEGIN
    SELECT count_type
    INTO parent_count_type
    FROM erp_stock_count
    WHERE id = NEW.count_id
      AND tenant_id = NEW.tenant_id
      AND deleted_at IS NULL;

    IF parent_count_type = 'COUNT' AND NEW.warehouse_id IS NULL THEN
        RAISE EXCEPTION '库存调整明细必须选择仓库';
    END IF;

    IF NEW.location_id IS NOT NULL THEN
        IF NEW.warehouse_id IS NULL THEN
            RAISE EXCEPTION '选择库位时必须同时选择仓库';
        END IF;

        SELECT warehouse_id
        INTO location_warehouse_id
        FROM erp_location
        WHERE id = NEW.location_id
          AND tenant_id = NEW.tenant_id
          AND deleted_at IS NULL;

        IF location_warehouse_id IS NULL THEN
            RAISE EXCEPTION '库存调整库位不存在';
        END IF;

        IF location_warehouse_id <> NEW.warehouse_id THEN
            RAISE EXCEPTION '库存调整库位不属于所选仓库';
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_erp_stock_count_item_scope_guard ON erp_stock_count_item;

CREATE TRIGGER trg_erp_stock_count_item_scope_guard
    BEFORE INSERT OR UPDATE ON erp_stock_count_item
    FOR EACH ROW
    EXECUTE FUNCTION erp_stock_count_item_scope_guard();
