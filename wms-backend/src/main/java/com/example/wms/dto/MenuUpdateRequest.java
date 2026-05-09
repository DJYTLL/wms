package com.example.wms.dto;

import jakarta.validation.constraints.NotBlank;

// 菜单更新请求
public record MenuUpdateRequest(
    @NotBlank String code,
    Long parentId,
    @NotBlank String title,
    String i18nKey,
    String path,
    String icon,
    String permissionCode,
    Integer sort,
    Boolean enabled
) {
}
