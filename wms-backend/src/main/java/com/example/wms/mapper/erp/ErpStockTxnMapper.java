package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpStockTxn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

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
}
