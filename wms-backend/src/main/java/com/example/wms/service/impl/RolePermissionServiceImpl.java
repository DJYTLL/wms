package com.example.wms.service.impl;

import com.example.wms.entity.Permission;
import com.example.wms.entity.Role;
import com.example.wms.mapper.PermissionMapper;
import com.example.wms.mapper.RoleMapper;
import com.example.wms.mapper.RolePermissionMapper;
import com.example.wms.mapper.TenantColumnSettingMapper;
import com.example.wms.mapper.UserAccountMapper;
import com.example.wms.mapper.UserRoleMapper;
import com.example.wms.service.RolePermissionService;
import com.example.wms.aop.AuditLog;
import com.example.wms.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

// 角色权限服务实现
@Service
public class RolePermissionServiceImpl implements RolePermissionService {
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final TenantColumnSettingMapper tenantColumnSettingMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserAccountMapper userAccountMapper;

    public RolePermissionServiceImpl(RoleMapper roleMapper,
                                     PermissionMapper permissionMapper,
                                     RolePermissionMapper rolePermissionMapper,
                                     TenantColumnSettingMapper tenantColumnSettingMapper,
                                     UserRoleMapper userRoleMapper,
                                     UserAccountMapper userAccountMapper) {
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.tenantColumnSettingMapper = tenantColumnSettingMapper;
        this.userRoleMapper = userRoleMapper;
        this.userAccountMapper = userAccountMapper;
    }

    @Override
    public List<Permission> listPermissions(Long roleId) {
        Role role = loadRole(roleId);
        Long tenantId = TenantContext.requireTenantId();
        List<Permission> permissions = rolePermissionMapper.findPermissionsByRoleId(tenantId, roleId);
        ensurePermissionsWithinActorScope(permissions);
        return permissions;
    }

    @Override
    public List<Permission> listColumnPermissions(Long roleId) {
        List<Permission> permissions = listPermissions(roleId);
        return permissions.stream()
            .filter(permission -> permission.getCode() != null && permission.getCode().startsWith("column:"))
            .toList();
    }

    @Override
    @Transactional
    @AuditLog(action = "ROLE_PERMISSION_SET", entityType = "role", entityId = "{arg0}", detail = "permissionIds={arg1.permissionIds}")
    public void setPermissions(Long roleId, List<Long> permissionIds) {
        Role role = loadRole(roleId);
        ensurePermissionMutableRole(role);
        ensureCurrentRolePermissionsWithinActorScope(role);
        validatePermissions(role, permissionIds);
        Long tenantId = TenantContext.requireTenantId();
        // 先清空，再重建
        rolePermissionMapper.deleteByRoleId(tenantId, roleId);
        for (Long permissionId : permissionIds) {
            rolePermissionMapper.insertIgnore(tenantId, roleId, permissionId);
        }
        // 角色权限变更，刷新用户权限版本
        bumpUsersByRole(roleId);
    }

