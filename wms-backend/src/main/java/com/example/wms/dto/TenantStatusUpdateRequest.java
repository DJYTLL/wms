package com.example.wms.dto;

import jakarta.validation.constraints.NotNull;

// 租户状态更新请求
public record TenantStatusUpdateRequest(@NotNull Boolean enabled) {
}
