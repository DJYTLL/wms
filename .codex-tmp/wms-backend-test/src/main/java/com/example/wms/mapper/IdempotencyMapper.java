package com.example.wms.mapper;

import com.example.wms.entity.IdempotencyRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

// 幂等记录 Mapper
@Mapper
public interface IdempotencyMapper {
    @Select("""
        SELECT *
        FROM app_idempotency
        WHERE idempotency_key = #{key}
          AND expires_at > NOW()
          AND deleted_at IS NULL
        """)
    IdempotencyRecord findValid(@Param("key") String key);

    @Update("""
        UPDATE app_idempotency
        SET deleted_at = NOW()
        WHERE idempotency_key = #{key} AND expires_at <= NOW()
          AND deleted_at IS NULL
        """)
    int deleteExpired(@Param("key") String key);

    @Insert("""
        INSERT INTO app_idempotency (idempotency_key, method, path, tenant_id, username, created_at, expires_at)
        VALUES (#{idempotencyKey}, #{method}, #{path}, #{tenantId}, #{username}, NOW(), #{expiresAt})
        """)
    int insert(IdempotencyRecord record);
}
