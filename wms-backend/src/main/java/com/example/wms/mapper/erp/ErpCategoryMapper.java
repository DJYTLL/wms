package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

// 分类 Mapper（ERP进销存）
@Mapper
public interface ErpCategoryMapper extends BaseMapper<ErpCategory> {
    // 按编码查询
    @Select("SELECT * FROM erp_category WHERE tenant_id = #{tenantId} AND code = #{code} AND deleted_at IS NULL")
    ErpCategory findByCode(@Param("tenantId") Long tenantId, @Param("code") String code);

    @Select("SELECT * FROM erp_category WHERE tenant_id = #{tenantId} AND is_default = TRUE AND deleted_at IS NULL ORDER BY id LIMIT 1")
    ErpCategory findDefault(@Param("tenantId") Long tenantId);

    @Update("UPDATE erp_category SET is_default = FALSE, updated_at = NOW() WHERE tenant_id = #{tenantId} AND id <> #{categoryId} AND is_default = TRUE AND deleted_at IS NULL")
    int clearDefault(@Param("tenantId") Long tenantId, @Param("categoryId") Long categoryId);

    @Update("UPDATE erp_category SET is_default = FALSE, updated_at = NOW() WHERE tenant_id = #{tenantId} AND is_default = TRUE AND deleted_at IS NULL")
    int clearDefault(@Param("tenantId") Long tenantId);
}
