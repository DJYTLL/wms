package com.example.wms.service;

import com.example.wms.entity.Permission;
import com.example.wms.entity.Role;
import com.example.wms.entity.UserAccount;
import com.example.wms.mapper.RolePermissionMapper;
import com.example.wms.mapper.UserAccountMapper;
import com.example.wms.mapper.UserRoleMapper;
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

// 当前账号对角色的可见/可维护范围。
@Service
public class RoleScopeService {
    private final RolePermissionMapper rolePermissionMapper;
    private final UserAccountMapper userAccountMapper;
    private final UserRoleMapper userRoleMapper;

    public RoleScopeService(RolePermissionMapper rolePermissionMapper,
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
        if (role != null && isCurrentActorRole(role.getId())) {
            return false;
        }
        String normalizedCode = normalizeRoleCode(role == null ? null : role.getCode());
        if (("admin".equals(normalizedCode) || "super_admin".equals(normalizedCode))
            && !currentActorHasRole("super_admin")) {
            return false;
        }
        return rolePermissionsWithinActorScope(role.getId());
    }

    public boolean isCurrentActorRole(Long roleId) {
        if (roleId == null) {
            return false;
        }
        UserAccount actor = currentActor();
        if (actor == null || actor.getId() == null) {
            return false;
        }
        Long tenantId = TenantContext.requireTenantId();
        List<Long> userIds = userRoleMapper.findUserIdsByRoleId(tenantId, roleId);
        return userIds != null && userIds.contains(actor.getId());
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
        List<Permission> permissions = rolePermissionMapper.findPermissionsByRoleId(tenantId, roleId);
        return permissions.stream()
            .map(Permission::getCode)
            .filter(code -> code != null && !code.isBlank())
            .allMatch(actorPermissionCodes::contains);
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

    private UserAccount currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        Long tenantId = TenantContext.requireTenantId();
        return userAccountMapper.findActiveByUsername(tenantId, authentication.getName());
    }

    private String normalizeRoleCode(String code) {
        return code == null ? "" : code.trim().toLowerCase(Locale.ROOT);
    }
}
