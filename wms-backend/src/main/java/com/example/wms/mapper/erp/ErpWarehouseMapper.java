package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpWarehouse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

// 仓库 Mapper（ERP进销存）
@Mapper
public interface ErpWarehouseMapper extends BaseMapper<ErpWarehouse> {
    // 按编码查询
    @Select("SELECT * FROM erp_warehouse WHERE tenant_id = #{tenantId} AND code = #{code} AND deleted_at IS NULL")
    ErpWarehouse findByCode(@Param("tenantId") Long tenantId, @Param("code") String code);

    @Select("SELECT * FROM erp_warehouse WHERE tenant_id = #{tenantId} AND id = #{id} AND deleted_at IS NULL")
    ErpWarehouse findActiveById(@Param("tenantId") Long tenantId, @Param("id") Long id);
}
