package com.example.wms.mapper;

import com.example.wms.entity.RoleColumnSetting;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

// 角色列配置 Mapper
@Mapper
public interface RoleColumnSettingMapper {
    @Select("""
        SELECT tenant_id, role_id, page_key, visible_columns, updated_by, updated_at
        FROM app_role_column_setting
        WHERE tenant_id = #{tenantId} AND role_id = #{roleId} AND page_key = #{pageKey}
        """)
    RoleColumnSetting findOne(@Param("tenantId") Long tenantId,
                              @Param("roleId") Long roleId,
                              @Param("pageKey") String pageKey);

    @Insert("""
        INSERT INTO app_role_column_setting (tenant_id, role_id, page_key, visible_columns, updated_by, updated_at)
        VALUES (#{tenantId}, #{roleId}, #{pageKey}, #{visibleColumns}, #{updatedBy}, NOW())
        """)
    int insert(RoleColumnSetting setting);

    @Update("""
        UPDATE app_role_column_setting
        SET visible_columns = #{visibleColumns}, updated_by = #{updatedBy}, updated_at = NOW()
        WHERE tenant_id = #{tenantId} AND role_id = #{roleId} AND page_key = #{pageKey}
        """)
    int update(RoleColumnSetting setting);
}
