package com.example.wms.dto;

import jakarta.validation.constraints.NotBlank;

// 菜单新增请求
public record MenuCreateRequest(
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
