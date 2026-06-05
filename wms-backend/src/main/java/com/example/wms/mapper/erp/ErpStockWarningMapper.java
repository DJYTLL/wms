package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.dto.erp.ErpStockWarningView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

// Stock warning mapper
@Mapper
public interface ErpStockWarningMapper {
    @Select("""
        WITH balance_by_warehouse AS (
            SELECT b.tenant_id,
                   b.product_id,
                   b.warehouse_id,
                   COALESCE(SUM(b.qty_on_hand), 0) AS total_qty
            FROM erp_stock_balance b
            GROUP BY b.tenant_id, b.product_id, b.warehouse_id
        ),
        fallback_candidates AS (
            SELECT p.id AS product_id,
                   p.code AS product_code,
                   p.name AS product_name,
                   c.name AS category_name,
                   u.name AS unit_name,
                   NULL::BIGINT AS warehouse_id,
                   NULL::VARCHAR(200) AS warehouse_name,
                   NULL::BIGINT AS location_id,
                   NULL::VARCHAR(200) AS location_name,
                   COALESCE(SUM(b.qty_on_hand), 0) AS total_qty,
                   p.safety_stock AS safety_stock,
                   p.min_stock AS min_stock,
                   p.max_stock AS max_stock
            FROM erp_product p
            LEFT JOIN erp_stock_balance b
              ON b.tenant_id = p.tenant_id
             AND b.product_id = p.id
            LEFT JOIN erp_category c
              ON c.tenant_id = p.tenant_id
             AND c.id = p.category_id
             AND c.deleted_at IS NULL
            LEFT JOIN erp_unit u
              ON u.tenant_id = p.tenant_id
             AND u.id = p.unit_id
             AND u.deleted_at IS NULL
            WHERE p.tenant_id = #{tenantId}
              AND p.deleted_at IS NULL
              AND CAST(#{warehouseId} AS BIGINT) IS NULL
              AND (COALESCE(CAST(#{keyword} AS TEXT), '') = ''
                   OR LOWER(p.name) LIKE CONCAT('%', LOWER(CAST(#{keyword} AS TEXT)), '%')
                   OR LOWER(p.code) LIKE CONCAT('%', LOWER(CAST(#{keyword} AS TEXT)), '%'))
            GROUP BY p.id, p.code, p.name, c.name, u.name,
                     p.safety_stock, p.min_stock, p.max_stock
        ),
        policy_warnings AS (
            SELECT p.id AS product_id,
                   p.code AS product_code,
                   p.name AS product_name,
                   c.name AS category_name,
                   u.name AS unit_name,
                   s.warehouse_id AS warehouse_id,
                   w.name AS warehouse_name,
                   NULL::BIGINT AS location_id,
                   NULL::VARCHAR(200) AS location_name,
                   COALESCE(bw.total_qty, 0) AS total_qty,
                   COALESCE(s.safety_stock, p.safety_stock) AS safety_stock,
                   s.min_stock AS min_stock,
                   s.max_stock AS max_stock,
                   'WAREHOUSE_POLICY' AS policy_source,
                   FALSE AS has_policy_anomaly,
                   '' AS anomaly_types_text,
                   CASE
                       WHEN s.min_stock IS NOT NULL AND s.min_stock > 0 AND COALESCE(bw.total_qty, 0) < s.min_stock THEN 'LOW'
                       WHEN s.max_stock IS NOT NULL AND s.max_stock > 0 AND COALESCE(bw.total_qty, 0) > s.max_stock THEN 'HIGH'
                       ELSE 'NORMAL'
                   END AS status
            FROM erp_product p
            JOIN erp_product_stock_policy s
              ON s.tenant_id = p.tenant_id
             AND s.product_id = p.id
             AND s.deleted_at IS NULL
            LEFT JOIN balance_by_warehouse bw
              ON bw.tenant_id = s.tenant_id
             AND bw.product_id = s.product_id
             AND bw.warehouse_id = s.warehouse_id
            LEFT JOIN erp_category c
              ON c.tenant_id = p.tenant_id
             AND c.id = p.category_id
             AND c.deleted_at IS NULL
            LEFT JOIN erp_unit u
              ON u.tenant_id = p.tenant_id
             AND u.id = p.unit_id
             AND u.deleted_at IS NULL
            LEFT JOIN erp_warehouse w
              ON w.tenant_id = s.tenant_id
             AND w.id = s.warehouse_id
             AND w.deleted_at IS NULL
            WHERE p.tenant_id = #{tenantId}
              AND p.deleted_at IS NULL
              AND (CAST(#{warehouseId} AS BIGINT) IS NULL OR s.warehouse_id = CAST(#{warehouseId} AS BIGINT))
              AND (COALESCE(CAST(#{keyword} AS TEXT), '') = ''
                   OR LOWER(p.name) LIKE CONCAT('%', LOWER(CAST(#{keyword} AS TEXT)), '%')
                   OR LOWER(p.code) LIKE CONCAT('%', LOWER(CAST(#{keyword} AS TEXT)), '%'))
              AND (
                   (s.min_stock IS NOT NULL AND s.min_stock > 0 AND COALESCE(bw.total_qty, 0) < s.min_stock)
                OR (s.max_stock IS NOT NULL AND s.max_stock > 0 AND COALESCE(bw.total_qty, 0) > s.max_stock)
              )
        ),
        product_fallback_warnings AS (
            SELECT fc.product_id,
                   fc.product_code,
                   fc.product_name,
                   fc.category_name,
                   fc.unit_name,
                   fc.warehouse_id,
                   fc.warehouse_name,
                   fc.location_id,
                   fc.location_name,
                   fc.total_qty,
                   fc.safety_stock,
                   fc.min_stock,
                   fc.max_stock,
                   'PRODUCT_FALLBACK' AS policy_source,
                   TRUE AS has_policy_anomaly,
                   'PRODUCT_FALLBACK_ONLY' AS anomaly_types_text,
                   CASE
                       WHEN fc.min_stock IS NOT NULL AND fc.min_stock > 0 AND fc.total_qty < fc.min_stock THEN 'LOW'
                       WHEN fc.max_stock IS NOT NULL AND fc.max_stock > 0 AND fc.total_qty > fc.max_stock THEN 'HIGH'
                       ELSE 'NORMAL'
                   END AS status
            FROM fallback_candidates fc
            WHERE (fc.min_stock IS NOT NULL AND fc.min_stock > 0 AND fc.total_qty < fc.min_stock)
               OR (fc.max_stock IS NOT NULL AND fc.max_stock > 0 AND fc.total_qty > fc.max_stock)
        )
        SELECT *
        FROM (
            SELECT * FROM policy_warnings
            UNION ALL
            SELECT * FROM product_fallback_warnings
        ) warnings
        WHERE (COALESCE(CAST(#{status} AS TEXT), '') = '' OR warnings.status = #{status})
          AND (COALESCE(CAST(#{policySource} AS TEXT), '') = '' OR warnings.policy_source = #{policySource})
          AND (CAST(#{hasPolicyAnomaly} AS BOOLEAN) IS NULL OR warnings.has_policy_anomaly = CAST(#{hasPolicyAnomaly} AS BOOLEAN))
        ORDER BY status DESC, total_qty ASC, product_id ASC, warehouse_id ASC
        """)
    IPage<ErpStockWarningView> pageWarnings(Page<?> page,
                                            @Param("tenantId") Long tenantId,
                                            @Param("keyword") String keyword,
                                            @Param("warehouseId") Long warehouseId,
                                            @Param("status") String status,
                                            @Param("policySource") String policySource,
                                            @Param("hasPolicyAnomaly") Boolean hasPolicyAnomaly);

