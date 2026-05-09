package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpStockTxn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

// 库存流水 Mapper（ERP进销存）
@Mapper
public interface ErpStockTxnMapper extends BaseMapper<ErpStockTxn> {
    // 按流水号查询
    @Select("SELECT * FROM erp_stock_txn WHERE tenant_id = #{tenantId} AND txn_no = #{txnNo}")
    ErpStockTxn findByTxnNo(@Param("tenantId") Long tenantId, @Param("txnNo") String txnNo);
}
