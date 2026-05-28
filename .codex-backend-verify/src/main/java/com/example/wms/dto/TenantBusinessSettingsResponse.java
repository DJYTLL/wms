package com.example.wms.dto;

import java.util.List;

// 租户业务配置响应
public record TenantBusinessSettingsResponse(
    List<TenantBusinessSettingItemResponse> codeRules,
    List<TenantBusinessSettingItemResponse> orderRules
) {
}
