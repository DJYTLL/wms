package com.example.wms.dto;

import java.time.Instant;

// 系统配置响应
public record SystemConfigResponse(
    Long id,
    String key,
    String value,
    String valueType,
    String description,
    boolean isPublic,
    Instant createdAt,
    Instant updatedAt
) {
}
