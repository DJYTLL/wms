package com.example.wms.service;

import com.example.wms.entity.Permission;

import java.util.List;

// 角色权限服务接口
public interface RolePermissionService {
    // 查询角色的权限列表
    List<Permission> listPermissions(Long roleId);

    // 查询角色的列权限列表
    List<Permission> listColumnPermissions(Long roleId);

    // 批量设置角色权限
    void setPermissions(Long roleId, List<Long> permissionIds);

    // 批量设置角色列权限
    void setColumnPermissions(Long roleId, List<Long> permissionIds);

    // 追加单个权限
    void addPermission(Long roleId, Long permissionId);

    // 移除单个权限
    void removePermission(Long roleId, Long permissionId);
}
