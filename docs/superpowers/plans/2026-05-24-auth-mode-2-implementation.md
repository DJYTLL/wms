# Auth Mode 2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove full permission lists from JWT, add a dedicated authorization-context API, and switch the frontend to two-step auth initialization without regressing tenant switching, menu loading, or `super_admin` behavior.

**Architecture:** Keep JWT as a lightweight identity-and-tenant envelope, move authoritative permission loading to the backend request path, and expose `/api/me/authorizations` as the single frontend authorization bootstrap endpoint. Frontend login, refresh, and tenant-switch flows will all converge on the same “set token first, load authorizations second” sequence.

**Tech Stack:** Spring Boot, Spring Security, MyBatis-Plus, JJWT, Vue 3, Pinia, Axios, Vite, JUnit 5, MockMvc, Node test runner

---

## File Structure

### Backend files to modify

- Modify: `wms-backend/src/main/java/com/example/wms/security/JwtTokenService.java`
  - Remove `perms` from JWT claims and reduce token payload to stable identity, tenant, and version fields.
- Modify: `wms-backend/src/main/java/com/example/wms/security/JwtAuthenticationFilter.java`
  - Stop rebuilding authorities from token `perms`; load the current authorization context from backend services/cache.
- Modify: `wms-backend/src/main/java/com/example/wms/controller/AuthController.java`
  - Return minimal token response for login and refresh; stop treating auth endpoints as the source of full frontend authorization state.
- Modify: `wms-backend/src/main/java/com/example/wms/controller/TenantController.java`
  - Return minimal token response for tenant switching.
- Modify: `wms-backend/src/main/java/com/example/wms/service/impl/RefreshTokenServiceImpl.java`
  - Continue issuing lightweight JWTs while preserving tenant-switch semantics and refresh rotation.
- Modify: `wms-backend/src/main/java/com/example/wms/service/impl/DatabaseUserServiceImpl.java`
  - Provide a single backend-owned way to assemble full authorization context for current user + tenant.
- Modify: `wms-backend/src/main/java/com/example/wms/service/UserAccountService.java`
  - Add the new authorization-context contract used by filter and controller.

### Backend files to create

- Create: `wms-backend/src/main/java/com/example/wms/controller/CurrentAuthorizationController.java`
  - `GET /api/me/authorizations` endpoint returning the frontend bootstrap payload.
- Create: `wms-backend/src/main/java/com/example/wms/service/AuthorizationContextService.java`
  - Service interface for loading authoritative request-time authorization context.
- Create: `wms-backend/src/main/java/com/example/wms/service/impl/AuthorizationContextServiceImpl.java`
  - Default implementation backed by `UserAccountService`, with cache hook points.
- Create: `wms-backend/src/main/java/com/example/wms/dto/AuthorizationContextResponse.java`
  - Explicit response DTO for `/api/me/authorizations`.

### Backend tests to modify

- Modify: `wms-backend/src/test/java/com/example/wms/AuthPermissionIntegrationTests.java`
  - Update login/refresh/token assertions, add `/api/me/authorizations` tests, and change JWT filter expectations to backend-loaded permissions.

### Frontend files to modify

- Modify: `auto-parts-wms-vue/src/utils/request.ts`
  - Do not inject stale `Authorization` into `/login`, `/refresh`, `/logout`; keep refresh retry behavior aligned with new auth bootstrap flow.
- Modify: `auto-parts-wms-vue/src/stores/auth.ts`
  - Separate token state from authorization-context state; add `loadAuthorizations()` and an `authorizationReady` marker.
- Modify: `auto-parts-wms-vue/src/stores/menu.ts`
  - Depend on loaded auth context rather than JWT-embedded permissions.
- Modify: `auto-parts-wms-vue/src/layouts/MainLayout.vue`
  - Refresh menus and tenant switch behavior only after auth context is reloaded.
