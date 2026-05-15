package com.example.wms.dto;

import java.util.List;

/**

 * 租户菜单用于返回接口响应数据。

 */
public record TenantMenuResponse(
    /**
     * 表示数据的主键 ID。
     */
    Long id,
    /**
     * 表示键名。
     */
    String key,
    /**
     * 表示标题。
     */
    String title,
    /**
     * 表示路径。
     */
    String path,
    /**
     * 表示图标。
     */
    String icon,
    /**
     * 表示是否启用。
     */
    boolean enabled,
    /**
     * 表示子节点。
     */
    List<TenantMenuResponse> children
) {
}
