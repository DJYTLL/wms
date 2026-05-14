package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.dto.erp.ErpSaleOrderHistoryItem;
import com.example.wms.dto.erp.ErpSaleOrderRecentItem;
import com.example.wms.entity.erp.ErpSaleOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

// 销售单明细 Mapper（ERP进销存）
@Mapper
public interface ErpSaleOrderItemMapper extends BaseMapper<ErpSaleOrderItem> {
    // 查询订单明细
    @Select("SELECT * FROM erp_sale_order_item WHERE tenant_id = #{tenantId} AND order_id = #{orderId} AND deleted_at IS NULL ORDER BY sort_no, id")
    List<ErpSaleOrderItem> findByOrderId(@Param("tenantId") Long tenantId, @Param("orderId") Long orderId);

    @Select("""
        <script>
        SELECT *
        FROM erp_sale_order_item
        WHERE tenant_id = #{tenantId}
          AND deleted_at IS NULL
          AND order_id IN
          <foreach collection='orderIds' item='orderId' open='(' separator=',' close=')'>
            #{orderId}
          </foreach>
        ORDER BY order_id, sort_no, id
        </script>
        """)
    List<ErpSaleOrderItem> findByOrderIds(@Param("tenantId") Long tenantId, @Param("orderIds") List<Long> orderIds);

    @Select("""
        SELECT o.id AS orderId,
               o.order_no AS orderNo,
               o.order_at AS orderAt,
               i.product_id AS productId,
               i.qty AS qty,
               GREATEST(i.qty - COALESCE(r.returned_qty, 0), 0) AS remainingQty,
               i.price AS price
        FROM erp_sale_order o
        JOIN erp_sale_order_item i
          ON i.order_id = o.id
         AND i.tenant_id = o.tenant_id
        LEFT JOIN (
            SELECT r.sale_order_id,
                   ri.product_id,
                   SUM(ri.qty) AS returned_qty
            FROM erp_sale_return r
            JOIN erp_sale_return_item ri
              ON ri.return_id = r.id
             AND ri.tenant_id = r.tenant_id
             AND ri.deleted_at IS NULL
            WHERE r.tenant_id = #{tenantId}
              AND r.deleted_at IS NULL
              AND r.status = 'APPROVED'
            GROUP BY r.sale_order_id, ri.product_id
        ) r
          ON r.sale_order_id = o.id
         AND r.product_id = i.product_id
        WHERE o.tenant_id = #{tenantId}
          AND o.deleted_at IS NULL
          AND i.deleted_at IS NULL
          AND o.customer_id = #{customerId}
          AND i.product_id = #{productId}
          AND o.status = 'APPROVED'
        ORDER BY o.order_at DESC NULLS LAST, o.id DESC
        LIMIT #{limit}
        """)
    List<ErpSaleOrderRecentItem> findRecentItems(@Param("tenantId") Long tenantId,
                                                 @Param("customerId") Long customerId,
                                                 @Param("productId") Long productId,
                                                 @Param("limit") int limit);

    @Select("""
        SELECT COUNT(1)
        FROM erp_sale_order o
        JOIN erp_sale_order_item i
          ON i.order_id = o.id
         AND i.tenant_id = o.tenant_id
        WHERE o.tenant_id = #{tenantId}
          AND o.deleted_at IS NULL
          AND i.deleted_at IS NULL
          AND o.customer_id = #{customerId}
          AND i.product_id = #{productId}
          AND o.status = 'APPROVED'
        """)
    long countRecentItems(@Param("tenantId") Long tenantId,
                          @Param("customerId") Long customerId,
                          @Param("productId") Long productId);

