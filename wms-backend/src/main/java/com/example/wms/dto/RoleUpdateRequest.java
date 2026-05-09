package com.example.wms.dto;

import jakarta.validation.constraints.NotBlank;

// 更新角色请求
public record RoleUpdateRequest(
    @NotBlank String code,
    @NotBlank String name,
    String description,
    Boolean enabled
) {
}
