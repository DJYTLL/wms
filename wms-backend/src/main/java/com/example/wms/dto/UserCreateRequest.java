package com.example.wms.dto;

import jakarta.validation.constraints.NotBlank;

// 新增用户请求
public record UserCreateRequest(
    @NotBlank String username,
    @NotBlank String password,
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
