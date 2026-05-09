package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpPaymentMethod;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

// 付款方式 Mapper（ERP进销存）
@Mapper
public interface ErpPaymentMethodMapper extends BaseMapper<ErpPaymentMethod> {
    // 按编码查询
    @Select("SELECT * FROM erp_payment_method WHERE tenant_id = #{tenantId} AND code = #{code}")
    ErpPaymentMethod findByCode(@Param("tenantId") Long tenantId, @Param("code") String code);

    // 查询默认付款方式
    @Select("SELECT * FROM erp_payment_method WHERE tenant_id = #{tenantId} AND is_default = true LIMIT 1")
    ErpPaymentMethod findDefault(@Param("tenantId") Long tenantId);
}
