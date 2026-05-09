package com.example.wms.dto;

import java.util.List;

// 租户菜单节点响应（包含启用状态）
public record TenantMenuResponse(
    Long id,
    String key,
    String title,
    String path,
    String icon,
    boolean enabled,
    List<TenantMenuResponse> children
) {
}
