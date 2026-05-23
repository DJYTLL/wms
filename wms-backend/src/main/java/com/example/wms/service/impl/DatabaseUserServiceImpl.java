package com.example.wms.service.impl;

import com.example.wms.dto.AuthPayload;
import com.example.wms.dto.UserClaim;
import com.example.wms.entity.Permission;
import com.example.wms.entity.Role;
import com.example.wms.entity.UserAccount;
import com.example.wms.mapper.PermissionMapper;
import com.example.wms.mapper.RoleMapper;
import com.example.wms.mapper.TenantMapper;
import com.example.wms.mapper.UserAccountMapper;
import com.example.wms.security.AuthenticatedUser;
import com.example.wms.service.UserAccountService;
import com.example.wms.tenant.TenantContext;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

// 基于数据库的用户服务实现
@Service
public class DatabaseUserServiceImpl implements UserAccountService {
    private final UserAccountMapper userAccountMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final TenantMapper tenantMapper;

    public DatabaseUserServiceImpl(UserAccountMapper userAccountMapper,
                                   RoleMapper roleMapper,
                                   PermissionMapper permissionMapper,
                                   TenantMapper tenantMapper) {
        this.userAccountMapper = userAccountMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.tenantMapper = tenantMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 加载用户及其角色权限
        AuthContext context = loadAuthContext(username);
        return AuthenticatedUser.fromDatabase(
            context.user(),
            buildAuthPayload(context),
            buildAuthorities(context.roles(), context.permissions())
        );
    }

    @Override
    public AuthPayload loadAuthPayload(String username) {
        // 组装 JWT 载荷需要的用户对象与权限列表
        return buildAuthPayload(loadAuthContext(username));
    }

    private AuthPayload buildAuthPayload(AuthContext context) {
        UserAccount user = context.user();
        String role = resolvePrimaryRole(context.roles());
        List<String> roleCodes = context.roles().stream()
            .map(Role::getCode)
            .filter(code -> code != null && !code.isBlank())
            .map(code -> code.toLowerCase(Locale.ROOT))
            .distinct()
            .collect(Collectors.toList());
        UserClaim userClaim = new UserClaim(user.getId(), user.getUsername(), role, user.getAvatarUrl(), roleCodes);
        List<String> permissionCodes = context.permissions().stream()
            .map(Permission::getCode)
            .collect(Collectors.toList());
        return new AuthPayload(userClaim,
            permissionCodes,
            user.getAuthVersion(),
            user.getTenantId(),
            context.tenantCode(),
            user.getTenantId(),
            context.tenantCode());
    }

    private List<GrantedAuthority> buildAuthorities(List<Role> roles, List<Permission> permissions) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (Role role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode()));
        }
        for (Permission permission : permissions) {
            authorities.add(new SimpleGrantedAuthority("PERM_" + permission.getCode()));
        }
        return authorities;
    }

    @Override
    public long loadAuthVersion(String username) {
        Long tenantId = TenantContext.requireTenantId();
        Long version = userAccountMapper.findAuthVersionByUsername(tenantId, username);
        return version == null ? 0L : version;
    }

    @Override
    public UserAccount loadUserAccount(String username) {
        Long tenantId = TenantContext.requireTenantId();
        UserAccount user = userAccountMapper.findActiveByUsername(tenantId, username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }
        return user;
    }

    // 加载用户、角色与权限的组合上下文
    private AuthContext loadAuthContext(String username) {
        Long tenantId = TenantContext.requireTenantId();
        UserAccount user = userAccountMapper.findActiveByUsername(tenantId, username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        List<Role> roles = roleMapper.findByUserId(tenantId, user.getId());
        List<Long> roleIds = roles.stream().map(Role::getId).collect(Collectors.toList());
        List<Permission> permissions = roleIds.isEmpty()
            ? List.of()
            : permissionMapper.findByRoleIds(roleIds);

        return new AuthContext(user, roles, permissions, resolveTenantCode(user.getTenantId()));
    }

    // 内部上下文：一次性返回用户、角色、权限
    private record AuthContext(UserAccount user,
                               List<Role> roles,
                               List<Permission> permissions,
                               String tenantCode) {
    }

    private String resolvePrimaryRole(List<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return "user";
        }
        boolean hasSuperAdmin = roles.stream()
            .map(Role::getCode)
            .anyMatch(code -> code != null && "super_admin".equalsIgnoreCase(code));
        if (hasSuperAdmin) {
            return "super_admin";
        }
        return roles.get(0).getCode().toLowerCase(Locale.ROOT);
    }

    private String resolveTenantCode(Long tenantId) {
        if (tenantId == null) {
            return null;
        }
        var tenant = tenantMapper.selectById(tenantId);
        return tenant == null ? null : tenant.getCode();
    }
}