    @Select("""
        WITH fallback_candidates AS (
            SELECT p.id AS product_id,
                   p.code AS product_code,
                   p.name AS product_name,
                   c.name AS category_name,
                   u.name AS unit_name,
                   NULL::BIGINT AS warehouse_id,
                   NULL::VARCHAR(200) AS warehouse_name,
                   NULL::BIGINT AS location_id,
                   NULL::VARCHAR(200) AS location_name,
                   COALESCE(SUM(b.qty_on_hand), 0) AS total_qty,
                   p.safety_stock AS safety_stock,
                   p.min_stock AS min_stock,
                   p.max_stock AS max_stock
            FROM erp_product p
            LEFT JOIN erp_stock_balance b
              ON b.tenant_id = p.tenant_id
             AND b.product_id = p.id
            LEFT JOIN erp_category c
              ON c.tenant_id = p.tenant_id
             AND c.id = p.category_id
             AND c.deleted_at IS NULL
            LEFT JOIN erp_unit u
              ON u.tenant_id = p.tenant_id
             AND u.id = p.unit_id
             AND u.deleted_at IS NULL
            WHERE p.tenant_id = #{tenantId}
              AND p.deleted_at IS NULL
              AND CAST(#{warehouseId} AS BIGINT) IS NULL
              AND (COALESCE(CAST(#{keyword} AS TEXT), '') = ''
                   OR LOWER(p.name) LIKE CONCAT('%', LOWER(CAST(#{keyword} AS TEXT)), '%')
                   OR LOWER(p.code) LIKE CONCAT('%', LOWER(CAST(#{keyword} AS TEXT)), '%'))
            GROUP BY p.id, p.code, p.name, c.name, u.name,
                     p.safety_stock, p.min_stock, p.max_stock
        )
        SELECT fc.product_id,
               fc.product_code,
               fc.product_name,
               fc.category_name,
               fc.unit_name,
               fc.warehouse_id,
               fc.warehouse_name,
               fc.location_id,
               fc.location_name,
               fc.total_qty,
               fc.safety_stock,
               fc.min_stock,
               fc.max_stock,
               'PRODUCT_FALLBACK' AS policy_source,
               TRUE AS has_policy_anomaly,
               'PRODUCT_FALLBACK_ONLY' AS anomaly_types_text,
               CASE
                   WHEN fc.min_stock IS NOT NULL AND fc.min_stock > 0 AND fc.total_qty < fc.min_stock THEN 'LOW'
                   WHEN fc.max_stock IS NOT NULL AND fc.max_stock > 0 AND fc.total_qty > fc.max_stock THEN 'HIGH'
                   ELSE 'NORMAL'
               END AS status
        FROM fallback_candidates fc
        WHERE ((fc.min_stock IS NOT NULL AND fc.min_stock > 0 AND fc.total_qty < fc.min_stock)
            OR (fc.max_stock IS NOT NULL AND fc.max_stock > 0 AND fc.total_qty > fc.max_stock))
          AND (COALESCE(CAST(#{anomalyType} AS TEXT), '') = '' OR 'PRODUCT_FALLBACK_ONLY' = #{anomalyType})
        ORDER BY fc.product_id ASC, fc.warehouse_id ASC
        """)
    IPage<ErpStockWarningView> pageAnomalies(Page<?> page,
                                             @Param("tenantId") Long tenantId,
                                             @Param("keyword") String keyword,
                                             @Param("warehouseId") Long warehouseId,
                                             @Param("anomalyType") String anomalyType);
}
