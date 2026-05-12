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
}
