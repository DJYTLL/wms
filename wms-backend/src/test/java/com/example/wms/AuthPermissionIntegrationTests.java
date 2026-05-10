package com.example.wms;

import com.example.wms.audit.RequestAuditContext;
import com.example.wms.controller.AuthController;
import com.example.wms.controller.UserController;
import com.example.wms.dto.AuthPayload;
import com.example.wms.dto.TokenPairResponse;
import com.example.wms.dto.RoleUpdateRequest;
import com.example.wms.dto.RoleCreateRequest;
import com.example.wms.dto.UserClaim;
import com.example.wms.dto.UserRoleUpdateRequest;
import com.example.wms.entity.Role;
import com.example.wms.entity.Tenant;
import com.example.wms.entity.UserAccount;
import com.example.wms.mapper.MenuMapper;
import com.example.wms.mapper.PermissionMapper;
import com.example.wms.mapper.RoleMapper;
import com.example.wms.mapper.RolePermissionMapper;
import com.example.wms.mapper.TenantMapper;
import com.example.wms.mapper.TenantMenuMapper;
import com.example.wms.mapper.UserAccountMapper;
import com.example.wms.mapper.UserRoleMapper;
import com.example.wms.security.JwtAuthenticationFilter;
import com.example.wms.security.JwtTokenService;
import com.example.wms.service.RefreshTokenService;
import com.example.wms.service.UserAccountService;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    private RolePermissionMapper rolePermissionMapper;
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
        TokenPairResponse issued = new TokenPairResponse("access-token", "refresh-token");
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

        assertThat(response.token()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isNull();
        assertThat(result.getResponse().getHeader("Set-Cookie"))
            .contains("refreshToken=refresh-token")
            .contains("HttpOnly")
            .contains("Path=/api")
            .contains("SameSite=Strict");
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
        when(refreshTokenService.issueTokens(eq(systemUser), any())).thenReturn(new TokenPairResponse("token-default", "refresh-a"));
        when(refreshTokenService.issueTokens(eq(secondUser), any())).thenReturn(new TokenPairResponse("token-tenant-b", "refresh-b"));

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
        RoleServiceImpl service = new RoleServiceImpl(roleMapper, rolePermissionMapper, userRoleMapper, userAccountMapper);
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
        UserServiceImpl service = new UserServiceImpl(userAccountMapper, roleMapper, userRoleMapper, passwordEncoder);
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

    private AuthPayload authPayload(String username, Long tenantId, String tenantCode, List<String> roles) {
        return new AuthPayload(
            new UserClaim(username, roles.isEmpty() ? null : roles.get(0), null, roles),
            List.of("PERM_user:view"),
            0L,
            tenantId,
            tenantCode,
            tenantId,
            tenantCode
        );
    }
}
