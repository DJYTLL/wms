package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpAssemblyOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

// Assembly order mapper
@Mapper
public interface ErpAssemblyOrderMapper extends BaseMapper<ErpAssemblyOrder> {
    @Select("SELECT * FROM erp_assembly_order WHERE tenant_id = #{tenantId} AND order_no = #{orderNo} AND deleted_at IS NULL")
    ErpAssemblyOrder findByOrderNo(@Param("tenantId") Long tenantId, @Param("orderNo") String orderNo);

    @Select("SELECT * FROM erp_assembly_order WHERE tenant_id = #{tenantId} AND id = #{id} AND deleted_at IS NULL FOR UPDATE")
    ErpAssemblyOrder findByIdForUpdate(@Param("tenantId") Long tenantId, @Param("id") Long id);
}