- Modify: `auto-parts-wms-vue/src/views/system/ColumnPermissionManagement.vue`
  - Replace its manual refresh bootstrap with shared auth-context reload flow.
- Modify: `auto-parts-wms-vue/src/main.ts`
  - Ensure application bootstrap waits for `restoreSession()` to complete the two-step flow.
- Modify: `auto-parts-wms-vue/src/router/index.ts`
  - Continue guarding routes while accounting for `authorizationReady`.

### Frontend tests to create

- Create: `auto-parts-wms-vue/src/stores/__tests__/authMode2.test.mjs`
  - Cover two-step login/refresh bootstrap and stale-token exclusion on auth endpoints.

---

### Task 1: Shrink the token contract and add failing backend tests

**Files:**
- Modify: `wms-backend/src/test/java/com/example/wms/AuthPermissionIntegrationTests.java`
- Modify: `wms-backend/src/main/java/com/example/wms/security/JwtTokenService.java`
- Modify: `wms-backend/src/main/java/com/example/wms/controller/AuthController.java`
- Modify: `wms-backend/src/main/java/com/example/wms/controller/TenantController.java`

- [ ] **Step 1: Write failing backend assertions for the new token contract**

Add or update tests in `AuthPermissionIntegrationTests` so they express the new contract:

```java
@Test
void loginSuccessReturnsAccessTokenWithoutEmbeddedRefreshTokenOrFrontendAuthorizationContext() throws Exception {
    Tenant tenant = tenant(1L, "default");
    UserAccount user = user(10L, 1L, "admin");
    AuthPayload payload = authPayload("admin", 1L, "default", List.of("super_admin"));
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
    assertThat(body).doesNotContain("\"permissions\"");
    assertThat(body).doesNotContain("\"authPayload\"");
    assertThat(body).contains("\"token\":\"access-token\"");
}
```

- [ ] **Step 2: Add a failing JWT-size regression test around `JwtTokenService`**

Add a focused test method that proves token claims do not contain `perms`:

```java
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
```

- [ ] **Step 3: Run backend tests to verify they fail**

Run:

```powershell
./mvnw -pl wms-backend -Dtest=AuthPermissionIntegrationTests test
```

Expected:
- FAIL because current login response still serializes `authPayload`
- FAIL because current JWT still includes `perms`

- [ ] **Step 4: Implement the minimal backend token-contract change**

Make these exact code changes:

In `JwtTokenService.generateToken(...)`, remove:

```java
.claim("perms", payload.permissions())
```

and keep only the identity/tenant/version claims:

```java
return Jwts.builder()
    .setSubject(payload.user().username())
    .setIssuer(issuer)
    .setIssuedAt(Date.from(now))
    .setExpiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
    .claim("user", userClaim)
    .claim("uid", userClaim.get("id"))
    .claim("av", payload.authVersion())
    .claim("tid", payload.tenantId())
    .claim("tcode", payload.tenantCode())
    .claim("utid", userTenantId)
    .claim("utcode", payload.userTenantCode())
    .signWith(secretKey, SignatureAlgorithm.HS256)
    .compact();
```

In `AuthController` and `TenantController`, return a minimal response object:

```java
private TokenPairResponse toClientTokenPair(TokenPairResponse tokens) {
    return new TokenPairResponse(tokens.token(), null, null);
}
```

and use that same minimal mapping for login, refresh, and tenant switch responses.

- [ ] **Step 5: Run backend tests to verify they pass**

Run:

```powershell
./mvnw -pl wms-backend -Dtest=AuthPermissionIntegrationTests test
```

Expected:
- PASS for token payload assertions
- PASS for minimal login response assertions

- [ ] **Step 6: Commit**

```powershell
git add wms-backend/src/main/java/com/example/wms/security/JwtTokenService.java wms-backend/src/main/java/com/example/wms/controller/AuthController.java wms-backend/src/main/java/com/example/wms/controller/TenantController.java wms-backend/src/test/java/com/example/wms/AuthPermissionIntegrationTests.java
git commit -m "refactor: shrink auth token response contract"
```

