package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.dto.erp.ErpSaleOrderFlowSnapshot;
import com.example.wms.dto.erp.ErpSaleReturnSourceSaleOrderOption;
import com.example.wms.entity.erp.ErpSaleOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

// 销售单 Mapper（ERP进销存）
@Mapper
public interface ErpSaleOrderMapper extends BaseMapper<ErpSaleOrder> {
    // 按单号查询
    @Select("SELECT * FROM erp_sale_order WHERE tenant_id = #{tenantId} AND order_no = #{orderNo} AND deleted_at IS NULL")
    ErpSaleOrder findByOrderNo(@Param("tenantId") Long tenantId, @Param("orderNo") String orderNo);

    @Select("""
        UPDATE erp_sale_order
        SET status = 'APPROVED',
            approved_by = #{operator},
            approved_at = NOW(),
            updated_at = NOW(),
            updated_by = #{operator},
            version = COALESCE(version, 0) + 1
        WHERE tenant_id = #{tenantId}
          AND id = #{id}
          AND status = 'DRAFT'
          AND deleted_at IS NULL
        RETURNING *
        """)
    ErpSaleOrder approveDraft(@Param("tenantId") Long tenantId,
                              @Param("id") Long id,
                              @Param("operator") String operator);

    @Select("""
        UPDATE erp_sale_order
        SET status = 'RED_FLUSHED',
            red_flush_source_type = 'SALE_ORDER',
            red_flush_source_id = id,
            remark = #{remark},
            updated_at = NOW(),
            updated_by = #{operator},
            version = COALESCE(version, 0) + 1
        WHERE tenant_id = #{tenantId}
          AND id = #{id}
          AND status = 'APPROVED'
          AND deleted_at IS NULL
        RETURNING *
        """)
    ErpSaleOrder redFlushApproved(@Param("tenantId") Long tenantId,
                                  @Param("id") Long id,
                                  @Param("remark") String remark,
                                  @Param("operator") String operator);

