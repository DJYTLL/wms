package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpPaymentPayable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

// ERP付款单-应付分摊 Mapper
@Mapper
public interface ErpPaymentPayableMapper extends BaseMapper<ErpPaymentPayable> {
    @Select("SELECT * FROM erp_payment_payable WHERE tenant_id = #{tenantId} AND payment_id = #{paymentId}")
    List<ErpPaymentPayable> findByPaymentId(@Param("tenantId") Long tenantId, @Param("paymentId") Long paymentId);

    @Select("SELECT * FROM erp_payment_payable WHERE tenant_id = #{tenantId} AND payable_id = #{payableId}")
    List<ErpPaymentPayable> findByPayableId(@Param("tenantId") Long tenantId, @Param("payableId") Long payableId);
}