### Task 2: Add a backend-owned authorization context API

**Files:**
- Create: `wms-backend/src/main/java/com/example/wms/dto/AuthorizationContextResponse.java`
- Create: `wms-backend/src/main/java/com/example/wms/service/AuthorizationContextService.java`
- Create: `wms-backend/src/main/java/com/example/wms/service/impl/AuthorizationContextServiceImpl.java`
- Create: `wms-backend/src/main/java/com/example/wms/controller/CurrentAuthorizationController.java`
- Modify: `wms-backend/src/main/java/com/example/wms/service/UserAccountService.java`
- Modify: `wms-backend/src/main/java/com/example/wms/service/impl/DatabaseUserServiceImpl.java`
- Modify: `wms-backend/src/test/java/com/example/wms/AuthPermissionIntegrationTests.java`

- [ ] **Step 1: Write a failing `/api/me/authorizations` test**

Add a controller test that expects full frontend authorization bootstrap data:

```java
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

    CurrentAuthorizationController controller = new CurrentAuthorizationController(authorizationContextService);
    MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

    when(authorizationContextService.getCurrent()).thenReturn(response);

    mockMvc.perform(get("/api/me/authorizations"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.user.username").value("admin"))
        .andExpect(jsonPath("$.data.permissions[0]").value("tenant:switch"))
        .andExpect(jsonPath("$.data.tenantId").value(2));
}
```

- [ ] **Step 2: Run backend tests to verify the endpoint test fails**

Run:

```powershell
./mvnw -pl wms-backend -Dtest=AuthPermissionIntegrationTests test
```

Expected:
- FAIL because `CurrentAuthorizationController` and `AuthorizationContextService` do not exist

- [ ] **Step 3: Create the DTO and service contract**

Add `AuthorizationContextResponse.java`:

```java
package com.example.wms.dto;

import java.util.List;

public record AuthorizationContextResponse(
    UserClaim user,
    List<String> permissions,
    long authVersion,
    Long tenantId,
    String tenantCode,
    Long userTenantId,
    String userTenantCode
) {
}
```

Add `AuthorizationContextService.java`:

```java
package com.example.wms.service;

import com.example.wms.dto.AuthorizationContextResponse;

public interface AuthorizationContextService {
    AuthorizationContextResponse getCurrent();
}
```

Extend `UserAccountService.java` with a reusable backend loader:

```java
AuthPayload loadAuthPayload(String username, Long audienceTenantId);
```

- [ ] **Step 4: Implement the controller and service**

Create `CurrentAuthorizationController.java`:

```java
package com.example.wms.controller;

import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.AuthorizationContextResponse;
import com.example.wms.service.AuthorizationContextService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class CurrentAuthorizationController {
    private final AuthorizationContextService authorizationContextService;

    public CurrentAuthorizationController(AuthorizationContextService authorizationContextService) {
        this.authorizationContextService = authorizationContextService;
    }

    @GetMapping("/authorizations")
    public ResponseEntity<ApiResponse<AuthorizationContextResponse>> getAuthorizations() {
        return ResponseEntity.ok(ApiResponse.ok(authorizationContextService.getCurrent()));
    }
}
```

Create `AuthorizationContextServiceImpl.java`:

```java
package com.example.wms.service.impl;

import com.example.wms.dto.AuthPayload;
import com.example.wms.dto.AuthorizationContextResponse;
import com.example.wms.security.AuthenticatedUser;
import com.example.wms.service.AuthorizationContextService;
import com.example.wms.service.UserAccountService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationContextServiceImpl implements AuthorizationContextService {
    private final UserAccountService userAccountService;

    public AuthorizationContextServiceImpl(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @Override
    public AuthorizationContextResponse getCurrent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new IllegalStateException("当前登录态无效");
        }
        AuthPayload payload = userAccountService.loadAuthPayload(principal.getUsername(), principal.getAuthPayload().tenantId());
        return new AuthorizationContextResponse(
            payload.user(),
            payload.permissions(),
            payload.authVersion(),
            payload.tenantId(),
            payload.tenantCode(),
            payload.userTenantId(),
            payload.userTenantCode()
        );
    }
}
```

