package com.example.wms.service;

import com.example.wms.dto.RoleCapabilitiesResponse;
import com.example.wms.entity.Permission;
import com.example.wms.entity.Role;
import com.example.wms.entity.UserAccount;
import com.example.wms.mapper.RolePermissionMapper;
import com.example.wms.mapper.UserAccountMapper;
import com.example.wms.mapper.UserRoleMapper;
import com.example.wms.security.AuthenticatedUser;
import com.example.wms.tenant.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RolePolicyService {
    private final RolePermissionMapper rolePermissionMapper;
    private final UserAccountMapper userAccountMapper;
    private final UserRoleMapper userRoleMapper;

    public RolePolicyService(RolePermissionMapper rolePermissionMapper,
                             UserAccountMapper userAccountMapper,
                             UserRoleMapper userRoleMapper) {
        this.rolePermissionMapper = rolePermissionMapper;
        this.userAccountMapper = userAccountMapper;
        this.userRoleMapper = userRoleMapper;
    }

    public boolean canViewRole(Role role) {
        if (role == null || role.getId() == null) {
            return false;
        }
        if (currentActorHasRole("super_admin")) {
            return true;
        }
        return isCurrentActorRole(role.getId()) || rolePermissionsWithinActorScope(role.getId());
    }

    public boolean canManageRole(Role role) {
        if (!canViewRole(role)) {
            return false;
        }
        if (isCurrentActorRole(role == null ? null : role.getId())) {
            return false;
        }
        String normalizedCode = normalizeRoleCode(role == null ? null : role.getCode());
        if (("admin".equals(normalizedCode) || "super_admin".equals(normalizedCode))
            && !currentActorHasRole("super_admin")) {
            return false;
        }
        return rolePermissionsWithinActorScope(role.getId());
    }

    public boolean canEditRoleMetadata(Role role) {
        return canManageRole(role) && !isReservedRole(role);
    }

    public boolean canDeleteRole(Role role) {
        return canManageRole(role) && !isReservedRole(role);
    }

    public boolean canManageRolePermissions(Role role) {
        return canManageRole(role);
    }

    public boolean canAssignPermissionToRole(Role role, Permission permission) {
        if (permission == null || permission.getCode() == null) {
            return false;
        }
        if (!currentActorHasRole("super_admin")
            && !currentActorPermissionCodes().contains(permission.getCode())) {
            return false;
        }
        if (isSuperAdminRole(role)) {
            return true;
        }
        if (permission.getCode().startsWith("tenant:")) {
            return false;
        }
        if (permission.getCode().startsWith("system-config:")) {
            return currentActorHasRole("super_admin") && isAdminRole(role);
        }
        return true;
    }

    public RoleCapabilitiesResponse capabilities(Role role) {
        boolean currentActorRole = isCurrentActorRole(role == null ? null : role.getId());
        boolean canManage = canManageRole(role);
        boolean reserved = isReservedRole(role);
        boolean canEdit = canManage && !reserved;
        boolean canDelete = canManage && !reserved;
        boolean canEditPermissions = canManage;
        String editReason = "";
        String deleteReason = "";
        if (currentActorRole) {
            editReason = "不能修改当前登录账号所属角色";
            deleteReason = "不能修改当前登录账号所属角色";
        } else if (reserved) {
            editReason = currentActorHasRole("super_admin")
                ? ""
                : "仅系统超级管理员可维护 " + normalizeRoleCode(role.getCode()) + " 角色权限";
            deleteReason = "保留角色不允许通过角色管理接口删除";
        } else if (!canManage) {
            editReason = "无权限维护该角色";
            deleteReason = "无权限维护该角色";
        }
        return new RoleCapabilitiesResponse(
            canEdit,
            canDelete,
            canEditPermissions,
            canEditPermissions,
            editReason,
            deleteReason
        );
    }

    public boolean isCurrentActorRole(Long roleId) {
        if (roleId == null) {
            return false;
        }
        ActorIdentity actor = currentActorIdentity();
        if (actor.userId() == null) {
            return false;
        }
        Long tenantId = TenantContext.requireTenantId();
        if (actor.tenantId() != null && !actor.tenantId().equals(tenantId)) {
            return false;
        }
        List<Long> userIds = userRoleMapper.findUserIdsByRoleId(tenantId, roleId);
        return userIds != null && userIds.contains(actor.userId());
    }

    public boolean rolePermissionsWithinActorScope(Long roleId) {
        if (roleId == null) {
            return false;
        }
        if (currentActorHasRole("super_admin")) {
            return true;
        }
        Long tenantId = TenantContext.requireTenantId();
        Set<String> actorPermissionCodes = currentActorPermissionCodes();
        return isRolePermissionSubset(tenantId, roleId, actorPermissionCodes);
    }

    public boolean isRolePermissionSubset(Long tenantId, Role role, Set<String> actorPermissionCodes) {
        if (role == null || role.getId() == null) {
            return false;
        }
        return isRolePermissionSubset(tenantId, role.getId(), actorPermissionCodes);
    }

    public boolean currentActorHasRole(String roleCode) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        String expected = "ROLE_" + roleCode.toLowerCase(Locale.ROOT);
        return authentication.getAuthorities().stream()
            .anyMatch(authority -> authority != null && expected.equalsIgnoreCase(authority.getAuthority()));
    }

    public Set<String> currentActorPermissionCodes() {
        Set<String> permissionCodes = new HashSet<>();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return permissionCodes;
        }
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        for (GrantedAuthority authority : authorities) {
            String raw = authority == null ? null : authority.getAuthority();
            if (raw != null && raw.startsWith("PERM_") && raw.length() > 5) {
                permissionCodes.add(raw.substring(5));
            }
        }
        return permissionCodes;
    }

    public boolean isReservedRole(Role role) {
        String normalized = normalizeRoleCode(role == null ? null : role.getCode());
        return "admin".equals(normalized) || "super_admin".equals(normalized);
    }

    public boolean isAdminRole(Role role) {
        return "admin".equals(normalizeRoleCode(role == null ? null : role.getCode()));
    }

    public boolean isSuperAdminRole(Role role) {
        return "super_admin".equals(normalizeRoleCode(role == null ? null : role.getCode()));
    }

    public String normalizeRoleCode(String code) {
        return code == null ? "" : code.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isRolePermissionSubset(Long tenantId, Long roleId, Set<String> actorPermissionCodes) {
        List<Permission> permissions = rolePermissionMapper.findPermissionsByRoleId(tenantId, roleId);
        return permissions.stream()
            .map(Permission::getCode)
            .filter(code -> code != null && !code.isBlank())
            .allMatch(actorPermissionCodes::contains);
    }

    private ActorIdentity currentActorIdentity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return new ActorIdentity(null, null);
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser user) {
            Long tenantId = user.getAuthPayload() == null ? null : user.getAuthPayload().userTenantId();
            if (tenantId == null && user.getUserAccount() != null) {
                tenantId = user.getUserAccount().getTenantId();
            }
            return new ActorIdentity(user.getUserId(), tenantId);
        }
        if (authentication.getName() == null) {
            return new ActorIdentity(null, null);
        }
        Long tenantId = TenantContext.requireTenantId();
        UserAccount actor = userAccountMapper.findActiveByUsername(tenantId, authentication.getName());
        return new ActorIdentity(actor == null ? null : actor.getId(), tenantId);
    }

    private record ActorIdentity(Long userId, Long tenantId) {
    }
}
