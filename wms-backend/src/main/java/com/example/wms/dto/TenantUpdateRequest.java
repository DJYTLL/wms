package com.example.wms.dto;

import jakarta.validation.constraints.NotBlank;

// 租户名称更新请求
public record TenantUpdateRequest(@NotBlank String name) {
}