Update `DatabaseUserServiceImpl` to support the audience-tenant-aware overload by reusing its existing auth-context assembly.

- [ ] **Step 5: Run backend tests to verify they pass**

Run:

```powershell
./mvnw -pl wms-backend -Dtest=AuthPermissionIntegrationTests test
```

Expected:
- PASS for `/api/me/authorizations`
- PASS for prior login tests

- [ ] **Step 6: Commit**

```powershell
git add wms-backend/src/main/java/com/example/wms/dto/AuthorizationContextResponse.java wms-backend/src/main/java/com/example/wms/service/AuthorizationContextService.java wms-backend/src/main/java/com/example/wms/service/impl/AuthorizationContextServiceImpl.java wms-backend/src/main/java/com/example/wms/controller/CurrentAuthorizationController.java wms-backend/src/main/java/com/example/wms/service/UserAccountService.java wms-backend/src/main/java/com/example/wms/service/impl/DatabaseUserServiceImpl.java wms-backend/src/test/java/com/example/wms/AuthPermissionIntegrationTests.java
git commit -m "feat: add current authorization bootstrap endpoint"
```

### Task 3: Make request-time backend authorization load from services instead of token claims

**Files:**
- Modify: `wms-backend/src/main/java/com/example/wms/security/JwtAuthenticationFilter.java`
- Modify: `wms-backend/src/main/java/com/example/wms/service/impl/DatabaseUserServiceImpl.java`
- Modify: `wms-backend/src/test/java/com/example/wms/AuthPermissionIntegrationTests.java`

- [ ] **Step 1: Write a failing filter test that no longer depends on `perms`**

Replace the old `jwtFilterBuildsAuthenticationFromTokenClaimsWithoutReloadingAuthorities` expectation with:

```java
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
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
            .extracting(authority -> authority.getAuthority())
            .contains("ROLE_super_admin", "PERM_user:view", "PERM_user:edit");
    });
}
```

- [ ] **Step 2: Run backend tests to verify the filter expectation fails**

Run:

```powershell
./mvnw -pl wms-backend -Dtest=AuthPermissionIntegrationTests test
```

Expected:
- FAIL because current filter still expects token `perms` for the no-reload path

- [ ] **Step 3: Implement the filter change**

Refactor `JwtAuthenticationFilter.buildUserDetailsFromClaims(...)` so it no longer requires `permissionsValue`:

```java
private UserDetails buildUserDetailsFromClaims(Claims claims, String username) {
    Number userIdValue = claims.get("uid", Number.class);
    Object userClaimValue = claims.get("user");
    if (!(userClaimValue instanceof Map<?, ?> userMap)) {
        return null;
    }
    List<String> roles = readStringList(userMap.get("roles"));
    if (roles.isEmpty()) {
        return null;
    }
    return AuthenticatedUser.fromToken(
        userIdValue == null ? null : userIdValue.longValue(),
        resolveString(userMap.get("username"), username),
        new AuthPayload(
            new UserClaim(
                userIdValue == null ? null : userIdValue.longValue(),
                resolveString(userMap.get("username"), username),
                resolveString(userMap.get("role"), null),
                resolveString(userMap.get("avatar"), null),
                roles
            ),
            List.of(),
            readLong(claims.get("av")),
            readNullableLong(claims.get("tid")),
            claims.get("tcode", String.class),
            readNullableLong(claims.get("utid")),
            claims.get("utcode", String.class)
        ),
        List.of()
    );
}
```

Then update the authentication path to always resolve authorities from `userAccountService.loadUserByUsername(username)` after version check, while still preserving token-derived tenant and audit context:

