package com.example.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

// 用户 Mapper：查询用户基础信息
@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {
    // 根据用户名查询未删除用户
    @Select("SELECT * FROM app_user WHERE tenant_id = #{tenantId} AND username = #{username} AND deleted_at IS NULL")
    UserAccount findActiveByUsername(@Param("tenantId") Long tenantId, @Param("username") String username);

    // 按用户名查询（全局唯一方案）
    @Select("SELECT * FROM app_user WHERE username = #{username} AND deleted_at IS NULL")
    List<UserAccount> findActiveByUsernameGlobal(@Param("username") String username);

    // 根据 ID 查询未删除用户
    @Select("SELECT * FROM app_user WHERE tenant_id = #{tenantId} AND id = #{id} AND deleted_at IS NULL")
    UserAccount findActiveById(@Param("tenantId") Long tenantId, @Param("id") Long id);

    // 更新用户密码哈希
    @Update("""
        UPDATE app_user
        SET password_hash = #{passwordHash},
            updated_at = NOW()
        WHERE tenant_id = #{tenantId}
          AND id = #{id}
        """)
    int updatePasswordHash(@Param("tenantId") Long tenantId,
                           @Param("id") Long id,
                           @Param("passwordHash") String passwordHash);

    // 查询用户权限版本
    @Select("SELECT auth_version FROM app_user WHERE tenant_id = #{tenantId} AND username = #{username} AND deleted_at IS NULL")
    Long findAuthVersionByUsername(@Param("tenantId") Long tenantId, @Param("username") String username);

    // 权限版本自增（单用户）
    @Update("""
        UPDATE app_user
        SET auth_version = auth_version + 1,
            updated_at = NOW()
        WHERE tenant_id = #{tenantId}
          AND id = #{id}
        """)
    int incrementAuthVersionById(@Param("tenantId") Long tenantId, @Param("id") Long id);

    // 权限版本自增（批量）
    @Update("""
        <script>
        UPDATE app_user
        SET auth_version = auth_version + 1,
            updated_at = NOW()
        WHERE tenant_id = #{tenantId}
          AND id IN
        <foreach item="id" collection="ids" open="(" separator="," close=")">
            #{id}
        </foreach>
        </script>
        """)
    int incrementAuthVersionByIds(@Param("tenantId") Long tenantId, @Param("ids") List<Long> ids);

    // 软删除用户
    @Update("""
        UPDATE app_user
        SET deleted_at = NOW(),
            updated_at = NOW()
        WHERE tenant_id = #{tenantId}
          AND id = #{id}
        """)
    int softDelete(@Param("tenantId") Long tenantId, @Param("id") Long id);

    // 更新最近登录时间
    @Update("""
        UPDATE app_user
        SET last_login_at = NOW(),
            updated_at = NOW()
        WHERE tenant_id = #{tenantId}
          AND id = #{id}
        """)
    int updateLastLoginAt(@Param("tenantId") Long tenantId, @Param("id") Long id);
}
