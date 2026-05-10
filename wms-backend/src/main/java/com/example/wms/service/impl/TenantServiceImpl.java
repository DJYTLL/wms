package com.example.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.wms.config.PermissionSeedProvider;
import com.example.wms.dto.AuthPayload;
import com.example.wms.dto.TenantCreateRequest;
import com.example.wms.dto.TenantResponse;
import com.example.wms.dto.TenantStatusUpdateRequest;
import com.example.wms.dto.TenantSwitchRequest;
import com.example.wms.dto.TenantUpdateRequest;
import com.example.wms.dto.TokenPairResponse;
import com.example.wms.entity.Menu;
import com.example.wms.entity.Permission;
import com.example.wms.entity.Role;
import com.example.wms.entity.Tenant;
import com.example.wms.entity.TenantMenu;
import com.example.wms.entity.UserAccount;
import com.example.wms.exception.NotFoundException;
import com.example.wms.mapper.MenuMapper;
import com.example.wms.mapper.PermissionMapper;
import com.example.wms.mapper.RoleMapper;
import com.example.wms.mapper.RolePermissionMapper;
import com.example.wms.mapper.TenantMapper;
import com.example.wms.mapper.TenantMenuMapper;
import com.example.wms.mapper.UserAccountMapper;
import com.example.wms.mapper.UserRoleMapper;
import com.example.wms.security.JwtTokenService;
import com.example.wms.service.RefreshTokenService;
import com.example.wms.service.TenantService;
import com.example.wms.service.UserAccountService;
import com.example.wms.tenant.TenantContext;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 租户管理服务实现
@Service
public class TenantServiceImpl implements TenantService {
    private final TenantMapper tenantMapper;
    private final PermissionMapper permissionMapper;
    private final MenuMapper menuMapper;
    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final UserAccountMapper userAccountMapper;
    private final UserRoleMapper userRoleMapper;
    private final TenantMenuMapper tenantMenuMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserAccountService userAccountService;
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenService jwtTokenService;

    @Value("${app.admin.username:admin}")
    private String defaultAdminUsername;

    @Value("${app.admin.password:password}")
    private String defaultAdminPassword;

