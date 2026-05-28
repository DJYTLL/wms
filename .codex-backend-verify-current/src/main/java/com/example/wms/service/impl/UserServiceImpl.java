package com.example.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.dto.RoleOptionResponse;
import com.example.wms.dto.UserCreateRequest;
import com.example.wms.dto.UserPasswordChangeRequest;
import com.example.wms.dto.UserPasswordResetRequest;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.UserResponse;
import com.example.wms.dto.UserRoleUpdateRequest;
import com.example.wms.dto.UserStatusUpdateRequest;
import com.example.wms.dto.UserUpdateRequest;
import com.example.wms.aop.AuditLog;
import com.example.wms.entity.Role;
import com.example.wms.entity.UserAccount;
import com.example.wms.mapper.RoleMapper;
import com.example.wms.mapper.RolePermissionMapper;
import com.example.wms.mapper.UserAccountMapper;
import com.example.wms.mapper.UserRoleMapper;
import com.example.wms.service.RolePolicyService;
import com.example.wms.service.UserService;
import com.example.wms.tenant.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

// 用户管理服务实现
@Service
public class UserServiceImpl implements UserService {
    private final UserAccountMapper userAccountMapper;
    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final RolePolicyService rolePolicyService;

    public UserServiceImpl(UserAccountMapper userAccountMapper,
                           RoleMapper roleMapper,
                           RolePermissionMapper rolePermissionMapper,
                           UserRoleMapper userRoleMapper,
                           PasswordEncoder passwordEncoder,
                           RolePolicyService rolePolicyService) {
        this.userAccountMapper = userAccountMapper;
        this.roleMapper = roleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.rolePolicyService = rolePolicyService;
    }