    @Select("""
        <script>
        WITH page_orders AS (
            SELECT id, status
            FROM erp_sale_order
            WHERE tenant_id = #{tenantId}
              AND deleted_at IS NULL
              AND id IN
              <foreach collection='saleOrderIds' item='saleOrderId' open='(' separator=',' close=')'>
                #{saleOrderId}
              </foreach>
        ),
        latest_receivable AS (
            SELECT DISTINCT ON (sale_order_id)
                   sale_order_id,
                   status,
                   unpaid_amount
            FROM erp_accounts_receivable
            WHERE tenant_id = #{tenantId}
              AND total_amount &gt;= 0
              AND deleted_at IS NULL
              AND sale_order_id IN
              <foreach collection='saleOrderIds' item='saleOrderId' open='(' separator=',' close=')'>
                #{saleOrderId}
              </foreach>
            ORDER BY sale_order_id, id DESC
        ),
        approved_returns AS (
            SELECT sale_order_id,
                   COUNT(1) AS approved_return_count,
                   COALESCE(SUM(total_amount_incl_tax), 0) AS cumulative_return_amount
            FROM erp_sale_return
            WHERE tenant_id = #{tenantId}
              AND status = 'APPROVED'
              AND deleted_at IS NULL
              AND sale_order_id IN
              <foreach collection='saleOrderIds' item='saleOrderId' open='(' separator=',' close=')'>
                #{saleOrderId}
              </foreach>
            GROUP BY sale_order_id
        ),
        sale_costs AS (
            SELECT biz_id AS sale_order_id,
                   COALESCE(SUM(ABS(total_cost)), 0) AS sale_cost
            FROM erp_stock_txn
            WHERE tenant_id = #{tenantId}
              AND biz_type = 'SALE_APPROVE'
              AND biz_id IN
              <foreach collection='saleOrderIds' item='saleOrderId' open='(' separator=',' close=')'>
                #{saleOrderId}
              </foreach>
            GROUP BY biz_id
        ),
        return_costs AS (
            SELECT r.sale_order_id,
                   COALESCE(SUM(ABS(txn.total_cost)), 0) AS cumulative_return_cost
            FROM erp_sale_return r
            JOIN erp_stock_txn txn
              ON txn.tenant_id = r.tenant_id
             AND txn.biz_type = 'SALE_RETURN_RESTOCK'
             AND txn.biz_id = r.id
            WHERE r.tenant_id = #{tenantId}
              AND r.status = 'APPROVED'
              AND r.deleted_at IS NULL
              AND r.sale_order_id IN
              <foreach collection='saleOrderIds' item='saleOrderId' open='(' separator=',' close=')'>
                #{saleOrderId}
              </foreach>
            GROUP BY r.sale_order_id
        ),
        draft_costs AS (
            SELECT i.order_id AS sale_order_id,
                   COALESCE(SUM(COALESCE(p.cost_price, 0) * COALESCE(i.qty, 0)), 0) AS draft_cost
            FROM erp_sale_order_item i
            LEFT JOIN erp_product p
              ON p.tenant_id = i.tenant_id
             AND p.id = i.product_id
             AND p.deleted_at IS NULL
            WHERE i.tenant_id = #{tenantId}
              AND i.deleted_at IS NULL
              AND i.order_id IN
              <foreach collection='saleOrderIds' item='saleOrderId' open='(' separator=',' close=')'>
                #{saleOrderId}
              </foreach>
            GROUP BY i.order_id
        )
        SELECT o.id AS saleOrderId,
               lr.status AS receivableStatus,
               lr.unpaid_amount AS receivableUnpaidAmount,
               COALESCE(ar.approved_return_count, 0) AS approvedReturnCount,
               COALESCE(ar.cumulative_return_amount, 0) AS cumulativeReturnAmount,
               CASE
                   WHEN o.status = 'DRAFT' THEN COALESCE(dc.draft_cost, 0)
                   ELSE COALESCE(sc.sale_cost, 0)
               END AS saleCost,
               COALESCE(rc.cumulative_return_cost, 0) AS cumulativeReturnCost
        FROM page_orders o
        LEFT JOIN latest_receivable lr ON lr.sale_order_id = o.id
        LEFT JOIN approved_returns ar ON ar.sale_order_id = o.id
        LEFT JOIN sale_costs sc ON sc.sale_order_id = o.id
        LEFT JOIN return_costs rc ON rc.sale_order_id = o.id
        LEFT JOIN draft_costs dc ON dc.sale_order_id = o.id
        </script>
        """)
    List<ErpSaleOrderFlowSnapshot> findFlowSnapshotsByIds(@Param("tenantId") Long tenantId,
                                                          @Param("saleOrderIds") List<Long> saleOrderIds);

    @Select("""
        <script>
        WITH approved_return AS (
            SELECT COALESCE(ri.source_sale_order_id, r.sale_order_id) AS sale_order_id,
                   ri.source_sale_order_item_id,
                   SUM(ri.qty) AS returned_qty
            FROM erp_sale_return r
            JOIN erp_sale_return_item ri
              ON ri.return_id = r.id
             AND ri.tenant_id = r.tenant_id
             AND ri.deleted_at IS NULL
            WHERE r.tenant_id = #{tenantId}
              AND r.deleted_at IS NULL
              AND r.status = 'APPROVED'
              AND ri.source_sale_order_item_id IS NOT NULL
            GROUP BY COALESCE(ri.source_sale_order_id, r.sale_order_id), ri.source_sale_order_item_id
        ),
        draft_return AS (
            SELECT COALESCE(ri.source_sale_order_id, r.sale_order_id) AS sale_order_id,
                   ri.source_sale_order_item_id,
                   SUM(ri.qty) AS draft_qty
            FROM erp_sale_return r
            JOIN erp_sale_return_item ri
              ON ri.return_id = r.id
             AND ri.tenant_id = r.tenant_id
             AND ri.deleted_at IS NULL
            WHERE r.tenant_id = #{tenantId}
              AND r.deleted_at IS NULL
              AND r.status = 'DRAFT'
              AND ri.source_sale_order_item_id IS NOT NULL
              <if test='currentReturnId != null'>AND r.id != #{currentReturnId}</if>
            GROUP BY COALESCE(ri.source_sale_order_id, r.sale_order_id), ri.source_sale_order_item_id
        )
        SELECT COUNT(1)
        FROM erp_sale_order o
        WHERE o.tenant_id = #{tenantId}
          AND o.deleted_at IS NULL
          AND o.status = 'APPROVED'
          <if test='customerId != null'>AND o.customer_id = #{customerId}</if>
          <if test='keyword != null and keyword != ""'>AND o.order_no LIKE CONCAT('%', #{keyword}, '%')</if>
          AND EXISTS (
              SELECT 1
              FROM erp_sale_order_item i
              LEFT JOIN approved_return ar
                ON ar.sale_order_id = o.id
               AND ar.source_sale_order_item_id = i.id
              LEFT JOIN draft_return dr
                ON dr.sale_order_id = o.id
               AND dr.source_sale_order_item_id = i.id
              WHERE i.tenant_id = o.tenant_id
                AND i.order_id = o.id
                AND i.deleted_at IS NULL
                AND GREATEST(i.qty - COALESCE(ar.returned_qty, 0) - COALESCE(dr.draft_qty, 0), 0) &gt; 0
          )
        </script>
        """)
    long countReturnableSourceOrders(@Param("tenantId") Long tenantId,
                                     @Param("customerId") Long customerId,
                                     @Param("keyword") String keyword,
                                     @Param("currentReturnId") Long currentReturnId);

