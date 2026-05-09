package com.example.wms.dto;

import jakarta.validation.constraints.NotBlank;

// 新增权限请求
public record PermissionCreateRequest(
    @NotBlank String code,
    @NotBlank String name,
    String description,
    Boolean enabled
) {
}
