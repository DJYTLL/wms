package com.example.wms.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

// 角色权限批量设置请求
public record RolePermissionUpdateRequest(
    @NotEmpty List<Long> permissionIds
) {
}
