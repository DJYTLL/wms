package com.example.wms.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 角色列权限更新请求。
 */
public record RoleColumnPermissionUpdateRequest(
    /**
     * 页面标识。为空时兼容旧的全量列权限替换语义。
     */
    String pageKey,

    /**
     * 当前页面选中的列权限 ID，允许为空以清空当前页列权限。
     */
    @NotNull List<Long> permissionIds
) {
}
