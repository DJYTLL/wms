package com.example.wms.dto;

// 系统配置请求
public record SystemConfigRequest(
    String value,
    String valueType,
    String description,
    Boolean isPublic
) {
}
