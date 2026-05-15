package com.example.wms.dto;

import java.time.Instant;

/**

 * 用户用于返回接口响应数据。

 */
public record UserResponse(
    /**
     * 表示数据的主键 ID。
     */
    Long id,
    /**
     * 表示用户登录名。
     */
    String username,
    /**
     * 表示用户显示名称。
     */
    String displayName,
    /**
     * 表示邮箱地址。
     */
    String email,
    /**
     * 表示联系电话。
     */
    String phone,
    /**
     * 表示头像地址。
     */
    String avatarUrl,
    /**
     * 表示是否启用。
     */
    boolean enabled,
    /**
     * 表示账户是否未过期。
     */
    boolean accountNonExpired,
    /**
     * 表示账户是否未锁定。
     */
    boolean accountNonLocked,
    /**
     * 表示登录凭证是否未过期。
     */
    boolean credentialsNonExpired,
    /**
     * 表示lastLogin时间。
     */
    Instant lastLoginAt,
    /**
     * 表示创建时间。
     */
    Instant createdAt,
    /**
     * 表示更新时间。
     */
    Instant updatedAt
) {
}