    @Select("""
        <script>
        WITH approved_return AS (
            SELECT COALESCE(ri.source_sale_order_id, r.sale_order_id) AS sale_order_id,
                   ri.source_sale_order_item_id,
                   SUM(ri.qty) AS returned_qty
            FROM erp_sale_return r
            JOIN erp_sale_return_item ri
              ON ri.return_id = r.id
             AND ri.tenant_id = r.tenant_id
             AND ri.deleted_at IS NULL
            WHERE r.tenant_id = #{tenantId}
              AND r.deleted_at IS NULL
              AND r.status = 'APPROVED'
              AND ri.source_sale_order_item_id IS NOT NULL
            GROUP BY COALESCE(ri.source_sale_order_id, r.sale_order_id), ri.source_sale_order_item_id
        ),
        draft_return AS (
            SELECT COALESCE(ri.source_sale_order_id, r.sale_order_id) AS sale_order_id,
                   ri.source_sale_order_item_id,
                   SUM(ri.qty) AS draft_qty
            FROM erp_sale_return r
            JOIN erp_sale_return_item ri
              ON ri.return_id = r.id
             AND ri.tenant_id = r.tenant_id
             AND ri.deleted_at IS NULL
            WHERE r.tenant_id = #{tenantId}
              AND r.deleted_at IS NULL
              AND r.status = 'DRAFT'
              AND ri.source_sale_order_item_id IS NOT NULL
              <if test='currentReturnId != null'>AND r.id != #{currentReturnId}</if>
            GROUP BY COALESCE(ri.source_sale_order_id, r.sale_order_id), ri.source_sale_order_item_id
        )
        SELECT o.id,
               o.order_no AS orderNo,
               o.customer_id AS customerId,
               o.order_at AS orderAt
        FROM erp_sale_order o
        WHERE o.tenant_id = #{tenantId}
          AND o.deleted_at IS NULL
          AND o.status = 'APPROVED'
          <if test='customerId != null'>AND o.customer_id = #{customerId}</if>
          <if test='keyword != null and keyword != ""'>AND o.order_no LIKE CONCAT('%', #{keyword}, '%')</if>
          AND EXISTS (
              SELECT 1
              FROM erp_sale_order_item i
              LEFT JOIN approved_return ar
                ON ar.sale_order_id = o.id
               AND ar.source_sale_order_item_id = i.id
              LEFT JOIN draft_return dr
                ON dr.sale_order_id = o.id
               AND dr.source_sale_order_item_id = i.id
              WHERE i.tenant_id = o.tenant_id
                AND i.order_id = o.id
                AND i.deleted_at IS NULL
                AND GREATEST(i.qty - COALESCE(ar.returned_qty, 0) - COALESCE(dr.draft_qty, 0), 0) &gt; 0
          )
        ORDER BY o.order_at DESC NULLS LAST, o.id DESC
        LIMIT #{size} OFFSET #{offset}
        </script>
        """)
    List<ErpSaleReturnSourceSaleOrderOption> findReturnableSourceOrdersPage(@Param("tenantId") Long tenantId,
                                                                            @Param("customerId") Long customerId,
                                                                            @Param("keyword") String keyword,
                                                                            @Param("currentReturnId") Long currentReturnId,
                                                                            @Param("size") int size,
                                                                            @Param("offset") long offset);
}
