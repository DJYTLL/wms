package com.example.wms.dto;

// 租户业务配置项响应
public record TenantBusinessSettingItemResponse(
    String key,
    String label,
    String valueType,
    String value,
    String defaultValue,
    String description
) {
}
