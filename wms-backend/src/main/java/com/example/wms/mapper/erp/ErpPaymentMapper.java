package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpPayment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

// ERP付款单 Mapper
@Mapper
public interface ErpPaymentMapper extends BaseMapper<ErpPayment> {
    @Select("SELECT * FROM erp_payment WHERE tenant_id = #{tenantId} AND purchase_order_id = #{purchaseOrderId} LIMIT 1")
    ErpPayment findByPurchaseOrderId(@Param("tenantId") Long tenantId, @Param("purchaseOrderId") Long purchaseOrderId);
}
