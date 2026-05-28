package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpVehicleModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

// 车型 Mapper（ERP进销存）
@Mapper
public interface ErpVehicleModelMapper extends BaseMapper<ErpVehicleModel> {
    @Select("SELECT * FROM erp_vehicle_model WHERE tenant_id = #{tenantId} AND series_id = #{seriesId} AND code = #{code} AND deleted_at IS NULL")
    ErpVehicleModel findByCode(@Param("tenantId") Long tenantId,
                               @Param("seriesId") Long seriesId,
                               @Param("code") String code);
}
