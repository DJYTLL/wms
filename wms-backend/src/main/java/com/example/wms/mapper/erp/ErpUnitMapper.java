package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpUnit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

// 单位 Mapper（ERP进销存）
@Mapper
public interface ErpUnitMapper extends BaseMapper<ErpUnit> {
    // 按编码查询
    @Select("SELECT * FROM erp_unit WHERE tenant_id = #{tenantId} AND code = #{code}")
    ErpUnit findByCode(@Param("tenantId") Long tenantId, @Param("code") String code);
}
