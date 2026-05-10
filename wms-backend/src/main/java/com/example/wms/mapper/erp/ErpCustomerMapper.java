package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpCustomer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

// 客户 Mapper（ERP进销存）
@Mapper
public interface ErpCustomerMapper extends BaseMapper<ErpCustomer> {
    // 按编码查询
    @Select("SELECT * FROM erp_customer WHERE tenant_id = #{tenantId} AND code = #{code} AND deleted_at IS NULL")
    ErpCustomer findByCode(@Param("tenantId") Long tenantId, @Param("code") String code);
}