    @Override
    public List<UserResponse> listAll() {
        // 查询未删除用户
        Long tenantId = TenantContext.requireTenantId();
        List<UserAccount> users = userAccountMapper.selectList(
            new QueryWrapper<UserAccount>()
                .eq("tenant_id", tenantId)
                .isNull("deleted_at")
                .orderByAsc("id")
        );
        return users.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public PageResponse<UserResponse> page(long page, long size, String keyword, Boolean enabled) {
        // 构建分页与查询条件
        Long tenantId = TenantContext.requireTenantId();
        Page<UserAccount> pageReq = Page.of(page, size);
        QueryWrapper<UserAccount> wrapper = new QueryWrapper<UserAccount>()
            .eq("tenant_id", tenantId)
            .isNull("deleted_at")
            .orderByAsc("id");
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(q -> q.like("username", keyword)
                .or()
                .like("display_name", keyword)
                .or()
                .like("email", keyword)
                .or()
                .like("phone", keyword));
        }
        if (enabled != null) {
            wrapper.eq("is_enabled", enabled);
        }
        Page<UserAccount> result = userAccountMapper.selectPage(pageReq, wrapper);
        List<UserResponse> items = result.getRecords().stream().map(this::toResponse).collect(Collectors.toList());
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), items);
    }

    @Override
    public UserResponse getById(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        UserAccount user = userAccountMapper.findActiveById(tenantId, id);
        if (user == null) {
            return null;
        }
        return toResponse(user);
    }

    @Override
    @Transactional
    @AuditLog(action = "USER_CREATE", entityType = "user", entityId = "{result.id}", detail = "username={arg0.username}")
    public UserResponse create(UserCreateRequest request) {
        // 校验用户名唯一
        Long tenantId = TenantContext.requireTenantId();
        List<Role> roles = validateRoles(tenantId, request.roleIds());
        ensureAssignableRoles(tenantId, roles);
        enforceReservedRoleAssignmentRules(List.of(), roles);
        UserAccount existing = userAccountMapper.findActiveByUsername(tenantId, request.username());
        if (existing != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        UserAccount user = new UserAccount();
        user.setTenantId(tenantId);
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setAvatarUrl(request.avatarUrl());
        user.setEnabled(request.enabled() == null || request.enabled());
        user.setAccountNonExpired(request.accountNonExpired() == null || request.accountNonExpired());
        user.setAccountNonLocked(request.accountNonLocked() == null || request.accountNonLocked());
        user.setCredentialsNonExpired(request.credentialsNonExpired() == null || request.credentialsNonExpired());
        user.setAuthVersion(0);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        userAccountMapper.insert(user);
        for (Role role : roles) {
            userRoleMapper.insertIgnore(tenantId, user.getId(), role.getId());
        }
        return toResponse(user);
    }

    @Override
    @Transactional
    @AuditLog(action = "USER_UPDATE", entityType = "user", entityId = "{arg0}", detail = "username={arg1.username}")
    public UserResponse update(Long id, UserUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        UserAccount user = userAccountMapper.findActiveById(tenantId, id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        List<Role> roles = validateRoles(tenantId, request.roleIds());
        List<Role> currentRoles = roleMapper.findByUserId(tenantId, id);
        ensureAssignableRoles(tenantId, roles);
        enforceReservedRoleAssignmentRules(currentRoles, roles);
        UserAccount existing = userAccountMapper.findActiveByUsername(tenantId, request.username());
        if (existing != null && !existing.getId().equals(id)) {
            throw new IllegalArgumentException("用户名已存在");
        }
        user.setUsername(request.username());
        user.setDisplayName(request.displayName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setAvatarUrl(request.avatarUrl());
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }
        if (request.accountNonExpired() != null) {
            user.setAccountNonExpired(request.accountNonExpired());
        }
        if (request.accountNonLocked() != null) {
            user.setAccountNonLocked(request.accountNonLocked());
        }
        if (request.credentialsNonExpired() != null) {
            user.setCredentialsNonExpired(request.credentialsNonExpired());
        }
        user.setUpdatedAt(Instant.now());
        userAccountMapper.updateById(user);
        userRoleMapper.deleteByUserId(tenantId, id);
        for (Role role : roles) {
            userRoleMapper.insertIgnore(tenantId, id, role.getId());
        }
        userAccountMapper.incrementAuthVersionById(tenantId, id);
        return toResponse(user);
    }

    @Override
    @AuditLog(action = "USER_STATUS_UPDATE", entityType = "user", entityId = "{arg0}", detail = "enabled={arg1.enabled}")
    @Transactional
    public void updateStatus(Long id, UserStatusUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        UserAccount user = userAccountMapper.findActiveById(tenantId, id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        // 禁止禁用自身
        if (isDisableRequest(request)) {
            String currentUser = resolveCurrentUsername();
            if (currentUser != null && currentUser.equals(user.getUsername())) {
                throw new IllegalArgumentException("不能禁用自身");
            }
            ensureReservedRoleMemberCanBeDisabled(tenantId, id);
        }
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }
        if (request.accountNonExpired() != null) {
            user.setAccountNonExpired(request.accountNonExpired());
        }
        if (request.accountNonLocked() != null) {
            user.setAccountNonLocked(request.accountNonLocked());
        }
        if (request.credentialsNonExpired() != null) {
            user.setCredentialsNonExpired(request.credentialsNonExpired());
        }
        user.setUpdatedAt(Instant.now());
        userAccountMapper.updateById(user);
    }

    @Override
    @Transactional
    @AuditLog(action = "USER_DELETE", entityType = "user", entityId = "{arg0}")
    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        UserAccount user = userAccountMapper.findActiveById(tenantId, id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        // 禁止删除自身
        String currentUser = resolveCurrentUsername();
        if (currentUser != null && currentUser.equals(user.getUsername())) {
            throw new IllegalArgumentException("不能删除自身");
        }
        ensureReservedRoleMemberCanBeDeleted(tenantId, id);
        userAccountMapper.softDelete(tenantId, id);
        userRoleMapper.deleteByUserId(tenantId, id);
    }

    @Override
    @AuditLog(action = "USER_PASSWORD_CHANGE", entityType = "user", entityId = "{arg0}")
    @Transactional
    public void changePassword(Long id, UserPasswordChangeRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        UserAccount user = userAccountMapper.findActiveById(tenantId, id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("原密码不正确");
        }
        userAccountMapper.updatePasswordHash(tenantId, id, passwordEncoder.encode(request.newPassword()));
    }

    @Override
    @AuditLog(action = "USER_PASSWORD_RESET", entityType = "user", entityId = "{arg0}")
    @Transactional
    public void resetPassword(Long id, UserPasswordResetRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        UserAccount user = userAccountMapper.findActiveById(tenantId, id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        userAccountMapper.updatePasswordHash(tenantId, id, passwordEncoder.encode(request.newPassword()));
    }

    @Override
    public List<Role> listRoles(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        UserAccount user = userAccountMapper.findActiveById(tenantId, id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return roleMapper.findByUserId(tenantId, id);
    }

    @Override
    public List<RoleOptionResponse> listRoleOptions() {
        Long tenantId = TenantContext.requireTenantId();
        boolean actorIsSuperAdmin = rolePolicyService.currentActorHasRole("super_admin");
        List<Role> roles = roleMapper.selectList(new QueryWrapper<Role>()
            .eq("tenant_id", tenantId)
            .eq("is_enabled", true)
            .orderByAsc("id"));
        Set<String> actorPermissionCodes = rolePolicyService.currentActorPermissionCodes();
        return roles.stream()
            .filter(role -> actorIsSuperAdmin || rolePolicyService.isRolePermissionSubset(tenantId, role, actorPermissionCodes))
            .map(role -> new RoleOptionResponse(role.getId(), role.getCode(), role.getName()))
            .toList();
    }

    @Override
    @Transactional
    @AuditLog(action = "USER_ROLE_SET", entityType = "user", entityId = "{arg0}", detail = "roleIds={arg1.roleIds}")
    public void setRoles(Long id, UserRoleUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        UserAccount user = userAccountMapper.findActiveById(tenantId, id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        List<Long> roleIds = request.roleIds();
        List<Role> roles = validateRoles(tenantId, roleIds);
        List<Role> currentRoles = roleMapper.findByUserId(tenantId, id);
        ensureAssignableRoles(tenantId, roles);
        enforceReservedRoleAssignmentRules(currentRoles, roles);
        userRoleMapper.deleteByUserId(tenantId, id);
        for (Long roleId : roleIds) {
            userRoleMapper.insertIgnore(tenantId, id, roleId);
        }
        // 角色变更后刷新权限版本
        userAccountMapper.incrementAuthVersionById(tenantId, id);
    }

    // 转换为响应对象
    private UserResponse toResponse(UserAccount user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getDisplayName(),
            user.getEmail(),
            user.getPhone(),
            user.getAvatarUrl(),
            user.isEnabled(),
            user.isAccountNonExpired(),
            user.isAccountNonLocked(),
            user.isCredentialsNonExpired(),
            user.getLastLoginAt(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }

    // 获取当前登录用户名
    private String resolveCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }

    // 判断是否为禁用请求
    private boolean isDisableRequest(UserStatusUpdateRequest request) {
        return Boolean.FALSE.equals(request.enabled())
            || Boolean.FALSE.equals(request.accountNonExpired())
            || Boolean.FALSE.equals(request.accountNonLocked())
            || Boolean.FALSE.equals(request.credentialsNonExpired());
    }

    private void enforceReservedRoleAssignmentRules(List<Role> currentRoles, List<Role> nextRoles) {
        Set<String> currentRoleCodes = normalizeRoleCodes(currentRoles);
        Set<String> nextRoleCodes = normalizeRoleCodes(nextRoles);
        boolean actorIsAdmin = rolePolicyService.currentActorHasRole("admin");
        boolean actorIsSuperAdmin = rolePolicyService.currentActorHasRole("super_admin");

        if (nextRoleCodes.contains("super_admin") && !actorIsSuperAdmin) {
            throw new IllegalArgumentException("仅系统超级管理员可分配 super_admin 角色");
        }
        if ((nextRoleCodes.contains("admin") || currentRoleCodes.contains("admin")) && !(actorIsAdmin || actorIsSuperAdmin)) {
            throw new IllegalArgumentException("仅租户管理员或系统超级管理员可调整 admin 角色");
        }
        if (currentRoleCodes.contains("admin") && !nextRoleCodes.contains("admin")) {
            ensureRoleHasOtherMembers("admin", "不能移除当前租户最后一个 admin");
        }
        if (currentRoleCodes.contains("super_admin") && !nextRoleCodes.contains("super_admin")) {
            ensureRoleHasOtherMembers("super_admin", "不能移除系统最后一个 super_admin");
        }
    }

    private void ensureReservedRoleMemberCanBeDisabled(Long tenantId, Long userId) {
        if (isLastRoleMember(tenantId, userId, "admin")) {
            throw new IllegalArgumentException("不能禁用当前租户最后一个 admin");
        }
        if (isLastRoleMember(tenantId, userId, "super_admin")) {
            throw new IllegalArgumentException("不能禁用系统最后一个 super_admin");
        }
    }

    private void ensureReservedRoleMemberCanBeDeleted(Long tenantId, Long userId) {
        if (isLastRoleMember(tenantId, userId, "admin")) {
            throw new IllegalArgumentException("不能删除当前租户最后一个 admin");
        }
        if (isLastRoleMember(tenantId, userId, "super_admin")) {
            throw new IllegalArgumentException("不能删除系统最后一个 super_admin");
        }
    }

    private boolean isLastRoleMember(Long tenantId, Long userId, String roleCode) {
        Role role = roleMapper.findByCode(tenantId, roleCode);
        if (role == null) {
            return false;
        }
        List<Long> userIds = userRoleMapper.findUserIdsByRoleId(tenantId, role.getId());
        return userIds.contains(userId) && userIds.size() <= 1;
    }

    private void ensureRoleHasOtherMembers(String roleCode, String message) {
        Long tenantId = TenantContext.requireTenantId();
        Role role = roleMapper.findByCode(tenantId, roleCode);
        if (role == null) {
            return;
        }
        long memberCount = userRoleMapper.countUsersByRoleId(tenantId, role.getId());
        if (memberCount <= 1) {
            throw new IllegalArgumentException(message);
        }
    }

    private void ensureAssignableRoles(Long tenantId, List<Role> roles) {
        if (rolePolicyService.currentActorHasRole("super_admin")) {
            return;
        }
        Set<String> actorPermissionCodes = rolePolicyService.currentActorPermissionCodes();
        List<String> exceededRoles = new ArrayList<>();
        for (Role role : roles) {
            if (!rolePolicyService.isRolePermissionSubset(tenantId, role, actorPermissionCodes)) {
                exceededRoles.add(role.getName() == null || role.getName().isBlank() ? role.getCode() : role.getName());
            }
        }
        if (!exceededRoles.isEmpty()) {
            throw new IllegalArgumentException("存在权限集合超出当前账号范围的角色，不能分配: " + String.join("、", exceededRoles));
        }
    }

    private List<Role> validateRoles(Long tenantId, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            throw new IllegalArgumentException("角色列表不能为空");
        }
        List<Role> roles = roleMapper.selectList(new QueryWrapper<Role>()
            .eq("tenant_id", tenantId)
            .in("id", roleIds));
        if (roles.size() != roleIds.size()) {
            throw new IllegalArgumentException("存在无效角色 ID");
        }
        return roles;
    }

    private Set<String> normalizeRoleCodes(List<Role> roles) {
        Set<String> roleCodes = new HashSet<>();
        if (roles == null) {
            return roleCodes;
        }
        for (Role role : roles) {
            if (role != null && role.getCode() != null && !role.getCode().isBlank()) {
                roleCodes.add(role.getCode().trim().toLowerCase(Locale.ROOT));
            }
        }
        return roleCodes;
    }
}