    @Override
    @Transactional
    @AuditLog(action = "ROLE_PERMISSION_SET", entityType = "role", entityId = "{arg0}", detail = "pageKey={arg1}, columnPermissionIds={arg2}")
    public void setColumnPermissions(Long roleId, String pageKey, List<Long> permissionIds) {
        Role role = loadRole(roleId);
        ensurePermissionMutableRole(role);
        ensureCurrentRolePermissionsWithinActorScope(role);
        String normalizedPageKey = pageKey == null ? "" : pageKey.trim();
        String prefix = normalizedPageKey.isBlank()
            ? "column:"
            : "column:" + normalizedPageKey + ":";
        List<Permission> columnPermissions = permissionMapper.findByCodePrefix(prefix);
        if (!normalizedPageKey.isBlank() && columnPermissions.isEmpty()) {
            throw new IllegalArgumentException("页面不存在或未注册列权限");
        }
        java.util.Map<Long, Permission> columnPermissionMap = columnPermissions.stream()
            .collect(java.util.stream.Collectors.toMap(Permission::getId, p -> p));
        List<Long> columnPermissionIds = columnPermissions.stream().map(Permission::getId).toList();
        java.util.Set<Long> columnPermissionIdSet = new java.util.HashSet<>(columnPermissionIds);

        if (permissionIds == null) {
            throw new IllegalArgumentException("权限列表不能为空");
        }
        List<Long> sanitizedPermissionIds = permissionIds.stream()
            .filter(id -> id != null)
            .distinct()
            .toList();
        if (!sanitizedPermissionIds.isEmpty()) {
            boolean allColumn = sanitizedPermissionIds.stream().allMatch(columnPermissionIdSet::contains);
            if (!allColumn) {
                throw new IllegalArgumentException(normalizedPageKey.isBlank() ? "仅允许配置列权限" : "仅允许配置当前页面列权限");
            }
        }

        Long tenantId = TenantContext.requireTenantId();
        // 强校验：列权限必须受租户列配置约束（若已配置）
        validateAgainstTenantColumns(tenantId, sanitizedPermissionIds, columnPermissionMap);
        List<Permission> selectedColumnPermissions = sanitizedPermissionIds.stream()
            .map(columnPermissionMap::get)
            .filter(permission -> permission != null)
            .toList();
        ensurePermissionsWithinActorScope(selectedColumnPermissions);
        if (!columnPermissionIds.isEmpty()) {
            rolePermissionMapper.deleteByRoleIdAndPermissionIds(tenantId, roleId, columnPermissionIds);
        }
        for (Long permissionId : sanitizedPermissionIds) {
            rolePermissionMapper.insertIgnore(tenantId, roleId, permissionId);
        }
        bumpUsersByRole(roleId);
    }

    @Override
    @AuditLog(action = "ROLE_PERMISSION_ADD", entityType = "role", entityId = "{arg0}", detail = "permissionId={arg1}")
    public void addPermission(Long roleId, Long permissionId) {
        Role role = loadRole(roleId);
        ensurePermissionMutableRole(role);
        ensureCurrentRolePermissionsWithinActorScope(role);
        validatePermissions(role, List.of(permissionId));
        Long tenantId = TenantContext.requireTenantId();
        rolePermissionMapper.insertIgnore(tenantId, roleId, permissionId);
        bumpUsersByRole(roleId);
    }

    @Override
    @AuditLog(action = "ROLE_PERMISSION_REMOVE", entityType = "role", entityId = "{arg0}", detail = "permissionId={arg1}")
    public void removePermission(Long roleId, Long permissionId) {
        Role role = loadRole(roleId);
        ensurePermissionMutableRole(role);
        ensureCurrentRolePermissionsWithinActorScope(role);
        Long tenantId = TenantContext.requireTenantId();
        rolePermissionMapper.deleteByRoleIdAndPermissionId(tenantId, roleId, permissionId);
        bumpUsersByRole(roleId);
    }

    // 校验角色存在
    private void validateRole(Long roleId) {
        loadRole(roleId);
    }

