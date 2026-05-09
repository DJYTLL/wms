package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpSupplier;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

// 供应商 Mapper（ERP进销存）
@Mapper
public interface ErpSupplierMapper extends BaseMapper<ErpSupplier> {
    // 按编码查询
    @Select("SELECT * FROM erp_supplier WHERE tenant_id = #{tenantId} AND code = #{code}")
    ErpSupplier findByCode(@Param("tenantId") Long tenantId, @Param("code") String code);
}
