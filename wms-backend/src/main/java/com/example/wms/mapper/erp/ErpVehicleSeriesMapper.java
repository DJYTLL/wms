package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpVehicleSeries;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

// 车型车系 Mapper（ERP进销存）
@Mapper
public interface ErpVehicleSeriesMapper extends BaseMapper<ErpVehicleSeries> {
    @Select("SELECT * FROM erp_vehicle_series WHERE tenant_id = #{tenantId} AND brand_id = #{brandId} AND code = #{code}")
    ErpVehicleSeries findByCode(@Param("tenantId") Long tenantId,
                                @Param("brandId") Long brandId,
                                @Param("code") String code);
}