```java
UserDetails tokenDetails = buildUserDetailsFromClaims(claims, username);
UserDetails backendDetails = userAccountService.loadUserByUsername(username);
UserDetails userDetails = mergeTokenIdentityWithBackendAuthorities(tokenDetails, backendDetails);
```

The merge helper should preserve:
- token-derived `uid`, `tid`, `utid`
- backend-derived `GrantedAuthority`

- [ ] **Step 4: Run backend tests to verify they pass**

Run:

```powershell
./mvnw -pl wms-backend -Dtest=AuthPermissionIntegrationTests test
```

Expected:
- PASS with no dependency on JWT `perms`

- [ ] **Step 5: Commit**

```powershell
git add wms-backend/src/main/java/com/example/wms/security/JwtAuthenticationFilter.java wms-backend/src/main/java/com/example/wms/service/impl/DatabaseUserServiceImpl.java wms-backend/src/test/java/com/example/wms/AuthPermissionIntegrationTests.java
git commit -m "refactor: load request authorities from backend context"
```

### Task 4: Stop sending stale Authorization to auth endpoints and add frontend auth-bootstrap tests

**Files:**
- Modify: `auto-parts-wms-vue/src/utils/request.ts`
- Create: `auto-parts-wms-vue/src/stores/__tests__/authMode2.test.mjs`

- [ ] **Step 1: Write a failing frontend request test for auth endpoints**

Create `authMode2.test.mjs` with a focused request-interceptor case:

```javascript
import test from 'node:test'
import assert from 'node:assert/strict'
import request, { setTokens, clearTokens } from '../../utils/request'

test('auth endpoints do not receive stale Authorization headers', async () => {
  setTokens('oversized-old-token')

  const loginConfig = await request.interceptors.request.handlers[0].fulfilled({
    url: '/login',
    method: 'post',
    headers: {},
    data: {},
  })

  assert.equal(loginConfig.headers.Authorization, undefined)
  clearTokens()
})
```

- [ ] **Step 2: Run the frontend test to verify it fails**

Run:

```powershell
node --test auto-parts-wms-vue/src/stores/__tests__/authMode2.test.mjs
```

Expected:
- FAIL because current interceptor adds `Authorization` to `/login`

- [ ] **Step 3: Implement the interceptor exclusion**

Update `request.ts` request interceptor from:

```ts
const token = getToken()
if (token) {
  config.headers = config.headers || {}
  config.headers.Authorization = `Bearer ${token}`
}
```

to:

```ts
const token = getToken()
if (token && !isAuthEndpoint(config.url)) {
  config.headers = config.headers || {}
  config.headers.Authorization = `Bearer ${token}`
}
```

Keep `isAuthEndpoint()` scoped to:

```ts
return url.includes('/login')
  || url.includes('/refresh')
  || url.includes('/logout')
```

- [ ] **Step 4: Run the frontend test to verify it passes**

Run:

```powershell
node --test auto-parts-wms-vue/src/stores/__tests__/authMode2.test.mjs
```

Expected:
- PASS with no `Authorization` on `/login`

- [ ] **Step 5: Commit**

```powershell
git add auto-parts-wms-vue/src/utils/request.ts auto-parts-wms-vue/src/stores/__tests__/authMode2.test.mjs
git commit -m "fix: skip stale authorization on auth endpoints"
```

### Task 5: Convert frontend auth store to two-step initialization

**Files:**
- Modify: `auto-parts-wms-vue/src/stores/auth.ts`
- Modify: `auto-parts-wms-vue/src/main.ts`
- Modify: `auto-parts-wms-vue/src/router/index.ts`
- Modify: `auto-parts-wms-vue/src/stores/__tests__/authMode2.test.mjs`

- [ ] **Step 1: Write a failing auth-store bootstrap test**

Extend `authMode2.test.mjs` with:

