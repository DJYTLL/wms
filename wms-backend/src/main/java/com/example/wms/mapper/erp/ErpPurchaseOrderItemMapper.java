package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.dto.erp.ErpPurchaseOrderHistoryItem;
import com.example.wms.dto.erp.ErpPurchaseOrderRecentItem;
import com.example.wms.entity.erp.ErpPurchaseOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

// 采购单明细 Mapper（ERP进销存）
@Mapper
public interface ErpPurchaseOrderItemMapper extends BaseMapper<ErpPurchaseOrderItem> {
    // 查询订单明细
    @Select("SELECT * FROM erp_purchase_order_item WHERE tenant_id = #{tenantId} AND order_id = #{orderId} ORDER BY sort_no, id")
    List<ErpPurchaseOrderItem> findByOrderId(@Param("tenantId") Long tenantId, @Param("orderId") Long orderId);

    @Select("""
        SELECT o.id AS orderId,
               o.order_no AS orderNo,
               o.order_at AS orderAt,
               i.product_id AS productId,
               i.qty AS qty,
               i.price AS price
        FROM erp_purchase_order o
        JOIN erp_purchase_order_item i
          ON i.order_id = o.id
         AND i.tenant_id = o.tenant_id
        WHERE o.tenant_id = #{tenantId}
          AND o.supplier_id = #{supplierId}
          AND i.product_id = #{productId}
          AND o.status = 'APPROVED'
        ORDER BY o.order_at DESC NULLS LAST, o.id DESC
        LIMIT #{limit}
        """)
    List<ErpPurchaseOrderRecentItem> findRecentItems(@Param("tenantId") Long tenantId,
                                                     @Param("supplierId") Long supplierId,
                                                     @Param("productId") Long productId,
                                                     @Param("limit") int limit);

    @Select("""
        <script>
        SELECT COUNT(1)
        FROM erp_purchase_order o
        JOIN erp_supplier s
          ON s.id = o.supplier_id
         AND s.tenant_id = o.tenant_id
        JOIN erp_purchase_order_item i
          ON i.order_id = o.id
         AND i.tenant_id = o.tenant_id
        WHERE o.tenant_id = #{tenantId}
          <if test='supplierId != null'>AND o.supplier_id = #{supplierId}</if>
          AND i.product_id = #{productId}
          AND o.status = 'APPROVED'
          <if test='keyword != null and keyword != \"\"'>
            AND (
              LOWER(s.name) LIKE CONCAT('%', LOWER(#{keyword}), '%')
              OR LOWER(o.order_no) LIKE CONCAT('%', LOWER(#{keyword}), '%')
            )
          </if>
          <if test='startAt != null'>AND o.order_at <![CDATA[>=]]> #{startAt}</if>
          <if test='endAt != null'>AND o.order_at <![CDATA[<=]]> #{endAt}</if>
        </script>
        """)
    long countProductHistory(@Param("tenantId") Long tenantId,
                             @Param("supplierId") Long supplierId,
                             @Param("productId") Long productId,
                             @Param("keyword") String keyword,
                             @Param("startAt") java.time.Instant startAt,
                             @Param("endAt") java.time.Instant endAt);

    @Select("""
        <script>
        SELECT o.id AS orderId,
               o.order_no AS orderNo,
               o.order_at AS orderAt,
               i.product_id AS productId,
               i.qty AS qty,
               i.price AS price,
               i.price_incl_tax AS priceInclTax,
               o.supplier_id AS supplierId,
               s.name AS supplierName
        FROM erp_purchase_order o
        JOIN erp_supplier s
          ON s.id = o.supplier_id
         AND s.tenant_id = o.tenant_id
        JOIN erp_purchase_order_item i
          ON i.order_id = o.id
         AND i.tenant_id = o.tenant_id
        WHERE o.tenant_id = #{tenantId}
          <if test='supplierId != null'>AND o.supplier_id = #{supplierId}</if>
          AND i.product_id = #{productId}
          AND o.status = 'APPROVED'
          <if test='keyword != null and keyword != \"\"'>
            AND (
              LOWER(s.name) LIKE CONCAT('%', LOWER(#{keyword}), '%')
              OR LOWER(o.order_no) LIKE CONCAT('%', LOWER(#{keyword}), '%')
            )
          </if>
          <if test='startAt != null'>AND o.order_at <![CDATA[>=]]> #{startAt}</if>
          <if test='endAt != null'>AND o.order_at <![CDATA[<=]]> #{endAt}</if>
        ORDER BY o.order_at DESC NULLS LAST, o.id DESC
        LIMIT #{size} OFFSET #{offset}
        </script>
        """)
    List<ErpPurchaseOrderHistoryItem> findProductHistoryPage(@Param("tenantId") Long tenantId,
                                                             @Param("supplierId") Long supplierId,
                                                             @Param("productId") Long productId,
                                                             @Param("keyword") String keyword,
                                                             @Param("startAt") java.time.Instant startAt,
                                                             @Param("endAt") java.time.Instant endAt,
                                                             @Param("size") int size,
                                                             @Param("offset") long offset);
}
