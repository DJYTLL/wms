package com.example.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.dto.PermissionCreateRequest;
import com.example.wms.dto.PermissionDiagnosticResponse;
import com.example.wms.dto.PermissionUpdateRequest;
import com.example.wms.dto.PageResponse;
import com.example.wms.entity.Permission;
import com.example.wms.mapper.MenuMapper;
import com.example.wms.mapper.PermissionMapper;
import com.example.wms.mapper.RolePermissionMapper;
import com.example.wms.mapper.UserAccountMapper;
import com.example.wms.mapper.UserRoleMapper;
import com.example.wms.aop.AuditLog;
import com.example.wms.service.PermissionService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// 权限服务实现
@Service
public class PermissionServiceImpl implements PermissionService {
    private static final Set<String> MENU_REFERENCE_OPTIONAL_VIEW_PERMISSIONS = Set.of(
        "erp-finance-summary:view"
    );

    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserAccountMapper userAccountMapper;
    private final MenuMapper menuMapper;

    public PermissionServiceImpl(PermissionMapper permissionMapper,
                                 RolePermissionMapper rolePermissionMapper,
                                 UserRoleMapper userRoleMapper,
                                 UserAccountMapper userAccountMapper,
                                 MenuMapper menuMapper) {
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.userRoleMapper = userRoleMapper;
        this.userAccountMapper = userAccountMapper;
        this.menuMapper = menuMapper;
    }

    @Override
    public List<Permission> listAll() {
        QueryWrapper<Permission> wrapper = new QueryWrapper<Permission>()
            .orderByAsc("id");
        if (!currentActorHasRole("super_admin")) {
            Set<String> permissionCodes = currentActorPermissionCodes();
            if (permissionCodes.isEmpty()) {
                return List.of();
            }
            wrapper.in("code", permissionCodes);
        }
        return permissionMapper.selectList(wrapper);
    }

    @Override
    public PageResponse<Permission> page(long page, long size, String keyword, Boolean enabled) {
        Page<Permission> pageReq = Page.of(page, size);
        QueryWrapper<Permission> wrapper = new QueryWrapper<Permission>()
            .orderByAsc("id");
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(q -> q.like("code", keyword)
                .or()
                .like("name", keyword)
                .or()
                .like("description", keyword));
        }
        if (enabled != null) {
            wrapper.eq("is_enabled", enabled);
        }
        if (!currentActorHasRole("super_admin")) {
            Set<String> permissionCodes = currentActorPermissionCodes();
            if (permissionCodes.isEmpty()) {
                return new PageResponse<>(0, page, size, List.of());
            }
            wrapper.in("code", permissionCodes);
        }
        Page<Permission> result = permissionMapper.selectPage(pageReq, wrapper);
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public Permission getById(Long id) {
        return permissionMapper.selectOne(new QueryWrapper<Permission>()
            .eq("id", id));
    }

    @Override
    @AuditLog(action = "PERMISSION_CREATE", entityType = "permission", entityId = "{result.id}", detail = "code={arg0.code}")
    public Permission create(PermissionCreateRequest request) {
        // 校验编码唯一
        Permission existing = permissionMapper.findByCode(request.code());
        if (existing != null) {
            throw new IllegalArgumentException("权限编码已存在");
        }
        Permission permission = new Permission();
        permission.setCode(request.code());
        permission.setName(request.name());
        permission.setDescription(request.description());
        permission.setEnabled(request.enabled() == null || request.enabled());
        permission.setCreatedAt(Instant.now());
        permission.setUpdatedAt(Instant.now());
        permissionMapper.insert(permission);
        return permission;
    }

    @Override
    @AuditLog(action = "PERMISSION_UPDATE", entityType = "permission", entityId = "{arg0}", detail = "code={arg1.code}")
    public Permission update(Long id, PermissionUpdateRequest request) {
        Permission permission = permissionMapper.selectOne(new QueryWrapper<Permission>()
            .eq("id", id));
        if (permission == null) {
            throw new IllegalArgumentException("权限不存在");
        }
        Permission existing = permissionMapper.findByCode(request.code());
        if (existing != null && !existing.getId().equals(id)) {
            throw new IllegalArgumentException("权限编码已存在");
        }
        boolean authRelevantChange = isAuthRelevantChange(permission, request);
        permission.setCode(request.code());
        permission.setName(request.name());
        permission.setDescription(request.description());
        if (request.enabled() != null) {
            permission.setEnabled(request.enabled());
        }
        if (authRelevantChange) {
            bumpUsersByPermission(id);
        }
        permission.setUpdatedAt(Instant.now());
        permissionMapper.updateById(permission);
        return permission;
    }

