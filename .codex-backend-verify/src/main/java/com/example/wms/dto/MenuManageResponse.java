package com.example.wms.dto;

import java.util.List;

/**

 * 菜单用于返回接口响应数据。

 */
public record MenuManageResponse(
    /**
     * 表示数据的主键 ID。
     */
    Long id,
    /**
     * 表示业务编码。
     */
    String code,
    /**
     * 表示父级 ID。
     */
    Long parentId,
    /**
     * 表示标题。
     */
    String title,
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
    boolean enabled,
    /**
     * 表示子节点。
     */
    List<MenuManageResponse> children
) {
}
