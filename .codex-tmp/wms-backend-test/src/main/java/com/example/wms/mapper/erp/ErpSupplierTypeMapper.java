package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpSupplierType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

// 供应商类型 Mapper（ERP进销存）
@Mapper
public interface ErpSupplierTypeMapper extends BaseMapper<ErpSupplierType> {
    @Select("SELECT * FROM erp_supplier_type WHERE tenant_id = #{tenantId} AND code = #{code} AND deleted_at IS NULL")
    ErpSupplierType findByCode(@Param("tenantId") Long tenantId, @Param("code") String code);

    @Select("SELECT * FROM erp_supplier_type WHERE tenant_id = #{tenantId} AND name = #{name} AND deleted_at IS NULL")
    ErpSupplierType findByName(@Param("tenantId") Long tenantId, @Param("name") String name);
}