    @Override
    @AuditLog(action = "PERMISSION_DELETE", entityType = "permission", entityId = "{arg0}")
    public void delete(Long id) {
        Permission permission = permissionMapper.selectOne(new QueryWrapper<Permission>()
            .eq("id", id));
        if (permission == null) {
            throw new IllegalArgumentException("权限不存在");
        }
        bumpUsersByPermission(id);
        // 先删除角色权限关联
        rolePermissionMapper.deleteByPermissionId(id);
        permissionMapper.deleteById(id);
    }

    @Override
    public List<PermissionDiagnosticResponse> listDiagnostics() {
        List<Permission> permissions = listAll();
        return permissions.stream()
            .map(this::toDiagnosticResponse)
            .toList();
    }

    private PermissionDiagnosticResponse toDiagnosticResponse(Permission permission) {
        long roleCount = rolePermissionMapper.countActiveRolesByPermissionId(permission.getId());
        long menuCount = permission.getCode() == null
            ? 0L
            : menuMapper.countActiveMenusByPermissionCode(permission.getCode());
        List<String> warnings = buildWarnings(permission, roleCount, menuCount);
        String riskLevel = warnings.isEmpty() ? "ok" : "warning";
        return new PermissionDiagnosticResponse(
            permission.getId(),
            permission.getCode(),
            roleCount,
            menuCount,
            riskLevel,
            warnings
        );
    }

    private List<String> buildWarnings(Permission permission, long roleCount, long menuCount) {
        List<String> warnings = new java.util.ArrayList<>();
        if (!permission.isEnabled()) {
            warnings.add("权限已停用");
        }
        if (roleCount == 0) {
            warnings.add("未分配给任何角色");
        }
        if (requiresMenuReference(permission.getCode()) && menuCount == 0) {
            warnings.add("未被菜单引用");
        }
        return warnings;
    }

    private boolean requiresMenuReference(String code) {
        if (code == null || code.isBlank() || code.startsWith("column:")) {
            return false;
        }
        if (MENU_REFERENCE_OPTIONAL_VIEW_PERMISSIONS.contains(code)) {
            return false;
        }
        String[] parts = code.split(":", 2);
        return parts.length == 2 && "view".equals(parts[1]);
    }

    private boolean isAuthRelevantChange(Permission permission, PermissionUpdateRequest request) {
        boolean codeChanged = request.code() != null && !request.code().equals(permission.getCode());
        boolean enabledChanged = request.enabled() != null && request.enabled() != permission.isEnabled();
        return codeChanged || enabledChanged;
    }

    private void bumpUsersByPermission(Long permissionId) {
        List<com.example.wms.mapper.RolePermissionMapper.RoleTenantPair> pairs =
            rolePermissionMapper.findRoleTenantPairsByPermissionId(permissionId);
        if (pairs != null && !pairs.isEmpty()) {
            java.util.Map<Long, java.util.Set<Long>> tenantRoleIds = new java.util.HashMap<>();
            for (com.example.wms.mapper.RolePermissionMapper.RoleTenantPair pair : pairs) {
                tenantRoleIds.computeIfAbsent(pair.getTenantId(), key -> new java.util.HashSet<>())
                    .add(pair.getRoleId());
            }
            for (java.util.Map.Entry<Long, java.util.Set<Long>> entry : tenantRoleIds.entrySet()) {
                Long tenantId = entry.getKey();
                java.util.Set<Long> roleIds = entry.getValue();
                java.util.Set<Long> userIds = new java.util.HashSet<>();
                for (Long roleId : roleIds) {
                    userIds.addAll(userRoleMapper.findUserIdsByRoleId(tenantId, roleId));
                }
                if (!userIds.isEmpty()) {
                    userAccountMapper.incrementAuthVersionByIds(tenantId, new java.util.ArrayList<>(userIds));
                }
            }
        }
    }

    @Override
    public List<Permission> listColumnPermissions() {
        List<Permission> permissions = permissionMapper.findByCodePrefix("column:");
        if (currentActorHasRole("super_admin")) {
            return permissions;
        }
        Set<String> permissionCodes = currentActorPermissionCodes();
        return permissions.stream()
            .filter(permission -> permission.getCode() != null && permissionCodes.contains(permission.getCode()))
            .toList();
    }

    private boolean currentActorHasRole(String roleCode) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        String expected = "ROLE_" + roleCode.toLowerCase(java.util.Locale.ROOT);
        return authentication.getAuthorities().stream()
            .anyMatch(authority -> authority != null && expected.equalsIgnoreCase(authority.getAuthority()));
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
}
