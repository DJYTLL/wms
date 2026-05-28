package com.example.wms.mapper;

import com.example.wms.entity.UserTableSetting;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

// 用户表格配置 Mapper
@Mapper
public interface UserTableSettingMapper {
    @Select("""
        SELECT tenant_id, user_id, page_key, config_json::text AS config_json, updated_by, updated_at
        FROM app_user_table_setting
        WHERE tenant_id = #{tenantId} AND user_id = #{userId} AND page_key = #{pageKey}
        """)
    UserTableSetting findOne(@Param("tenantId") Long tenantId,
                             @Param("userId") Long userId,
                             @Param("pageKey") String pageKey);

    @Insert("""
        INSERT INTO app_user_table_setting (tenant_id, user_id, page_key, config_json, updated_by, updated_at)
        VALUES (#{tenantId}, #{userId}, #{pageKey}, #{configJson}::jsonb, #{updatedBy}, NOW())
        """)
    int insert(UserTableSetting setting);

    @Update("""
        UPDATE app_user_table_setting
        SET config_json = #{configJson}::jsonb, updated_by = #{updatedBy}, updated_at = NOW()
        WHERE tenant_id = #{tenantId} AND user_id = #{userId} AND page_key = #{pageKey}
        """)
    int update(UserTableSetting setting);
}
