package com.example.wms.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

// 用户-角色关联 Mapper：用于初始化或赋权
@Mapper
public interface UserRoleMapper {
    // 插入关联记录，冲突时忽略
    @Insert("""
        INSERT INTO app_user_role (tenant_id, user_id, role_id, created_at, updated_at)
        VALUES (#{tenantId}, #{userId}, #{roleId}, NOW(), NOW())
        ON CONFLICT DO NOTHING
        """)
    int insertIgnore(@Param("tenantId") Long tenantId,
                     @Param("userId") Long userId,
                     @Param("roleId") Long roleId);

    // 删除用户的全部角色
    @Update("""
        UPDATE app_user_role
        SET deleted_at = NOW(), updated_at = NOW()
        WHERE tenant_id = #{tenantId}
          AND user_id = #{userId}
          AND deleted_at IS NULL
        """)
    int deleteByUserId(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    // 查询角色下的用户 ID 列表
    @Select("""
        SELECT user_id
        FROM app_user_role
        WHERE tenant_id = #{tenantId}
          AND role_id = #{roleId}
          AND deleted_at IS NULL
        """)
    List<Long> findUserIdsByRoleId(@Param("tenantId") Long tenantId, @Param("roleId") Long roleId);

    // 统计角色下的用户数量
    @Select("""
        SELECT COUNT(1)
        FROM app_user_role
        WHERE tenant_id = #{tenantId}
          AND role_id = #{roleId}
          AND deleted_at IS NULL
        """)
    long countUsersByRoleId(@Param("tenantId") Long tenantId, @Param("roleId") Long roleId);

    // 删除角色关联的所有用户
    @Update("""
        UPDATE app_user_role
        SET deleted_at = NOW(), updated_at = NOW()
        WHERE tenant_id = #{tenantId}
          AND role_id = #{roleId}
          AND deleted_at IS NULL
        """)
    int deleteByRoleId(@Param("tenantId") Long tenantId, @Param("roleId") Long roleId);
}