    public TenantServiceImpl(TenantMapper tenantMapper,
                             PermissionMapper permissionMapper,
                             MenuMapper menuMapper,
                             RoleMapper roleMapper,
                             RolePermissionMapper rolePermissionMapper,
                             UserAccountMapper userAccountMapper,
                             UserRoleMapper userRoleMapper,
                             TenantMenuMapper tenantMenuMapper,
                             PasswordEncoder passwordEncoder,
                             UserAccountService userAccountService,
                             RefreshTokenService refreshTokenService,
                             JwtTokenService jwtTokenService) {
        this.tenantMapper = tenantMapper;
        this.permissionMapper = permissionMapper;
        this.menuMapper = menuMapper;
        this.roleMapper = roleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.userAccountMapper = userAccountMapper;
        this.userRoleMapper = userRoleMapper;
        this.tenantMenuMapper = tenantMenuMapper;
        this.passwordEncoder = passwordEncoder;
        this.userAccountService = userAccountService;
        this.refreshTokenService = refreshTokenService;
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public List<TenantResponse> listAll() {
        return tenantMapper.selectList(new QueryWrapper<Tenant>()
                .isNull("deleted_at")
                .orderByAsc("id"))
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    public TenantResponse getById(Long id) {
        Tenant tenant = tenantMapper.selectById(id);
        return tenant == null || tenant.getDeletedAt() != null ? null : toResponse(tenant);
    }

    @Override
    public TenantResponse create(TenantCreateRequest request) {
        Tenant existing = tenantMapper.selectOne(new QueryWrapper<Tenant>().eq("code", request.code()));
        if (existing != null) {
            throw new IllegalArgumentException("租户编码已存在");
        }
        Tenant tenant = new Tenant();
        tenant.setCode(request.code());
        tenant.setName(request.name());
        tenant.setEnabled(true);
        tenant.setCreatedAt(Instant.now());
        tenant.setUpdatedAt(Instant.now());
        tenantMapper.insert(tenant);

        Long tenantId = tenant.getId();
        List<Permission> permissions = PermissionSeedProvider.permissionSeeds().stream()
            .map(seed -> ensurePermission(seed.code(), seed.name(), seed.description()))
            .toList();
        List<Menu> menus = menuMapper.listAllOrdered();
        initTenantMenus(tenantId, menus, request.menuIds());

        Role adminRole = ensureRole(tenantId, "admin", "租户管理员", "当前租户内的超级管理员");
        for (Permission permission : permissions) {
            if (!isTenantPermission(permission)) {
                rolePermissionMapper.insertIgnore(tenantId, adminRole.getId(), permission.getId());
            }
        }

        String adminUsername = resolveAdminUsername(request.adminUsername());
        String adminPassword = resolveAdminPassword(request.adminPassword());
        UserAccount existingAdmin = userAccountMapper.findActiveByUsername(tenantId, adminUsername);
        if (existingAdmin != null) {
            throw new IllegalArgumentException("租户管理员用户名已存在");
        }
        UserAccount admin = new UserAccount();
        admin.setTenantId(tenantId);
        admin.setUsername(adminUsername);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setDisplayName("租户管理员");
        admin.setEnabled(true);
        admin.setAccountNonExpired(true);
        admin.setAccountNonLocked(true);
        admin.setCredentialsNonExpired(true);
        admin.setAuthVersion(0);
        admin.setCreatedAt(Instant.now());
        admin.setUpdatedAt(Instant.now());
        userAccountMapper.insert(admin);
        userRoleMapper.insertIgnore(tenantId, admin.getId(), adminRole.getId());

        return toResponse(tenant);
    }

    @Override
    public TenantResponse updateStatus(Long id, TenantStatusUpdateRequest request) {
        Tenant tenant = tenantMapper.selectById(id);
        if (tenant == null || tenant.getDeletedAt() != null) {
            throw new NotFoundException("租户不存在");
        }
        tenant.setEnabled(request.enabled());
        tenant.setUpdatedAt(Instant.now());
        tenantMapper.updateById(tenant);
        return toResponse(tenant);
    }

    @Override
    public TenantResponse updateName(Long id, TenantUpdateRequest request) {
        Tenant tenant = tenantMapper.selectById(id);
        if (tenant == null || tenant.getDeletedAt() != null) {
            throw new NotFoundException("租户不存在");
        }
        tenant.setName(request.name());
        tenant.setUpdatedAt(Instant.now());
        tenantMapper.updateById(tenant);
        return toResponse(tenant);
    }

    @Override
    public void delete(Long id) {
        Tenant tenant = tenantMapper.selectById(id);
        if (tenant == null || tenant.getDeletedAt() != null) {
            throw new NotFoundException("租户不存在");
        }
        if ("default".equalsIgnoreCase(tenant.getCode())) {
            throw new IllegalArgumentException("默认租户不允许删除");
        }
        tenant.setEnabled(false);
        tenant.setDeletedAt(Instant.now());
        tenant.setUpdatedAt(Instant.now());
        tenantMapper.updateById(tenant);
    }

    @Override
    public TokenPairResponse switchTenant(TenantSwitchRequest request, String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("缺少访问令牌");
        }
        String currentUsername = resolveCurrentUsername();
        if (currentUsername == null || currentUsername.isBlank()) {
            throw new IllegalArgumentException("当前登录态无效");
        }
        Claims claims = jwtTokenService.parseToken(authorizationHeader.substring(7));
        Long authTenantId = resolveAuthTenantId(claims);
        if (authTenantId == null) {
            throw new IllegalArgumentException("访问令牌缺少认证租户");
        }
        Tenant targetTenant = tenantMapper.findByCode(request.tenantCode());
        if (targetTenant == null || targetTenant.getDeletedAt() != null || !targetTenant.isEnabled()) {
            throw new IllegalArgumentException("目标租户不存在或已停用");
        }
        try {
            TenantContext.setTenantId(authTenantId);
            AuthPayload sourcePayload = userAccountService.loadAuthPayload(currentUsername);
            UserAccount sourceUser = userAccountService.loadUserAccount(currentUsername);
            if (!hasRole(sourcePayload, "super_admin")) {
                throw new IllegalArgumentException("仅系统超级管理员可跨租户操作");
            }
            AuthPayload targetPayload = new AuthPayload(
                sourcePayload.user(),
                sourcePayload.permissions(),
                sourcePayload.authVersion(),
                targetTenant.getId(),
                targetTenant.getCode(),
                sourcePayload.userTenantId(),
                sourcePayload.userTenantCode()
            );
            return refreshTokenService.issueTokens(sourceUser, targetPayload);
        } finally {
            TenantContext.clear();
        }
    }

    private Permission ensurePermission(String code, String name, String description) {
        Permission existing = permissionMapper.findByCode(code);
        if (existing != null) {
            return existing;
        }
        Permission permission = new Permission();
        permission.setCode(code);
        permission.setName(name);
        permission.setDescription(description);
        permission.setEnabled(true);
        permission.setCreatedAt(Instant.now());
        permission.setUpdatedAt(Instant.now());
        permissionMapper.insert(permission);
        return permission;
    }

    private Role ensureRole(Long tenantId, String code, String name, String description) {
        Role role = roleMapper.findByCode(tenantId, code);
        if (role != null) {
            return role;
        }
        Role created = new Role();
        created.setTenantId(tenantId);
        created.setCode(code);
        created.setName(name);
        created.setDescription(description);
        created.setEnabled(true);
        created.setCreatedAt(Instant.now());
        created.setUpdatedAt(Instant.now());
        roleMapper.insert(created);
        return created;
    }

    private String resolveAdminUsername(String adminUsername) {
        if (adminUsername != null && !adminUsername.isBlank()) {
            return adminUsername;
        }
        return defaultAdminUsername;
    }

    private String resolveAdminPassword(String adminPassword) {
        return adminPassword == null || adminPassword.isBlank() ? defaultAdminPassword : adminPassword;
    }

    private String resolveCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }

