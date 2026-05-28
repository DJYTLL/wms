package com.example.wms;

import com.example.wms.audit.RequestAuditContext;
import com.example.wms.config.PermissionSeedProvider;
import com.example.wms.config.SecurityConfig;
import com.example.wms.controller.AuthController;
import com.example.wms.controller.CurrentAuthorizationController;
import com.example.wms.controller.UserController;
import com.example.wms.dto.AuthPayload;
import com.example.wms.dto.AuthorizationContextResponse;
import com.example.wms.dto.PermissionUpdateRequest;
import com.example.wms.dto.TokenPairResponse;
import com.example.wms.dto.RoleUpdateRequest;
import com.example.wms.dto.RoleCreateRequest;
import com.example.wms.dto.UserClaim;
import com.example.wms.dto.UserCreateRequest;
import com.example.wms.dto.UserRoleUpdateRequest;
import com.example.wms.entity.Role;
import com.example.wms.entity.Permission;
import com.example.wms.entity.Tenant;
import com.example.wms.entity.UserAccount;
import com.example.wms.mapper.MenuMapper;
import com.example.wms.mapper.PermissionMapper;
import com.example.wms.mapper.RoleColumnSettingMapper;
import com.example.wms.mapper.RoleMapper;
import com.example.wms.mapper.RolePermissionMapper;
import com.example.wms.mapper.TenantMapper;
import com.example.wms.mapper.TenantColumnSettingMapper;
import com.example.wms.mapper.TenantMenuMapper;
import com.example.wms.mapper.UserAccountMapper;
import com.example.wms.mapper.UserRoleMapper;
import com.example.wms.security.AuthenticatedUser;
import com.example.wms.security.JwtAuthenticationFilter;
import com.example.wms.security.JwtTokenService;
import com.example.wms.service.RefreshTokenService;
import com.example.wms.service.AuthorizationContextService;
import com.example.wms.service.RolePolicyService;
import com.example.wms.service.RoleScopeService;
import com.example.wms.service.UserAccountService;
import com.example.wms.service.impl.RolePermissionServiceImpl;
import com.example.wms.service.impl.PermissionServiceImpl;
import com.example.wms.service.impl.RoleServiceImpl;
import com.example.wms.service.impl.TenantServiceImpl;
import com.example.wms.service.impl.UserServiceImpl;
import com.example.wms.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthPermissionIntegrationTests {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserAccountService userAccountService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private AuthorizationContextService authorizationContextService;
    @Mock
    private TenantMapper tenantMapper;
    @Mock
    private UserAccountMapper userAccountMapper;
    @Mock
    private PermissionMapper permissionMapper;
    @Mock
    private MenuMapper menuMapper;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private RoleColumnSettingMapper roleColumnSettingMapper;
    @Mock
    private RolePermissionMapper rolePermissionMapper;
    @Mock
    private TenantColumnSettingMapper tenantColumnSettingMapper;
    @Mock
    private UserRoleMapper userRoleMapper;
    @Mock
    private TenantMenuMapper tenantMenuMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
        RequestAuditContext.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
        RequestAuditContext.clear();
    }

    @Test
    void loginSuccessReturnsAccessTokenAndRefreshCookie() throws Exception {
        Tenant tenant = tenant(1L, "default");
        UserAccount user = user(10L, 1L, "admin");
        AuthPayload payload = authPayload("admin", 1L, "default", List.of("admin"));
        TokenPairResponse issued = new TokenPairResponse("access-token", "refresh-token", payload);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(
            authenticationManager, userAccountService, refreshTokenService, tenantMapper, userAccountMapper
        )).build();

        when(tenantMapper.findByCode("default")).thenReturn(tenant);
        when(userAccountMapper.findActiveByUsername(1L, "admin")).thenReturn(user);
        when(authenticationManager.authenticate(any())).thenReturn(
            new UsernamePasswordAuthenticationToken("admin", "password", List.of())
        );
        when(userAccountService.loadAuthPayload("admin")).thenReturn(payload);
        when(userAccountService.loadUserAccount("admin")).thenReturn(user);
        when(refreshTokenService.issueTokens(user, payload)).thenReturn(issued);

        MvcResult result = mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"tenantCode":"default","username":"admin","password":"password"}
                    """))
            .andExpect(status().isOk())
            .andReturn();

        String body = result.getResponse().getContentAsString();
        TokenPairResponse response = OBJECT_MAPPER.readTree(body)
            .path("data")
            .traverse(OBJECT_MAPPER)
            .readValueAs(TokenPairResponse.class);

        assertThat(body).doesNotContain("\"permissions\"");
        assertThat(body).doesNotContain("\"authPayload\"");
        assertThat(response.token()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isNull();
        assertThat(response.authPayload()).isNull();
        assertThat(result.getResponse().getHeader("Set-Cookie"))
            .contains("refreshToken=refresh-token")
            .contains("HttpOnly")
            .contains("Path=/api")
            .contains("SameSite=Strict");
    }

    @Test
    void generatedJwtDoesNotContainPermissionsClaim() {
        JwtTokenService service = new JwtTokenService(
            "ChangeThisToAStrongSecretKeyForJwtSigning123!",
            "wms-backend",
            120
        );

        AuthPayload payload = new AuthPayload(
            new UserClaim(10L, "admin", "super_admin", null, List.of("super_admin", "admin")),
            List.of("tenant:switch", "menu:view", "column:erp-product:cost"),
            7L,
            2L,
            "tenant-b",
            1L,
            "default"
        );

        Claims claims = service.parseToken(service.generateToken(payload));

        assertThat(claims.get("perms")).isNull();
        assertThat(claims.get("tid", Number.class).longValue()).isEqualTo(2L);
        assertThat(claims.get("utid", Number.class).longValue()).isEqualTo(1L);
    }

    @Test
    void superAdminRoleHierarchyReachesAllSeededPermissions() throws Exception {
        Method method = SecurityConfig.class.getDeclaredMethod("superAdminRoleHierarchy");
        RoleHierarchy roleHierarchy = (RoleHierarchy) method.invoke(new SecurityConfig());

        var authorities = roleHierarchy.getReachableGrantedAuthorities(List.of(
            new SimpleGrantedAuthority("ROLE_super_admin")
        ));
        var authorityCodes = authorities.stream()
            .map(authority -> authority.getAuthority())
            .collect(Collectors.toSet());

        assertThat(authorityCodes)
            .contains("ROLE_super_admin", "PERM_erp-product:view", "PERM_erp-sale-draft:add");
        assertThat(authorityCodes)
            .containsAll(PermissionSeedProvider.permissionSeeds().stream()
                .map(seed -> "PERM_" + seed.code())
                .collect(Collectors.toSet()));
    }

    @Test
    void methodSecurityExpressionHandlerUsesSuperAdminHierarchy() throws Exception {
        Method hierarchyMethod = SecurityConfig.class.getDeclaredMethod("superAdminRoleHierarchy");
        RoleHierarchy roleHierarchy = (RoleHierarchy) hierarchyMethod.invoke(new SecurityConfig());
        Method handlerMethod = SecurityConfig.class.getDeclaredMethod(
            "methodSecurityExpressionHandler",
            RoleHierarchy.class
        );
        MethodSecurityExpressionHandler handler =
            (MethodSecurityExpressionHandler) handlerMethod.invoke(new SecurityConfig(), roleHierarchy);

        var authentication = new UsernamePasswordAuthenticationToken(
            "sysadmin",
            "N/A",
            List.of(new SimpleGrantedAuthority("ROLE_super_admin"))
        );
        org.aopalliance.intercept.MethodInvocation invocation = org.mockito.Mockito.mock(
            org.aopalliance.intercept.MethodInvocation.class
        );

        var context = handler.createEvaluationContext(authentication, invocation);

        assertThat(handler.getExpressionParser()
            .parseExpression("hasAuthority('PERM_erp-product:view')")
            .getValue(context, Boolean.class)).isTrue();
        assertThat(handler.getExpressionParser()
            .parseExpression("hasAnyAuthority('PERM_erp-sale-approved:copy','PERM_erp-sale-draft:add')")
            .getValue(context, Boolean.class)).isTrue();
    }

    @Test
    void loginSuccessReusesAuthenticatedPrincipalInsteadOfReloadingUserContext() throws Exception {
        Tenant tenant = tenant(1L, "default");
        UserAccount user = user(10L, 1L, "admin");
        AuthPayload payload = authPayload("admin", 1L, "default", List.of("super_admin"));
        TokenPairResponse issued = new TokenPairResponse("access-token", "refresh-token", payload);
        AuthenticatedUser principal = AuthenticatedUser.fromDatabase(
            user,
            payload,
            List.of(new SimpleGrantedAuthority("ROLE_super_admin"))
        );
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(
            authenticationManager, userAccountService, refreshTokenService, tenantMapper, userAccountMapper
        )).build();

        when(tenantMapper.findByCode("default")).thenReturn(tenant);
        when(userAccountMapper.findActiveByUsername(1L, "admin")).thenReturn(user);
        when(authenticationManager.authenticate(any())).thenReturn(
            new UsernamePasswordAuthenticationToken(principal, "password", principal.getAuthorities())
        );
        when(refreshTokenService.issueTokens(user, payload)).thenReturn(issued);

        mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"tenantCode":"default","username":"admin","password":"password"}
                    """))
            .andExpect(status().isOk());

        verifyNoInteractions(userAccountService);
    }

    @Test
    void currentAuthorizationsReturnsFrontendAuthorizationBootstrapPayload() throws Exception {
        AuthorizationContextResponse response = new AuthorizationContextResponse(
            new UserClaim(10L, "admin", "super_admin", null, List.of("super_admin", "admin")),
            List.of("tenant:switch", "menu:view"),
            12L,
            2L,
            "tenant-b",
            1L,
            "default"
        );

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new CurrentAuthorizationController(authorizationContextService)
        ).build();

        when(authorizationContextService.getCurrent()).thenReturn(response);

        mockMvc.perform(get("/api/me/authorizations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.user.username").value("admin"))
            .andExpect(jsonPath("$.data.permissions[0]").value("tenant:switch"))
            .andExpect(jsonPath("$.data.tenantId").value(2));
    }

    @Test
    void sameUsernameCanLoginInDifferentTenantsWithTenantCode() throws Exception {
        Tenant systemTenant = tenant(1L, "default");
        Tenant secondTenant = tenant(2L, "tenant-b");
        UserAccount systemUser = user(10L, 1L, "admin");
        UserAccount secondUser = user(20L, 2L, "admin");
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(
            authenticationManager, userAccountService, refreshTokenService, tenantMapper, userAccountMapper
        )).build();

        when(authenticationManager.authenticate(any())).thenReturn(
            new UsernamePasswordAuthenticationToken("admin", "password", List.of())
        );
        when(tenantMapper.findByCode("default")).thenReturn(systemTenant);
        when(tenantMapper.findByCode("tenant-b")).thenReturn(secondTenant);
        when(userAccountMapper.findActiveByUsername(1L, "admin")).thenReturn(systemUser);
        when(userAccountMapper.findActiveByUsername(2L, "admin")).thenReturn(secondUser);
        when(userAccountService.loadAuthPayload("admin"))
            .thenReturn(authPayload("admin", 1L, "default", List.of("super_admin")))
            .thenReturn(authPayload("admin", 2L, "tenant-b", List.of("admin")));
        when(userAccountService.loadUserAccount("admin"))
            .thenReturn(systemUser)
            .thenReturn(secondUser);
        when(refreshTokenService.issueTokens(eq(systemUser), any())).thenReturn(new TokenPairResponse(
            "token-default",
            "refresh-a",
            authPayload("admin", 1L, "default", List.of("super_admin"))
        ));
        when(refreshTokenService.issueTokens(eq(secondUser), any())).thenReturn(new TokenPairResponse(
            "token-tenant-b",
            "refresh-b",
            authPayload("admin", 2L, "tenant-b", List.of("admin"))
        ));

        mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"tenantCode":"default","username":"admin","password":"password"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"tenantCode":"tenant-b","username":"admin","password":"password"}
                    """))
            .andExpect(status().isOk());

        verify(userAccountMapper).findActiveByUsername(1L, "admin");
        verify(userAccountMapper).findActiveByUsername(2L, "admin");
    }

    @Test
    void regularTenantAdminCannotSwitchTenant() {
        TenantServiceImpl service = tenantService();
        Tenant targetTenant = tenant(2L, "tenant-b");
        DefaultClaims claims = new DefaultClaims();
        claims.setSubject("tenant-admin");
        claims.put("utid", 1L);

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("tenant-admin", "N/A", List.of())
        );
        when(jwtTokenService.parseToken("token")).thenReturn(claims);
        when(tenantMapper.findByCode("tenant-b")).thenReturn(targetTenant);
        when(userAccountService.loadAuthPayload("tenant-admin"))
            .thenReturn(authPayload("tenant-admin", 1L, "default", List.of("admin")));
        when(userAccountService.loadUserAccount("tenant-admin")).thenReturn(user(10L, 1L, "tenant-admin"));

        assertThatThrownBy(() -> service.switchTenant(new com.example.wms.dto.TenantSwitchRequest("tenant-b"), "Bearer token"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("仅系统超级管理员可跨租户操作");
    }

    @Test
    void reservedRolesCannotBeCreatedOrRenamed() {
        RoleServiceImpl service = new RoleServiceImpl(
            roleMapper,
            rolePermissionMapper,
            userRoleMapper,
            userAccountMapper,
            rolePolicyService()
        );
        TenantContext.setTenantId(1L);
        Role role = new Role();
        role.setId(8L);
        role.setTenantId(1L);
        role.setCode("ops");

        assertThatThrownBy(() -> service.create(new RoleCreateRequest("super_admin", "系统管理员", null, true)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("super_admin 为系统保留角色，不能手动创建或修改");

        when(roleMapper.selectOne(any())).thenReturn(role);
        assertThatThrownBy(() -> service.update(8L, new RoleUpdateRequest("admin", "租户管理员", null, true)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("admin 为系统保留角色，不能手动创建或修改");
    }

    @Test
    void nonSuperAdminCannotAssignSuperAdminRole() {
        UserServiceImpl service = new UserServiceImpl(
            userAccountMapper,
            roleMapper,
            rolePermissionMapper,
            userRoleMapper,
            passwordEncoder,
            rolePolicyService()
        );
        TenantContext.setTenantId(1L);
        Role superAdminRole = role(99L, 1L, "super_admin");
        UserAccount targetUser = user(22L, 1L, "alice");

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "tenant-admin",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_admin"))
            )
        );
        when(userAccountMapper.findActiveById(1L, 22L)).thenReturn(targetUser);
        when(roleMapper.selectList(any())).thenReturn(List.of(superAdminRole));
        when(roleMapper.findByUserId(1L, 22L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.setRoles(22L, new UserRoleUpdateRequest(List.of(99L))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("仅系统超级管理员可分配 super_admin 角色");

        verify(userRoleMapper, never()).deleteByUserId(1L, 22L);
    }

    @Test
    void createUserWithoutRolesDoesNotInsertUser() {
        UserServiceImpl service = new UserServiceImpl(
            userAccountMapper,
            roleMapper,
            rolePermissionMapper,
            userRoleMapper,
            passwordEncoder,
            rolePolicyService()
        );
        TenantContext.setTenantId(1L);

        assertThatThrownBy(() -> service.create(new UserCreateRequest(
                "new-user",
                "password",
                "新用户",
                null,
                null,
                null,
                true,
                true,
                true,
                true,
                List.of()
            )))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("角色列表不能为空");

        verify(userAccountMapper, never()).insert(any(UserAccount.class));
        verify(userRoleMapper, never()).insertIgnore(any(), any(), any());
    }

    @Test
    void rolePermissionSavePreservesConcreteColumnPermissions() {
        RolePermissionServiceImpl service = new RolePermissionServiceImpl(
            roleMapper,
            permissionMapper,
            roleColumnSettingMapper,
            rolePermissionMapper,
            tenantColumnSettingMapper,
            userRoleMapper,
            userAccountMapper,
            roleScopeService()
        );
        TenantContext.setTenantId(1L);
        Role role = role(8L, 1L, "ops");
        Permission oldFeature = permission(1L, "erp-sale:view");
        Permission retainedColumn = permission(2L, "column:erp-sale-draft:totalAmount");
        Permission newFeature = permission(3L, "erp-purchase:view");
        Permission requestedColumn = permission(4L, "column:erp-purchase-draft:supplier");
        Permission columnManager = permission(5L, "column:role:manage");

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "sysadmin",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_super_admin"))
            )
        );
        when(roleMapper.selectOne(any())).thenReturn(role);
        when(permissionMapper.selectList(any())).thenReturn(List.of(newFeature, requestedColumn, columnManager));
        when(rolePermissionMapper.findPermissionsByRoleId(1L, 8L)).thenReturn(List.of(oldFeature, retainedColumn));
        when(userRoleMapper.findUserIdsByRoleId(1L, 8L)).thenReturn(List.of());

        service.setPermissions(8L, List.of(3L, 4L, 5L));

        verify(rolePermissionMapper).deleteByRoleId(1L, 8L);
        verify(rolePermissionMapper).insertIgnore(1L, 8L, 3L);
        verify(rolePermissionMapper).insertIgnore(1L, 8L, 5L);
        verify(rolePermissionMapper).insertIgnore(1L, 8L, 2L);
        verify(rolePermissionMapper, never()).insertIgnore(1L, 8L, 1L);
        verify(rolePermissionMapper, never()).insertIgnore(1L, 8L, 4L);
    }

    @Test
    void switchedSuperAdminCanSetTargetTenantAdminPermissions() {
        RolePermissionServiceImpl service = new RolePermissionServiceImpl(
            roleMapper,
            permissionMapper,
            roleColumnSettingMapper,
            rolePermissionMapper,
            tenantColumnSettingMapper,
            userRoleMapper,
            userAccountMapper,
            roleScopeService()
        );
        TenantContext.setTenantId(6L);
        Role tenantAdminRole = role(16L, 6L, "admin");
        Permission roleView = permission(31L, "role:view");
        Permission systemConfigView = permission(32L, "system-config:view");

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "superadmin",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_super_admin"))
            )
        );
        when(roleMapper.selectOne(any())).thenReturn(tenantAdminRole);
        when(permissionMapper.selectList(any())).thenReturn(List.of(roleView, systemConfigView));
        when(rolePermissionMapper.findPermissionsByRoleId(6L, 16L)).thenReturn(List.of());
        when(userRoleMapper.findUserIdsByRoleId(6L, 16L)).thenReturn(List.of(18L));

        service.setPermissions(16L, List.of(31L, 32L));

        verify(rolePermissionMapper).deleteByRoleId(6L, 16L);
        verify(rolePermissionMapper).insertIgnore(6L, 16L, 31L);
        verify(rolePermissionMapper).insertIgnore(6L, 16L, 32L);
        verify(userAccountMapper).incrementAuthVersionByIds(6L, List.of(18L));
    }

    @Test
    void rolePolicyUsesAuthenticatedTenantForCurrentActorRoleChecks() {
        RolePolicyService policyService = rolePolicyService();
        TenantContext.setTenantId(6L);
        Role tenantAdminRole = role(16L, 6L, "admin");
        UserAccount authUser = user(10L, 1L, "admin");
        AuthPayload payload = authPayload("admin", 6L, "fycdz", List.of("super_admin"));
        payload = new AuthPayload(
            payload.user(),
            payload.permissions(),
            payload.authVersion(),
            6L,
            "fycdz",
            1L,
            "default"
        );

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                AuthenticatedUser.fromDatabase(
                    authUser,
                    payload,
                    List.of(new SimpleGrantedAuthority("ROLE_super_admin"))
                ),
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_super_admin"))
            )
        );
        when(userRoleMapper.findUserIdsByRoleId(6L, 16L)).thenReturn(List.of(18L));

        assertThat(policyService.isCurrentActorRole(tenantAdminRole.getId())).isFalse();
        assertThat(policyService.canManageRolePermissions(tenantAdminRole)).isTrue();
    }

    @Test
    void assignablePermissionsFollowBackendRolePolicy() {
        PermissionServiceImpl service = new PermissionServiceImpl(
            permissionMapper,
            rolePermissionMapper,
            userRoleMapper,
            userAccountMapper,
            menuMapper,
            rolePolicyService()
        );
        Permission erpPermission = permission(31L, "erp-product:view");
        Permission tenantPermission = permission(32L, "tenant:view");
        Permission systemConfigPermission = permission(33L, "system-config:view");

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "superadmin",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_super_admin"))
            )
        );
        when(permissionMapper.selectList(any()))
            .thenReturn(List.of(erpPermission, tenantPermission, systemConfigPermission));

        assertThat(service.listAssignableToRole("admin"))
            .extracting(Permission::getCode)
            .containsExactly("erp-product:view", "system-config:view");
        assertThat(service.listAssignableToRole("super_admin"))
            .extracting(Permission::getCode)
            .containsExactly("erp-product:view", "tenant:view", "system-config:view");
    }

    @Test
    void permissionDiagnosticsSummarizeRoleMenuAndRisk() {
        PermissionServiceImpl service = new PermissionServiceImpl(
            permissionMapper,
            rolePermissionMapper,
            userRoleMapper,
            userAccountMapper,
            menuMapper,
            rolePolicyService()
        );
        Permission permission = permission(11L, "erp-sale:view");
        Permission orphan = permission(12L, "orphan:view");
        Permission actionPermission = permission(13L, "erp-product:add");
        Permission apiViewPermission = permission(14L, "erp-finance-summary:view");
        orphan.setEnabled(false);

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "sysadmin",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_super_admin"))
            )
        );
        when(permissionMapper.selectList(any())).thenReturn(List.of(permission, orphan, actionPermission, apiViewPermission));
        var roleCount11 = new RolePermissionMapper.RolePermissionCountRow();
        roleCount11.setPermissionId(11L);
        roleCount11.setRoleCount(2L);
        var roleCount13 = new RolePermissionMapper.RolePermissionCountRow();
        roleCount13.setPermissionId(13L);
        roleCount13.setRoleCount(1L);
        var roleCount14 = new RolePermissionMapper.RolePermissionCountRow();
        roleCount14.setPermissionId(14L);
        roleCount14.setRoleCount(3L);
        when(rolePermissionMapper.countActiveRolesByPermissionIds(List.of(11L, 12L, 13L, 14L))).thenReturn(
            Map.of(11L, roleCount11, 13L, roleCount13, 14L, roleCount14)
        );
        var menuCount11 = new MenuMapper.MenuPermissionCountRow();
        menuCount11.setPermissionCode("erp-sale:view");
        menuCount11.setMenuCount(1L);
        when(menuMapper.countActiveMenusByPermissionCodes(List.of(
            "erp-sale:view",
            "orphan:view",
            "erp-product:add",
            "erp-finance-summary:view"
        ))).thenReturn(
            Map.of("erp-sale:view", menuCount11)
        );

        var diagnostics = service.listDiagnostics();

        assertThat(diagnostics).hasSize(4);
        assertThat(diagnostics.get(0).permissionId()).isEqualTo(11L);
        assertThat(diagnostics.get(0).roleCount()).isEqualTo(2L);
        assertThat(diagnostics.get(0).menuCount()).isEqualTo(1L);
        assertThat(diagnostics.get(0).riskLevel()).isEqualTo("ok");
        assertThat(diagnostics.get(1).permissionId()).isEqualTo(12L);
        assertThat(diagnostics.get(1).riskLevel()).isEqualTo("warning");
        assertThat(diagnostics.get(1).warnings()).contains("权限已停用", "未分配给任何角色", "未被菜单引用");
        assertThat(diagnostics.get(2).permissionId()).isEqualTo(13L);
        assertThat(diagnostics.get(2).riskLevel()).isEqualTo("ok");
        assertThat(diagnostics.get(2).warnings()).doesNotContain("未被菜单引用");
        assertThat(diagnostics.get(3).permissionId()).isEqualTo(14L);
        assertThat(diagnostics.get(3).riskLevel()).isEqualTo("ok");
        assertThat(diagnostics.get(3).warnings()).doesNotContain("未被菜单引用");
        verify(rolePermissionMapper).countActiveRolesByPermissionIds(List.of(11L, 12L, 13L, 14L));
        verify(menuMapper).countActiveMenusByPermissionCodes(List.of(
            "erp-sale:view",
            "orphan:view",
            "erp-product:add",
            "erp-finance-summary:view"
        ));
        verify(rolePermissionMapper, never()).countActiveRolesByPermissionId(any());
        verify(menuMapper, never()).countActiveMenusByPermissionCode(any());
    }

    @Test
    void permissionUpdateBumpsUsersAffectedByAssignedRoles() {
        PermissionServiceImpl service = new PermissionServiceImpl(
            permissionMapper,
            rolePermissionMapper,
            userRoleMapper,
            userAccountMapper,
            menuMapper,
            rolePolicyService()
        );
        Permission permission = permission(11L, "erp-sale:view");
        permission.setEnabled(true);
        PermissionUpdateRequest request = new PermissionUpdateRequest("erp-sale:view", "销售查看", "销售查看", false);
        var pair = new RolePermissionMapper.RoleTenantPair();
        pair.setTenantId(1L);
        pair.setRoleId(8L);

        when(permissionMapper.selectOne(any())).thenReturn(permission);
        when(permissionMapper.findByCode("erp-sale:view")).thenReturn(permission);
        when(rolePermissionMapper.findRoleTenantPairsByPermissionId(11L)).thenReturn(List.of(pair));
        when(userRoleMapper.findUserIdsByRoleId(1L, 8L)).thenReturn(List.of(21L, 22L));

        service.update(11L, request);

        verify(userAccountMapper).incrementAuthVersionByIds(1L, List.of(21L, 22L));
    }

    @Test
    void columnPermissionRoleOptionsHideRolesOutsideActorScope() {
        RolePermissionServiceImpl service = new RolePermissionServiceImpl(
            roleMapper,
            permissionMapper,
            roleColumnSettingMapper,
            rolePermissionMapper,
            tenantColumnSettingMapper,
            userRoleMapper,
            userAccountMapper,
            roleScopeService()
        );
        TenantContext.setTenantId(1L);
        Role role = role(8L, 1L, "ops");
        Permission allowed = permission(1L, "erp-sale:view");
        Permission exceeded = permission(2L, "tenant:view");

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "tenant-admin",
                "N/A",
                List.of(
                    new SimpleGrantedAuthority("ROLE_admin"),
                    new SimpleGrantedAuthority("PERM_erp-sale:view")
                )
            )
        );
        when(roleMapper.selectOne(any())).thenReturn(role);
        when(rolePermissionMapper.findPermissionsByRoleId(1L, 8L))
            .thenReturn(List.of(allowed, exceeded))
            .thenReturn(List.of(allowed));

        assertThat(service.canManageColumnPermissions(8L)).isFalse();
        assertThat(service.canManageColumnPermissions(8L)).isTrue();
    }

    @Test
    void crossTenantFilterWritesStructuredAuditFields() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenService, userAccountService);
        DefaultClaims claims = new DefaultClaims();
        claims.setSubject("sysadmin");
        claims.put("tid", 2L);
        claims.put("utid", 1L);
        claims.put("utcode", "default");
        claims.put("av", 3L);
        RequestAuditContext auditContext = new RequestAuditContext();
        RequestAuditContext.set(auditContext);

        when(jwtTokenService.parseToken("token")).thenReturn(claims);
        when(userAccountService.loadAuthVersion("sysadmin")).thenReturn(3L);
        when(userAccountService.loadUserByUsername("sysadmin")).thenReturn(
            new User("sysadmin", "N/A", List.of(new SimpleGrantedAuthority("ROLE_super_admin")))
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            assertThat(auditContext.getAuthTenantId()).isEqualTo(1L);
            assertThat(auditContext.getAuthTenantCode()).isEqualTo("default");
            assertThat(auditContext.getCrossTenant()).isTrue();
            assertThat(TenantContext.requireTenantId()).isEqualTo(2L);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        });

        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void jwtFilterLoadsAuthoritiesFromBackendContextWhenTokenHasNoPermissionsClaim() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenService, userAccountService);
        DefaultClaims claims = new DefaultClaims();
        claims.setSubject("sysadmin");
        claims.put("uid", 9L);
        claims.put("tid", 2L);
        claims.put("tcode", "tenant-b");
        claims.put("utid", 1L);
        claims.put("utcode", "default");
        claims.put("av", 3L);
        claims.put("user", java.util.Map.of(
            "username", "sysadmin",
            "role", "super_admin",
            "avatar", "avatar.png",
            "roles", List.of("super_admin")
        ));

        when(jwtTokenService.parseToken("token")).thenReturn(claims);
        when(userAccountService.loadAuthVersion("sysadmin")).thenReturn(3L);
        when(userAccountService.loadUserByUsername("sysadmin")).thenReturn(
            new User("sysadmin", "N/A", List.of(
                new SimpleGrantedAuthority("ROLE_super_admin"),
                new SimpleGrantedAuthority("PERM_user:view"),
                new SimpleGrantedAuthority("PERM_user:edit")
            ))
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isInstanceOf(AuthenticatedUser.class);
            AuthenticatedUser principal =
                (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            assertThat(principal.getUserId()).isEqualTo(9L);
            assertThat(principal.getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .contains("ROLE_super_admin", "PERM_user:view", "PERM_user:edit");
            assertThat(principal.getAuthPayload()).isNotNull();
            assertThat(principal.getAuthPayload().permissions()).isEmpty();
        });
    }

    @Test
    void userControllerListRequiresUserViewPermission() throws Exception {
        Method method = UserController.class.getMethod("list");
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("hasAuthority('PERM_user:view')");
    }

    @Test
    void userControllerSetRolesRequiresUserEditPermission() throws Exception {
        Method method = UserController.class.getMethod("setRoles", Long.class, UserRoleUpdateRequest.class);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("hasAuthority('PERM_user:edit')");
    }

    @Test
    void assemblyDeleteRequiresDeletePermission() throws Exception {
        Method method = com.example.wms.controller.erp.ErpAssemblyOrderController.class.getMethod(
            "delete",
            Long.class,
            com.example.wms.dto.DeleteRequest.class
        );
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("hasAuthority('PERM_erp-assembly:delete')");
    }

    private TenantServiceImpl tenantService() {
        return new TenantServiceImpl(
            tenantMapper,
            permissionMapper,
            menuMapper,
            roleMapper,
            rolePermissionMapper,
            userAccountMapper,
            userRoleMapper,
            tenantMenuMapper,
            passwordEncoder,
            userAccountService,
            refreshTokenService,
            jwtTokenService
        );
    }

    private RoleScopeService roleScopeService() {
        return new RoleScopeService(rolePolicyService());
    }

    private RolePolicyService rolePolicyService() {
        return new RolePolicyService(rolePermissionMapper, userAccountMapper, userRoleMapper);
    }

    private Tenant tenant(Long id, String code) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setCode(code);
        tenant.setName(code);
        tenant.setEnabled(true);
        return tenant;
    }

    private UserAccount user(Long id, Long tenantId, String username) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setTenantId(tenantId);
        user.setUsername(username);
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        return user;
    }

    private Role role(Long id, Long tenantId, String code) {
        Role role = new Role();
        role.setId(id);
        role.setTenantId(tenantId);
        role.setCode(code);
        role.setEnabled(true);
        return role;
    }

    private Permission permission(Long id, String code) {
        Permission permission = new Permission();
        permission.setId(id);
        permission.setCode(code);
        permission.setName(code);
        permission.setEnabled(true);
        return permission;
    }

    private AuthPayload authPayload(String username, Long tenantId, String tenantCode, List<String> roles) {
        return new AuthPayload(
            new UserClaim(10L, username, roles.isEmpty() ? null : roles.get(0), null, roles),
            List.of("PERM_user:view"),
            0L,
            tenantId,
            tenantCode,
            tenantId,
            tenantCode
        );
    }
}
