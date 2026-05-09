package com.example.wms.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

// 新增租户请求
public record TenantCreateRequest(
    @NotBlank String code,
    @NotBlank String name,
    String adminUsername,
    String adminPassword,
    List<Long> menuIds
) {
}