    private Long resolveAuthTenantId(Claims claims) {
        Number userTenantValue = claims.get("utid", Number.class);
        if (userTenantValue != null) {
            return userTenantValue.longValue();
        }
        Number tenantValue = claims.get("tid", Number.class);
        return tenantValue == null ? null : tenantValue.longValue();
    }

    private boolean hasRole(AuthPayload payload, String roleCode) {
        if (payload == null || payload.user() == null || payload.user().roles() == null) {
            return false;
        }
        return payload.user().roles().stream().anyMatch(roleCode::equalsIgnoreCase);
    }

    private TenantResponse toResponse(Tenant tenant) {
        return new TenantResponse(
            tenant.getId(),
            tenant.getCode(),
            tenant.getName(),
            tenant.isEnabled(),
            tenant.getCreatedAt(),
            tenant.getUpdatedAt()
        );
    }

    private boolean isTenantPermission(Permission permission) {
        String code = permission.getCode();
        return code != null && code.startsWith("tenant:");
    }

    private void initTenantMenus(Long tenantId, List<Menu> menus, List<Long> enabledMenuIds) {
        List<TenantMenu> existing = tenantMenuMapper.findByTenantId(tenantId);
        if (existing != null && !existing.isEmpty()) {
            return;
        }
        Set<Long> menuIdSet = menus.stream().map(Menu::getId).collect(java.util.stream.Collectors.toSet());
        Set<Long> enabledIds = resolveEnabledMenuIds(enabledMenuIds, menuIdSet, menus);
        for (Menu menu : menus) {
            TenantMenu tenantMenu = new TenantMenu();
            tenantMenu.setTenantId(tenantId);
            tenantMenu.setMenuId(menu.getId());
            tenantMenu.setEnabled(enabledIds.contains(menu.getId()));
            tenantMenu.setCreatedAt(Instant.now());
            tenantMenu.setUpdatedAt(Instant.now());
            tenantMenuMapper.insert(tenantMenu);
        }
    }

    private Set<Long> resolveEnabledMenuIds(List<Long> enabledMenuIds,
                                            Set<Long> menuIdSet,
                                            List<Menu> menus) {
        if (enabledMenuIds == null || enabledMenuIds.isEmpty()) {
            return new HashSet<>(menuIdSet);
        }
        Set<Long> enabled = enabledMenuIds.stream()
            .filter(menuIdSet::contains)
            .collect(java.util.stream.Collectors.toSet());
        return expandParentMenuIds(enabled, menus);
    }

    private Set<Long> expandParentMenuIds(Set<Long> enabledIds, List<Menu> menus) {
        Map<Long, Menu> menuMap = menus.stream()
            .collect(java.util.stream.Collectors.toMap(Menu::getId, menu -> menu));
        Set<Long> expanded = new HashSet<>(enabledIds);
        for (Long menuId : enabledIds) {
            Menu menu = menuMap.get(menuId);
            while (menu != null && menu.getParentId() != null) {
                Long parentId = menu.getParentId();
                if (!expanded.add(parentId)) {
                    break;
                }
                menu = menuMap.get(parentId);
            }
        }
        return expanded;
    }
}
