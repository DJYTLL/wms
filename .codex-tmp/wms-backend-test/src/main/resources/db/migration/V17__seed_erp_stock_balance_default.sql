-- 默认仓库/库位库存初始化（仅为测试模拟数据）

-- 为商品默认仓库/库位生成库存台账
INSERT INTO erp_stock_balance (tenant_id, product_id, warehouse_id, location_id, qty_on_hand, updated_by, updated_at)
SELECT p.tenant_id,
       p.id,
       p.default_warehouse_id,
       p.default_location_id,
       (10 + (p.id % 50))::NUMERIC(18,4) AS qty_on_hand,
       'system',
       NOW()
FROM erp_product p
JOIN app_tenant t
  ON t.id = p.tenant_id
 AND t.deleted_at IS NULL
JOIN erp_warehouse w
  ON w.id = p.default_warehouse_id
 AND w.tenant_id = p.tenant_id
JOIN erp_location l
  ON l.id = p.default_location_id
 AND l.tenant_id = p.tenant_id
WHERE p.default_warehouse_id IS NOT NULL
  AND p.default_location_id IS NOT NULL
ON CONFLICT (tenant_id, product_id, warehouse_id, location_id) DO NOTHING;

-- 为商品默认仓库/库位生成一条初始化库存流水
INSERT INTO erp_stock_txn (
    tenant_id,
    txn_no,
    biz_type,
    biz_id,
    biz_item_id,
    product_id,
    warehouse_id,
    location_id,
    qty_delta,
    qty_before,
    qty_after,
    operator,
    operator_id,
    remark,
    created_at
)
SELECT p.tenant_id,
       'INIT-' || p.tenant_id || '-' || p.id AS txn_no,
       'INIT' AS biz_type,
       NULL AS biz_id,
       NULL AS biz_item_id,
       p.id,
       p.default_warehouse_id,
       p.default_location_id,
       (10 + (p.id % 50))::NUMERIC(18,4) AS qty_delta,
       0::NUMERIC(18,4) AS qty_before,
       (10 + (p.id % 50))::NUMERIC(18,4) AS qty_after,
       'system',
       NULL,
       '默认库存初始化',
       NOW()
FROM erp_product p
JOIN app_tenant t
  ON t.id = p.tenant_id
 AND t.deleted_at IS NULL
JOIN erp_warehouse w
  ON w.id = p.default_warehouse_id
 AND w.tenant_id = p.tenant_id
JOIN erp_location l
  ON l.id = p.default_location_id
 AND l.tenant_id = p.tenant_id
WHERE p.default_warehouse_id IS NOT NULL
  AND p.default_location_id IS NOT NULL
ON CONFLICT (tenant_id, txn_no) DO NOTHING;
