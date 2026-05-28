package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

// 分类 Mapper（ERP进销存）
@Mapper
public interface ErpCategoryMapper extends BaseMapper<ErpCategory> {
    // 按编码查询
    @Select("SELECT * FROM erp_category WHERE tenant_id = #{tenantId} AND code = #{code} AND deleted_at IS NULL")
    ErpCategory findByCode(@Param("tenantId") Long tenantId, @Param("code") String code);
}
