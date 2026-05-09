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

import java.util.List;
import java.util.Locale;

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
        validateRole(roleId);
        Long tenantId = TenantContext.requireTenantId();
        return rolePermissionMapper.findPermissionsByRoleId(tenantId, roleId);
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
    @AuditLog(action = "ROLE_PERMISSION_SET", entityType = "role", entityId = "{arg0}", detail = "columnPermissionIds={arg1}")
    public void setColumnPermissions(Long roleId, List<Long> permissionIds) {
        Role role = loadRole(roleId);
        ensurePermissionMutableRole(role);
        List<Permission> columnPermissions = permissionMapper.findByCodePrefix("column:");
        java.util.Map<Long, Permission> columnPermissionMap = columnPermissions.stream()
            .collect(java.util.stream.Collectors.toMap(Permission::getId, p -> p));
        List<Long> columnPermissionIds = columnPermissions.stream().map(Permission::getId).toList();

        if (permissionIds == null) {
            throw new IllegalArgumentException("权限列表不能为空");
        }
        if (!permissionIds.isEmpty()) {
            boolean allColumn = permissionIds.stream().allMatch(columnPermissionIds::contains);
            if (!allColumn) {
                throw new IllegalArgumentException("仅允许配置列权限");
            }
        }

        Long tenantId = TenantContext.requireTenantId();
        // 强校验：列权限必须受租户列配置约束（若已配置）
        validateAgainstTenantColumns(tenantId, permissionIds, columnPermissionMap);
        if (!columnPermissionIds.isEmpty()) {
            rolePermissionMapper.deleteByRoleIdAndPermissionIds(tenantId, roleId, columnPermissionIds);
        }
        for (Long permissionId : permissionIds) {
            rolePermissionMapper.insertIgnore(tenantId, roleId, permissionId);
        }
        bumpUsersByRole(roleId);
    }

    @Override
    @AuditLog(action = "ROLE_PERMISSION_ADD", entityType = "role", entityId = "{arg0}", detail = "permissionId={arg1}")
    public void addPermission(Long roleId, Long permissionId) {
        Role role = loadRole(roleId);
        ensurePermissionMutableRole(role);
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
    }

    private boolean isSuperAdmin(Role role) {
        String code = role.getCode();
        return code != null && "super_admin".equalsIgnoreCase(code);
    }

    private void ensurePermissionMutableRole(Role role) {
        String code = role == null || role.getCode() == null
            ? ""
            : role.getCode().trim().toLowerCase(Locale.ROOT);
        if ("admin".equals(code) || "super_admin".equals(code)) {
            throw new IllegalArgumentException("保留角色权限不允许通过角色权限接口修改");
        }
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
            if (setting == null || setting.getVisibleColumns() == null || setting.getVisibleColumns().isBlank()) {
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
