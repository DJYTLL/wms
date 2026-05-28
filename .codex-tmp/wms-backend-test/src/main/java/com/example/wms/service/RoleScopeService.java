package com.example.wms.service;

import com.example.wms.entity.Role;
import com.example.wms.entity.Permission;
import org.springframework.stereotype.Service;

import java.util.Set;

// 兼容旧调用点的角色范围门面；核心规则集中在 RolePolicyService。
@Service
public class RoleScopeService {
    private final RolePolicyService rolePolicyService;

    public RoleScopeService(RolePolicyService rolePolicyService) {
        this.rolePolicyService = rolePolicyService;
    }

    public boolean canViewRole(Role role) {
        return rolePolicyService.canViewRole(role);
    }

    public boolean canManageRole(Role role) {
        return rolePolicyService.canManageRole(role);
    }

    public boolean isCurrentActorRole(Long roleId) {
        return rolePolicyService.isCurrentActorRole(roleId);
    }

    public boolean rolePermissionsWithinActorScope(Long roleId) {
        return rolePolicyService.rolePermissionsWithinActorScope(roleId);
    }

    public boolean currentActorHasRole(String roleCode) {
        return rolePolicyService.currentActorHasRole(roleCode);
    }

    public Set<String> currentActorPermissionCodes() {
        return rolePolicyService.currentActorPermissionCodes();
    }

    public boolean canAssignPermissionToRole(Role role, Permission permission) {
        return rolePolicyService.canAssignPermissionToRole(role, permission);
    }
}
