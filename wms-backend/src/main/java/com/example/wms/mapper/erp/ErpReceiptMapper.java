package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpReceipt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

// ERP收款单 Mapper
@Mapper
public interface ErpReceiptMapper extends BaseMapper<ErpReceipt> {
    @Select("SELECT * FROM erp_receipt WHERE tenant_id = #{tenantId} AND sale_order_id = #{saleOrderId} LIMIT 1")
    ErpReceipt findBySaleOrderId(@Param("tenantId") Long tenantId, @Param("saleOrderId") Long saleOrderId);
}
