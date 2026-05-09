package com.example.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

// 权限 Mapper：查询角色权限
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {
    // 按权限编码查询
    @Select("SELECT * FROM app_permission WHERE code = #{code}")
    Permission findByCode(@Param("code") String code);

    // 根据角色列表批量查询权限
    @Select("""
        <script>
        SELECT p.*
        FROM app_permission p
        JOIN app_role_permission rp ON p.id = rp.permission_id
        WHERE rp.role_id IN
        <foreach item="id" collection="roleIds" open="(" separator="," close=")">
            #{id}
        </foreach>
          AND p.is_enabled = TRUE
        </script>
        """)
    List<Permission> findByRoleIds(@Param("roleIds") List<Long> roleIds);

    // 按权限编码前缀查询
    @Select("""
        SELECT *
        FROM app_permission
        WHERE code LIKE CONCAT(#{prefix}, '%')
          AND is_enabled = TRUE
        ORDER BY id
        """)
    List<Permission> findByCodePrefix(@Param("prefix") String prefix);
}
