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
    @Select("SELECT * FROM erp_purchase_order_item WHERE tenant_id = #{tenantId} AND order_id = #{orderId} AND deleted_at IS NULL ORDER BY sort_no, id")
    List<ErpPurchaseOrderItem> findByOrderId(@Param("tenantId") Long tenantId, @Param("orderId") Long orderId);

    @Select("""
        <script>
        SELECT *
        FROM erp_purchase_order_item
        WHERE tenant_id = #{tenantId}
          AND deleted_at IS NULL
          AND id IN
          <foreach collection='ids' item='id' open='(' separator=',' close=')'>
            #{id}
          </foreach>
        FOR UPDATE
        </script>
        """)
    List<ErpPurchaseOrderItem> findByIdsForUpdate(@Param("tenantId") Long tenantId, @Param("ids") List<Long> ids);

    @Select("""
        <script>
        SELECT o.id AS orderId,
               i.id AS orderItemId,
               i.sort_no AS orderItemSortNo,
               o.order_no AS orderNo,
               o.order_at AS orderAt,
               i.product_id AS productId,
               i.warehouse_id AS warehouseId,
               i.location_id AS locationId,
               i.qty AS qty,
               GREATEST(i.qty - COALESCE(r.returned_qty, 0) - COALESCE(d.draft_qty, 0), 0) AS remainingQty,
               COALESCE(r.returned_qty, 0) AS approvedReturnedQty,
               COALESCE(d.draft_qty, 0) AS draftOccupiedQty,
               i.price AS price,
               i.price_incl_tax AS priceInclTax,
               i.tax_rate AS taxRate
        FROM erp_purchase_order o
        JOIN erp_purchase_order_item i
          ON i.order_id = o.id
         AND i.tenant_id = o.tenant_id
        LEFT JOIN (
            SELECT COALESCE(ri.source_purchase_order_id, r.purchase_order_id) AS purchase_order_id,
                   ri.source_purchase_order_item_id,
                   SUM(ri.qty) AS returned_qty
            FROM erp_purchase_return r
            JOIN erp_purchase_return_item ri
              ON ri.return_id = r.id
             AND ri.tenant_id = r.tenant_id
             AND ri.deleted_at IS NULL
            WHERE r.tenant_id = #{tenantId}
              AND r.deleted_at IS NULL
              AND r.status = 'APPROVED'
              AND ri.source_purchase_order_item_id IS NOT NULL
            GROUP BY COALESCE(ri.source_purchase_order_id, r.purchase_order_id), ri.source_purchase_order_item_id
        ) r
          ON r.purchase_order_id = o.id
         AND r.source_purchase_order_item_id = i.id
        LEFT JOIN (
            SELECT COALESCE(ri.source_purchase_order_id, r.purchase_order_id) AS purchase_order_id,
                   ri.source_purchase_order_item_id,
                   SUM(ri.qty) AS draft_qty
            FROM erp_purchase_return r
            JOIN erp_purchase_return_item ri
              ON ri.return_id = r.id
             AND ri.tenant_id = r.tenant_id
             AND ri.deleted_at IS NULL
            WHERE r.tenant_id = #{tenantId}
              AND r.deleted_at IS NULL
              AND r.status = 'DRAFT'
              AND ri.source_purchase_order_item_id IS NOT NULL
              <if test='currentReturnId != null'>AND r.id != #{currentReturnId}</if>
            GROUP BY COALESCE(ri.source_purchase_order_id, r.purchase_order_id), ri.source_purchase_order_item_id
        ) d
          ON d.purchase_order_id = o.id
         AND d.source_purchase_order_item_id = i.id
        WHERE o.tenant_id = #{tenantId}
          AND o.deleted_at IS NULL
          AND i.deleted_at IS NULL
          AND o.supplier_id = #{supplierId}
          AND i.product_id = #{productId}
          AND o.status = 'APPROVED'
          AND GREATEST(i.qty - COALESCE(r.returned_qty, 0) - COALESCE(d.draft_qty, 0), 0) > 0
        ORDER BY o.order_at DESC NULLS LAST, o.id DESC
        LIMIT #{limit}
        </script>
        """)
    List<ErpPurchaseOrderRecentItem> findRecentItems(@Param("tenantId") Long tenantId,
                                                     @Param("supplierId") Long supplierId,
                                                     @Param("productId") Long productId,
                                                     @Param("limit") int limit,
                                                     @Param("currentReturnId") Long currentReturnId);

    default List<ErpPurchaseOrderRecentItem> findRecentItems(Long tenantId, Long supplierId, Long productId, int limit) {
        return findRecentItems(tenantId, supplierId, productId, limit, null);
    }

    @Select("""
        <script>
        WITH approved_return AS (
            SELECT COALESCE(ri.source_purchase_order_id, r.purchase_order_id) AS purchase_order_id,
                   ri.source_purchase_order_item_id,
                   SUM(ri.qty) AS returned_qty
            FROM erp_purchase_return r
            JOIN erp_purchase_return_item ri
              ON ri.return_id = r.id
             AND ri.tenant_id = r.tenant_id
             AND ri.deleted_at IS NULL
            WHERE r.tenant_id = #{tenantId}
              AND r.deleted_at IS NULL
              AND r.status = 'APPROVED'
              AND ri.source_purchase_order_item_id IS NOT NULL
            GROUP BY COALESCE(ri.source_purchase_order_id, r.purchase_order_id), ri.source_purchase_order_item_id
        ),
        draft_return AS (
            SELECT COALESCE(ri.source_purchase_order_id, r.purchase_order_id) AS purchase_order_id,
                   ri.source_purchase_order_item_id,
                   SUM(ri.qty) AS draft_qty
            FROM erp_purchase_return r
            JOIN erp_purchase_return_item ri
              ON ri.return_id = r.id
             AND ri.tenant_id = r.tenant_id
             AND ri.deleted_at IS NULL
            WHERE r.tenant_id = #{tenantId}
              AND r.deleted_at IS NULL
              AND r.status = 'DRAFT'
              AND ri.source_purchase_order_item_id IS NOT NULL
              <if test='currentReturnId != null'>AND r.id != #{currentReturnId}</if>
            GROUP BY COALESCE(ri.source_purchase_order_id, r.purchase_order_id), ri.source_purchase_order_item_id
        )
        SELECT COUNT(1)
        FROM erp_purchase_order o
        JOIN erp_purchase_order_item i
          ON i.order_id = o.id
         AND i.tenant_id = o.tenant_id
        LEFT JOIN approved_return r
          ON r.purchase_order_id = o.id
         AND r.source_purchase_order_item_id = i.id
        LEFT JOIN draft_return d
          ON d.purchase_order_id = o.id
         AND d.source_purchase_order_item_id = i.id
        WHERE o.tenant_id = #{tenantId}
          AND o.deleted_at IS NULL
          AND i.deleted_at IS NULL
          AND o.supplier_id = #{supplierId}
          AND i.product_id = #{productId}
          AND o.status = 'APPROVED'
          AND GREATEST(i.qty - COALESCE(r.returned_qty, 0) - COALESCE(d.draft_qty, 0), 0) > 0
        </script>
        """)
    long countRecentItems(@Param("tenantId") Long tenantId,
                          @Param("supplierId") Long supplierId,
                          @Param("productId") Long productId,
                          @Param("currentReturnId") Long currentReturnId);

    default long countRecentItems(Long tenantId, Long supplierId, Long productId) {
        return countRecentItems(tenantId, supplierId, productId, null);
    }

    @Select("""
        <script>
        SELECT o.id AS orderId,
               i.id AS orderItemId,
               i.sort_no AS orderItemSortNo,
               o.order_no AS orderNo,
               o.order_at AS orderAt,
               i.product_id AS productId,
               i.warehouse_id AS warehouseId,
               i.location_id AS locationId,
               i.qty AS qty,
               GREATEST(i.qty - COALESCE(r.returned_qty, 0) - COALESCE(d.draft_qty, 0), 0) AS remainingQty,
               COALESCE(r.returned_qty, 0) AS approvedReturnedQty,
               COALESCE(d.draft_qty, 0) AS draftOccupiedQty,
               i.price AS price,
               i.price_incl_tax AS priceInclTax,
               i.tax_rate AS taxRate
        FROM erp_purchase_order o
        JOIN erp_purchase_order_item i
          ON i.order_id = o.id
         AND i.tenant_id = o.tenant_id
        LEFT JOIN (
            SELECT COALESCE(ri.source_purchase_order_id, r.purchase_order_id) AS purchase_order_id,
                   ri.source_purchase_order_item_id,
                   SUM(ri.qty) AS returned_qty
            FROM erp_purchase_return r
            JOIN erp_purchase_return_item ri
              ON ri.return_id = r.id
             AND ri.tenant_id = r.tenant_id
             AND ri.deleted_at IS NULL
            WHERE r.tenant_id = #{tenantId}
              AND r.deleted_at IS NULL
              AND r.status = 'APPROVED'
              AND ri.source_purchase_order_item_id IS NOT NULL
            GROUP BY COALESCE(ri.source_purchase_order_id, r.purchase_order_id), ri.source_purchase_order_item_id
        ) r
          ON r.purchase_order_id = o.id
         AND r.source_purchase_order_item_id = i.id
        LEFT JOIN (
            SELECT COALESCE(ri.source_purchase_order_id, r.purchase_order_id) AS purchase_order_id,
                   ri.source_purchase_order_item_id,
                   SUM(ri.qty) AS draft_qty
            FROM erp_purchase_return r
            JOIN erp_purchase_return_item ri
              ON ri.return_id = r.id
             AND ri.tenant_id = r.tenant_id
             AND ri.deleted_at IS NULL
            WHERE r.tenant_id = #{tenantId}
              AND r.deleted_at IS NULL
              AND r.status = 'DRAFT'
              AND ri.source_purchase_order_item_id IS NOT NULL
              <if test='currentReturnId != null'>AND r.id != #{currentReturnId}</if>
            GROUP BY COALESCE(ri.source_purchase_order_id, r.purchase_order_id), ri.source_purchase_order_item_id
        ) d
          ON d.purchase_order_id = o.id
         AND d.source_purchase_order_item_id = i.id
        WHERE o.tenant_id = #{tenantId}
          AND o.deleted_at IS NULL
          AND i.deleted_at IS NULL
          AND o.supplier_id = #{supplierId}
          AND i.product_id = #{productId}
          AND o.status = 'APPROVED'
          AND GREATEST(i.qty - COALESCE(r.returned_qty, 0) - COALESCE(d.draft_qty, 0), 0) > 0
        ORDER BY o.order_at DESC NULLS LAST, o.id DESC
        LIMIT #{size} OFFSET #{offset}
        </script>
        """)
    List<ErpPurchaseOrderRecentItem> findRecentItemsPage(@Param("tenantId") Long tenantId,
                                                         @Param("supplierId") Long supplierId,
                                                         @Param("productId") Long productId,
                                                         @Param("size") int size,
                                                         @Param("offset") long offset,
                                                         @Param("currentReturnId") Long currentReturnId);

    default List<ErpPurchaseOrderRecentItem> findRecentItemsPage(Long tenantId,
                                                                 Long supplierId,
                                                                 Long productId,
                                                                 int size,
                                                                 long offset) {
        return findRecentItemsPage(tenantId, supplierId, productId, size, offset, null);
    }

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
          AND o.deleted_at IS NULL
          AND s.deleted_at IS NULL
          AND i.deleted_at IS NULL
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
          AND o.deleted_at IS NULL
          AND s.deleted_at IS NULL
          AND i.deleted_at IS NULL
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
