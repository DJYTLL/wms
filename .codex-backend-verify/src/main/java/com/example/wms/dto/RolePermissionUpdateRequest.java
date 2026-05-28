package com.example.wms.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**

 * 角色权限用于接收更新操作的请求参数。

 */
public record RolePermissionUpdateRequest(
    /**
     * 表示权限 ID 列表。
     */
    @NotEmpty List<Long> permissionIds
) {
}