    private Role loadRole(Long roleId) {
        Long tenantId = TenantContext.requireTenantId();
        Role role = roleMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Role>()
            .eq("tenant_id", tenantId)
            .eq("id", roleId));
        if (role == null) {
            throw new IllegalArgumentException("角色不存在");
        }
        return role;
    }

    // 校验权限列表存在
    private void validatePermissions(Role role, List<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            throw new IllegalArgumentException("权限列表不能为空");
        }
        List<Permission> permissions = permissionMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Permission>()
                .in("id", permissionIds)
        );
        if (permissions.size() != permissionIds.size()) {
            throw new IllegalArgumentException("存在无效权限 ID");
        }
        if (role != null && !isSuperAdmin(role)) {
            boolean hasTenantPermission = permissions.stream()
                .map(Permission::getCode)
                .anyMatch(code -> code != null && (code.startsWith("tenant:") || code.startsWith("system-config:")));
            if (hasTenantPermission) {
                throw new IllegalArgumentException("仅超级管理员角色可分配租户管理权限");
            }
        }
        ensurePermissionsWithinActorScope(permissions);
    }

    private boolean isSuperAdmin(Role role) {
        String code = role.getCode();
        return code != null && "super_admin".equalsIgnoreCase(code);
    }

    private void ensurePermissionMutableRole(Role role) {
        String code = role == null || role.getCode() == null
            ? ""
            : role.getCode().trim().toLowerCase(Locale.ROOT);
        boolean actorIsSuperAdmin = currentActorHasRole("super_admin");
        if ("super_admin".equals(code) && !actorIsSuperAdmin) {
            throw new IllegalArgumentException("仅系统超级管理员可调整 super_admin 角色权限");
        }
        if ("admin".equals(code) && !actorIsSuperAdmin) {
            throw new IllegalArgumentException("仅系统超级管理员可调整 admin 角色权限");
        }
    }

    private boolean currentActorHasRole(String roleCode) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        String expected = "ROLE_" + roleCode.toLowerCase(Locale.ROOT);
        return authentication.getAuthorities().stream()
            .anyMatch(authority -> authority != null && expected.equalsIgnoreCase(authority.getAuthority()));
    }

    private void ensurePermissionsWithinActorScope(List<Permission> permissions) {
        if (currentActorHasRole("super_admin")) {
            return;
        }
        Set<String> actorPermissionCodes = currentActorPermissionCodes();
        List<String> exceeded = permissions.stream()
            .map(Permission::getCode)
            .filter(code -> code != null && !code.isBlank())
            .filter(code -> !actorPermissionCodes.contains(code))
            .toList();
        if (!exceeded.isEmpty()) {
            throw new IllegalArgumentException("存在超出当前账号范围的权限，不能分配: " + String.join("、", exceeded));
        }
    }

    private void ensureCurrentRolePermissionsWithinActorScope(Role role) {
        if (currentActorHasRole("super_admin") || role == null || role.getId() == null) {
            return;
        }
        Long tenantId = TenantContext.requireTenantId();
        List<Permission> currentPermissions = rolePermissionMapper.findPermissionsByRoleId(tenantId, role.getId());
        ensurePermissionsWithinActorScope(currentPermissions);
    }

    private Set<String> currentActorPermissionCodes() {
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

    private void validateAgainstTenantColumns(Long tenantId,
                                              List<Long> permissionIds,
                                              java.util.Map<Long, Permission> columnPermissionMap) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return;
        }
        for (Long permissionId : permissionIds) {
            Permission permission = columnPermissionMap.get(permissionId);
            if (permission == null || permission.getCode() == null) {
                throw new IllegalArgumentException("存在无效列权限");
            }
            String[] parts = permission.getCode().split(":", 3);
            if (parts.length < 3) {
                throw new IllegalArgumentException("列权限编码格式不正确");
            }
            String pageKey = parts[1];
            String columnKey = parts[2];
            com.example.wms.entity.TenantColumnSetting setting = tenantColumnSettingMapper.findOne(tenantId, pageKey);
            if (setting == null || setting.getVisibleColumns() == null) {
                continue;
            }
            java.util.Set<String> allowed = java.util.Arrays.stream(setting.getVisibleColumns().split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(java.util.stream.Collectors.toSet());
            if (!allowed.contains(columnKey)) {
                throw new IllegalArgumentException("列权限超出租户可见列范围");
            }
        }
    }

    // 角色权限变更时递增用户权限版本
    private void bumpUsersByRole(Long roleId) {
        Long tenantId = TenantContext.requireTenantId();
        List<Long> userIds = userRoleMapper.findUserIdsByRoleId(tenantId, roleId);
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        userAccountMapper.incrementAuthVersionByIds(tenantId, userIds);
    }
}
