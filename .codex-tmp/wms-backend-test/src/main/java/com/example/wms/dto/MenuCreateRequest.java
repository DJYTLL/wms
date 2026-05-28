package com.example.wms.dto;

import jakarta.validation.constraints.NotBlank;

/**

 * 菜单用于接收新增操作的请求参数。

 */
public record MenuCreateRequest(
    /**
     * 表示业务编码。
     */
    @NotBlank String code,
    /**
     * 表示父级 ID。
     */
    Long parentId,
    /**
     * 表示标题。
     */
    @NotBlank String title,
    /**
     * 表示国际化键名。
     */
    String i18nKey,
    /**
     * 表示路径。
     */
    String path,
    /**
     * 表示图标。
     */
    String icon,
    /**
     * 表示权限编码。
     */
    String permissionCode,
    /**
     * 表示排序。
     */
    Integer sort,
    /**
     * 表示是否启用。
     */
    Boolean enabled
) {
}
