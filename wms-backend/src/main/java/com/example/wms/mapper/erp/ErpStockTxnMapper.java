package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.dto.erp.ErpIdAmountPair;
import com.example.wms.dto.erp.ErpSaleOrderItemCostSnapshot;
import com.example.wms.entity.erp.ErpStockTxn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

// 库存流水 Mapper（ERP进销存）
@Mapper
public interface ErpStockTxnMapper extends BaseMapper<ErpStockTxn> {
    // 按流水号查询
    @Select("SELECT * FROM erp_stock_txn WHERE tenant_id = #{tenantId} AND txn_no = #{txnNo}")
    ErpStockTxn findByTxnNo(@Param("tenantId") Long tenantId, @Param("txnNo") String txnNo);

    @Select("""
        SELECT COALESCE(
            SUM(ABS(total_cost)) / NULLIF(SUM(ABS(qty_delta)), 0),
            0
        )
        FROM erp_stock_txn
        WHERE tenant_id = #{tenantId}
          AND biz_type = 'SALE_APPROVE'
          AND biz_id = #{saleOrderId}
          AND product_id = #{productId}
        """)
    BigDecimal findSaleIssueUnitCost(@Param("tenantId") Long tenantId,
                                     @Param("saleOrderId") Long saleOrderId,
                                     @Param("productId") Long productId);

    @Select("""
        SELECT COALESCE(
            SUM(ABS(total_cost)) / NULLIF(SUM(ABS(qty_delta)), 0),
            0
        )
        FROM erp_stock_txn
        WHERE tenant_id = #{tenantId}
          AND biz_type = 'PURCHASE_RETURN'
          AND biz_id = #{purchaseReturnId}
          AND product_id = #{productId}
        """)
    BigDecimal findPurchaseReturnIssueUnitCost(@Param("tenantId") Long tenantId,
                                               @Param("purchaseReturnId") Long purchaseReturnId,
                                               @Param("productId") Long productId);

    @Select("""
        SELECT COALESCE(SUM(ABS(total_cost)), 0)
        FROM erp_stock_txn
        WHERE tenant_id = #{tenantId}
          AND biz_type = 'SALE_APPROVE'
          AND biz_id = #{saleOrderId}
        """)
    BigDecimal sumSaleIssueCost(@Param("tenantId") Long tenantId,
                                @Param("saleOrderId") Long saleOrderId);

    @Select("""
        SELECT COALESCE(SUM(ABS(total_cost)), 0)
        FROM erp_stock_txn
        WHERE tenant_id = #{tenantId}
          AND biz_type = 'SALE_RETURN_RESTOCK'
          AND biz_id IN (
              SELECT id
              FROM erp_sale_return
              WHERE tenant_id = #{tenantId}
                AND sale_order_id = #{saleOrderId}
                AND status = 'APPROVED'
                AND deleted_at IS NULL
          )
        """)
    BigDecimal sumApprovedSaleReturnCost(@Param("tenantId") Long tenantId,
                                         @Param("saleOrderId") Long saleOrderId);

    @Select("""
        <script>
        SELECT biz_id AS id,
               COALESCE(SUM(ABS(total_cost)), 0) AS amount
        FROM erp_stock_txn
        WHERE tenant_id = #{tenantId}
          AND biz_type = 'SALE_APPROVE'
          AND biz_id IN
          <foreach collection='saleOrderIds' item='saleOrderId' open='(' separator=',' close=')'>
            #{saleOrderId}
          </foreach>
        GROUP BY biz_id
        </script>
        """)
    List<ErpIdAmountPair> sumSaleIssueCostsBySaleOrderIds(@Param("tenantId") Long tenantId,
                                                          @Param("saleOrderIds") List<Long> saleOrderIds);

    @Select("""
        <script>
        SELECT r.sale_order_id AS id,
               COALESCE(SUM(ABS(txn.total_cost)), 0) AS amount
        FROM erp_stock_txn txn
        JOIN erp_sale_return r
          ON r.id = txn.biz_id
         AND r.tenant_id = txn.tenant_id
        WHERE txn.tenant_id = #{tenantId}
          AND txn.biz_type = 'SALE_RETURN_RESTOCK'
          AND r.status = 'APPROVED'
          AND r.deleted_at IS NULL
          AND r.sale_order_id IN
          <foreach collection='saleOrderIds' item='saleOrderId' open='(' separator=',' close=')'>
            #{saleOrderId}
          </foreach>
        GROUP BY r.sale_order_id
        </script>
        """)
    List<ErpIdAmountPair> sumApprovedSaleReturnCostsBySaleOrderIds(@Param("tenantId") Long tenantId,
                                                                   @Param("saleOrderIds") List<Long> saleOrderIds);

    @Select("""
        SELECT biz_item_id AS bizItemId,
               COALESCE(
                   SUM(ABS(total_cost)) / NULLIF(SUM(ABS(qty_delta)), 0),
                   0
               ) AS unitCost
        FROM erp_stock_txn
        WHERE tenant_id = #{tenantId}
          AND biz_type = 'SALE_APPROVE'
          AND biz_id = #{saleOrderId}
          AND biz_item_id IS NOT NULL
        GROUP BY biz_item_id
        """)
    List<ErpSaleOrderItemCostSnapshot> findSaleItemCostSnapshots(@Param("tenantId") Long tenantId,
                                                                 @Param("saleOrderId") Long saleOrderId);

    @Select("""
        <script>
        SELECT EXISTS (
            SELECT 1
            FROM erp_stock_txn txn
            JOIN erp_stock_count_item item
              ON item.tenant_id = txn.tenant_id
             AND item.count_id = #{countId}
             AND item.deleted_at IS NULL
             AND item.product_id = txn.product_id
             AND (
                    (item.warehouse_id = txn.warehouse_id)
                 OR (item.warehouse_id IS NULL AND txn.warehouse_id IS NULL)
             )
             AND (
                    (item.location_id = txn.location_id)
                 OR (item.location_id IS NULL AND txn.location_id IS NULL)
             )
            WHERE txn.tenant_id = #{tenantId}
              AND txn.created_at > #{approvedAt}
              AND NOT (txn.biz_type = 'STOCK_INIT' AND txn.biz_id = #{countId})
        )
        </script>
        """)
    boolean existsLaterTxnForInit(@Param("tenantId") Long tenantId,
                                  @Param("countId") Long countId,
                                  @Param("approvedAt") Instant approvedAt);
}