    @Select("""
        SELECT o.id AS orderId,
               o.order_no AS orderNo,
               o.order_at AS orderAt,
               i.product_id AS productId,
               i.qty AS qty,
               GREATEST(i.qty - COALESCE(r.returned_qty, 0), 0) AS remainingQty,
               i.price AS price
        FROM erp_sale_order o
        JOIN erp_sale_order_item i
          ON i.order_id = o.id
         AND i.tenant_id = o.tenant_id
        LEFT JOIN (
            SELECT r.sale_order_id,
                   ri.product_id,
                   SUM(ri.qty) AS returned_qty
            FROM erp_sale_return r
            JOIN erp_sale_return_item ri
              ON ri.return_id = r.id
             AND ri.tenant_id = r.tenant_id
             AND ri.deleted_at IS NULL
            WHERE r.tenant_id = #{tenantId}
              AND r.deleted_at IS NULL
              AND r.status = 'APPROVED'
            GROUP BY r.sale_order_id, ri.product_id
        ) r
          ON r.sale_order_id = o.id
         AND r.product_id = i.product_id
        WHERE o.tenant_id = #{tenantId}
          AND o.deleted_at IS NULL
          AND i.deleted_at IS NULL
          AND o.customer_id = #{customerId}
          AND i.product_id = #{productId}
          AND o.status = 'APPROVED'
        ORDER BY o.order_at DESC NULLS LAST, o.id DESC
        LIMIT #{size} OFFSET #{offset}
        """)
    List<ErpSaleOrderRecentItem> findRecentItemsPage(@Param("tenantId") Long tenantId,
                                                     @Param("customerId") Long customerId,
                                                     @Param("productId") Long productId,
                                                     @Param("size") int size,
                                                     @Param("offset") long offset);

    @Select("""
        <script>
        SELECT COUNT(1)
        FROM erp_sale_order o
        JOIN erp_customer c
          ON c.id = o.customer_id
         AND c.tenant_id = o.tenant_id
        JOIN erp_sale_order_item i
          ON i.order_id = o.id
         AND i.tenant_id = o.tenant_id
        WHERE o.tenant_id = #{tenantId}
          AND o.deleted_at IS NULL
          AND c.deleted_at IS NULL
          AND i.deleted_at IS NULL
          <if test='customerId != null'>AND o.customer_id = #{customerId}</if>
          AND i.product_id = #{productId}
          AND o.status = 'APPROVED'
          <if test='keyword != null and keyword != \"\"'>
            AND (
              LOWER(c.name) LIKE CONCAT('%', LOWER(#{keyword}), '%')
              OR LOWER(o.order_no) LIKE CONCAT('%', LOWER(#{keyword}), '%')
            )
          </if>
          <if test='startAt != null'>AND o.order_at <![CDATA[>=]]> #{startAt}</if>
          <if test='endAt != null'>AND o.order_at <![CDATA[<=]]> #{endAt}</if>
        </script>
        """)
    long countProductHistory(@Param("tenantId") Long tenantId,
                             @Param("customerId") Long customerId,
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
               o.customer_id AS customerId,
               c.name AS customerName
        FROM erp_sale_order o
        JOIN erp_customer c
          ON c.id = o.customer_id
         AND c.tenant_id = o.tenant_id
        JOIN erp_sale_order_item i
          ON i.order_id = o.id
         AND i.tenant_id = o.tenant_id
        WHERE o.tenant_id = #{tenantId}
          AND o.deleted_at IS NULL
          AND c.deleted_at IS NULL
          AND i.deleted_at IS NULL
          <if test='customerId != null'>AND o.customer_id = #{customerId}</if>
          AND i.product_id = #{productId}
          AND o.status = 'APPROVED'
          <if test='keyword != null and keyword != \"\"'>
            AND (
              LOWER(c.name) LIKE CONCAT('%', LOWER(#{keyword}), '%')
              OR LOWER(o.order_no) LIKE CONCAT('%', LOWER(#{keyword}), '%')
            )
          </if>
          <if test='startAt != null'>AND o.order_at <![CDATA[>=]]> #{startAt}</if>
          <if test='endAt != null'>AND o.order_at <![CDATA[<=]]> #{endAt}</if>
        ORDER BY o.order_at DESC NULLS LAST, o.id DESC
        LIMIT #{size} OFFSET #{offset}
        </script>
        """)
    List<ErpSaleOrderHistoryItem> findProductHistoryPage(@Param("tenantId") Long tenantId,
                                                         @Param("customerId") Long customerId,
                                                         @Param("productId") Long productId,
                                                         @Param("keyword") String keyword,
                                                         @Param("startAt") java.time.Instant startAt,
                                                         @Param("endAt") java.time.Instant endAt,
                                                         @Param("size") int size,
                                                         @Param("offset") long offset);
}
