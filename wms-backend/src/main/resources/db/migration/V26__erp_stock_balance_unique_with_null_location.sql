-- ERP库存台账唯一性修复（库位为空也唯一）

-- 合并重复数据（同租户/商品/仓库/库位为空或相同）
WITH dup AS (
    SELECT
        tenant_id,
        product_id,
        warehouse_id,
        COALESCE(location_id, -1) AS location_key,
        MIN(id) AS keep_id,
        SUM(qty_on_hand) AS total_qty
    FROM erp_stock_balance
    GROUP BY tenant_id, product_id, warehouse_id, COALESCE(location_id, -1)
    HAVING COUNT(*) > 1
),
upd AS (
    UPDATE erp_stock_balance b
    SET qty_on_hand = d.total_qty,
        updated_by = 'system',
        updated_at = NOW()
    FROM dup d
    WHERE b.id = d.keep_id
    RETURNING b.id
)
DELETE FROM erp_stock_balance b
USING dup d
WHERE b.id <> d.keep_id
  AND b.tenant_id = d.tenant_id
  AND b.product_id = d.product_id
  AND b.warehouse_id = d.warehouse_id
  AND COALESCE(b.location_id, -1) = d.location_key;

-- 新增唯一索引（库位为空也唯一）
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_stock_balance_key_full
    ON erp_stock_balance (tenant_id, product_id, warehouse_id, COALESCE(location_id, -1));
