package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpCustomerCategory;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

// 客户类别Mapper
public interface ErpCustomerCategoryMapper extends BaseMapper<ErpCustomerCategory> {
    @Select("SELECT * FROM erp_customer_category WHERE tenant_id = #{tenantId} AND code = #{code}")
    ErpCustomerCategory findByCode(@Param("tenantId") Long tenantId, @Param("code") String code);

    @Update("UPDATE erp_customer_category SET is_default = FALSE WHERE tenant_id = #{tenantId} AND is_default = TRUE AND id <> #{excludeId}")
    void clearDefault(@Param("tenantId") Long tenantId, @Param("excludeId") Long excludeId);

    @Select("SELECT * FROM erp_customer_category WHERE tenant_id = #{tenantId} AND is_default = TRUE LIMIT 1")
    ErpCustomerCategory findDefault(@Param("tenantId") Long tenantId);
}
