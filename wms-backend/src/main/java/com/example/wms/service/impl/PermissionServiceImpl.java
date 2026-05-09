package com.example.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.dto.PermissionCreateRequest;
import com.example.wms.dto.PermissionUpdateRequest;
import com.example.wms.dto.PageResponse;
import com.example.wms.entity.Permission;
import com.example.wms.mapper.PermissionMapper;
import com.example.wms.mapper.RolePermissionMapper;
import com.example.wms.mapper.UserAccountMapper;
import com.example.wms.mapper.UserRoleMapper;
import com.example.wms.aop.AuditLog;
import com.example.wms.service.PermissionService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

// 权限服务实现
@Service
public class PermissionServiceImpl implements PermissionService {
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserAccountMapper userAccountMapper;

    public PermissionServiceImpl(PermissionMapper permissionMapper,
                                 RolePermissionMapper rolePermissionMapper,
                                 UserRoleMapper userRoleMapper,
                                 UserAccountMapper userAccountMapper) {
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.userRoleMapper = userRoleMapper;
        this.userAccountMapper = userAccountMapper;
    }

    @Override
    public List<Permission> listAll() {
        // 按 ID 排序查询
        return permissionMapper.selectList(new QueryWrapper<Permission>()
            .orderByAsc("id"));
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
        permission.setCode(request.code());
        permission.setName(request.name());
        permission.setDescription(request.description());
        if (request.enabled() != null) {
            permission.setEnabled(request.enabled());
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
        // 获取受影响的角色与用户
        List<com.example.wms.mapper.RolePermissionMapper.RoleTenantPair> pairs =
            rolePermissionMapper.findRoleTenantPairsByPermissionId(id);
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
        // 先删除角色权限关联
        rolePermissionMapper.deleteByPermissionId(id);
        permissionMapper.deleteById(id);
    }

    @Override
    public List<Permission> listColumnPermissions() {
        return permissionMapper.findByCodePrefix("column:");
    }
}
