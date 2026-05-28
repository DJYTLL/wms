package com.example.wms.dto;

import java.time.Instant;

// 当前用户列表偏好响应
public record MyListPreferencesResponse(
    Integer pageSize,
    Instant updatedAt
) {
}
