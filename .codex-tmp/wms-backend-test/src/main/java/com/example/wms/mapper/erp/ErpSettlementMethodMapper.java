package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpSettlementMethod;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

// 结算方式 Mapper（ERP进销存）
@Mapper
public interface ErpSettlementMethodMapper extends BaseMapper<ErpSettlementMethod> {
    // 按编码查询
    @Select("SELECT * FROM erp_settlement_method WHERE tenant_id = #{tenantId} AND code = #{code} AND deleted_at IS NULL")
    ErpSettlementMethod findByCode(@Param("tenantId") Long tenantId, @Param("code") String code);

    // 按名称查询
    @Select("SELECT * FROM erp_settlement_method WHERE tenant_id = #{tenantId} AND name = #{name} AND deleted_at IS NULL")
    ErpSettlementMethod findByName(@Param("tenantId") Long tenantId, @Param("name") String name);

    // 查询默认结算方式
    @Select("SELECT * FROM erp_settlement_method WHERE tenant_id = #{tenantId} AND is_default = true AND deleted_at IS NULL LIMIT 1")
    ErpSettlementMethod findDefault(@Param("tenantId") Long tenantId);
}