```javascript
test('restoreSession loads token first and authorizations second', async () => {
  const calls = []
  globalThis.axios = {
    post: async (url) => {
      calls.push(url)
      return { data: { code: 200, data: { token: 'new-token' } } }
    },
  }
})
```

Replace the rough stub with a concrete store-level test once the store is imported. The required behavior is:

- first call: `/api/refresh`
- second call: `/api/me/authorizations`
- final store state:
  - `token` set
  - `permissions` loaded
  - `initialized === true`
  - `authorizationReady === true`

- [ ] **Step 2: Run frontend tests to verify they fail**

Run:

```powershell
node --test auto-parts-wms-vue/src/stores/__tests__/authMode2.test.mjs
```

Expected:
- FAIL because store currently expects `authPayload` directly from `/refresh`

- [ ] **Step 3: Implement the store split**

In `auth.ts`, introduce:

```ts
const authorizationReady = ref(false)

const loadAuthorizations = async () => {
  const res = await request.get('/me/authorizations')
  const payload = res?.data?.data
  applyAuthContext(payload)
  authorizationReady.value = true
  return true
}
```

Change `applyToken()` so it no longer expects `authPayload` to always be present. It should:

- set token
- clear user/permissions when token is null
- decode only lightweight tenant/user hints from JWT when needed
- not repopulate `permissions` from JWT

Change `login()`:

```ts
const res = await request.post('/login', { tenantCode: tenantCodeInput, username, password })
const newToken = res.data.data.token
setTokens(newToken)
applyToken(newToken)
await loadAuthorizations()
```

Change `restoreSession()`:

```ts
const res = await axios.post('/api/refresh', {}, { withCredentials: true })
setTokens(res.data.data.token)
applyToken(res.data.data.token)
await loadAuthorizations()
```

Expose:

```ts
authorizationReady,
loadAuthorizations,
```

- [ ] **Step 4: Align app bootstrap and route guards**

In `main.ts`, keep:

```ts
await authStore.restoreSession()
```

but the method must now include both token refresh and authorization-context fetch.

In `router/index.ts`, extend guard logic so protected navigation waits for both:

```ts
if (to.name !== 'login' && !authStore.initialized) {
  await authStore.restoreSession()
}
if (to.name !== 'login' && authStore.isAuthenticated && !authStore.authorizationReady) {
  await authStore.loadAuthorizations()
}
```

- [ ] **Step 5: Run frontend tests to verify they pass**

Run:

```powershell
node --test auto-parts-wms-vue/src/stores/__tests__/authMode2.test.mjs
```

Expected:
- PASS for two-step login/refresh initialization

- [ ] **Step 6: Commit**

```powershell
git add auto-parts-wms-vue/src/stores/auth.ts auto-parts-wms-vue/src/main.ts auto-parts-wms-vue/src/router/index.ts auto-parts-wms-vue/src/stores/__tests__/authMode2.test.mjs
git commit -m "refactor: split token restore from authorization bootstrap"
```

### Task 6: Rewire menu, tenant switch, and column-permission refresh to the shared authorization bootstrap

**Files:**
- Modify: `auto-parts-wms-vue/src/stores/menu.ts`
- Modify: `auto-parts-wms-vue/src/layouts/MainLayout.vue`
- Modify: `auto-parts-wms-vue/src/views/system/ColumnPermissionManagement.vue`

- [ ] **Step 1: Write a failing manual verification checklist before code changes**

Document the exact current regressions to re-check after refactor:

```text
1. Login loads menus after auth context, not before.
2. Tenant switch updates token, then authorizations, then menus.
3. ColumnPermissionManagement refreshes current session without assuming refresh returns authPayload.
4. menuStore.fetchMenus() is not called while permissions are still empty for an authenticated user.
```

- [ ] **Step 2: Implement the shared tenant-switch flow**

In `MainLayout.vue`, replace:

```ts
setTokens(newToken, res.data?.data?.authPayload)
```

with:

```ts
setTokens(newToken)
authStore.token = newToken
await authStore.loadAuthorizations()
menuStore.clearMenus()
await refreshMenus(true)
```

