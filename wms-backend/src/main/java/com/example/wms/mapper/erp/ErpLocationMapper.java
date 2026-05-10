package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpLocation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

// 库位 Mapper（ERP进销存）
@Mapper
public interface ErpLocationMapper extends BaseMapper<ErpLocation> {
    // 按编码查询
    @Select("SELECT * FROM erp_location WHERE tenant_id = #{tenantId} AND warehouse_id = #{warehouseId} AND code = #{code} AND deleted_at IS NULL")
    ErpLocation findByCode(@Param("tenantId") Long tenantId,
                           @Param("warehouseId") Long warehouseId,
                           @Param("code") String code);
}
