package com.example.wms.dto;

// 当前用户生效列表偏好响应
public record EffectiveListPreferencesResponse(
    Integer pageSize,
    String source
) {
}
