package com.example.wms.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

// 租户菜单更新请求
public record TenantMenuUpdateRequest(@NotNull List<Long> menuIds) {
}
