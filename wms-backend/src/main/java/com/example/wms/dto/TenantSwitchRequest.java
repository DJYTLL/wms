package com.example.wms.dto;

import jakarta.validation.constraints.NotBlank;

// 切换租户请求
public record TenantSwitchRequest(@NotBlank String tenantCode) {
}
