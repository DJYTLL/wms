package com.example.wms.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**

 * 租户菜单用于接收更新操作的请求参数。

 */
public record TenantMenuUpdateRequest(
    /**
     * 表示菜单 ID 列表。
     */
    @NotNull List<Long> menuIds
) {
}
