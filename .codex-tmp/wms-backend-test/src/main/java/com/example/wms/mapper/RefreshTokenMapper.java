package com.example.wms.mapper;

import com.example.wms.entity.RefreshToken;
import org.apache.ibatis.annotations.*;

// 刷新令牌 Mapper
@Mapper
public interface RefreshTokenMapper {
    // 插入刷新令牌
    @Insert("""
        INSERT INTO app_refresh_token (tenant_id, user_id, audience_tenant_id, token_hash, expires_at, created_at, updated_at)
        VALUES (#{tenantId}, #{userId}, #{audienceTenantId}, #{tokenHash}, #{expiresAt}, NOW(), NOW())
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RefreshToken token);

    // 按哈希查询
    @Select("""
        SELECT id,
               tenant_id,
               user_id,
               audience_tenant_id,
               token_hash,
               expires_at,
               revoked_at,
               created_at,
               updated_at
        FROM app_refresh_token
        WHERE token_hash = #{tokenHash}
        """)
    RefreshToken findByTokenHash(String tokenHash);

    // 标记为撤销
    @Update("""
        UPDATE app_refresh_token
        SET revoked_at = NOW(),
            updated_at = NOW()
        WHERE id = #{id}
        """)
    int revokeById(Long id);

    // 撤销用户全部刷新令牌
    @Update("""
        UPDATE app_refresh_token
        SET revoked_at = NOW(),
            updated_at = NOW()
        WHERE tenant_id = #{tenantId}
          AND user_id = #{userId}
          AND revoked_at IS NULL
        """)
    int revokeByUserId(@Param("tenantId") Long tenantId, @Param("userId") Long userId);
}
