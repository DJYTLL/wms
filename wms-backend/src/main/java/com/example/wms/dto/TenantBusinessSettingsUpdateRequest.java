package com.example.wms.dto;

import java.util.Map;

// 租户业务配置更新请求
public record TenantBusinessSettingsUpdateRequest(
    Map<String, String> values
) {
}
