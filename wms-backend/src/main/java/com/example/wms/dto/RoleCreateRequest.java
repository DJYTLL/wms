package com.example.wms.dto;

import jakarta.validation.constraints.NotBlank;

// 新增角色请求
public record RoleCreateRequest(
    @NotBlank String code,
    @NotBlank String name,
    String description,
    Boolean enabled
) {
}
