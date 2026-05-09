package com.example.wms.dto;

import java.util.List;

// 菜单节点响应
public record MenuResponse(
    Long id,
    String key,
    String title,
    String path,
    String icon,
    List<MenuResponse> children
) {
}
