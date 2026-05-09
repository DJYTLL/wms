package com.example.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.dto.RoleCreateRequest;
import com.example.wms.dto.RoleUpdateRequest;
import com.example.wms.dto.PageResponse;
import com.example.wms.entity.Role;
import com.example.wms.mapper.RoleMapper;
import com.example.wms.mapper.RolePermissionMapper;
import com.example.wms.mapper.UserAccountMapper;
import com.example.wms.mapper.UserRoleMapper;
import com.example.wms.aop.AuditLog;
import com.example.wms.service.RoleService;
import com.example.wms.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.List;

// 角色服务实现
@Service
public class RoleServiceImpl implements RoleService {
    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserAccountMapper userAccountMapper;

    public RoleServiceImpl(RoleMapper roleMapper,
                           RolePermissionMapper rolePermissionMapper,
                           UserRoleMapper userRoleMapper,
                           UserAccountMapper userAccountMapper) {
        this.roleMapper = roleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.userRoleMapper = userRoleMapper;
        this.userAccountMapper = userAccountMapper;
    }

    @Override
    public List<Role> listAll() {
        // 按 ID 排序查询
        Long tenantId = TenantContext.requireTenantId();
        return roleMapper.selectList(new QueryWrapper<Role>()
            .eq("tenant_id", tenantId)
            .orderByAsc("id"));
    }

    @Override
    public PageResponse<Role> page(long page, long size, String keyword, Boolean enabled) {
        Long tenantId = TenantContext.requireTenantId();
        Page<Role> pageReq = Page.of(page, size);
        QueryWrapper<Role> wrapper = new QueryWrapper<Role>()
            .eq("tenant_id", tenantId)
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
        Page<Role> result = roleMapper.selectPage(pageReq, wrapper);
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public Role getById(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        return roleMapper.selectOne(new QueryWrapper<Role>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
    }

    @Override
    @AuditLog(action = "ROLE_CREATE", entityType = "role", entityId = "{result.id}", detail = "code={arg0.code}")
    public Role create(RoleCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        validateCreatableRoleCode(tenantId, request.code());
        Role existing = roleMapper.findByCode(tenantId, request.code());
        if (existing != null) {
            throw new IllegalArgumentException("角色编码已存在");
        }
        Role role = new Role();
        role.setTenantId(tenantId);
        role.setCode(request.code());
        role.setName(request.name());
        role.setDescription(request.description());
        role.setEnabled(request.enabled() == null || request.enabled());
        role.setCreatedAt(Instant.now());
        role.setUpdatedAt(Instant.now());
        roleMapper.insert(role);
        return role;
    }

    @Override
    @AuditLog(action = "ROLE_UPDATE", entityType = "role", entityId = "{arg0}", detail = "code={arg1.code}")
    public Role update(Long id, RoleUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        Role role = roleMapper.selectOne(new QueryWrapper<Role>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (role == null) {
            throw new IllegalArgumentException("角色不存在");
        }
        ensureMutableRole(role);
        validateCreatableRoleCode(tenantId, request.code());
        Role existing = roleMapper.findByCode(tenantId, request.code());
        if (existing != null && !existing.getId().equals(id)) {
            throw new IllegalArgumentException("角色编码已存在");
        }
        role.setCode(request.code());
        role.setName(request.name());
        role.setDescription(request.description());
        if (request.enabled() != null) {
            role.setEnabled(request.enabled());
        }
        role.setUpdatedAt(Instant.now());
        roleMapper.updateById(role);
        return role;
    }

    @Override
    @Transactional
    @AuditLog(action = "ROLE_DELETE", entityType = "role", entityId = "{arg0}")
    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        Role role = roleMapper.selectOne(new QueryWrapper<Role>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (role == null) {
            throw new IllegalArgumentException("角色不存在");
        }
        ensureMutableRole(role);
        // 先清理角色关联
        List<Long> userIds = userRoleMapper.findUserIdsByRoleId(tenantId, id);
        rolePermissionMapper.deleteByRoleId(tenantId, id);
        userRoleMapper.deleteByRoleId(tenantId, id);
        if (userIds != null && !userIds.isEmpty()) {
            userAccountMapper.incrementAuthVersionByIds(tenantId, userIds);
        }
        roleMapper.deleteById(id);
    }

    private void validateCreatableRoleCode(Long tenantId, String code) {
        String normalized = normalizeCode(code);
        if ("admin".equals(normalized)) {
            throw new IllegalArgumentException("admin 为系统保留角色，不能手动创建或修改");
        }
        if ("super_admin".equals(normalized)) {
            throw new IllegalArgumentException("super_admin 为系统保留角色，不能手动创建或修改");
        }
    }

    private void ensureMutableRole(Role role) {
        String normalized = normalizeCode(role.getCode());
        if ("admin".equals(normalized) || "super_admin".equals(normalized)) {
            throw new IllegalArgumentException("保留角色不允许通过角色管理接口修改或删除");
        }
    }

    private String normalizeCode(String code) {
        return code == null ? "" : code.trim().toLowerCase(Locale.ROOT);
    }
}
