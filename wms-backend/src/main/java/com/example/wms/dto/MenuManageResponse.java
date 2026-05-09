package com.example.wms.dto;

import java.util.List;

// 菜单管理响应
public record MenuManageResponse(
    Long id,
    String code,
    Long parentId,
    String title,
    String i18nKey,
    String path,
    String icon,
    String permissionCode,
    Integer sort,
    boolean enabled,
    List<MenuManageResponse> children
) {
}
