package com.example.wms.dto;

import java.time.Instant;

// 租户展示默认配置响应
public record TenantDisplaySettingsResponse(
    Integer defaultPageSize,
    String updatedBy,
    Instant updatedAt
) {
}
