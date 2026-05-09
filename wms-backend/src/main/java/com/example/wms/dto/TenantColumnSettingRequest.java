package com.example.wms.dto;

import java.util.List;

// 租户列配置请求
public record TenantColumnSettingRequest(
    List<String> visibleColumns
) {
}
