package com.example.wms.dto;

import java.time.Instant;
import java.util.List;

// 租户列配置响应
public record TenantColumnSettingResponse(
    String pageKey,
    List<String> visibleColumns,
    String updatedBy,
    Instant updatedAt
) {
}
