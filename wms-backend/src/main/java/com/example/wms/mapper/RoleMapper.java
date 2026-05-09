package com.example.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

// 角色 Mapper：查询用户角色
@Mapper
public interface RoleMapper extends BaseMapper<Role> {
    // 按角色编码查询
    @Select("SELECT * FROM app_role WHERE tenant_id = #{tenantId} AND code = #{code}")
    Role findByCode(@Param("tenantId") Long tenantId, @Param("code") String code);

    // 查询用户拥有的角色
    @Select("""
        SELECT r.*
        FROM app_role r
        JOIN app_user_role ur ON r.id = ur.role_id
        WHERE ur.tenant_id = #{tenantId}
          AND ur.user_id = #{userId}
          AND r.tenant_id = #{tenantId}
          AND r.is_enabled = TRUE
        """)
    List<Role> findByUserId(@Param("tenantId") Long tenantId, @Param("userId") Long userId);
}