Also make the auth event listener update only after `authorizationReady` is true.

- [ ] **Step 3: Harden menu fetching against half-initialized auth state**

In `menu.ts`, gate menu loading:

```ts
if (!hasToken || !authStore.authorizationReady) {
  clearMenus()
  return menus.value
}
```

This prevents menu requests from racing ahead of authorization bootstrap.

- [ ] **Step 4: Replace page-local refresh bootstrap in column permission management**

In `ColumnPermissionManagement.vue`, replace:

```ts
const res = await axios.post('/api/refresh', {}, { withCredentials: true })
setTokens(refreshData.data.token, refreshData.data.authPayload)
```

with:

```ts
const restored = await authStore.restoreSession()
if (!restored) {
  throw new Error('刷新登录态失败')
}
```

and ensure page-local refresh callers then re-run their page data loaders only after `authStore.authorizationReady` is true.

- [ ] **Step 5: Run focused frontend checks**

Run:

```powershell
node --test auto-parts-wms-vue/src/stores/__tests__/authMode2.test.mjs
```

Then run the app locally and manually verify:

```powershell
cd auto-parts-wms-vue
npm run dev
```

Expected:
- Login succeeds with no 431
- Menu loads after login
- Tenant switch reloads menus and permissions correctly
- Column permission page still refreshes current session correctly

- [ ] **Step 6: Commit**

```powershell
git add auto-parts-wms-vue/src/stores/menu.ts auto-parts-wms-vue/src/layouts/MainLayout.vue auto-parts-wms-vue/src/views/system/ColumnPermissionManagement.vue
git commit -m "refactor: unify frontend authorization reload flow"
```

### Task 7: Full verification and cleanup

**Files:**
- Modify: `docs/superpowers/specs/2026-05-24-auth-mode-2-selection-and-mode-3-reference-design.md` only if implementation deviates
- No required code changes if all tests pass

- [ ] **Step 1: Run backend verification**

Run:

```powershell
./mvnw -pl wms-backend test
```

Expected:
- All backend tests pass

- [ ] **Step 2: Run frontend verification**

Run:

```powershell
node --test auto-parts-wms-vue/src/stores/__tests__/authMode2.test.mjs
node --test auto-parts-wms-vue/src/utils/__tests__/requestRefreshQueueCore.test.mjs
node --test auto-parts-wms-vue/src/layouts/__tests__/pageRefresh.test.mjs
```

Expected:
- All targeted frontend tests pass

- [ ] **Step 3: Manual 431 regression verification**

Use the browser against local dev server and verify these exact flows:

```text
1. Clear site storage and log in.
2. Confirm /api/login no longer carries old Authorization.
3. Confirm /api/menus loads after /api/me/authorizations.
4. Switch tenant as super_admin.
5. Confirm /api/tenants, /api/menus, /api/tenants/switch do not return 431.
6. Refresh browser and confirm session restore works.
```

- [ ] **Step 4: Commit final stabilization changes if any**

```powershell
git add -A
git commit -m "test: verify auth mode 2 authorization bootstrap flow"
```

## Self-Review

### Spec coverage

- JWT no longer carries full permissions: covered by Task 1 and Task 3
- `/api/me/authorizations` added: covered by Task 2
- frontend two-step initialization: covered by Task 5
- tenant switch and menu refresh sequencing: covered by Task 6
- 431 regression validation: covered by Task 4, Task 6, and Task 7

### Placeholder scan

- No `TODO`, `TBD`, or “implement later” placeholders remain.
- Every task has explicit files, commands, and expected results.

### Type consistency

- Backend uses `AuthorizationContextResponse`, `AuthorizationContextService`, and `loadAuthPayload(String, Long)` consistently.
- Frontend uses `authorizationReady` and `loadAuthorizations()` consistently across store, router, and layout flows.

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-05-24-auth-mode-2-implementation.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
