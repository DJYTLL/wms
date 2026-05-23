package com.example.wms.security;

import com.example.wms.audit.RequestAuditContext;
import com.example.wms.dto.AuthPayload;
import com.example.wms.dto.UserClaim;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import com.example.wms.service.UserAccountService;
import com.example.wms.tenant.TenantContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

// 每次请求解析 JWT 并写入安全上下文
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenService jwtTokenService;
    private final UserAccountService userAccountService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, UserAccountService userAccountService) {
        this.jwtTokenService = jwtTokenService;
        this.userAccountService = userAccountService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        boolean tenantBound = false;
        boolean authTenantBound = false;
        Long targetTenantId = null;
        try {
            // 从 Authorization 头提取 Bearer Token
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                try {
                    // 解析 Token 并构建认证信息
                    Claims claims = jwtTokenService.parseToken(token);
                    String username = claims.getSubject();
                    Number tenantValue = claims.get("tid", Number.class);
                    if (tenantValue != null) {
                        targetTenantId = tenantValue.longValue();
                        TenantContext.setTenantId(targetTenantId);
                        tenantBound = true;
                    }
                    Number userTenantValue = claims.get("utid", Number.class);
                    Long authTenantId = userTenantValue == null
                        ? targetTenantId
                        : Long.valueOf(userTenantValue.longValue());
                    RequestAuditContext auditContext = RequestAuditContext.get();
                    if (auditContext != null) {
                        auditContext.setAuthTenantId(authTenantId);
                        auditContext.setAuthTenantCode(claims.get("utcode", String.class));
                        auditContext.setCrossTenant(authTenantId != null
                            && targetTenantId != null
                            && !authTenantId.equals(targetTenantId));
                    }
                    if (StringUtils.hasText(username) && SecurityContextHolder.getContext().getAuthentication() == null) {
                        if (authTenantId != null && (targetTenantId == null || !authTenantId.equals(targetTenantId))) {
                            TenantContext.setTenantId(authTenantId);
                            authTenantBound = true;
                        }
                        // 校验权限版本，变更后强制重新认证
                        Number tokenVersionValue = claims.get("av", Number.class);
                        long tokenVersion = tokenVersionValue == null ? 0L : tokenVersionValue.longValue();
                        long currentVersion = userAccountService.loadAuthVersion(username);
                        if (tokenVersion != currentVersion) {
                            if (authTenantBound) {
                                if (targetTenantId != null) {
                                    TenantContext.setTenantId(targetTenantId);
                                } else {
                                    TenantContext.clear();
                                }
                            }
                            filterChain.doFilter(request, response);
                            return;
                        }

                        UserDetails userDetails = buildUserDetailsFromClaims(claims, username);
                        if (userDetails == null) {
                            userDetails = userAccountService.loadUserByUsername(username);
                        }
                        if (authTenantId != null
                            && targetTenantId != null
                            && !authTenantId.equals(targetTenantId)
                            && userDetails.getAuthorities().stream().noneMatch(authority ->
                                authority != null && "ROLE_super_admin".equalsIgnoreCase(authority.getAuthority()))) {
                            if (authTenantBound) {
                                if (targetTenantId != null) {
                                    TenantContext.setTenantId(targetTenantId);
                                } else {
                                    TenantContext.clear();
                                }
                            }
                            filterChain.doFilter(request, response);
                            return;
                        }
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                        );
                        // 绑定请求细节
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        if (authTenantBound) {
                            if (targetTenantId != null) {
                                TenantContext.setTenantId(targetTenantId);
                            } else {
                                TenantContext.clear();
                            }
                            authTenantBound = false;
                        }
                    }
                } catch (Exception ex) {
                    // Token 无效，继续链路以便受保护接口返回 401
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            if (tenantBound || authTenantBound) {
                TenantContext.clear();
            }
        }
    }

    private UserDetails buildUserDetailsFromClaims(Claims claims, String username) {
        Number userIdValue = claims.get("uid", Number.class);
        Object userClaimValue = claims.get("user");
        Object permissionsValue = claims.get("perms");
        if (!(userClaimValue instanceof Map<?, ?> userMap) || !(permissionsValue instanceof Collection<?> permissions)) {
            return null;
        }
        List<String> roles = readStringList(userMap.get("roles"));
        if (roles.isEmpty()) {
            return null;
        }
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        for (String permission : readStringList(permissions)) {
            authorities.add(new SimpleGrantedAuthority("PERM_" + permission));
        }
        UserClaim userClaim = new UserClaim(
            userIdValue == null ? null : userIdValue.longValue(),
            resolveString(userMap.get("username"), username),
            resolveString(userMap.get("role"), null),
            resolveString(userMap.get("avatar"), null),
            roles
        );
        AuthPayload authPayload = new AuthPayload(
            userClaim,
            readStringList(permissions),
            readLong(claims.get("av")),
            readNullableLong(claims.get("tid")),
            claims.get("tcode", String.class),
            readNullableLong(claims.get("utid")),
            claims.get("utcode", String.class)
        );
        return AuthenticatedUser.fromToken(userClaim.id(), userClaim.username(), authPayload, authorities);
    }

    private long readLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private Long readNullableLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private String resolveString(Object value, String fallback) {
        return value instanceof String text && StringUtils.hasText(text) ? text : fallback;
    }

    private List<String> readStringList(Object value) {
        if (!(value instanceof Collection<?> items)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : items) {
            if (item instanceof String text && StringUtils.hasText(text)) {
                result.add(text);
            }
        }
        return result;
    }
}
