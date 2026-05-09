package com.example.wms.dto;

import java.time.Instant;

// 用户响应数据
public record UserResponse(
    Long id,
    String username,
    String displayName,
    String email,
    String phone,
    String avatarUrl,
    boolean enabled,
    boolean accountNonExpired,
    boolean accountNonLocked,
    boolean credentialsNonExpired,
    Instant lastLoginAt,
    Instant createdAt,
    Instant updatedAt
) {
}
