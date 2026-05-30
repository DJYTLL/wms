package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpLocation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

// 库位 Mapper（ERP进销存）
@Mapper
public interface ErpLocationMapper extends BaseMapper<ErpLocation> {
    // 按编码查询
    @Select("SELECT * FROM erp_location WHERE tenant_id = #{tenantId} AND warehouse_id = #{warehouseId} AND code = #{code} AND deleted_at IS NULL")
    ErpLocation findByCode(@Param("tenantId") Long tenantId,
                           @Param("warehouseId") Long warehouseId,
                           @Param("code") String code);

    @Select("SELECT * FROM erp_location WHERE tenant_id = #{tenantId} AND id = #{id} AND deleted_at IS NULL")
    ErpLocation findActiveById(@Param("tenantId") Long tenantId, @Param("id") Long id);

    @Select("SELECT * FROM erp_location WHERE tenant_id = #{tenantId} AND warehouse_id = #{warehouseId} AND is_default = TRUE AND deleted_at IS NULL ORDER BY id LIMIT 1")
    ErpLocation findDefault(@Param("tenantId") Long tenantId, @Param("warehouseId") Long warehouseId);

    @Update("""
        UPDATE erp_location
        SET is_default = FALSE, updated_at = NOW()
        WHERE tenant_id = #{tenantId}
          AND warehouse_id = #{warehouseId}
          AND (#{locationId} IS NULL OR id <> #{locationId})
          AND is_default = TRUE
          AND deleted_at IS NULL
        """)
    int clearDefault(@Param("tenantId") Long tenantId,
                     @Param("warehouseId") Long warehouseId,
                     @Param("locationId") Long locationId);
}
