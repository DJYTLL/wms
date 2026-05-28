package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.dto.erp.ErpAssemblySourceSaleOrderItem;
import com.example.wms.dto.erp.ErpAssemblySourceSaleOrderOption;
import com.example.wms.entity.erp.ErpAssemblyOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

// Assembly order mapper
@Mapper
public interface ErpAssemblyOrderMapper extends BaseMapper<ErpAssemblyOrder> {
    @Select("SELECT * FROM erp_assembly_order WHERE tenant_id = #{tenantId} AND order_no = #{orderNo} AND deleted_at IS NULL")
    ErpAssemblyOrder findByOrderNo(@Param("tenantId") Long tenantId, @Param("orderNo") String orderNo);

    @Select("SELECT * FROM erp_assembly_order WHERE tenant_id = #{tenantId} AND id = #{id} AND deleted_at IS NULL FOR UPDATE")
    ErpAssemblyOrder findByIdForUpdate(@Param("tenantId") Long tenantId, @Param("id") Long id);

    @Select("""
        <script>
        SELECT COUNT(DISTINCT o.id)
        FROM erp_sale_order o
        JOIN erp_customer c
          ON c.id = o.customer_id
         AND c.tenant_id = o.tenant_id
        LEFT JOIN erp_sale_order_item i
          ON i.order_id = o.id
         AND i.tenant_id = o.tenant_id
         AND i.deleted_at IS NULL
        WHERE o.tenant_id = #{tenantId}
          AND o.deleted_at IS NULL
          AND c.deleted_at IS NULL
          <if test='customerId != null'>AND o.customer_id = #{customerId}</if>
          <if test='keyword != null and keyword != ""'>
            AND (
              LOWER(o.order_no) LIKE CONCAT('%', LOWER(#{keyword}), '%')
              OR LOWER(c.name) LIKE CONCAT('%', LOWER(#{keyword}), '%')
              OR LOWER(COALESCE(i.product_name, '')) LIKE CONCAT('%', LOWER(#{keyword}), '%')
              OR LOWER(COALESCE(i.product_code, '')) LIKE CONCAT('%', LOWER(#{keyword}), '%')
            )
          </if>
        </script>
        """)
    long countSourceSaleOrders(@Param("tenantId") Long tenantId,
                               @Param("keyword") String keyword,
                               @Param("customerId") Long customerId);

    @Select("""
        <script>
        SELECT DISTINCT o.id,
               o.order_no AS orderNo,
               o.status,
               o.customer_id AS customerId,
               c.name AS customerName,
               o.order_at AS orderAt
        FROM erp_sale_order o
        JOIN erp_customer c
          ON c.id = o.customer_id
         AND c.tenant_id = o.tenant_id
        LEFT JOIN erp_sale_order_item i
          ON i.order_id = o.id
         AND i.tenant_id = o.tenant_id
         AND i.deleted_at IS NULL
        WHERE o.tenant_id = #{tenantId}
          AND o.deleted_at IS NULL
          AND c.deleted_at IS NULL
          <if test='customerId != null'>AND o.customer_id = #{customerId}</if>
          <if test='keyword != null and keyword != ""'>
            AND (
              LOWER(o.order_no) LIKE CONCAT('%', LOWER(#{keyword}), '%')
              OR LOWER(c.name) LIKE CONCAT('%', LOWER(#{keyword}), '%')
              OR LOWER(COALESCE(i.product_name, '')) LIKE CONCAT('%', LOWER(#{keyword}), '%')
              OR LOWER(COALESCE(i.product_code, '')) LIKE CONCAT('%', LOWER(#{keyword}), '%')
            )
          </if>
        ORDER BY o.order_at DESC NULLS LAST, o.id DESC
        LIMIT #{size} OFFSET #{offset}
        </script>
        """)
    List<ErpAssemblySourceSaleOrderOption> findSourceSaleOrders(@Param("tenantId") Long tenantId,
                                                                @Param("keyword") String keyword,
                                                                @Param("customerId") Long customerId,
                                                                @Param("size") int size,
                                                                @Param("offset") long offset);

    @Select("""
        SELECT i.id,
               i.sort_no AS sortNo,
               i.product_id AS productId,
               i.product_code AS productCode,
               i.product_name AS productName,
               i.warehouse_id AS warehouseId,
               i.location_id AS locationId,
               i.qty,
               COALESCE(SUM(a.finished_qty), 0) AS linkedAssemblyQty,
               COALESCE(SUM(CASE WHEN a.status = 'APPROVED' THEN a.finished_qty ELSE 0 END), 0) AS approvedAssemblyQty
        FROM erp_sale_order_item i
        LEFT JOIN erp_assembly_order a
          ON a.tenant_id = i.tenant_id
         AND a.source_sale_order_item_id = i.id
         AND a.deleted_at IS NULL
        WHERE i.tenant_id = #{tenantId}
          AND i.order_id = #{saleOrderId}
          AND i.deleted_at IS NULL
        GROUP BY i.id, i.sort_no, i.product_id, i.product_code, i.product_name, i.warehouse_id, i.location_id, i.qty
        ORDER BY i.sort_no, i.id
        """)
    List<ErpAssemblySourceSaleOrderItem> findSourceSaleOrderItems(@Param("tenantId") Long tenantId,
                                                                  @Param("saleOrderId") Long saleOrderId);

    @Select("SELECT * FROM erp_assembly_order WHERE tenant_id = #{tenantId} AND source_sale_order_id = #{saleOrderId} AND deleted_at IS NULL ORDER BY created_at DESC, id DESC")
    List<ErpAssemblyOrder> findBySourceSaleOrderId(@Param("tenantId") Long tenantId,
                                                   @Param("saleOrderId") Long saleOrderId);

    @Select("SELECT COALESCE(SUM(finished_qty), 0) FROM erp_assembly_order WHERE tenant_id = #{tenantId} AND source_sale_order_item_id = #{saleOrderItemId} AND deleted_at IS NULL")
    BigDecimal sumFinishedQtyBySourceSaleOrderItem(@Param("tenantId") Long tenantId,
                                                   @Param("saleOrderItemId") Long saleOrderItemId);
}
