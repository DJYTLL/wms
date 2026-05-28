package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpDeliveryMethod;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

// 送货方式 Mapper（ERP进销存）
@Mapper
public interface ErpDeliveryMethodMapper extends BaseMapper<ErpDeliveryMethod> {
    // 按编码查询
    @Select("SELECT * FROM erp_delivery_method WHERE tenant_id = #{tenantId} AND code = #{code} AND deleted_at IS NULL")
    ErpDeliveryMethod findByCode(@Param("tenantId") Long tenantId, @Param("code") String code);

    // 查询默认送货方式
    @Select("SELECT * FROM erp_delivery_method WHERE tenant_id = #{tenantId} AND is_default = true AND deleted_at IS NULL LIMIT 1")
    ErpDeliveryMethod findDefault(@Param("tenantId") Long tenantId);
}
