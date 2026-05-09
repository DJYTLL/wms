package com.example.wms.dto;

import jakarta.validation.constraints.NotBlank;

// 更新用户请求
public record UserUpdateRequest(
    @NotBlank String username,
    String displayName,
    String email,
    String phone,
    String avatarUrl,
    Boolean enabled,
    Boolean accountNonExpired,
    Boolean accountNonLocked,
    Boolean credentialsNonExpired
) {
}
