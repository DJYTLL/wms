package com.example.wms.dto;

import jakarta.validation.constraints.NotBlank;

// 更新权限请求
public record PermissionUpdateRequest(
    @NotBlank String code,
    @NotBlank String name,
    String description,
    Boolean enabled
) {
}
