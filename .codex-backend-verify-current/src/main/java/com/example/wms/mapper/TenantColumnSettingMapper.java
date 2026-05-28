package com.example.wms.mapper;

import com.example.wms.entity.TenantColumnSetting;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

// 租户列配置 Mapper
@Mapper
public interface TenantColumnSettingMapper {
    @Select("""
        SELECT tenant_id, page_key, visible_columns, updated_by, updated_at
        FROM app_tenant_column_setting
        WHERE tenant_id = #{tenantId} AND page_key = #{pageKey}
        """)
    TenantColumnSetting findOne(@Param("tenantId") Long tenantId,
                                @Param("pageKey") String pageKey);

    @Insert("""
        INSERT INTO app_tenant_column_setting (tenant_id, page_key, visible_columns, updated_by, updated_at)
        VALUES (#{tenantId}, #{pageKey}, #{visibleColumns}, #{updatedBy}, NOW())
        """)
    int insert(TenantColumnSetting setting);

    @Update("""
        UPDATE app_tenant_column_setting
        SET visible_columns = #{visibleColumns}, updated_by = #{updatedBy}, updated_at = NOW()
        WHERE tenant_id = #{tenantId} AND page_key = #{pageKey}
        """)
    int update(TenantColumnSetting setting);
}
