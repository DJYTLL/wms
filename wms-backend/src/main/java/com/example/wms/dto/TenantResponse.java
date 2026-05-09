package com.example.wms.dto;

import java.time.Instant;

// 租户响应
public record TenantResponse(
    Long id,
    String code,
    String name,
    boolean enabled,
    Instant createdAt,
    Instant updatedAt
) {
}
