package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpVehicleBrand;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

// 车型品牌 Mapper（ERP进销存）
@Mapper
public interface ErpVehicleBrandMapper extends BaseMapper<ErpVehicleBrand> {
    @Select("SELECT * FROM erp_vehicle_brand WHERE tenant_id = #{tenantId} AND code = #{code}")
    ErpVehicleBrand findByCode(@Param("tenantId") Long tenantId, @Param("code") String code);
}
