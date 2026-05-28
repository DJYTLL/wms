package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.dto.erp.ErpPurchaseReturnSourcePurchaseOrderOption;
import com.example.wms.entity.erp.ErpPurchaseOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

// 采购单 Mapper（ERP进销存）
@Mapper
public interface ErpPurchaseOrderMapper extends BaseMapper<ErpPurchaseOrder> {
    // 按单号查询
    @Select("SELECT * FROM erp_purchase_order WHERE tenant_id = #{tenantId} AND order_no = #{orderNo} AND deleted_at IS NULL")
    ErpPurchaseOrder findByOrderNo(@Param("tenantId") Long tenantId, @Param("orderNo") String orderNo);

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
        WHERE o.tenant_id = #{tenantId}
          AND o.deleted_at IS NULL
          AND o.status = 'APPROVED'
          <if test='supplierId != null'>AND o.supplier_id = #{supplierId}</if>
          <if test='keyword != null and keyword != ""'>AND o.order_no LIKE CONCAT('%', #{keyword}, '%')</if>
          AND EXISTS (
              SELECT 1
              FROM erp_purchase_order_item i
              LEFT JOIN approved_return ar
                ON ar.purchase_order_id = o.id
               AND ar.source_purchase_order_item_id = i.id
              LEFT JOIN draft_return dr
                ON dr.purchase_order_id = o.id
               AND dr.source_purchase_order_item_id = i.id
              WHERE i.tenant_id = o.tenant_id
                AND i.order_id = o.id
                AND i.deleted_at IS NULL
                AND GREATEST(i.qty - COALESCE(ar.returned_qty, 0) - COALESCE(dr.draft_qty, 0), 0) &gt; 0
          )
        </script>
        """)
    long countReturnableSourceOrders(@Param("tenantId") Long tenantId,
                                     @Param("supplierId") Long supplierId,
                                     @Param("keyword") String keyword,
                                     @Param("currentReturnId") Long currentReturnId);

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
        SELECT o.id,
               o.order_no AS orderNo,
               o.supplier_id AS supplierId,
               o.order_at AS orderAt
        FROM erp_purchase_order o
        WHERE o.tenant_id = #{tenantId}
          AND o.deleted_at IS NULL
          AND o.status = 'APPROVED'
          <if test='supplierId != null'>AND o.supplier_id = #{supplierId}</if>
          <if test='keyword != null and keyword != ""'>AND o.order_no LIKE CONCAT('%', #{keyword}, '%')</if>
          AND EXISTS (
              SELECT 1
              FROM erp_purchase_order_item i
              LEFT JOIN approved_return ar
                ON ar.purchase_order_id = o.id
               AND ar.source_purchase_order_item_id = i.id
              LEFT JOIN draft_return dr
                ON dr.purchase_order_id = o.id
               AND dr.source_purchase_order_item_id = i.id
              WHERE i.tenant_id = o.tenant_id
                AND i.order_id = o.id
                AND i.deleted_at IS NULL
                AND GREATEST(i.qty - COALESCE(ar.returned_qty, 0) - COALESCE(dr.draft_qty, 0), 0) &gt; 0
          )
        ORDER BY o.order_at DESC NULLS LAST, o.id DESC
        LIMIT #{size} OFFSET #{offset}
        </script>
        """)
    List<ErpPurchaseReturnSourcePurchaseOrderOption> findReturnableSourceOrdersPage(@Param("tenantId") Long tenantId,
                                                                                     @Param("supplierId") Long supplierId,
                                                                                     @Param("keyword") String keyword,
                                                                                     @Param("currentReturnId") Long currentReturnId,
                                                                                     @Param("size") int size,
                                                                                     @Param("offset") long offset);
}
