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
        SELECT p.id AS product_id,
               p.code AS product_code,
               p.name AS product_name,
               c.name AS category_name,
               u.name AS unit_name,
               p.default_warehouse_id AS default_warehouse_id,
               w.name AS default_warehouse_name,
               p.default_location_id AS default_location_id,
               l.name AS default_location_name,
               COALESCE(SUM(b.qty_on_hand), 0) AS total_qty,
               p.min_stock AS min_stock,
               p.max_stock AS max_stock,
               CASE
                   WHEN p.min_stock IS NOT NULL AND COALESCE(SUM(b.qty_on_hand), 0) < p.min_stock THEN 'LOW'
                   WHEN p.max_stock IS NOT NULL AND COALESCE(SUM(b.qty_on_hand), 0) > p.max_stock THEN 'HIGH'
                   ELSE 'NORMAL'
               END AS status
        FROM erp_product p
        LEFT JOIN erp_stock_balance b
          ON b.tenant_id = p.tenant_id
         AND b.product_id = p.id
        LEFT JOIN erp_category c
          ON c.tenant_id = p.tenant_id
         AND c.id = p.category_id
        LEFT JOIN erp_unit u
          ON u.tenant_id = p.tenant_id
         AND u.id = p.unit_id
        LEFT JOIN erp_warehouse w
          ON w.tenant_id = p.tenant_id
         AND w.id = p.default_warehouse_id
        LEFT JOIN erp_location l
          ON l.tenant_id = p.tenant_id
         AND l.id = p.default_location_id
        WHERE p.tenant_id = #{tenantId}
          AND (COALESCE(CAST(#{keyword} AS TEXT), '') = ''
               OR LOWER(p.name) LIKE CONCAT('%', LOWER(CAST(#{keyword} AS TEXT)), '%')
               OR LOWER(p.code) LIKE CONCAT('%', LOWER(CAST(#{keyword} AS TEXT)), '%'))
        GROUP BY p.id, p.code, p.name, c.name, u.name,
                 p.default_warehouse_id, w.name, p.default_location_id, l.name,
                 p.min_stock, p.max_stock
        HAVING (p.min_stock IS NOT NULL AND COALESCE(SUM(b.qty_on_hand), 0) < p.min_stock)
            OR (p.max_stock IS NOT NULL AND COALESCE(SUM(b.qty_on_hand), 0) > p.max_stock)
        ORDER BY status DESC, total_qty ASC, product_id ASC
        """)
    IPage<ErpStockWarningView> pageWarnings(Page<?> page,
                                           @Param("tenantId") Long tenantId,
                                           @Param("keyword") String keyword);
}
